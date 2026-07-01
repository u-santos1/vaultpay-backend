package com.vaultpay.api.infra.exception;

public class TransacaoDuplicadaException extends RuntimeException {
    public TransacaoDuplicadaException(String message) {
        super(message);
    }
}
