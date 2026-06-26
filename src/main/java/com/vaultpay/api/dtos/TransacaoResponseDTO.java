package com.vaultpay.api.dtos;

import com.vaultpay.api.model.Transacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransacaoResponseDTO(
        UUID id,
        Long idContaOrigem,
        Long idContaDestino,
        BigDecimal valor,
        LocalDateTime dataHora
) {
    public static TransacaoResponseDTO fromEntity(Transacao transacao) {
        return new TransacaoResponseDTO(
                transacao.getId(),
                transacao.getContaOrigem().getId(),
                transacao.getContaDestino().getId(),
                transacao.getValor(),
                transacao.getDataHora()
        );
    }
}
