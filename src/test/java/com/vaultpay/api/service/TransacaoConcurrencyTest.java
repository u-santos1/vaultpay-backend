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
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:concurrency_db;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
public class TransacaoConcurrencyTest {

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
                .saldo(new BigDecimal("500.00")) 
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
    @DisplayName("Deve serializar 10 saques concorrentes via Lock Pessimista: exatamente 5 passam e saldo final é zero")
    void devePrevenirCondicaoDeCorridaEmSaquesSimultaneos() throws InterruptedException {
        int totalThreads = 10;
        BigDecimal valorSaque = new BigDecimal("100.00");

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(totalThreads);

        AtomicInteger sucessos = new AtomicInteger(0);
        AtomicInteger falhasSaldo = new AtomicInteger(0);

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
                    
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        
        startSignal.countDown();

        
        boolean finalizou = doneSignal.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        
        assertEquals(true, finalizou, "O processamento concorrente não deve travar em timeout");
        assertEquals(5, sucessos.get(), "Exatamente 5 saques de R$ 100 devem ter sucesso");
        assertEquals(5, falhasSaldo.get(), "Exatamente 5 saques devem falhar por Saldo Insuficiente");

        
        Conta contaAtualizada = contaRepository.findById(conta.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("0.00").compareTo(contaAtualizada.getSaldo()),
                "O saldo final deve ser exatamente R$ 0,00 sem perda de atualização");
    }
}
