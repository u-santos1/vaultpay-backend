package com.vaultpay.api.dtos;

import java.math.BigDecimal;

public record TransacaoRequestDTO(Long idContaOrigem, Long idContaDestino, BigDecimal valor) {
}
