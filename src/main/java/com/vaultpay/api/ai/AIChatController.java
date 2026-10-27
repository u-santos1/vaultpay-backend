package com.vaultpay.api.ai;

import com.vaultpay.api.model.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-key")
@Tag(name = "AI Concierge", description = "Assistente Financeiro Inteligente Autônomo com Function Calling")
public class AIChatController {

    private final VaultPayAgentService agentService;

    @Operation(
            summary = "Conversa com o Assistente Financeiro do VaultPay",
            description = "Envia uma mensagem em linguagem natural para o assistente. O assistente possui autonomia para consultar saldo, extrato e realizar transferências seguras da conta do usuário autenticado."
    )
    @ApiResponse(responseCode = "200", description = "Resposta estruturada gerada pelo assistente com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso não autorizado - Token JWT ausente ou inválido")
    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDTO> conversar(
            @RequestBody @Valid ChatRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        ChatResponseDTO response = agentService.conversar(
                request.mensagem(),
                usuarioLogado,
                request.sessionId()
        );

        return ResponseEntity.ok(response);
    }
}
