package com.vaultpay.api.infra;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vaultpay.api.infra.exception.AcessoNegadoException;
import com.vaultpay.api.infra.exception.ContaInativaException;
import com.vaultpay.api.infra.exception.ContaNaoEncontradaException;
import com.vaultpay.api.infra.exception.LimiteTransacionalExcedidoException;
import com.vaultpay.api.infra.exception.SaldoInsuficienteException;
import com.vaultpay.api.infra.exception.TransacaoDuplicadaException;

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

    @ExceptionHandler(TransacaoDuplicadaException.class)
    public ResponseEntity<ErrorDTO> tratarTransacaoDuplicada(TransacaoDuplicadaException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDTO(e.getMessage()));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErrorDTO> tratarAcessoNegado(AcessoNegadoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorDTO(e.getMessage()));
    }

    @ExceptionHandler({ ContaInativaException.class, LimiteTransacionalExcedidoException.class })
    public ResponseEntity<ErrorDTO> tratarRegraDeNegocio(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorDTO(e.getMessage()));
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
