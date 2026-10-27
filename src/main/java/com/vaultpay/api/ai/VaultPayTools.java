package com.vaultpay.api.ai;

import com.vaultpay.api.dtos.TransacaoRequestDTO;
import com.vaultpay.api.dtos.TransacaoResponseDTO;
import com.vaultpay.api.model.Conta;
import com.vaultpay.api.model.Transacao;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.repository.ContaRepository;
import com.vaultpay.api.repository.TransacaoRepository;
import com.vaultpay.api.repository.UsuarioRepository;
import com.vaultpay.api.service.TransacaoService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class VaultPayTools {

    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransacaoService transacaoService;

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Tool("Consulta o saldo disponível, limite transacional diário e status da conta do cliente atual")
    public String consultarSaldo(
            @P("ID do usuário autenticado no sistema") Long usuarioId
    ) {
        log.info("[AI TOOL] Consultando saldo para usuarioId={}", usuarioId);

        Conta conta = contaRepository.findByUsuarioId(usuarioId)
                .orElse(null);

        if (conta == null) {
            return "Erro: Conta não encontrada para este usuário.";
        }

        NumberFormat nf = NumberFormat.getCurrencyInstance(PT_BR);
        String saldoFormatado = nf.format(conta.getSaldo());
        String limiteFormatado = conta.getLimiteTransacao() != null
                ? nf.format(conta.getLimiteTransacao())
                : "Sem limite definido";

        return String.format(
                "Dados da Conta:\n- Número da Conta: %s (ID: %d)\n- Status: %s\n- Saldo Disponível: %s\n- Limite por Transação: %s",
                conta.getNumero(),
                conta.getId(),
                conta.getAtivo() ? "Ativa" : "Inativa/Bloqueada",
                saldoFormatado,
                limiteFormatado
        );
    }

    @Tool("Consulta os lançamentos e movimentações mais recentes do extrato bancário do cliente")
    public String consultarExtrato(
            @P("ID do usuário autenticado no sistema") Long usuarioId
    ) {
        log.info("[AI TOOL] Consultando extrato para usuarioId={}", usuarioId);

        Conta conta = contaRepository.findByUsuarioId(usuarioId)
                .orElse(null);

        if (conta == null) {
            return "Erro: Nenhuma conta associada a este usuário.";
        }

        Page<Transacao> ultimasTransacoes = transacaoRepository.findByContaOrigemIdOrContaDestinoId(
                conta.getId(),
                conta.getId(),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "dataHora"))
        );

        if (ultimasTransacoes.isEmpty()) {
            return "O extrato da sua conta está sem movimentações recentes.";
        }

        NumberFormat nf = NumberFormat.getCurrencyInstance(PT_BR);
        StringBuilder sb = new StringBuilder("Últimas Movimentações da Conta (" + conta.getNumero() + "):\n");

        for (Transacao t : ultimasTransacoes) {
            String tipoDesc;
            boolean isSaida = t.getContaOrigem() != null && t.getContaOrigem().getId().equals(conta.getId());

            switch (t.getTipo()) {
                case TRANSFERENCIA -> tipoDesc = isSaida ? "Transferência Enviada (Débito)" : "Transferência Recebida (Crédito)";
                case DEPOSITO -> tipoDesc = "Depósito Recebido (Crédito)";
                case SAQUE -> tipoDesc = "Saque Realizado (Débito)";
                default -> tipoDesc = t.getTipo().name();
            }

            String dataHora = t.getDataHora() != null ? t.getDataHora().format(DATE_FORMATTER) : "Data não registrada";
            sb.append(String.format("• %s | %s | %s | Status: %s\n",
                    dataHora,
                    tipoDesc,
                    nf.format(t.getValor()),
                    t.getStatus()
            ));
        }

        return sb.toString();
    }

    @Tool("Realiza uma transferência financeira entre a conta do cliente autenticado e uma conta de destino informada")
    public String transferir(
            @P("ID do usuário autenticado que está enviando a transferência") Long usuarioId,
            @P("ID numérico da conta de destino que receberá os fundos") Long contaDestinoId,
            @P("Valor numérico da transferência em Reais (ex: 50.00)") Double valor,
            @P("Motivo ou descrição opcional da transferência") String descricao
    ) {
        log.info("[AI TOOL] Realizando transferência: usuarioId={}, destino={}, valor={}", usuarioId, contaDestinoId, valor);

        try {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

            Conta contaOrigem = contaRepository.findByUsuarioId(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Conta de origem não encontrada."));

            if (valor == null || valor <= 0) {
                return "Erro: O valor da transferência deve ser maior que zero.";
            }

            BigDecimal valorBd = BigDecimal.valueOf(valor);
            TransacaoRequestDTO requestDTO = new TransacaoRequestDTO(contaOrigem.getId(), contaDestinoId, valorBd);
            String chaveIdempotencia = UUID.randomUUID().toString();

            TransacaoResponseDTO response = transacaoService.realizarTransferencia(requestDTO, chaveIdempotencia, usuario);

            NumberFormat nf = NumberFormat.getCurrencyInstance(PT_BR);
            return String.format(
                    "Transferência realizada com sucesso!\n" +
                    "- ID da Transação: %s\n" +
                    "- Valor Transferido: %s\n" +
                    "- Conta Origem ID: %d\n" +
                    "- Conta Destino ID: %d\n" +
                    "- Data/Hora: %s\n" +
                    "- Status: CONCLUÍDA",
                    response.id(),
                    nf.format(response.valor()),
                    response.idContaOrigem(),
                    response.idContaDestino(),
                    response.dataHora().format(DATE_FORMATTER)
            );
        } catch (Exception e) {
            log.error("[AI TOOL] Falha ao realizar transferência via IA: {}", e.getMessage());
            return "Falha ao realizar transferência: " + e.getMessage();
        }
    }
}
