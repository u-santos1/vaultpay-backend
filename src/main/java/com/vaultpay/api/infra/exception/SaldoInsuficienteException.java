package com.vaultpay.api.infra.exception;

public class SaldoInsuficienteException extends RuntimeException {
    public SaldoInsuficienteException(String messagem) {
        super(messagem);
    }
}
