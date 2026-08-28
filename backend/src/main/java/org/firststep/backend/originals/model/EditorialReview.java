package org.firststep.backend.originals.model;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The editorial review record attached to a First Step Original article.
 *
 * <p><b>This is the boundary between generating an article and publishing one.</b>
 * Every Original passes an explicit review that tests sourcing, attribution,
 * verification, neutrality, factual support and unsupported inference. The review
 * may FLAG rather than reject, but publication cannot bypass it (Decision 048).
 *
 * <p><b>{@code reviewer} is REVIEW provenance, and it is not the same field as the
 * article's generation provenance.</b> Draft generation and draft evaluation are
 * separate steps even when both are performed by AI, and keeping the two facts in
 * two fields is what preserves that distinction at the data layer — the layer
 * where it most needs to survive. A generator asked to review its own work tends
 * to ratify its own choices; it already holds a rationale for every sentence and
 * will supply it again rather than test it.
 *
 * <p><b>There is deliberately no review-level override field.</b> One existed and
 * was removed: with several flags on one article it could not say WHICH concern a
 * human had overruled, and it had no way to distinguish an override from a
 * reviewer's own withdrawal. Disposition belongs to the individual flag.
 *
 * <p>Note what this class is NOT. It is not {@code verified}, which asks whether
 * an item's details are accurate and current — a different question, and one that
 * nothing in the backend currently gates on. And it carries no PUBLISHED state,
 * because publication is a separate condition from review.
 *
 * <p><b>Absent review is not approval.</b> {@link #isApproved()} answers false for
 * a null status, an unrecognized status and every non-approved state, so the only
 * way to reach a public surface is to have been explicitly approved.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EditorialReview {

    /** Raw stored value — {@code draft}, {@code in-review}, {@code approved}, {@code flagged}. */
    public String status;

    /** {@code YYYY-MM-DD}. Present once a review has actually happened. */
    public String reviewedDate;

    /**
     * Who evaluated the article against the standard — REVIEW provenance.
     * Distinct from who wrote it; see the class comment.
     */
    public String reviewer;

    /**
     * Passage-level findings. Empty or null when the review found nothing.
     *
     * <p>Flags are never deleted once raised. Each carries its own
     * {@link FlagDisposition} recording what happened to it — see
     * {@link ReviewFlag}.
     */
    public List<ReviewFlag> flags;

    /** The resolved status, or empty when absent or unrecognized. Never guesses. */
    public Optional<ReviewStatus> resolvedStatus() {
        return ReviewStatus.fromKey(status);
    }

    /**
     * May a public reading surface serve this article?
     *
     * <p>True only for an explicit {@link ReviewStatus#APPROVED}. An article whose
     * review is missing, unfinished, unreadable or flagged is not public — the
     * question is not "is there a reason to withhold this?" but "is there a
     * recorded decision to release it?".
     */
    public boolean isApproved() {
        return resolvedStatus().filter(ReviewStatus.APPROVED::equals).isPresent();
    }

}
