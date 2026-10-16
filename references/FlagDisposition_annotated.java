/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../originals/model/FlagDisposition.java
 * Slice K. Replaces ReviewOverride. See references/decisions.md Decision 048.
 * =============================================================================
 *
 * WHAT IT IS: what happened to one review flag, and who decided it.
 *
 * THIS IS A GENERALIZATION, NOT A NEW STRUCTURE — the design constraint
 * ---------------------------------------------------------------------
 * The previous model had ReviewOverride { reason, by, date } hanging off
 * EditorialReview. The obvious way to add the other two outcomes was to add two
 * more fields beside it:
 *
 *     overrideReason / withdrawReason / resolveReason
 *
 * That was explicitly ruled out, and the reason is worth keeping: three
 * special-case fields make ONE LIFECYCLE look like THREE UNRELATED FEATURES.
 * They drift — one gains an actor, another does not — and no code can ask the
 * simple question "what happened to this flag?" without checking three places.
 *
 * So the existing structure was generalized instead. Three of its four fields
 * survive verbatim in meaning; `by` became `actor`; `status` is the only
 * addition. Two changes beyond that:
 *
 *   RENAMED   because "override" is now one of three values it CARRIES rather
 *             than the thing it IS.
 *   MOVED     from EditorialReview down onto ReviewFlag, because a review-level
 *             override could not say WHICH of several concerns was overruled.
 *             With four flags on the Rent Escrow article, that ambiguity was not
 *             theoretical.
 *
 * WHY status IS A STRING RESOLVED THROUGH AN ENUM
 * -----------------------------------------------
 * A directly-typed enum field looks stronger and is weaker here. Jackson would
 * either throw on an unrecognized value or null it away, and BOTH destroy the
 * required behaviour: an unknown status must stay UNRESOLVED and still be
 * VISIBLE. Holding the raw text means a typo like "withdrawed" can be seen,
 * logged and fixed rather than silently becoming nothing.
 *
 * The vocabulary is still controlled — DispositionStatus is the only authority on
 * what resolves — which is exactly the pattern EditorialReview.status already
 * uses with ReviewStatus. Same shape, same guarantees, one precedent.
 *
 * WHY ALL FOUR FIELDS MUST TRAVEL TOGETHER
 * ----------------------------------------
 * isComplete() requires status, date, actor AND reason. Anything less leaves the
 * flag open.
 *
 * A half-recorded disposition is indistinguishable from a flag someone started
 * thinking about and abandoned. If it counted as settled, a concern could retire
 * itself through incomplete data — the failure mode where nothing looks broken
 * and an objection quietly disappears.
 *
 * This generalizes a rule the old wasOverridden() already enforced for `reason`
 * alone: an override with no stated reason was refused, because it is
 * indistinguishable from the flag having been ignored. That instinct was right;
 * it just applied to one field instead of four.
 * ============================================================================= */

package org.firststep.backend.originals.model;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * What happened to one review flag, and who decided it.
 *
 * <p><b>This is {@code ReviewOverride} generalized, not a new parallel
 * structure.</b> Its {@code date}, {@code actor} (formerly {@code by}) and
 * {@code reason} are that class's fields with their meanings unchanged; the only
 * addition is {@link #status}. It was renamed because "override" is now ONE OF
 * THREE values it can carry rather than the thing it is, and it moved from
 * {@code EditorialReview} down onto {@link ReviewFlag} because a review-level
 * override could not say WHICH of several concerns a human had overruled.
 *
 * <p>Building three structures — {@code overrideReason}, {@code withdrawReason},
 * {@code resolveReason} — was the alternative, and it would have made the three
 * outcomes look like three unrelated features instead of one lifecycle.
 *
 * <p><b>The four fields travel together.</b> {@link #isComplete()} requires all
 * of them, so a disposition missing its actor or its reason does NOT count as a
 * disposition and the flag remains open. A half-recorded disposition is
 * indistinguishable from a flag someone stopped thinking about, and treating it
 * as settled would let a concern retire itself.
 *
 * <p>This carries forward the rule the old {@code wasOverridden()} enforced for
 * {@code reason} alone, now applied to all four — an override without a stated
 * reason was already refused, because it is indistinguishable from the flag
 * having been ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlagDisposition {

    /**
     * The raw stored value — {@code resolved}, {@code withdrawn} or
     * {@code overridden}.
     *
     * <p><b>Held as a String and resolved through {@link DispositionStatus},</b>
     * exactly as {@code EditorialReview.status} is held and resolved through
     * {@link ReviewStatus}. That is what lets an unrecognized value stay
     * UNRESOLVED rather than being guessed or rejected: the vocabulary is
     * controlled by the enum, while the raw text survives deserialization so a
     * typo can be seen and logged instead of vanishing.
     */
    public String status;

    /** {@code YYYY-MM-DD}. */
    public String date;

    /** Who decided — the human or agent that disposed of this flag. */
    public String actor;

    /** Why. Required: an unexplained disposition is not a disposition. */
    public String reason;

    /** The resolved status, or empty when absent or unrecognized. Never guesses. */
    public Optional<DispositionStatus> resolvedStatus() {
        return DispositionStatus.fromKey(status);
    }

    /**
     * True only when all four fields are present and the status is recognized.
     *
     * <p>Anything less leaves the flag OPEN — the safe direction, because the
     * alternative lets an incomplete record silently close a live concern.
     */
    public boolean isComplete() {
        return resolvedStatus().isPresent()
                && notBlank(date)
                && notBlank(actor)
                && notBlank(reason);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
