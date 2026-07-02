package com.vaultpay.api.infra.exception;

public class LimiteTransacionalExcedidoException extends RuntimeException {
    public LimiteTransacionalExcedidoException(String message) {
        super(message);
    }
}
