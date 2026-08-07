package org.firststep.backend.shared.dto;

import org.firststep.backend.shared.model.ContentType;

/**
 * One piece of CivicContent, normalized for display as a card.
 *
 * <p>The frontend renders this directly. Flattening the different domain shapes
 * happens ONCE, server-side, so the browser never asks "what kind of object is
 * this?" before it can read a title — the same rule {@code UpdateItem} follows
 * for the updates feeds.
 *
 * <p><b>{@code contentType} is the only type discriminator.</b> There is no
 * legacy {@code type} string here: this record was written after Decision 036's
 * exit criterion, so it starts where {@code UpdateItem} is heading.
 *
 * <p>Fields after {@code summary} are optional and populated by whichever types
 * have them — a resource has {@code cost} and {@code urgency}, a flyer has a
 * {@code date}. That is normal for a display projection and is exactly how
 * {@code UpdateItem} already behaves; it does not reintroduce per-type
 * special-casing, because the CARD reads the same fields for every type and
 * simply omits what is null.
 */
public record ContentItem(
        ContentType contentType,
        String id,
        String title,
        String summary,
        /** Who provides it — resource organization, flyer organization. */
        String organization,
        /** Where — the first location's city. Never a street address. */
        String location,
        /** Resource only: "free", "sliding scale", … */
        String cost,
        /** Resource only: "emergency", "time-limited", … */
        String urgency,
        /** Flyer event date, news publish date. Resources have no editorial date. */
        String date,
        /** Somewhere to go next — the provider's own site, never First Step's. */
        String url
) {
}
