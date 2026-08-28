/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../originals/model/ReviewFlag.java
 * Slice K. See references/decisions.md Decision 048.
 * =============================================================================
 *
 * WHAT IT IS: one editorial problem found in one passage.
 *
 * THE CORRECTION THIS CLASS ENCODES
 * ---------------------------------
 * Decision 048's own first draft treated article quality as a property of a
 * DOCUMENT. It is not, and a real failure proved it: the ChristianaCare draft was
 * correct in paragraph four and wrong in paragraph six.
 *
 * A boolean on the record cannot express that. Neither can a list of strings —
 * "contains advocacy" tells a human something is wrong somewhere in 600 words. So
 * a flag carries four things, and a reviewer who cannot supply all four has an
 * opinion rather than a finding:
 *
 *     passage         the exact text, quoted — a flag must point at words
 *     issue           which failure mode
 *     reason          why it breaches the standard
 *     recommendation  what to do about it
 *
 * WHY THE ISSUE VOCABULARY MAPS TO FOUR DRIFT AXES
 * ------------------------------------------------
 * An article may not be rendered MORE CERTAIN, MORE POSITIVE, MORE CONSEQUENTIAL
 * or MORE COMPLETE than its evidence supports. Every issue type traces to one of
 * those four, which keeps the vocabulary principled instead of becoming an
 * accreting list of things somebody once noticed.
 *
 * A KNOWN FALSE-POSITIVE MODE, learned in use
 * -------------------------------------------
 * `unattributed-claim` is the canonical failure — a silent promotion from
 * ATTRIBUTED CLAIM to VERIFIED FACT, as when ChristianaCare's spokesperson
 * statement became First Step's own assertion.
 *
 * It is also the issue type most easily raised WRONGLY. In the Rent Escrow
 * review it was raised against text carrying inline statutory citations
 * ("required under 25 Del. C. 5308", "in accordance with City Code Section
 * 34-151(e)") and was withdrawn: INLINE CITATION IS ATTRIBUTION, arguably its
 * strongest form, because a reader can check it. Restating an ordinance while
 * naming the section is not First Step asserting law on its own authority.
 *
 * That withdrawal is preserved in the record rather than deleted — it is the
 * evidence that this issue type needs care.
 *
 * WHY issue AND recommendation ARE STRINGS AND NOT ENUMS
 * ------------------------------------------------------
 * Deliberate, and worth defending because an enum is the reflex here. No code
 * branches on either value yet; the AI review workflow that would PRODUCE them is
 * not built. A controlled vocabulary fixed before its first producer exists tends
 * to be fixed wrongly, and an enum is the expensive thing to change later.
 *
 * Contrast `disposition.status`, which IS enum-controlled — because its three
 * values were specified before any code was written and their distinctness is
 * load-bearing. Vocabulary gets an enum when it is settled, not when it is
 * merely written down.
 *
 * SLICE K ADDITION — THE DISPOSITION, AND WHAT IT PROTECTS
 * --------------------------------------------------------
 * A flag now carries its own lifecycle: open → resolved | withdrawn | overridden.
 *
 * THE FINDING IS IMMUTABLE. passage, issue, reason and recommendation are never
 * rewritten, and the flag is never deleted however it is disposed of. REVIEW
 * HISTORY IS EVIDENCE, NOT DISPOSABLE WORKFLOW STATE.
 *
 * ABSENT MEANS OPEN. There is no status "open" object, because an open flag has
 * no date, no actor and no reason; the explicit form would carry three nulls to
 * encode one bit.
 *
 * isOpen() ERRS TOWARD THE CONCERN STANDING. True for a null disposition AND for
 * an incomplete or unreadable one. Read the direction: it asks whether a decision
 * was confidently recorded, not whether anything was written down.
 *
 * ONE CONSEQUENCE WORTH KNOWING
 * -----------------------------
 * An OPEN flag's passage must exist in the article text — a flag that cannot
 * point at words is an opinion. A RESOLVED flag's need not: resolving usually
 * means revising away exactly the passage it named. The record preserves WHAT WAS
 * FLAGGED, not what currently reads. Rent Escrow's attribution-mismatch flag is
 * the live example — its passage is gone from the body precisely because it was
 * fixed.
 * ============================================================================= */

package org.firststep.backend.originals.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One editorial problem found in one passage of an article.
 *
 * <p><b>Flags attach to PASSAGES, not to articles.</b> This is the correction
 * Decision 048 makes over its own first draft, and it comes from a real failure:
 * the ChristianaCare draft was correct in paragraph four and wrong in paragraph
 * six. Editorial quality is not a property of a document, so a boolean on the
 * record cannot express it and a reviewer must quote the text at issue.
 *
 * <p>Each {@link #issue} maps to one of the four drift axes — an article may not
 * be rendered <b>more certain</b>, <b>more positive</b>, <b>more consequential</b>
 * or <b>more complete</b> than its evidence supports:
 *
 * <ul>
 *   <li>{@code unattributed-claim} — a source's statement presented as First
 *       Step's. The ChristianaCare failure: a spokesperson's forward-looking
 *       remark became First Step's own assertion of the company's plans.</li>
 *   <li>{@code unsupported-inference} — a conclusion beyond the evidence.</li>
 *   <li>{@code advocacy} — evaluative language, unattributed.</li>
 *   <li>{@code certainty-drift} — a hedged source rendered as definite.</li>
 *   <li>{@code scope-drift} — broader or more complete than the evidence.</li>
 *   <li>{@code missing-citation} — a factual claim with no source.</li>
 *   <li>{@code attribution-mismatch} — the citation does not support the claim.</li>
 * </ul>
 *
 * <p><b>The finding itself is never mutated and never deleted.</b> The four
 * fields below record what a reviewer found; {@link #disposition} records what
 * happened to that finding afterwards. A flag is not rewritten when it is
 * resolved, and it is not removed when it is withdrawn — REVIEW HISTORY IS
 * EVIDENCE, NOT DISPOSABLE WORKFLOW STATE (Decision 048).
 *
 * <p>One consequence worth knowing: an OPEN flag's {@link #passage} must exist in
 * the article's text, but a RESOLVED one's need not — resolving a flag often
 * means revising exactly that passage away. The record preserves what was
 * flagged, not what currently reads.
 *
 * <p><b>Why these stay free-form strings for now.</b> An enum would be the usual
 * move, and it is deliberately deferred: no code branches on the issue type yet,
 * the AI review workflow that produces them is not built, and a vocabulary fixed
 * before its first producer exists tends to be fixed wrongly. The list above is
 * the intended vocabulary; it becomes an enum when something reads it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewFlag {

    /** The exact text at issue, quoted. A flag that cannot point at words is an opinion. */
    public String passage;

    /** Which editorial problem this is — see the class comment's vocabulary. */
    public String issue;

    /** Why it breaches the standard, in a sentence a human can act on. */
    public String reason;

    /** What to do: {@code cite}, {@code attribute}, {@code qualify}, {@code remove}, {@code verify}. */
    public String recommendation;

    /**
     * What happened to this flag, or null while it is still OPEN.
     *
     * <p>Absence is how "open" is represented: an open flag has no date, no actor
     * and no reason, so an explicit {@code status: "open"} object would carry
     * three nulls to encode one bit.
     */
    public FlagDisposition disposition;

    /**
     * Is this concern still outstanding?
     *
     * <p>True when there is no disposition AND when the disposition is
     * incomplete or unreadable. Note the direction: anything the system cannot
     * confidently read as a decision leaves the concern STANDING. A corrupted or
     * half-written disposition must never retire a valid objection — the same
     * never-guess rule the article-level status follows, pointed the safe way.
     */
    public boolean isOpen() {
        return disposition == null || !disposition.isComplete();
    }
}
