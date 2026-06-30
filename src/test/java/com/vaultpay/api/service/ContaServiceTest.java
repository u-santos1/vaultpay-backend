package com.vaultpay.api.service;

import com.vaultpay.api.dtos.ContaRequestDTO;
import com.vaultpay.api.dtosResponse.ContaResponse;
import com.vaultpay.api.model.Conta;
import com.vaultpay.api.repository.ContaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContaServiceTest {

    @Mock
    private ContaRepository contaRepository;

    @InjectMocks
    private ContaService contaService;

    private ContaRequestDTO contaRequestDTO;
    private Conta conta;

    @BeforeEach
    void setUp() {
        contaRequestDTO = new ContaRequestDTO("12345-6", new BigDecimal("100.00"));
        conta = Conta.builder()
                .id(1L)
                .numero("12345-6")
                .saldo(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void criarConta_DeveCriarContaComSucesso() {
        when(contaRepository.existsByNumero("12345-6")).thenReturn(false);
        when(contaRepository.save(any(Conta.class))).thenReturn(conta);

        ContaResponse response = contaService.CriarConta(contaRequestDTO);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("12345-6", response.numero());
        assertEquals(new BigDecimal("100.00"), response.saldo());

        verify(contaRepository, times(1)).existsByNumero("12345-6");
        verify(contaRepository, times(1)).save(any(Conta.class));
    }

    @Test
    void criarConta_DeveLancarExcecaoQuandoContaJaExistir() {
        when(contaRepository.existsByNumero("12345-6")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            contaService.CriarConta(contaRequestDTO);
        });

        assertEquals("Ja existe uma conta registrada com este numero", exception.getMessage());
        verify(contaRepository, times(1)).existsByNumero("12345-6");
        verify(contaRepository, never()).save(any(Conta.class));
    }
}
