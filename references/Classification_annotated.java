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

// =============================================================================
// WHAT THIS RECORD DOES
// =============================================================================
// Classification is what CategoryClassifier returns: the categories, the topic,
// how confident it is, and which keywords actually matched.
// =============================================================================

// =============================================================================
// SECTION 1 — WHY confidence IS NOT A FIELD ON CivicContent
// =============================================================================
// Adding `classificationConfidence` to the CivicContent contract was considered
// and rejected on the strongest available grounds: it would be a category error.
//
// Confidence is a property of the ACT of classifying, not of the content. The
// same flyer is not "0.8 confident" in itself. Putting it on the domain model
// would leak a mechanism detail into the contract every consumer reads, and
// would immediately raise a question with no good answer: what confidence does a
// HAND-AUTHORED editorial classification have? ("The question does not apply" is
// not a value you can store in a double.)
//
// So it lives here, on the result, where it is meaningful.
//
// =============================================================================
// SECTION 2 — CONFIDENCE IS USED, NOT DECORATIVE
// =============================================================================
// A confidence score that nothing consumes is dead weight that looks like rigor.
// This one has two consumers:
//
//   1. GATING — CategoryClassifier declines below MIN_SCORE rather than
//      returning a low-confidence guess.
//   2. TUNING — CivicContentClassifier.summary() reports the classified /
//      editorial / unclassified split at startup, which is how the keyword
//      vocabulary gets improved against real data instead of by intuition. It
//      earned its keep immediately: the first live run showed 134 of 428 bills
//      classified, which prompted the inspection that found "court" scoring
//      below threshold and community-support having no education vocabulary.
//
// `evidence` serves the same purpose at record level — it is the answer to "why
// did this classify that way?", which is the question anyone tuning a keyword
// list actually has. It doubles as TagClassifier's input.
//
// =============================================================================
// SECTION 3 — WHY none() RATHER THAN null
// =============================================================================
// Callers should never branch on null. A shared immutable NONE instance costs
// nothing (the record is immutable and stateless) and means "we could not
// classify this" is a first-class answer rather than an absence — which matters
// because declining IS the correct outcome for a large share of real
// legislation, not an error path.
//
// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Produced by CategoryClassifier.classify().
// - Consumed by CivicContentClassifier, which applies the editorial policy and
//   forwards `evidence` to TagClassifier.
// - Never serialized and never persisted. It exists only during ingestion.
