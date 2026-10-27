package com.vaultpay.api.ai;

import dev.langchain4j.model.output.structured.Description;
import java.util.List;

public record ChatResponseDTO(
        @Description("Texto da resposta educada, clara e profissional do assistente bancário para o cliente ler")
        String resposta,

        @Description("Classifique o que o cliente deseja: CONSULTAR_SALDO, CONSULTAR_EXTRATO, TRANSFERIR ou OUTRO")
        String intencao,

        @Description("Lista com até 3 sugestões curtas de próximas ações ou botões rápidos para o cliente clicar")
        List<String> sugestoesBotoes
) {}
