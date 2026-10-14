package com.vaultpay.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ContaRequestDTO(
        @NotBlank String numero,
        BigDecimal saldo,
        @NotNull Long usuarioId,
        BigDecimal limiteTransacao
) {
}
