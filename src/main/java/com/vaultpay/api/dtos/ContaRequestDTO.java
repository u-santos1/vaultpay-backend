package com.vaultpay.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ContaRequestDTO(
        @NotBlank(message = "O número da conta é obrigatório") String numero,
        @NotNull(message = "O ID do usuário é obrigatório") Long usuarioId,
        BigDecimal limiteTransacao) {
}
