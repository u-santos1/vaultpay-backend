package com.vaultpay.api.service;

import com.vaultpay.api.dtos.TransacaoResponseDTO;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RelatorioTransacoes {

    public BigDecimal somaTotalTransacionado(List<TransacaoResponseDTO> transacoes) {
        if (transacoes == null || transacoes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return transacoes.stream()
                .map(TransacaoResponseDTO::valor)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<TransacaoResponseDTO> filtrarPorValorMinimo(
            List<TransacaoResponseDTO> transacoes, BigDecimal valorMinimo) {
        if (transacoes == null || transacoes.isEmpty()) {
            return Collections.emptyList();
        }
        return transacoes.stream()
                .filter(t -> t.valor() != null && t.valor().compareTo(valorMinimo) > 0)
                .collect(Collectors.toList());
    }

    public Optional<TransacaoResponseDTO> maiorTransacao(List<TransacaoResponseDTO> transacoes) {
        if (transacoes == null || transacoes.isEmpty()) {
            return Optional.empty();
        }

        return transacoes.stream()
                .max(Comparator.comparing(TransacaoResponseDTO::valor));

    }

    public Map<Long, List<TransacaoResponseDTO>> agruparPorContaOrigem(
            List<TransacaoResponseDTO> transacoes) {
        if (transacoes == null || transacoes.isEmpty()) {
            return Collections.emptyMap();
        }
        return transacoes.stream()
                .collect(Collectors.groupingBy(TransacaoResponseDTO::idContaOrigem));

    }

    public boolean todasPositivas(List<TransacaoResponseDTO> transacoes) {
        if (transacoes == null || transacoes.isEmpty()) {
            return true;
        }
        return transacoes.stream()
                .allMatch(t -> t.valor() != null && t.valor().compareTo(BigDecimal.ZERO) > 0);

    }
}
