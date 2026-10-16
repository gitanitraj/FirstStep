/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../originals/model/DispositionStatus.java
 * Slice K. See references/decisions.md Decision 048.
 * =============================================================================
 *
 * WHAT IT IS: how a single review flag was disposed of.
 *
 * WHY THREE VALUES AND NOT TWO
 * ----------------------------
 * The tempting simplification is "open / closed", or "open / overridden". Both
 * lose the fact that matters most:
 *
 *     resolved    the concern was RIGHT and got fixed
 *     withdrawn   the concern was WRONG and should not have been raised
 *     overridden  the concern is STILL RIGHT and a human published anyway
 *
 * Those describe three different states of the world. Collapsing withdrawn into
 * overridden claims a human published over a real objection when there was none.
 * Collapsing overridden into withdrawn erases a live objection by blaming the
 * reviewer. Either direction turns the review record into fiction.
 *
 * THE CALIBRATION ARGUMENT
 * ------------------------
 * The withdrawn/overridden split is also the only signal available for judging a
 * REVIEWER. "How often does it flag things that turn out to be fine?" is
 * answerable from withdrawn counts and from nothing else — an overridden flag was
 * a good catch that a human chose to accept, which is the opposite of a false
 * positive even though both end with the article published.
 *
 * This was discovered in use rather than designed in: the Rent Escrow review
 * produced one withdrawal and one resolution in a single round, and the model at
 * the time could record neither.
 *
 * WHAT IS DELIBERATELY ABSENT
 * ---------------------------
 * Any counting or scoring. The dispositions ARE the data; reading it is a future
 * question, and reviewer analytics is an explicit non-goal of Decision 048.
 * Recording the evidence and building the dashboard are separable, and only the
 * first is needed now.
 *
 * There is also no OPEN value. An open flag is one with no disposition at all —
 * see ReviewFlag.isOpen(). A status enum that included OPEN would invite a
 * disposition object with three null fields to say "nothing happened yet."
 *
 * fromKey NEVER DEFAULTS, and the direction matters
 * -------------------------------------------------
 * An unreadable status resolves to empty, which leaves the flag OPEN. Compare
 * ReviewStatus, where an unreadable value leaves an article UNPUBLISHED. Both
 * fail the same way: toward the outcome that keeps a human in the loop.
 * ============================================================================= */

package org.firststep.backend.originals.model;

import java.util.Locale;
import java.util.Optional;

/**
 * How an individual review flag was disposed of.
 *
 * <pre>
 *   open → resolved | withdrawn | overridden
 * </pre>
 *
 * <p><b>These three are not interchangeable, and collapsing any two of them
 * destroys the record's value</b> (Decision 048):
 *
 * <ul>
 *   <li>{@link #RESOLVED} — the concern was LEGITIMATE and was subsequently
 *       addressed, by revising the article or by supplying evidence.</li>
 *   <li>{@link #WITHDRAWN} — the reviewer was WRONG. The flag should not have
 *       been raised.</li>
 *   <li>{@link #OVERRIDDEN} — the concern REMAINS VALID, and an authorized human
 *       chose to publish anyway.</li>
 * </ul>
 *
 * <p><b>WITHDRAWN and OVERRIDDEN must never be conflated.</b> One records a
 * reviewer false positive; the other records a standing objection a human
 * deliberately overruled. Filing a withdrawal as an override would say a human
 * published over a real concern when in fact there was no concern — and filing an
 * override as a withdrawal would erase a live objection by calling the reviewer
 * mistaken. Either way the review history stops being evidence.
 *
 * <p>This distinction is not hypothetical. The Rent Escrow case (see
 * {@code docs/editorial/regression-cases.md}) produced one of each in a single
 * review round.
 *
 * <p><b>Deliberately absent: any counting or scoring.</b> The dispositions ARE
 * the calibration data — how often a reviewer flags things that turn out to be
 * fine is readable from them directly. Reviewer analytics is an explicit
 * non-goal of Decision 048.
 *
 * <p>There is no {@code OPEN} value. An open flag is one with no disposition at
 * all — see {@link ReviewFlag#isOpen()}.
 */
public enum DispositionStatus {

    RESOLVED,
    WITHDRAWN,
    OVERRIDDEN;

    /** JSON spelling — {@code resolved}, {@code withdrawn}, {@code overridden}. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves a stored value, or empty when absent or unrecognized.
     *
     * <p><b>Never defaults</b>, matching {@link ReviewStatus#fromKey}. An
     * unreadable disposition must not quietly retire a valid objection, so it
     * resolves to nothing and the flag stays open.
     */
    public static Optional<DispositionStatus> fromKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (DispositionStatus status : values()) {
            if (status.key().equals(normalized)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
