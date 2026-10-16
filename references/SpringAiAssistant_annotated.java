package org.firststep.backend.ai.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// SpringAiAssistant is the Spring AI-backed implementation of AiAssistant,
// replacing v1's OllamaService (which made raw java.net.http.HttpClient
// calls to Ollama's /api/generate endpoint with hand-rolled JSON parsing).
// It uses Spring AI's ChatClient — but as of this pass, no model-provider
// starter is on the classpath, so it has nothing to actually call yet.
// =============================================================================

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

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// THE KEY DESIGN CHOICE: this class takes an ObjectProvider<ChatClient.Builder>
// rather than a plain ChatClient.Builder constructor parameter. A plain
// dependency would make Spring's DI container fail at application startup
// with no provider starter on the classpath (no bean of that type would
// exist to inject) — meaning the entire app, including unrelated
// Resource/News features, couldn't boot until an AI provider was chosen.
// ObjectProvider<T>.getIfAvailable() returns null instead of failing when no
// bean exists, deferring the "no provider" problem from "the app can't
// start" to "this one method throws when actually called" — see
// AiProviderNotConfiguredException_annotated.java for what happens next.
//
// Uses Spring AI's generic, provider-agnostic ChatOptions (not a
// provider-specific options class like OllamaOptions) to carry temperature
// and max-tokens — this is deliberate: the pom.xml dependency here
// (spring-ai-client-chat) is the provider-agnostic ChatClient API only, no
// provider starter is declared, so importing a provider-specific options
// class isn't even possible yet. When a provider is chosen in a future pass,
// adding that provider's starter dependency should be the only pom.xml
// change needed — this class's logic doesn't need to change.
//
// MAX_TOKENS=350 is carried over unchanged from OllamaService, same
// rationale (keeps generations fast on CPU-only inference).
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Implements AiAssistant; registered as the sole @Service bean for that
//   interface, so DecisionAgentService's AiAssistant dependency resolves to
//   this class.
// - Depends on Spring AI's auto-configured ChatClient.Builder, which only
//   exists as a bean once a model-provider starter (e.g.
//   spring-ai-starter-model-ollama) is added to backend/pom.xml — not done
//   in this pass.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - A separate DisabledAiAssistant/NoOpAiAssistant bean, selected via
//   @ConditionalOnMissingBean, instead of one class handling both cases:
//   rejected as an extra class for no real benefit — the ObjectProvider
//   check is a few lines, and one implementation is easier to reason about
//   than two beans whose selection depends on classpath contents.
// - Adding spring-ai-starter-model-ollama now (restoring the same provider
//   v1 used): rejected — the user reported Ollama isn't currently accessible
//   and no AI provider subscription exists yet; the provider choice is
//   explicitly deferred to a future pass, not assumed to default back to
//   Ollama.
// =============================================================================
