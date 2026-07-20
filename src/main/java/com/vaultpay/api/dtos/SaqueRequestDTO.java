package com.vaultpay.api.dtos;

import java.math.BigDecimal;

public record SaqueRequestDTO(Long idConta, BigDecimal valor) {
}
