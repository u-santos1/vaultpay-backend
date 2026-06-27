package com.vaultpay.api.service;

import com.vaultpay.api.dtos.TransacaoRequestDTO;
import com.vaultpay.api.dtos.TransacaoResponseDTO;
import com.vaultpay.api.infra.exception.ContaNaoEncontradaException;
import com.vaultpay.api.infra.exception.SaldoInsuficienteException;
import com.vaultpay.api.model.Conta;
import com.vaultpay.api.model.Transacao;
import com.vaultpay.api.repository.ContaRepository;
import com.vaultpay.api.repository.TransacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransacaoService {

    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;

    @Transactional
    public TransacaoResponseDTO realizarTransferencia(TransacaoRequestDTO data) {
        if (data.idContaOrigem().equals(data.idContaDestino())) {
            throw new IllegalArgumentException("Conta de origem e destino não podem ser a mesma.");
        }
        if (data.valor() == null || data.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da transferência deve ser maior que zero.");
        }

        // Ordenando os IDs para evitar deadlocks em transferências concorrentes (ex: A -> B e B -> A simultaneamente)
        Long firstIdToLock = data.idContaOrigem() < data.idContaDestino() ? data.idContaOrigem() : data.idContaDestino();
        Long secondIdToLock = data.idContaOrigem() < data.idContaDestino() ? data.idContaDestino() : data.idContaOrigem();

        // Adquirindo lock pessimista definido no ContaRepository
        Conta firstConta = contaRepository.findByIdWithPessimisticLock(firstIdToLock)
                .orElseThrow(() -> new ContaNaoEncontradaException("Conta não encontrada: " + firstIdToLock));
        
        Conta secondConta = contaRepository.findByIdWithPessimisticLock(secondIdToLock)
                .orElseThrow(() -> new ContaNaoEncontradaException("Conta não encontrada: " + secondIdToLock));

        Conta contaOrigem = firstConta.getId().equals(data.idContaOrigem()) ? firstConta : secondConta;
        Conta contaDestino = firstConta.getId().equals(data.idContaDestino()) ? firstConta : secondConta;

        if (contaOrigem.getSaldo().compareTo(data.valor()) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente na conta de origem.");
        }

        // Atualizando os saldos
        contaOrigem.setSaldo(contaOrigem.getSaldo().subtract(data.valor()));
        contaDestino.setSaldo(contaDestino.getSaldo().add(data.valor()));

        contaRepository.save(contaOrigem);
        contaRepository.save(contaDestino);

        // Registrando a transação
        Transacao transacao = Transacao.builder()
                .contaOrigem(contaOrigem)
                .contaDestino(contaDestino)
                .valor(data.valor())
                .dataHora(LocalDateTime.now())
                .build();

        Transacao transacaoSalva = transacaoRepository.save(transacao);

        return TransacaoResponseDTO.fromEntity(transacaoSalva);
    }
}
