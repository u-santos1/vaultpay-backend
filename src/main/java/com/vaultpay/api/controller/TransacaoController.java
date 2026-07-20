package com.vaultpay.api.controller;

import com.vaultpay.api.dtos.DepositoRequestDTO;
import com.vaultpay.api.dtos.SaqueRequestDTO;
import com.vaultpay.api.dtos.TransacaoRequestDTO;
import com.vaultpay.api.dtos.TransacaoResponseDTO;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.service.TransacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(summary = "Realiza uma transferência", description = "Transfere um valor entre duas contas ativas de forma segura e assíncrona.")
    @ApiResponse(responseCode = "201", description = "Transferência realizada com sucesso")
    @ApiResponse(responseCode = "400", description = "Regra de negócio violada (ex: Saldo Insuficiente)")
    @ApiResponse(responseCode = "403", description = "Acesso Negado (Token inválido ou conta não pertence ao usuário)")
    @PostMapping
    public ResponseEntity<TransacaoResponseDTO> transferir(
            @RequestBody @Valid TransacaoRequestDTO data,
            UriComponentsBuilder uriComponentsBuilder,
            @RequestHeader("X-Idempotency-Key") String chaveIdempotencia,
            @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuarioLogado) {
        TransacaoResponseDTO response = transacaoService.realizarTransferencia(data, chaveIdempotencia, usuarioLogado);

        var uri = uriComponentsBuilder.path("/transacao/{id}").buildAndExpand(response.id()).toUri();

        return ResponseEntity.created(uri).body(response);
    }
    @GetMapping("/extrato/{contaId}")
    public ResponseEntity<Page<TransacaoResponseDTO>> extrato(@PathVariable Long contaId, @PageableDefault(size = 10, sort = {"dataHora"}, direction = Sort.Direction.DESC)Pageable pageable,
                                                              @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuarioLogado){
        Page<TransacaoResponseDTO> data = transacaoService.extratos(contaId, pageable, usuarioLogado);
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "Realiza um depósito", description = "Adiciona fundos a uma conta ativa.")
    @ApiResponse(responseCode = "201", description = "Depósito realizado com sucesso")
    @PostMapping("/deposito")
    public ResponseEntity<TransacaoResponseDTO> deposito(
            @RequestBody @Valid DepositoRequestDTO data,
            UriComponentsBuilder uriComponentsBuilder,
            @RequestHeader("X-Idempotency-Key") String chaveIdempotencia) {
        TransacaoResponseDTO response = transacaoService.realizarDeposito(data, chaveIdempotencia);
        var uri = uriComponentsBuilder.path("/transacao/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @Operation(summary = "Realiza um saque", description = "Remove fundos de uma conta ativa, respeitando saldo e limite.")
    @ApiResponse(responseCode = "201", description = "Saque realizado com sucesso")
    @PostMapping("/saque")
    public ResponseEntity<TransacaoResponseDTO> saque(
            @RequestBody @Valid SaqueRequestDTO data,
            UriComponentsBuilder uriComponentsBuilder,
            @RequestHeader("X-Idempotency-Key") String chaveIdempotencia,
            @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuarioLogado) {
        TransacaoResponseDTO response = transacaoService.realizarSaque(data, chaveIdempotencia, usuarioLogado);
        var uri = uriComponentsBuilder.path("/transacao/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }
}
