package com.vaultpay.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ContaRequestDTO(
        @NotBlank String numero,
        @NotNull BigDecimal saldo,
        @NotNull Long usuarioId
) {
}
