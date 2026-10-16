package org.firststep.backend.ai.dto;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// DecisionResponse is the response body for POST /api/decide — an
// AI-generated (or graceful-fallback) answer: a title, ordered steps, the
// citations backing them, and free-text notes.
// =============================================================================

import java.util.List;

import org.firststep.backend.shared.model.Citation;

public class DecisionResponse {

    public String answerTitle;
    public List<DecisionStep> steps;

    /**
     * Citations that reference which local items were used.
     */
    public List<Citation> citations;

    /**
     * If the system couldn't find relevant matches, this explains why.
     */
    public String notes;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Package move only (dto -> ai/dto) — no field changes. citations: List<Citation>
// already pointed at shared.model.Citation since Step 1 (Citation itself
// moved out of dto/ in that step, before DecisionResponse moved here in this
// step) — that's why the import is shared.model.Citation, not a
// locally-defined type.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Constructed by DecisionAgentService: either the AI-disabled/AI-call-failed
//   fallback paths, or parseDecisionResponse's parsing of the model's raw
//   JSON output.
// - citations' contentSource fields are populated by
//   DecisionAgentService.resolveCitationSources after parsing — see
//   DecisionAgentService_annotated.java.
// - Wrapped in ApiResponse<DecisionResponse> by DecisionController.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - None — straightforward package move.
// =============================================================================
