package com.vaultpay.api.service;

import com.vaultpay.api.PerfilDeAcesso;
import com.vaultpay.api.dtos.SaqueRequestDTO;
import com.vaultpay.api.infra.exception.SaldoInsuficienteException;
import com.vaultpay.api.model.Conta;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.repository.ContaRepository;
import com.vaultpay.api.repository.TransacaoRepository;
import com.vaultpay.api.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public class TransacaoConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private TransacaoService transacaoService;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    private Usuario usuario;
    private Conta conta;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setEmail("concorrencia@vaultpay.com");
        usuario.setSenha("123456");
        usuario.setAtivo(true);
        usuario.setPerfilDeAcesso(PerfilDeAcesso.USER);
        usuario.setDataUltimaAlteracaoSenha(Instant.now());
        usuario = usuarioRepository.save(usuario);

        conta = Conta.builder()
                .numero("99999-9")
                .saldo(new BigDecimal("1000.00"))
                .limiteTransacao(new BigDecimal("10000.00"))
                .ativo(true)
                .usuario(usuario)
                .build();
        conta = contaRepository.save(conta);
    }

    @AfterEach
    void tearDown() {
        transacaoRepository.deleteAll();
        contaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve serializar 20 saques concorrentes via Virtual Threads e Lock Pessimista no PostgreSQL: exatamente 10 passam e saldo final é zero")
    void devePrevenirCondicaoDeCorridaEmSaquesSimultaneosComVirtualThreadsEPostgres() throws InterruptedException {
        int totalThreads = 20;
        BigDecimal valorSaque = new BigDecimal("100.00");

        // Utiliza o executor dedicado de Virtual Threads do Project Loom / Java 21
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(totalThreads);

        AtomicInteger sucessos = new AtomicInteger(0);
        AtomicInteger falhasSaldo = new AtomicInteger(0);
        AtomicInteger outrosErros = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startSignal.await();
                    SaqueRequestDTO request = new SaqueRequestDTO(conta.getId(), valorSaque);
                    String chave = UUID.randomUUID().toString();

                    transacaoService.realizarSaque(request, chave, usuario);
                    sucessos.incrementAndGet();
                } catch (SaldoInsuficienteException e) {
                    falhasSaldo.incrementAndGet();
                } catch (Exception e) {
                    outrosErros.incrementAndGet();
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        // Dispara simultaneamente as 20 Virtual Threads
        startSignal.countDown();

        boolean finalizou = doneSignal.await(30, TimeUnit.SECONDS);
        executor.close();

        assertTrue(finalizou, "O processamento concorrente das Virtual Threads não deve travar em timeout");
        assertEquals(10, sucessos.get(), "Exatamente 10 saques de R$ 100 devem ter sucesso a partir do saldo de R$ 1000");
        assertEquals(10, falhasSaldo.get(), "Exatamente 10 saques devem falhar por Saldo Insuficiente");
        assertEquals(0, outrosErros.get(), "Nenhum erro de concorrência ou deadlock inesperado deve ocorrer");

        Conta contaAtualizada = contaRepository.findById(conta.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("0.00").compareTo(contaAtualizada.getSaldo()),
                "O saldo final no PostgreSQL deve ser exatamente R$ 0,00 sem perda de atualização (Lost Update)");
    }
}
