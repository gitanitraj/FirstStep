package org.firststep.backend.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class SpringAiAssistant implements AiAssistant {

    // Keeps generations fast on CPU-only inference — carried over from v1's OllamaService.
    private static final int MAX_TOKENS = 350;

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public SpringAiAssistant(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
    }

    @Override
    public String generate(String prompt, double temperature) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new AiProviderNotConfiguredException(
                    "No AI provider is configured (no ChatClient.Builder bean available). " +
                    "Add a Spring AI model-provider starter (e.g. spring-ai-starter-model-ollama) " +
                    "to enable AI features.");
        }

        ChatOptions options = ChatOptions.builder()
                .temperature(temperature)
                .maxTokens(MAX_TOKENS)
                .build();

        return builder.build()
                .prompt()
                .user(prompt)
                .options(options)
                .call()
                .content();
    }
}
