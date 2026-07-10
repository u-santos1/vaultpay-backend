package com.vaultpay.api.service;

import com.vaultpay.api.dtos.TransacaoRequestDTO;
import com.vaultpay.api.dtos.TransacaoResponseDTO;
import com.vaultpay.api.event.TransferenciaRealizadaEvent;
import com.vaultpay.api.infra.exception.*;
import com.vaultpay.api.model.Conta;
import com.vaultpay.api.model.Transacao;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.repository.ContaRepository;
import com.vaultpay.api.repository.TransacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransacaoService {

    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @CacheEvict(value = "extrato", allEntries = true)
    public TransacaoResponseDTO realizarTransferencia(TransacaoRequestDTO data, String chaveIdempotencia, Usuario usuarioLogado) {

        if (data.idContaOrigem().equals(data.idContaDestino())) {
            throw new IllegalArgumentException("Conta de origem e destino não podem ser a mesma.");
        }
        if (data.valor() == null || data.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da transferência deve ser maior que zero.");
        }
        if (transacaoRepository.existsByChaveIdempotencia(chaveIdempotencia)){
            throw new TransacaoDuplicadaException("Operação recusada: Uma transação com esta chave de idempotência já foi processada.");
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

        if (!contaOrigem.getUsuario().getId().equals(usuarioLogado.getId())){
            throw new AcessoNegadoException("Usuario diferente da conta origem");
        }

        if(!contaOrigem.getAtivo()){
            throw new ContaInativaException("Operação cancelada: A conta de origem está inativa.");
        }
        if(!contaDestino.getAtivo()){
            throw new ContaInativaException("Operação cancelada: A conta de destino está inativa.");
        }

        if (contaOrigem.getSaldo().compareTo(data.valor()) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente na conta de origem.");
        }
        if (contaOrigem.getLimiteTransacao() != null && data.valor()
                .compareTo(contaDestino.getLimiteTransacao()) > 0){
            throw new LimiteTransacionalExcedidoException(
                    "O valor da transferencia excede o limite permitido por transacao"
            );
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
                .chaveIdempotencia(chaveIdempotencia)
                .dataHora(LocalDateTime.now())
                .build();

        Transacao transacaoSalva = transacaoRepository.save(transacao);
        eventPublisher.publishEvent(new TransferenciaRealizadaEvent(transacaoSalva));

        return TransacaoResponseDTO.fromEntity(transacaoSalva);
    }
    @Cacheable(value = "extrato", key = "#contaId + '-' + #usuarioLogado.id + '-' + #pageable.pageNumber")
    @Transactional(readOnly = true)
    public Page<TransacaoResponseDTO> extratos(Long contaId,
                                               Pageable pageable,
                                               Usuario usuarioLogado){
        Conta conta = contaRepository.findById(contaId)
                .orElseThrow(()-> new ContaNaoEncontradaException("Não existe conta com esse Id."));

        if(!conta.getUsuario().getId().equals(usuarioLogado.getId())){
            throw new AcessoNegadoException("Usuario diferente da conta origem");
        }
        Page<Transacao> pagina = transacaoRepository.findByContaOrigemIdOrContaDestinoId(contaId, contaId, pageable);

        return pagina.map(TransacaoResponseDTO::fromEntity);
    }
}
