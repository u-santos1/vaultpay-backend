package com.vaultpay.api.service;

import com.vaultpay.api.dtos.DepositoRequestDTO;
import com.vaultpay.api.model.Conta;
import com.vaultpay.api.model.Transacao;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.repository.ContaRepository;
import com.vaultpay.api.repository.TransacaoRepository;
import com.vaultpay.api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})

public class TransacaoServiceCacheTest {
    @Autowired
    private TransacaoService service;

    @MockitoBean
    private TransacaoRepository transacaoRepository;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private ContaRepository contaRepository;

    @Test
    void extratos_DeveUsarCacheNaSegundaChamada(){
        Long contaId = 1L;
        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(10L);
        Conta conta = new Conta();
        conta.setId(contaId);
        conta.setUsuario(usuarioLogado);

        PageRequest page = PageRequest.of(0, 10);
        Page<Transacao> paginaVazia = new PageImpl<>(List.of());

        when(contaRepository.findById(contaId))
                .thenReturn(Optional.of(conta));
        when(transacaoRepository.findByContaOrigemIdOrContaDestinoId(
                eq(contaId), eq(contaId), any()
        )).thenReturn(paginaVazia);

        service.extratos(contaId, page, usuarioLogado);
        service.extratos(contaId, page, usuarioLogado);
        verify(transacaoRepository, times(1)).findByContaOrigemIdOrContaDestinoId(any(), any(), any());
    }

    @Test
    void extratos_DeveGerarChavesDiferentesParaPageSizeOuSortDiferente(){
        Long contaId = 2L;
        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(20L);
        Conta conta = new Conta();
        conta.setId(contaId);
        conta.setUsuario(usuarioLogado);

        PageRequest page1 = PageRequest.of(0, 10);
        PageRequest page2 = PageRequest.of(0, 20);
        Page<Transacao> paginaVazia = new PageImpl<>(List.of());

        when(contaRepository.findById(contaId))
                .thenReturn(Optional.of(conta));
        when(transacaoRepository.findByContaOrigemIdOrContaDestinoId(
                eq(contaId), eq(contaId), any()
        )).thenReturn(paginaVazia);

        // Primeira chamada com size=10
        service.extratos(contaId, page1, usuarioLogado);
        // Segunda chamada com size=20 (não deve reaproveitar o cache do size=10)
        service.extratos(contaId, page2, usuarioLogado);

        verify(transacaoRepository, times(2)).findByContaOrigemIdOrContaDestinoId(eq(contaId), eq(contaId), any());
    }

    @Test
    void invalidarCacheContas_DeveRemoverApenasChavesDaContaEspecificada() {
        Long conta1 = 100L;
        Long conta2 = 200L;
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Conta c1 = new Conta();
        c1.setId(conta1);
        c1.setUsuario(usuario);

        Conta c2 = new Conta();
        c2.setId(conta2);
        c2.setUsuario(usuario);

        PageRequest page = PageRequest.of(0, 10);
        Page<Transacao> paginaVazia = new PageImpl<>(List.of());

        when(contaRepository.findById(conta1)).thenReturn(Optional.of(c1));
        when(contaRepository.findById(conta2)).thenReturn(Optional.of(c2));
        when(transacaoRepository.findByContaOrigemIdOrContaDestinoId(any(), any(), any()))
                .thenReturn(paginaVazia);

        // Popula cache para conta 1 e conta 2
        service.extratos(conta1, page, usuario);
        service.extratos(conta2, page, usuario);

        // Efetua um depósito na conta 1 (que deve invalidar cirurgicamente apenas a conta 1)
        Conta c1Pessimistic = Conta.builder().id(conta1).saldo(BigDecimal.ZERO).ativo(true).build();
        when(contaRepository.findByIdWithPessimisticLock(conta1)).thenReturn(Optional.of(c1Pessimistic));
        when(transacaoRepository.existsByChaveIdempotencia("deposito-key-1")).thenReturn(false);
        when(transacaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.realizarDeposito(new DepositoRequestDTO(conta1, new BigDecimal("50.00")), "deposito-key-1");

        // Chama extratos novamente para conta 1 e conta 2
        service.extratos(conta1, page, usuario);
        service.extratos(conta2, page, usuario);

        // Conta 1 deve ter sido consultada 2 vezes no repositório (1ª inicial + 1ª após invalidação)
        verify(transacaoRepository, times(2)).findByContaOrigemIdOrContaDestinoId(eq(conta1), eq(conta1), any());
        // Conta 2 deve ter sido consultada apenas 1 vez (permaneceu intacta em cache!)
        verify(transacaoRepository, times(1)).findByContaOrigemIdOrContaDestinoId(eq(conta2), eq(conta2), any());
    }
}