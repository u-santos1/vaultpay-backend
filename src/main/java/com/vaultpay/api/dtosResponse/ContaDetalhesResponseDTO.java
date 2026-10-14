package com.vaultpay.api.dtosResponse;

import com.vaultpay.api.model.Conta;

import java.math.BigDecimal;

public record ContaDetalhesResponseDTO(
        Long id,
        String numero,
        BigDecimal saldo,
        BigDecimal limiteTransacao,
        Boolean ativo
) {
    public static ContaDetalhesResponseDTO fromEntity(Conta conta) {
        return new ContaDetalhesResponseDTO(
                conta.getId(),
                conta.getNumero(),
                conta.getSaldo(),
                conta.getLimiteTransacao(),
                conta.getAtivo()
        );
    }
}
