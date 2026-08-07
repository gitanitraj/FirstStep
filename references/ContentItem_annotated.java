/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../shared/dto/ContentItem.java
 * Slice F6. See references/decisions.md Decision 040.
 * =============================================================================
 *
 * WHAT IT IS: one piece of CivicContent normalized for display as a card.
 *
 * WHY IT LIVES IN shared/dto: ContentCard is meant to be reused (topic pages
 * today; search results, Important Notices and the Front Door's Latest Updates
 * next), so its input shape is not the property of one page package.
 *
 * WHAT IT DELIBERATELY LACKS: a legacy `type` string. This record was defined
 * AFTER Decision 036's exit criterion, so `contentType` is the only
 * discriminator and there is nothing here for Slice H to remove.
 * ============================================================================= */

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

// =============================================================================
// OPTIONAL FIELDS ARE NOT PER-TYPE SPECIAL-CASING
// =============================================================================
// cost and urgency are resource-only; date is flyer/news-only. That looks like
// the special-casing the CivicContent contract abolishes, and it is not:
//
//   The CONTRACT is about the DOMAIN — every content type must answer the same
//   six questions with the same fields, so no consumer has to ask what kind of
//   object it holds before it can classify or place it.
//
//   This is a DISPLAY PROJECTION. The card reads the same fields for every type
//   and omits whatever is null. There is no `if (contentType === …)` anywhere in
//   ContentCard, which is the property that actually matters.
//
// UpdateItem already behaves this way (urgency null for flyers, url null for
// flyers), so the precedent is established rather than newly invented.
// =============================================================================
