package com.vaultpay.api.controller;

import com.vaultpay.api.dtos.TransacaoRequestDTO;
import com.vaultpay.api.dtos.TransacaoResponseDTO;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.service.TransacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @RequestHeader("X-Idempotency-Key") String chaveIdempotencia,
            @AuthenticationPrincipal Usuario usuarioLogado) {
        TransacaoResponseDTO response = transacaoService.realizarTransferencia(data, chaveIdempotencia, usuarioLogado);

        var uri = uriComponentsBuilder.path("/transacao/{id}").buildAndExpand(response.id()).toUri();

        return ResponseEntity.created(uri).body(response);
    }
    @GetMapping("/extrato/{contaId}")
    public ResponseEntity<Page<TransacaoResponseDTO>> extrato(@PathVariable Long contaId, @PageableDefault(size = 10, sort = {"dataHora"}, direction = Sort.Direction.DESC)Pageable pageable,
                                                              @AuthenticationPrincipal Usuario usuarioLogado){
        Page<TransacaoResponseDTO> data = transacaoService.extratos(contaId, pageable, usuarioLogado);
        return ResponseEntity.ok(data);
    }
}
