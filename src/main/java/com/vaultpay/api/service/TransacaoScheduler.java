package com.vaultpay.api.service;

import com.vaultpay.api.StatusTransacao;
import com.vaultpay.api.repository.TransacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransacaoScheduler {

    private  final TransacaoRepository transacaoRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cancelarTransacoesPendentesAntigas(){
        log.info("Iniciando rotina de limpeza de transacao pendentes...");

        LocalDateTime limiteDeTempo = LocalDateTime.now().minusDays(1);

        int transacoesCanceladas = transacaoRepository.cancelarTransacoesAntigas(
                StatusTransacao.PENDENTE,
                StatusTransacao.CANCELADA,
                limiteDeTempo
        );
        log.info("Rotina finalizada. {} transações foram canceladas.", transacoesCanceladas);
    }
}
