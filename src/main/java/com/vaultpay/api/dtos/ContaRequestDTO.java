package com.vaultpay.api.dtos;

import java.math.BigDecimal;

public record ContaRequestDTO(String numero, BigDecimal saldo) {
}
