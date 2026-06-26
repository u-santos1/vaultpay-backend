package com.vaultpay.api.dtosResponse;

import com.vaultpay.api.model.Conta;

import java.math.BigDecimal;

public record ContaResponse(Long id, String numero, BigDecimal saldo) {
    public static ContaResponse dto(Conta conta){
        return new ContaResponse(
                conta.getId(),
                conta.getNumero(),
                conta.getSaldo()
        );
    }
}
