package org.firststep.backend.shared.classification;

import java.util.List;

/**
 * What the classification engine decided about one piece of content.
 *
 * <p><b>{@code relevant} is the admission decision, and it is set here — never
 * inferred by a caller.</b> "Should this enter First Step at all?" is a business
 * question, and if six ingestion points each answered it by checking whether
 * {@code categoryTags} happens to be empty, the answer could drift six ways.
 * Callers read {@link #relevant()} and nothing else.
 *
 * <p>The engine determines relevance while producing the result — the canonical
 * constructor <i>is</i> that step. The record stays immutable rather than
 * gaining a setter, matching every other value type in this codebase.
 *
 * <p><b>Both collections are Lists, deliberately.</b> {@code categoryTags} is
 * multi-valued because First Step intentionally supports content belonging to
 * more than one editorial category — "Eviction Prevention" appears under both
 * Housing and Legal. {@code tags} is an ordered List rather than a Set because
 * tag order is an editorial decision and must be preserved.
 *
 * <p><b>Why confidence lives here and not on CivicContent.</b> Confidence is a
 * property of the ACT of classifying, not of the content — the same flyer is not
 * "0.8 confident" in itself. Putting it on the domain model would leak a
 * mechanism detail into the contract every consumer reads, and would raise a
 * question with no good answer: what confidence does a hand-authored editorial
 * classification have? It is the measure supporting the relevance decision, not
 * a separate responsibility of the engine.
 *
 * @param relevant     whether this content should become CivicContent at all
 * @param categoryTags canonical category labels; empty when nothing was confident enough
 * @param subcategory  canonical topic, or null when there was no evidence
 * @param tags         descriptive tags, populated once TagClassifier has merged
 *                     evidence with whatever the content already carried
 * @param confidence   0.0–1.0; 1.0 for a deterministic source-mapping match
 * @param reason       human-readable justification — what makes the conservative
 *                     principle auditable ("source mapping (dscyf-directory):
 *                     Housing Assistance", "matched: eviction, tenant", "no
 *                     category evidence above threshold")
 * @param evidence     the keywords/phrases that actually matched, in match order
 */
public record ClassificationResult(
        boolean relevant,
        List<String> categoryTags,
        String subcategory,
        List<String> tags,
        double confidence,
        String reason,
        List<String> evidence
) {

    /** Content the engine could place. */
    public static ClassificationResult relevant(List<String> categoryTags, String subcategory,
                                                double confidence, String reason, List<String> evidence) {
        return new ClassificationResult(true, categoryTags, subcategory, List.of(), confidence, reason, evidence);
    }

    /**
     * Content that does not belong in First Step. Deliberately carries the reason:
     * "not relevant" with no explanation is unauditable, and tuning the vocabulary
     * depends on being able to see WHY something was turned away.
     */
    public static ClassificationResult irrelevant(String reason) {
        return new ClassificationResult(false, List.of(), null, List.of(), 0.0, reason, List.of());
    }

    /**
     * Content an editor already classified. Relevant by definition — a human
     * placing content IS the relevance decision, and the engine has no mandate to
     * second-guess it.
     */
    public static ClassificationResult editorial(List<String> categoryTags, String subcategory) {
        return new ClassificationResult(true, categoryTags, subcategory, List.of(), 1.0,
                "editorially classified", List.of());
    }

    /** Copy carrying the final descriptive tags, once TagClassifier has merged them. */
    public ClassificationResult withTags(List<String> merged) {
        return new ClassificationResult(relevant, categoryTags, subcategory, merged, confidence, reason, evidence);
    }
}

// =============================================================================
// WHAT THIS RECORD DOES
// =============================================================================
// ClassificationResult is everything the classification engine decided about one
// piece of content: whether it belongs in First Step at all, where it belongs,
// how it should be found, how certain the engine is, and why.
//
// It replaced the earlier `Classification` record, which carried only the
// where/how-certain half. The addition is `relevant` and `reason`.
// =============================================================================

// =============================================================================
// SECTION 1 — WHY relevant IS A FIELD AND NOT A CALLER'S DEDUCTION
// =============================================================================
// Every caller could compute it:
//
//     if (!item.categoryTags.isEmpty()) { admit(item); }     // FORBIDDEN
//
// It would even be correct today. It is forbidden because "should this content
// enter First Step?" is a BUSINESS question, and a business question answered
// independently at six ingestion points will eventually be answered six ways —
// the first time the rule gains a nuance (a confidence floor, a per-source
// policy, an expiry check), five of the six will not learn about it.
//
// Putting it on the result means the rule has exactly one home. Callers read
// relevant() and nothing else; CivicContentClassifier is the only place that
// decides. The instruction behind this was precise: no separate
// RelevanceAssessor class — a service whose entire body is
// `!categoryTags.isEmpty()` is ceremony — but the CONCEPT must be visible rather
// than inferred.
//
// LESSON: the choice is not "class or inline". It is "named in one place or
// re-derived in many". A field on a result object can carry a business concept
// perfectly well; what it cannot do is carry it implicitly.
//
// SECTION 2 — WHY IT IS IMMUTABLE DESPITE "setRelevant" IN THE SKETCH
// -----------------------------------------------------------------------------
// The shape was sketched with a setter. This is a record instead, because a
// record's canonical constructor IS the "set it while producing the result"
// step — the requirement was that the ENGINE determines relevance, not that the
// field be mutable. Every other value type in this codebase (CategoryDefinition,
// CategorySummary, UpdateItem, the navigation DTOs) is immutable; a lone mutable
// one would invite a caller to "correct" a verdict after the fact, which is the
// exact drift Section 1 exists to prevent.
//
// withTags() returns a copy rather than mutating, for the same reason.
//
// SECTION 3 — WHY BOTH COLLECTIONS ARE ORDERED LISTS
// -----------------------------------------------------------------------------
// categoryTags is a List, not a single Category: First Step intentionally
// supports content in more than one editorial category. "Eviction Prevention"
// exists under both Housing and Legal, flyer FL-002 is editorially classified as
// both, and live legislation reaches up to four. A singular field would be a
// regression, not a simplification.
//
// tags is a List, not a Set: tag order is an editorial decision. TagClassifier
// deliberately places hand-authored tags before machine-derived ones, and a Set
// would discard that the moment it was constructed.
//
// SECTION 4 — WHY reason EXISTS
// -----------------------------------------------------------------------------
// `evidence` says WHICH keywords matched. `reason` says what happened, in a
// sentence a human can act on:
//
//     "source mapping (dscyf-directory): Housing Assistance"
//     "matched: eviction, tenant, landlord"
//     "evidence below threshold (score 1 < 2)"
//     "no category keywords matched"
//
// It is what makes the conservative-by-design principle auditable. That
// principle says accuracy improves through better vocabulary, never through
// lower thresholds — which is only actionable if a declined item can tell you
// WHY it was declined. "evidence below threshold (score 1 < 2)" is a vocabulary
// task; "no category keywords matched" is a different one. Without the reason
// both look identical from outside: content that simply is not there.
//
// SECTION 5 — WHY confidence IS HERE AND NOT ON CivicContent
// -----------------------------------------------------------------------------
// Confidence is a property of the ACT of classifying, not of the content. The
// same flyer is not "0.8 confident" in itself. Putting it on the domain model
// would leak a mechanism detail into the contract every consumer reads, and
// would raise a question with no good answer: what confidence does a
// hand-authored editorial classification have? ("The question does not apply" is
// not storable in a double.)
//
// It is also why confidence is NOT counted as a fifth engine responsibility. The
// engine adapts source vocabularies, classifies, tags, and decides relevance;
// confidence is the measure supporting that fourth decision.
//
// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - CategoryClassifier produces it (relevant/irrelevant factories).
// - CivicContentClassifier returns it to callers, using editorial() for content
//   an editor already placed and withTags() to attach the merged descriptive tags.
// - RssFeedService is the only caller that acts on relevant() today — it is the
//   only ingestion source that is automated. Hand-authored files are relevant by
//   definition, because an editor placing content IS the relevance decision.
//
// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - A RelevanceAssessor service. Rejected — see Section 1; it would compute one
//   boolean and add a hop.
// - An enum (RELEVANT / IRRELEVANT / NEEDS_REVIEW) instead of a boolean. The
//   third state is genuinely interesting for a future editorial review queue, and
//   was left out because nothing consumes it yet. Adding it later is additive.
// - Returning null for irrelevant content. Rejected: callers should never branch
//   on null, and "we could not classify this" is a first-class answer here rather
//   than an error path — it is the correct outcome for most legislation.
