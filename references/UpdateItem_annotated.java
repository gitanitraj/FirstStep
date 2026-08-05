/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../updates/dto/UpdateItem.java
 * Homepage-redesign Step 5b; extended in Slices F1 and F5a.
 * See references/decisions.md Decisions 019, 031, 032, 036.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS RECORD IS
 *   One normalized, display-ready item in a cross-type updates feed. The
 *   homepage's "Important Updates" and a category page's "Stay Informed" both
 *   render this exact shape, so an update card looks and behaves the same
 *   wherever it appears.
 *
 * WHY IT IS FLAT
 *   It is derived from three different domain shapes (NewsItem, Flyer,
 *   ExpertAnswer/FAQ) with genuinely different fields. Flattening happens ONCE,
 *   server-side, in UpdatesService — so the browser never asks "what kind of
 *   object is this?" before it can read a title. Fields are plain camelCase with
 *   NO @JsonProperty, because these are new display fields rather than the
 *   snake_case domain models they derive from.
 * ============================================================================= */

package org.firststep.backend.updates.dto;

import java.util.List;

import org.firststep.backend.shared.model.ContentType;

public record UpdateItem(
        // DEPRECATED — removed in Slice H. See contentType.
        String type,     // "news" | "flyer" | "expert"
        ContentType contentType,
        String id,
        String title,
        String summary,
        String date,     // sort/display date (see UpdatesService for selection)
        String source,   // news: contentSource name; flyer: organization
        String url,      // news: contentSource url; flyer: null
        String urgency,  // news urgency; null for flyers and expert content
        List<String> categoryTags
) {
}

// =============================================================================
// WHY contentType WAS ADDED (F5a) — AND WHY type SURVIVED
// =============================================================================
// A category page must distinguish signed legislation from curated news. Its
// "Stay Informed" column shows both side by side:
//
//     [LAW ] Relating to Rent Increases.
//     [NEWS] SRAP waitlist opens
//
// The original `type` field CANNOT express that — it reports "news" for both,
// because it was written when the feed had exactly two shapes and no domain-wide
// vocabulary existed. ContentType (RESOURCE / NEWS / FLYER / LAW / EXPERT) came
// later with the CivicContent contract, and it is the one that belongs to the
// domain model.
//
// NOTHING IS INFERRED to populate it. Every CivicContent subtype already knows
// its own contentType — NewsItem defaults to NEWS and RssFeedService stamps LAW
// at ingestion — so the mappers read a field rather than guessing from a title.
//
// A MIGRATION STATE, TIME-BOXED — NOT AN ACCEPTED SHAPE. `type` survives only
// because the shipped homepage feed reads it, and removing a field mid-slice
// would break a working page to tidy a DTO.
//
// THE EXIT CRITERION (Decision 036, set by the user):
//
//     Slice H retires UpdateItem.type. contentType becomes the SINGLE semantic
//     identifier for CivicContent. Any presentation labels or badges are derived
//     from contentType by the frontend.
//
//       ContentType: RESOURCE · NEWS · LAW · FLYER · EXPERT
//
// WHY THE LABELS BELONG TO THE FRONTEND. "news"/"flyer"/"expert" are DISPLAY
// STRINGS sitting in a domain DTO — a presentation decision the backend has no
// business making. Once contentType is the only identifier, the backend states
// what a thing IS and the client decides how to render it. That is the same
// separation the CivicContent contract already draws between contentType
// (presentation) and category_tags (placement); `type` was quietly straddling it.
//
// DONE MEANS: `type` deleted; UpdatesService's four mappers no longer pass a
// literal; frontend/src/types/api.ts drops the field; every consumer branches on
// contentType; the F5a tests asserting `type` are UPDATED rather than deleted.
//
// Writing the end state down is the point. An undocumented redundancy is how a
// field becomes permanent by accident — nobody ever decides to keep it.
// =============================================================================

// =============================================================================
// WHY categoryTags IS HERE, AND WHY `tags` IS NOT
// =============================================================================
// Editorial classification is carried through so a page can group by category
// SERVER-SIDE (Decision 031) — the old static frontend derived its own tag set
// with `[...new Set(flatMap(tags))]` and filtered in the browser, which is
// exactly the client-side business logic the BFF principle removed.
//
// Descriptive `tags` are deliberately absent. They drive search, filtering and AI
// retrieval; they never drive placement. Putting them on a display DTO next to
// categoryTags would invite a future consumer to group by whichever one is
// non-empty — reintroducing the precise conflation Decisions 031/032 removed.
//
// HISTORY WORTH KEEPING: Decision 031 passed null here for flyers, correctly,
// because a Flyer had no editorial classification field at the time. Decision 032
// gave flyers real category_tags, so the field is now populated — the rule was
// satisfied by ADDING the missing data, not by relaxing the rule.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Produced solely by updates/service/UpdatesService (four private mappers, one
//   per source shape). Nothing else constructs it, which is what keeps date
//   selection and source/url resolution single-sourced.
// - Returned by GET /api/updates, embedded in HomePayload.updates, and embedded
//   in CategoryPage.updates.
// - Mirrored on the frontend by the UpdateItem interface in
//   frontend/src/types/api.ts.
// =============================================================================
