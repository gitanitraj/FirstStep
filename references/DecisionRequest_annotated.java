package org.firststep.backend.ai.dto;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// DecisionRequest is the request body for POST /api/decide — a resident's
// free-text question plus optional filters to bias retrieval.
// =============================================================================

import java.util.List;

public class DecisionRequest {

    /**
     * Free-text question from the user.
     */
    public String userQuery;

    /**
     * Optional urgency filter for narrowing relevant resources.
     * Values expected: "urgent" or null/empty.
     */
    public Boolean urgent;

    /**
     * Optional categories to bias retrieval.
     * Example: ["housing", "essentials"]
     */
    public List<String> preferredCategories;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Package move only (dto -> ai/dto) — no field changes. This class is
// ai-slice-specific request shape, not a shared/domain concept, so it
// belongs in ai/dto rather than shared.model.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Deserialized from the JSON body of POST /api/decide by DecisionController.
// - Consumed by DecisionAgentService.decide() to drive retrieval scoring
//   (selectTopResources/selectTopNews).
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - None — straightforward package move of an already-minimal DTO.
// =============================================================================
