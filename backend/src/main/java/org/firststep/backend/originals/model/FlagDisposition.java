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
