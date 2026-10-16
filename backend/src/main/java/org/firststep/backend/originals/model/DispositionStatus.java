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
