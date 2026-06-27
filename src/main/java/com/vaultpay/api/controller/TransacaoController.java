package com.vaultpay.api.controller;

import com.vaultpay.api.dtos.TransacaoRequestDTO;
import com.vaultpay.api.dtos.TransacaoResponseDTO;
import com.vaultpay.api.service.TransacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/transacao")
@RequiredArgsConstructor
public class TransacaoController {

    private final TransacaoService transacaoService;

    @PostMapping
    public ResponseEntity<TransacaoResponseDTO> transferir(@RequestBody TransacaoRequestDTO request,
            UriComponentsBuilder uriComponentsBuilder) {
        TransacaoResponseDTO response = transacaoService.realizarTransferencia(request);

        var uri = uriComponentsBuilder.path("/transacao/{id}").buildAndExpand(response.id()).toUri();

        return ResponseEntity.created(uri).body(response);
    }
}
