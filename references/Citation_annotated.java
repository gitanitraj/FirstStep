package org.firststep.backend.shared.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// Citation is how an AI-generated answer (DecisionResponse) points back to
// the specific Resource or NewsItem it drew from, so the user can verify the
// guidance against a real source rather than trusting the AI blindly.
// =============================================================================

public class Citation {

    /**
     * Which dataset the citation came from.
     */
    public String sourceType; // "resource" | "news"

    /**
     * ID of the cited item.
     */
    public String id;

    /**
     * Short human-readable label to display.
     */
    public String label;

    /**
     * Provenance of the cited item, resolved after retrieval (see
     * DecisionAgentService.resolveCitationSources). Null if the model cited
     * an id that didn't match any retrieved item.
     */
    public ContentSource contentSource;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// This class moved from dto/Citation.java to shared/model/Citation.java
// unchanged in its original three fields (sourceType, id, label) — it's used
// by more than one slice (ai's DecisionResponse today; potentially
// search/expert later), so it belongs in the shared kernel rather than one
// slice's dto package.
//
// The new contentSource field is what actually links a Citation to a real
// ContentSource, resolving the gap the v1 docs named explicitly: "Citation
// evolves from v1 dto/Citation... How a delivered answer points back to its
// ContentSource." It's nullable because the LLM that produces citations can
// (and occasionally does) hallucinate an id that doesn't match anything
// actually retrieved — see DecisionAgentService_annotated.java for how that
// case is handled (left null, not an exception).
//
// No @JsonIgnoreProperties(ignoreUnknown = true) annotation was added, even
// though most other shared-model classes have it — the original dto/Citation
// didn't have it either, and this move doesn't change how Citation is
// produced (it's only ever built by DecisionAgentService parsing an LLM
// response, never deserialized from an external JSON file the way
// Resource/NewsItem are), so there was nothing to preserve or fix here.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - DecisionResponse.citations is a List<Citation>, populated by
//   DecisionAgentService.parseDecisionResponse from the LLM's raw JSON.
// - DecisionAgentService.resolveCitationSources (added in this migration)
//   matches each Citation.id against the Resource/NewsItem lists already
//   fetched for the prompt and copies the match's contentSource onto the
//   citation.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Renaming the field to `source` instead of `contentSource`: rejected for
//   naming consistency with CivicContent.contentSource — a reader shouldn't
//   have to remember two different field names for "the ContentSource this
//   thing points to."
// =============================================================================
