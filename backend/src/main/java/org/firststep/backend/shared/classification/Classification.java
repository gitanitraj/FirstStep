package org.firststep.backend.shared.classification;

import java.util.List;

/**
 * The result of classifying one piece of content.
 *
 * <p><b>Why confidence lives here and not on CivicContent.</b> Confidence is a
 * property of the ACT of classifying, not of the content — the same flyer is not
 * "0.8 confident" in itself. Putting it on the domain model would leak a
 * mechanism detail into the contract every consumer reads, and would raise the
 * awkward question of what confidence a hand-authored editorial classification
 * has (the answer is "the question does not apply").
 *
 * <p>It is nonetheless <i>used</i>, not decorative: {@link CategoryClassifier}
 * gates on it, and {@link CivicContentClassifier} reports a startup summary so
 * the keyword vocabulary can be tuned against real data.
 *
 * @param categoryTags canonical category labels, empty when nothing was confident
 *                     enough — the classifier declines rather than guessing
 * @param subcategory  canonical topic, or null when there was no evidence
 * @param confidence   0.0–1.0; 1.0 for a deterministic source-vocabulary match
 * @param evidence     the keywords/phrases that actually matched, in match order.
 *                     Doubles as the descriptive tag source for {@link TagClassifier}
 *                     and as the explanation in the startup report.
 */
public record Classification(
        List<String> categoryTags,
        String subcategory,
        double confidence,
        List<String> evidence
) {

    private static final Classification NONE =
            new Classification(List.of(), null, 0.0, List.of());

    /** Nothing could be classified. Deliberately not null — callers should not branch on null. */
    public static Classification none() {
        return NONE;
    }

    public boolean isEmpty() {
        return categoryTags.isEmpty() && subcategory == null;
    }
}
