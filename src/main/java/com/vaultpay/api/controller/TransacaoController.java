package com.vaultpay.api.controller;

import com.vaultpay.api.dtos.TransacaoRequestDTO;
import com.vaultpay.api.dtos.TransacaoResponseDTO;
import com.vaultpay.api.service.TransacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/transacao")
@RequiredArgsConstructor
public class TransacaoController {

    private final TransacaoService transacaoService;

    @PostMapping
    public ResponseEntity<TransacaoResponseDTO> transferir(
            @RequestBody @Valid TransacaoRequestDTO data,
            UriComponentsBuilder uriComponentsBuilder,
            @RequestHeader("X-Idempotency-Key") String chaveIdempotencia) {
        TransacaoResponseDTO response = transacaoService.realizarTransferencia(data, chaveIdempotencia);

        var uri = uriComponentsBuilder.path("/transacao/{id}").buildAndExpand(response.id()).toUri();

        return ResponseEntity.created(uri).body(response);
    }
}
