package com.vaultpay.api.dtos;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransacaoRequestDTO(
                @NotNull(message = "O ID da conta de origem é obrigatório") Long idContaOrigem,
                @NotNull(message = "O ID da conta de destino é obrigatório") Long idContaDestino,
                @NotNull(message = "O valor da transação é obrigatório") @Positive(message = "O valor deve ser positivo") BigDecimal valor) {
}
