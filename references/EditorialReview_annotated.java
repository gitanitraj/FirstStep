/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../originals/model/EditorialReview.java
 * Slice K. See references/decisions.md Decision 048.
 * =============================================================================
 *
 * WHAT IT IS
 * ----------
 * The review record attached to an article: what editorial decided, when, who
 * decided it, what they found, and whether a human overrode a finding.
 *
 * THE ONE METHOD THAT MATTERS: isApproved()
 * -----------------------------------------
 * Read the direction of the test carefully. It asks whether there is a RECORDED
 * DECISION TO RELEASE — not whether there is a reason to withhold.
 *
 * That inversion is the whole safety property. "Withhold if something is wrong"
 * fails open: anything the system does not understand gets published. "Serve only
 * on an explicit approval" fails closed: a missing review, an unfinished review,
 * a corrupted status and a flagged article all produce the same answer, which is
 * the correct answer to "we do not know whether a human cleared this."
 *
 * WHY THIS IS NOT `verified`
 * --------------------------
 * CivicContent already carries a `verified` boolean, and reusing it was the
 * cheap option. Rejected on two grounds:
 *
 *   1. DIFFERENT QUESTION. `verified` asks "are these details accurate and
 *      current?" This asks "may First Step assert this?" A perfectly accurate
 *      article can still breach the standard by stating a source's opinion as
 *      First Step's own fact — which is exactly the ChristianaCare failure.
 *   2. `verified` IS INERT. Nothing in the backend filters on it. Overloading a
 *      field that no query reads would have produced a gate that looked real in
 *      the data and did nothing in the code.
 *
 * REVIEW PROVENANCE IS ITS OWN FIELD
 * ----------------------------------
 * `reviewer` answers "who evaluated this?" Article.generatedBy answers "who
 * wrote it?" news.json's `author` answers "how did the record get here?"
 * (ingestion — manual/rss/api). Three questions, three fields, and the
 * separation is load-bearing rather than tidy.
 *
 * Draft generation and draft evaluation are separate STEPS even when both are
 * performed by AI. A generator asked to review its own work tends to ratify its
 * own choices: it already holds a rationale for every sentence and will supply
 * it again rather than test it. Two fields is how that separation survives at the
 * data layer, which is the layer a future workflow will read.
 *
 * WHY flags IS A LIST OF OBJECTS AND NOT A LIST OF STRINGS
 * --------------------------------------------------------
 * A flag has to be actionable by a human who did not run the review: which
 * passage, what is wrong, why, and what to do about it. A string carries the
 * "why" and loses the other three. See ReviewFlag.
 *
 * SLICE K CHANGE — THE REVIEW-LEVEL OVERRIDE WAS REMOVED
 * -------------------------------------------------------
 * This class briefly carried `override { reason, by, date }`, and it was wrong at
 * the wrong level. With four flags on one article, a review-level override cannot
 * say WHICH concern a human overruled — and it had no way at all to express a
 * reviewer WITHDRAWING its own flag, which is a different event entirely.
 *
 * That structure was generalized into FlagDisposition and moved down onto the
 * individual flag. Nothing was thrown away: three of its four fields survive with
 * their meanings intact, and the completeness rule it enforced for `reason` alone
 * now covers all four.
 *
 * The lesson is about placement rather than fields: A DECISION BELONGS AT THE
 * LEVEL OF THE THING IT DECIDES. The review's status answers "may this article be
 * published"; a flag's disposition answers "what happened to this concern". They
 * were briefly the same field and could not both be right.
 * ============================================================================= */

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
