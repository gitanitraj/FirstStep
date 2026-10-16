/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../shared/dto/ContentItem.java
 * Slice F6 (Decision 040); `imageUrl` added in Slice J (Decision 046).
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
 *
 * SLICE J — WHY ADDING `imageUrl` DID NOT REOPEN PER-TYPE SPECIAL-CASING
 * ---------------------------------------------------------------------
 * The Community Notices flyer gallery needs a poster URL, and only flyers have
 * one. That LOOKS like the beginning of a type-specific field creep, so it is
 * worth being precise about why it is not.
 *
 * Every field after `summary` is already optional and populated by whichever
 * types have it — `cost` is resources, `urgency` is news, `date` means "event
 * date" for flyers and "publish date" for news. A card reads the SAME fields for
 * every type and omits what is null; no consumer branches on contentType to
 * decide which fields exist. `imageUrl` follows that established rule rather
 * than introducing a new one.
 *
 * The line that would have crossed it: a `FlyerContentItem` subtype, or a
 * consumer writing `if (contentType == FLYER) { … }` to decide whether to look.
 * NoticeGallery instead asks `item.imageUrl ? … : fallback` — a null check, not
 * a type check.
 *
 * The value is resolved and URL-encoded by FlyerService.imageUrlFor, which stays
 * the single owner of the encoding rule (Decision 025).
 *
 * COST OF THE CHANGE, RECORDED: this is a record, so adding a component broke
 * every call site at compile time — TopicPageService (x2), HomeService, and the
 * test fixtures. That is the constructor-arity tax on records, and it is the
 * GOOD failure mode: the compiler enumerated every place that had to decide what
 * the new field means, and none could be forgotten.
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
        String url,
        /**
         * Resolved, URL-encoded image path — flyers only, null everywhere else.
         *
         * <p>Follows the same rule as {@code cost} and {@code urgency}: fields
         * after {@code summary} are optional and populated by whichever types
         * have them. It does not reintroduce per-type special-casing, because a
         * card reads the same fields for every type and omits what is null.
         *
         * <p>Added in Slice J for the Community Notices flyer gallery, where the
         * IMAGE IS THE CONTENT — a list of flyer titles throws away the thing
         * worth browsing. Resolved by {@code FlyerService.imageUrlFor}, which
         * stays the single owner of the encoding rule.
         */
        String imageUrl
) {
}
