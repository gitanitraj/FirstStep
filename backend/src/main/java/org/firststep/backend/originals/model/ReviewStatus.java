package org.firststep.backend.originals.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Where a First Step Original article sits in editorial review.
 *
 * <pre>
 *   draft → in-review → approved
 *                     → flagged
 * </pre>
 *
 * <p><b>A successfully generated article is not a publishable article.</b> That
 * is the whole reason this enum exists (Decision 048). Generation and evaluation
 * are separate steps, so an article that was written but never reviewed has a
 * state that says so, and public reading surfaces do not serve it.
 *
 * <p><b>There is deliberately no PUBLISHED value.</b> Publication is a separate
 * condition: serving requires {@link #APPROVED}, but an approved article may
 * legitimately be unpublished or scheduled. Folding scheduling into review state
 * would make a scheduling change look like a re-review, and would leave no way to
 * express "reviewed and approved, not yet released".
 *
 * <p><b>Unapproved is not deleted.</b> Excluding an article from public queries
 * says nothing about whether it exists. DRAFT, IN_REVIEW and FLAGGED articles are
 * fully present and are the population a future administrative editorial queue
 * selects from — see {@code ArticleService} for that routing seam.
 */
public enum ReviewStatus {

    /** Written, not yet submitted for review. Never public. */
    DRAFT,

    /** Submitted; review not finished. Never public. */
    IN_REVIEW,

    /** Reviewed and cleared for public serving. The ONLY public state. */
    APPROVED,

    /**
     * Reviewed and held for human disposition.
     *
     * <p>Never served publicly and never routed around: when a human resolves or
     * overrides a flag, the flag and the override record are PRESERVED and the
     * article moves to {@link #APPROVED}. There is no public path that serves a
     * flagged article directly, because the alternative — serving flagged content
     * with a caveat — makes the flag decorative.
     */
    FLAGGED;

    /** JSON spelling — {@code in-review}, {@code approved}, … */
    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /**
     * Resolves a stored value, or empty when it is absent or unrecognized.
     *
     * <p><b>Never defaults.</b> An unreadable status must not become APPROVED by
     * accident, and it must not become DRAFT either — silently downgrading is
     * still guessing. The caller decides, and every caller in this codebase
     * decides "not public".
     */
    public static Optional<ReviewStatus> fromKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (ReviewStatus status : values()) {
            if (status.key().equals(normalized)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
