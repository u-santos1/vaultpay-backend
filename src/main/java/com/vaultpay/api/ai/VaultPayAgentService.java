package com.vaultpay.api.ai;

import com.vaultpay.api.model.Conta;
import com.vaultpay.api.model.Usuario;
import com.vaultpay.api.repository.ContaRepository;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class VaultPayAgentService {

    private final VaultPayAssistant assistant;
    private final ContaRepository contaRepository;

    public VaultPayAgentService(
            ChatLanguageModel chatLanguageModel,
            VaultPayTools vaultPayTools,
            ContaRepository contaRepository
    ) {
        this.contaRepository = contaRepository;

        // Constrói o assistente conectando o Modelo + Ferramentas Bancárias + Memória de Conversa (20 msgs)
        this.assistant = AiServices.builder(VaultPayAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(vaultPayTools)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }

    public ChatResponseDTO conversar(String mensagemUsuario, Usuario usuarioLogado, String sessionId) {
        String sessaoId = (sessionId != null && !sessionId.isBlank())
                ? sessionId
                : "user-session-" + usuarioLogado.getId();

        log.info("[VAULTPAY-AI] Mensagem recebida do usuário {} (Sessão: {}): {}",
                usuarioLogado.getEmail(), sessaoId, mensagemUsuario);

        Conta conta = contaRepository.findByUsuarioId(usuarioLogado.getId())
                .orElse(null);

        String contaNumero = conta != null ? conta.getNumero() : "SEM_CONTA";
        Long contaId = conta != null ? conta.getId() : 0L;
        String usuarioNome = usuarioLogado.getEmail().split("@")[0];
        String dataAtual = LocalDateTime.now().toString();

        return assistant.conversar(
                sessaoId,
                usuarioNome,
                usuarioLogado.getId(),
                contaNumero,
                contaId,
                dataAtual,
                mensagemUsuario
        );
    }
}
