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
