package com.vaultpay.api.dtos;

import java.math.BigDecimal;

public record DepositoRequestDTO(Long idConta, BigDecimal valor) {
}
