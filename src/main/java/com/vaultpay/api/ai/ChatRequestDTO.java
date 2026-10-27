package com.vaultpay.api.ai;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDTO(
        @NotBlank(message = "A mensagem não pode estar em branco")
        String mensagem,
        String sessionId
) {}
