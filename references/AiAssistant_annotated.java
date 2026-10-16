package org.firststep.backend.ai.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// AiAssistant is the seam between DecisionAgentService and whatever actually
// generates text (an LLM call). It has exactly one method — replacing the
// direct dependency v1's DecisionAgentService had on the concrete
// OllamaService class.
// =============================================================================

public interface AiAssistant {
    String generate(String prompt, double temperature);
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Same single-method signature as v1's OllamaService.generate(String, double)
// — this pass is an interface extraction, not a redesign of what the AI seam
// does. The two checked exceptions v1's method declared (IOException,
// InterruptedException, from its raw java.net.http.HttpClient usage) were
// dropped: Spring AI's ChatClient doesn't throw them, and the only caller
// (DecisionAgentService.decide()) already wraps the call in a broad
// try/catch(Exception), so nothing downstream needed the checked-exception
// contract.
//
// Named "AiAssistant" per explicit instruction, not "CivicAssistantService"
// (the placeholder name used in the pre-existing
// docs/architecture/02-information-flow.md UML sketch) — the docs are
// updated to match this class rather than introducing two names for the same
// seam.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - DecisionAgentService depends on this interface (constructor-injected),
//   not on any concrete implementation.
// - SpringAiAssistant is the one implementation in this pass — see
//   SpringAiAssistant_annotated.java for how it stays bootable with zero AI
//   providers configured.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Keeping the checked exceptions on the interface: rejected — would force
//   every current and future implementation to either declare or swallow
//   exceptions that don't apply to non-HTTP-based providers, for no benefit
//   given the sole caller already catches broadly.
// =============================================================================
