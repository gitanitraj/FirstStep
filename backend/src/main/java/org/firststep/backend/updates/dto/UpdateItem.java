package org.firststep.backend.updates.dto;

import java.util.List;

/**
 * A normalized, display-ready item for the homepage "Important Updates" feed.
 *
 * The frontend renders this directly — all merging, date-selection, and
 * source/url resolution happens server-side in UpdatesService so the browser
 * never stitches together News and Flyer shapes itself (backend aggregates,
 * frontend displays).
 */
public record UpdateItem(
        String type,     // "news" | "flyer"
        String id,
        String title,
        String summary,
        String date,     // sort/display date (see UpdatesService for selection)
        String source,   // news: contentSource name; flyer: organization
        String url,      // news: contentSource url; flyer: null
        String urgency,  // news urgency; null for flyers
        // Editorial classification, carried through so the Weekly Updates page can
        // group by category server-side (Decision 031). News only — a Flyer has no
        // editorial classification field, and its own `tags` are content
        // descriptors, not navigation.
        List<String> categoryTags
) {
}
