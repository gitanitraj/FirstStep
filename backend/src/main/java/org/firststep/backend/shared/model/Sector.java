package org.firststep.backend.shared.model;

import java.util.Locale;
import java.util.Optional;

/**
 * WHO produced a piece of civic content — the property that separates Latest
 * Updates from Community Notices.
 *
 * <p><b>This is a property of the PRODUCER, not of the content.</b> Wilmington
 * Housing Authority publishes both a news item and a flyer; both are government.
 * That single fact is why sector could not be derived from {@link ContentType}
 * and had to become part of the provenance model instead.
 *
 * <p><b>Three different questions, three different fields — do not conflate:</b>
 * <pre>
 *   contentType          what the content IS          NEWS · LAW · FLYER · EXPERT
 *   contentSource.type   what format / how ingested   manual · expert-session · faq
 *   Sector               what kind of producer        government · community · first-step
 * </pre>
 *
 * <p>The registry field is called {@code sector} rather than {@code type}
 * precisely so it cannot be misread as describing the content.
 */
public enum Sector {

    /** Agencies, officials, programs. Feeds Latest Updates. */
    GOVERNMENT,

    /** Churches, nonprofits, community groups. Feeds Community Notices. */
    COMMUNITY,

    /** First Step's own editorial output — First Step Originals (Decision 042). */
    FIRST_STEP;

    /** Registry spelling: {@code government}, {@code community}, {@code first-step}. */
    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /**
     * Parses a registry or URL value. Returns empty rather than throwing or
     * guessing — an unrecognised sector must never silently become one of the
     * real ones (Decision 045).
     */
    public static Optional<Sector> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (Sector sector : values()) {
            if (sector.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(sector);
            }
        }
        return Optional.empty();
    }
}
