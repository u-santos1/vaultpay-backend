package com.vaultpay.api.infra.exception;

public class AcessoNegadoException extends RuntimeException {
  public AcessoNegadoException(String message) {
    super(message);
  }
}
