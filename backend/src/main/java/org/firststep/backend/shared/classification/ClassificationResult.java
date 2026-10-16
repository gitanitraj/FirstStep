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
