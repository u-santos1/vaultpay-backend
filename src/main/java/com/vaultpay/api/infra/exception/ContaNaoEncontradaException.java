package com.vaultpay.api.infra.exception;

public class ContaNaoEncontradaException extends RuntimeException {
    public ContaNaoEncontradaException(String mensagem) {
        super(mensagem);
    }

}
