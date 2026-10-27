package com.vaultpay.api.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel(
            @Value("${langchain4j.google-ai.gemini.chat-model.api-key:mock-api-key}") String apiKey,
            @Value("${langchain4j.google-ai.gemini.chat-model.model-name:gemini-3.6-flash}") String modelName
    ) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.1)
                .maxOutputTokens(600)
                .logRequestsAndResponses(true)
                .build();
    }
}
