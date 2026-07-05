package com.vaultpay.api.service;

import com.vaultpay.api.event.TransferenciaRealizadaEvent;
import com.vaultpay.api.model.Transacao;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class NotificacaoService {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviarNotificacao(TransferenciaRealizadaEvent event){
        Transacao transacao = event.transacao();

        // Simulacao do envio de email demorado
        System.out.println("---- INÍCIO DA NOTIFICAÇÃO ----");
        System.out.println("A enviar e-mail para o destinatário da conta: " + transacao.getContaDestino().getNumero());
        System.out.println("Mensagem: Recebeste uma transferência de R$ " + transacao.getValor());
        System.out.println("---- NOTIFICAÇÃO CONCLUÍDA ----");
    }
}
