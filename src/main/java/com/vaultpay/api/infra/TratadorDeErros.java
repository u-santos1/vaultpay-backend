package com.vaultpay.api.infra;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vaultpay.api.infra.exception.ContaNaoEncontradaException;
import com.vaultpay.api.infra.exception.SaldoInsuficienteException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<ErrorDTO> tratarErroSaldoInsuficiente(SaldoInsuficienteException error) {
        return ResponseEntity.badRequest().body(new ErrorDTO(error.getMessage()));
    }

    @ExceptionHandler(ContaNaoEncontradaException.class)
    public ResponseEntity<ErrorDTO> tratarContaNaoEncontrada(ContaNaoEncontradaException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorDTO(error.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDTO> tratarIllegalArgumentException(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(new ErrorDTO(error.getMessage()));

    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> tratarErro500(Exception erro) {
        log.error("Erro inesperado: ", erro);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDTO("Erro interno no servidor."));
    }

    public record ErrorDTO(String erro) {
    }
}
