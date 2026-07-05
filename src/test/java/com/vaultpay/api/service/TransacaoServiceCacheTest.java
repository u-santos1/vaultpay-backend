package com.vaultpay.api.service;

import com.vaultpay.api.model.Conta;
import com.vaultpay.api.model.Transacao;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.repository.ContaRepository;
import com.vaultpay.api.repository.TransacaoRepository;
import com.vaultpay.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})

public class TransacaoServiceCacheTest {
    @Autowired
    private  TransacaoService service;

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
        conta.setId(contaId);

        PageRequest page = PageRequest.of(0, 10);
        Page<Transacao> pagenaVazia = new PageImpl<>(List.of());

        when(contaRepository.findByUsuarioId(contaId))
                .thenReturn(Optional.of(conta));
        when(transacaoRepository.findByContaOrigemIdOrContaDestinoId(
                eq(contaId), eq(contaId), any()
        )).thenReturn(pagenaVazia);

        service.extratos(contaId,page, usuarioLogado);
        service.extratos(contaId, page, usuarioLogado);
        verify(transacaoRepository, times(1)).findByContaOrigemIdOrContaDestinoId(any(), any(), any());
    }

}
       0-9 8765y43