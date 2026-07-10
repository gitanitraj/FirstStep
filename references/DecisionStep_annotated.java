package org.firststep.backend.ai.dto;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// DecisionStep is one ordered action within a DecisionResponse — what to do,
// and (optionally) why.
// =============================================================================

public class DecisionStep {
    public int order;
    public String title;
    public String action;

    /**
     * Optional explanation to help residents understand why.
     */
    public String why;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Package move only (dto -> ai/dto) — no field changes.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - DecisionResponse.steps is a List<DecisionStep>, parsed from the model's
//   raw JSON by DecisionAgentService.parseDecisionResponse, then sorted by
//   `order`.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - None — straightforward package move.
// =============================================================================
