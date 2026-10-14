package com.vaultpay.api.controller;

import com.vaultpay.api.PerfilDeAcesso;
import com.vaultpay.api.dtos.ContaRequestDTO;
import com.vaultpay.api.dtosResponse.ContaResponse;
import com.vaultpay.api.infra.exception.AcessoNegadoException;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.service.ContaService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/conta")
@RequiredArgsConstructor
public class ContaController {

    private final ContaService contaService;

    @PostMapping
    @SecurityRequirement(name = "bearer-key")
    public ResponseEntity<ContaResponse> conta(
            @RequestBody @Valid ContaRequestDTO data,
            UriComponentsBuilder componentsBuilder,
            @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuarioLogado
            ){
        boolean admin = usuarioLogado.getPerfilDeAcesso() == PerfilDeAcesso.ADMIN;
        if (!admin && !data.usuarioId().equals(usuarioLogado.getId())) {
            throw new AcessoNegadoException("Voce so pode criar conta para o seu proprio usuario");
        }
        ContaResponse dto = contaService.CriarConta(data);
        var uri = componentsBuilder.path("/conta/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    @org.springframework.web.bind.annotation.GetMapping("/me")
    @SecurityRequirement(name = "bearer-key")
    public ResponseEntity<com.vaultpay.api.dtosResponse.ContaDetalhesResponseDTO> minhaConta(
            @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        var response = contaService.buscarMinhaConta(usuarioLogado.getId());
        return ResponseEntity.ok(response);
    }
}
