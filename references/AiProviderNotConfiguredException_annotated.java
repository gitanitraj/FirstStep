package org.firststep.backend.ai.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// Signals that AiAssistant.generate() was called but no AI provider is
// actually wired in — thrown by SpringAiAssistant when no
// ChatClient.Builder bean exists (i.e. no Spring AI model-provider starter,
// such as spring-ai-starter-model-ollama, is on the classpath).
// =============================================================================

public class AiProviderNotConfiguredException extends RuntimeException {
    public AiProviderNotConfiguredException(String message) {
        super(message);
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// This pass wires in Spring AI's provider-agnostic ChatClient abstraction
// (spring-ai-client-chat) without picking an AI provider — none is available
// or subscribed to yet; that decision is explicitly deferred to a future
// pass. Without a provider starter, Spring AI's auto-configuration never
// produces a ChatClient.Builder bean, so SpringAiAssistant can't unconditionally
// depend on one. Rather than letting Spring's dependency injection fail
// loudly at application startup (which would make the whole app unbootable
// until a provider is chosen), SpringAiAssistant accepts the builder's
// absence via an ObjectProvider and throws this specific, catchable
// exception only when generate() is actually called.
//
// DecisionAgentService's existing broad try/catch(Exception) around its AI
// call (unchanged from v1) already handles this by falling back to a canned
// DecisionResponse — so today's actual runtime behavior (AI guidance
// unavailable) is unchanged by this migration; only the class that produces
// that outcome changed.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Thrown by SpringAiAssistant.generate() when its injected
//   ObjectProvider<ChatClient.Builder> has nothing available.
// - Caught by DecisionAgentService.decide()'s existing catch(Exception)
//   block, same as any other AI-call failure.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Returning a sentinel/empty string instead of throwing: rejected — would
//   be indistinguishable from a real (if unhelpful) model response, whereas
//   an exception lets DecisionAgentService's existing error-message-in-notes
//   fallback path explain *why* no guidance was generated.
// - Failing application startup when no provider is configured: rejected —
//   would make the entire backend unbootable (including unrelated features
//   like Resource/News browsing) over an AI feature that already had a
//   working "unavailable" fallback path in v1.
// =============================================================================
