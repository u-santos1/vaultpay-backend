package com.vaultpay.api.controller;

import com.vaultpay.api.dtos.CadastroUsuarioRequestDTO;
import com.vaultpay.api.dtosResponse.UsuarioResponseDTO;
import com.vaultpay.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Cadastro de novo usuário", description = "Cria um novo usuário no sistema e abre sua carteira digital automaticamente com saldo inicial R$ 0,00.")
    @ApiResponse(responseCode = "201", description = "Usuário e conta cadastrados com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou e-mail já cadastrado")
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrar(
            @RequestBody @Valid CadastroUsuarioRequestDTO data,
            UriComponentsBuilder uriBuilder) {
        UsuarioResponseDTO response = usuarioService.cadastrar(data);
        var uri = uriBuilder.path("/usuarios/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }
}
