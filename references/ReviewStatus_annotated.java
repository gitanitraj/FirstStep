/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../originals/model/ReviewStatus.java
 * Slice K. See references/decisions.md Decision 048.
 * =============================================================================
 *
 * WHAT IT IS
 * ----------
 * The four states of editorial review for a First Step Original article. The
 * enum that makes "a generated article is not a publishable article" a fact the
 * type system can express.
 *
 * WHY THERE IS NO `PUBLISHED` VALUE — the design question worth understanding
 * -------------------------------------------------------------------------
 * The obvious fifth value was rejected deliberately. Publication is a SEPARATE
 * CONDITION from review:
 *
 *   - Serving publicly REQUIRES approved.
 *   - But an approved article may legitimately be unpublished, or scheduled.
 *
 * Folding scheduling into review state would mean a scheduling change looked
 * like a re-review, and would leave no way to say "reviewed, approved, not yet
 * released." Two questions, two mechanisms. The enum answers only "what has
 * editorial decided?"
 *
 * WHY fromKey RETURNS Optional AND NEVER DEFAULTS
 * -----------------------------------------------
 * The same never-guess rule as ContentSourceService (045) and NoticeView (046),
 * but with higher stakes: here a wrong default PUBLISHES SOMETHING.
 *
 * Note it does not default to DRAFT either, which looks like the "safe" fallback.
 * Silently downgrading is still guessing — it would let a typo in a status
 * silently unpublish approved work with no error anywhere. Returning empty makes
 * the caller decide, and every caller in this codebase decides "not public."
 *
 * FLAGGED IS A HELD STATE, NOT A WARNING LABEL
 * --------------------------------------------
 * There is no public path that serves a flagged article with a caveat attached.
 * That option was considered and rejected: a flag a reader can see past is
 * decorative, and it shifts an editorial judgment onto the resident. When a
 * human resolves or overrides a flag the article moves to APPROVED and the flag
 * is PRESERVED — see ReviewOverride.
 *
 * UNAPPROVED IS NOT DELETED
 * -------------------------
 * Excluding an article from public queries says nothing about whether it exists.
 * DRAFT / IN_REVIEW / FLAGGED articles are fully present in the repository and
 * are exactly the population a future administrative editorial queue selects
 * from. That routing seam is why the states are modeled richly rather than
 * collapsed to a boolean.
 * ============================================================================= */

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
