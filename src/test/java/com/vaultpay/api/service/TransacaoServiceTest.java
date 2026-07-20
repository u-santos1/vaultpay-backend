package com.vaultpay.api.service;

import com.vaultpay.api.dtos.DepositoRequestDTO;
import com.vaultpay.api.dtos.SaqueRequestDTO;
import com.vaultpay.api.dtos.TransacaoRequestDTO;
import com.vaultpay.api.dtos.TransacaoResponseDTO;
import com.vaultpay.api.event.TransferenciaRealizadaEvent;
import com.vaultpay.api.infra.exception.*;
import com.vaultpay.api.model.Conta;
import com.vaultpay.api.model.Transacao;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.repository.ContaRepository;
import com.vaultpay.api.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransacaoServiceTest {

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<TransferenciaRealizadaEvent> argumentCaptor;

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private TransacaoService transacaoService;

    private Conta contaOrigem;
    private Conta contaDestino;

    @BeforeEach
    void setUp() {
        Usuario donoDaConta = new Usuario();
        donoDaConta.setId(1L);
        contaOrigem = Conta.builder()
                .id(1L)
                .numero("11111-1")
                .saldo(new BigDecimal("500.00"))
                .limiteTransacao(new BigDecimal("10000.00"))
                .ativo(true)
                .usuario(donoDaConta)
                .build();

        contaDestino = Conta.builder()
                .id(2L)
                .numero("22222-2")
                .saldo(new BigDecimal("100.00"))
                .limiteTransacao(new BigDecimal("10000.00"))
                .ativo(true)
                .usuario(new Usuario())
                .build();
    }

    @Test
    void realizarTransferencia_DeveOcorrerComSucesso() {
        TransacaoRequestDTO request = new TransacaoRequestDTO(1L, 2L, new BigDecimal("100.00"));
        String chave = String.valueOf(UUID.randomUUID());
        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(1L);


        when(contaRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findByIdWithPessimisticLock(2L)).thenReturn(Optional.of(contaDestino));

        Transacao transacao = Transacao.builder()
                .id(java.util.UUID.randomUUID())
                .contaOrigem(contaOrigem)
                .contaDestino(contaDestino)
                .valor(request.valor())
                .dataHora(LocalDateTime.now())
                .chaveIdempotencia(chave)
                .build();

        when(transacaoRepository.save(any(Transacao.class))).thenReturn(transacao);

        TransacaoResponseDTO response = transacaoService.realizarTransferencia(request,chave, usuarioLogado);

        assertNotNull(response);
        assertEquals(new BigDecimal("400.00"), contaOrigem.getSaldo());
        assertEquals(new BigDecimal("200.00"), contaDestino.getSaldo());

        verify(eventPublisher, times(1)).publishEvent(argumentCaptor.capture());
        TransferenciaRealizadaEvent eventoCapturado = argumentCaptor.getValue();

        assertEquals(new BigDecimal("100.00"), eventoCapturado.transacao().getValor());
        assertEquals(1L, eventoCapturado.transacao().getContaOrigem().getId());

        verify(contaRepository, times(1)).save(contaOrigem);
        verify(contaRepository, times(1)).save(contaDestino);
        verify(transacaoRepository, times(1)).save(any(Transacao.class));
    }

    @Test
    void realizarTransferencia_DeveFalharContasIguais() {
        TransacaoRequestDTO request = new TransacaoRequestDTO(1L, 1L, new BigDecimal("100.00"));
        String chave = UUID.randomUUID().toString();
        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(1L);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transacaoService.realizarTransferencia(request, chave, usuarioLogado);
        });

        assertEquals("Conta de origem e destino não podem ser a mesma.", exception.getMessage());
        verify(contaRepository, never()).findByIdWithPessimisticLock(anyLong());
    }

    @Test
    void realizarTransferencia_DeveFalharValorInvalido() {
        TransacaoRequestDTO request = new TransacaoRequestDTO(1L, 2L, BigDecimal.ZERO);
        String chave = String.valueOf(UUID.randomUUID());

        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(1L);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transacaoService.realizarTransferencia(request,chave,usuarioLogado);
        });

        assertEquals("Valor da transferência deve ser maior que zero.", exception.getMessage());
        verify(contaRepository, never()).findByIdWithPessimisticLock(anyLong());
    }

    @Test
    void realizarTransferencia_DeveFalharContaNaoEncontrada() {
        TransacaoRequestDTO request = new TransacaoRequestDTO(1L, 2L, new BigDecimal("100.00"));
        String chave = String.valueOf(UUID.randomUUID());

        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(1L);

        // Retorna a conta de origem mas simula que a de destino nao existe
        when(contaRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.empty());


        ContaNaoEncontradaException exception = assertThrows(ContaNaoEncontradaException.class, () -> {
            transacaoService.realizarTransferencia(request, chave,usuarioLogado);
        });

        assertTrue(exception.getMessage().contains("Conta não encontrada"));
    }

    @Test
    void realizarTransferencia_DeveFalharSaldoInsuficiente() {
        TransacaoRequestDTO request = new TransacaoRequestDTO(1L, 2L, new BigDecimal("600.00")); // maior que 500
        String chave = String.valueOf(UUID.randomUUID());
        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(1L);

        when(contaRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findByIdWithPessimisticLock(2L)).thenReturn(Optional.of(contaDestino));

        SaldoInsuficienteException exception = assertThrows(SaldoInsuficienteException.class, () -> {
            transacaoService.realizarTransferencia(request,chave, usuarioLogado);
        });

        assertEquals("Saldo insuficiente na conta de origem.", exception.getMessage());
        
        // Verifica se os saldos não foram alterados
        assertEquals(new BigDecimal("500.00"), contaOrigem.getSaldo());
        assertEquals(new BigDecimal("100.00"), contaDestino.getSaldo());
        
        verify(contaRepository, never()).save(any(Conta.class));
        verify(transacaoRepository, never()).save(any(Transacao.class));
    }
    @Test
    void realizarTransferencia_DeveFalharQuandoUsuarioNaoForDaConta(){
        TransacaoRequestDTO requestDTO = new TransacaoRequestDTO(1L,2L, new BigDecimal("100.00"));
        String chave = UUID.randomUUID().toString();

        Usuario hackerLogado = new Usuario();
        hackerLogado.setId(99L);

        when(contaRepository.findByIdWithPessimisticLock(1L))
                .thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findByIdWithPessimisticLock(2L))
                .thenReturn(Optional.of(contaDestino));

        AcessoNegadoException exception = assertThrows(AcessoNegadoException.class, () ->{
            transacaoService.realizarTransferencia(requestDTO, chave, hackerLogado);
        });
        assertEquals("Usuario diferente da conta origem", exception.getMessage());
        verify(contaRepository, never()).save(any(Conta.class));
    }
    @Test
    void realizarTransferencia_DeveFalharQuandoExcedeLimiteTransacional() {

        TransacaoRequestDTO request = new TransacaoRequestDTO(1L, 2L, new BigDecimal("15000.00"));
        String chave = UUID.randomUUID().toString();

        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(1L);
        contaOrigem.setSaldo(new BigDecimal("20000.00"));

        when(contaRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findByIdWithPessimisticLock(2L)).thenReturn(Optional.of(contaDestino));

        LimiteTransacionalExcedidoException exception = assertThrows(LimiteTransacionalExcedidoException.class, () -> {
            transacaoService.realizarTransferencia(request, chave, usuarioLogado);
        });

        assertTrue(exception.getMessage().contains("excede o limite permitido"));
        verify(contaRepository, never()).save(any(Conta.class));
    }
    @Test
    void realizarTransferencia_DeveFalharChaveIdempotenciaDuplicada() {
        TransacaoRequestDTO request = new TransacaoRequestDTO(1L, 2L, new BigDecimal("100.00"));
        String chave = UUID.randomUUID().toString();

        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(1L);

        when(transacaoRepository.existsByChaveIdempotencia(chave)).thenReturn(true);

        TransacaoDuplicadaException exception = assertThrows(TransacaoDuplicadaException.class, () -> {
            transacaoService.realizarTransferencia(request, chave, usuarioLogado);
        });

        assertTrue(exception.getMessage().contains("já foi processada"));
        verify(contaRepository, never()).findByIdWithPessimisticLock(anyLong());
    }

    @Test
    void realizarDeposito_DeveOcorrerComSucesso() {
        DepositoRequestDTO request = new DepositoRequestDTO(1L, new BigDecimal("100.00"));
        String chave = UUID.randomUUID().toString();

        when(contaRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(contaOrigem));

        Transacao transacao = Transacao.builder()
                .id(UUID.randomUUID())
                .contaOrigem(contaOrigem)
                .contaDestino(contaOrigem)
                .valor(request.valor())
                .dataHora(LocalDateTime.now())
                .chaveIdempotencia(chave)
                .build();

        when(transacaoRepository.save(any(Transacao.class))).thenReturn(transacao);

        TransacaoResponseDTO response = transacaoService.realizarDeposito(request, chave);

        assertNotNull(response);
        assertEquals(new BigDecimal("600.00"), contaOrigem.getSaldo()); // 500 + 100

        verify(contaRepository, times(1)).save(contaOrigem);
        verify(transacaoRepository, times(1)).save(any(Transacao.class));
    }

    @Test
    void realizarSaque_DeveOcorrerComSucesso() {
        SaqueRequestDTO request = new SaqueRequestDTO(1L, new BigDecimal("100.00"));
        String chave = UUID.randomUUID().toString();
        Usuario usuarioLogado = new Usuario();
        usuarioLogado.setId(1L);

        when(contaRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(contaOrigem));

        Transacao transacao = Transacao.builder()
                .id(UUID.randomUUID())
                .contaOrigem(contaOrigem)
                .contaDestino(contaOrigem)
                .valor(request.valor())
                .dataHora(LocalDateTime.now())
                .chaveIdempotencia(chave)
                .build();

        when(transacaoRepository.save(any(Transacao.class))).thenReturn(transacao);

        TransacaoResponseDTO response = transacaoService.realizarSaque(request, chave, usuarioLogado);

        assertNotNull(response);
        assertEquals(new BigDecimal("400.00"), contaOrigem.getSaldo()); // 500 - 100

        verify(contaRepository, times(1)).save(contaOrigem);
        verify(transacaoRepository, times(1)).save(any(Transacao.class));
    }
}
