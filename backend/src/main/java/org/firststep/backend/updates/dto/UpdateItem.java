package org.firststep.backend.updates.dto;

import java.util.List;

import org.firststep.backend.shared.model.ContentType;

/**
 * A normalized, display-ready item for a cross-type updates feed — the homepage's
 * "Important Updates" and a category page's "Stay Informed" both render this.
 *
 * The frontend renders this directly — all merging, date-selection, and
 * source/url resolution happens server-side in UpdatesService so the browser
 * never stitches together News and Flyer shapes itself (backend aggregates,
 * frontend displays).
 */
public record UpdateItem(
        // DEPRECATED — removed in Slice H. See contentType.
        String type,     // "news" | "flyer" | "expert"
        // The item's place in the CivicContent contract. Added in F5a because a
        // category page must distinguish signed legislation from curated news, and
        // `type` cannot: it reports "news" for both.
        //
        // MIGRATION STATE, time-boxed to Slice H. `type` survives only because the
        // shipped homepage feed reads it, and removing a field mid-slice would
        // break a working page to tidy a DTO.
        //
        // END STATE (Decision 036 exit criterion): Slice H retires
        // UpdateItem.type. contentType becomes the SINGLE semantic identifier for
        // CivicContent, and any presentation labels or badges are derived from it
        // by the frontend — "news"/"flyer"/"expert" are display strings that do
        // not belong in a domain DTO.
        ContentType contentType,
        String id,
        String title,
        String summary,
        String date,     // sort/display date (see UpdatesService for selection)
        String source,   // news: contentSource name; flyer: organization
        String url,      // news: contentSource url; flyer: null
        String urgency,  // news urgency; null for flyers and expert content
        // Editorial classification, carried through so the Weekly Updates page can
        // group by category server-side (Decision 031). An item's own `tags` are
        // content descriptors and never appear here — tags do not drive placement.
        List<String> categoryTags
) {
}
