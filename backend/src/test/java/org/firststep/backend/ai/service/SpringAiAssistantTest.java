package org.firststep.backend.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiAssistantTest {

    @Test
    void shouldThrowAiProviderNotConfiguredExceptionWhenNoChatClientBuilderAvailable() {
        ObjectProvider<ChatClient.Builder> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        SpringAiAssistant assistant = new SpringAiAssistant(provider);

        assertThrows(AiProviderNotConfiguredException.class,
                () -> assistant.generate("What housing help is available?", 0.2));
    }

    @Test
    void shouldDelegateToChatClientWhenBuilderAvailable() {
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callResponseSpec = mock(CallResponseSpec.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ObjectProvider<ChatClient.Builder> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Here is some guidance.");

        SpringAiAssistant assistant = new SpringAiAssistant(provider);

        String result = assistant.generate("What housing help is available?", 0.2);

        assertEquals("Here is some guidance.", result);
    }
}
