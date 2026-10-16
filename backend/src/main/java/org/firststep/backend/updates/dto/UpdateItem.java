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
        // THE SOLE SEMANTIC CONTENT-TYPE IDENTIFIER.
        //
        // A legacy `String type` sat above this from Step 5b until Decision 045
        // deleted it. It reported "news" for BOTH curated news and signed
        // legislation, so a resident could not tell a change in the law from an
        // announcement — which is why F5a added contentType alongside it, and why
        // the overlap was always a time-boxed migration rather than an accepted
        // shape (Decision 036's exit criterion).
        //
        // Presentation labels and badges are derived from this BY THE FRONTEND.
        // "news"/"flyer"/"expert" were display strings, and display strings do not
        // belong in a domain DTO.
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
