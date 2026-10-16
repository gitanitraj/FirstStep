package org.firststep.backend.category.dto;

import java.util.List;

import org.firststep.backend.shared.dto.ContentItem;

/**
 * The fourth and final level of the navigation hierarchy (Decision 021):
 * Category → topic group → topic → <b>CivicContent</b>. This is where the
 * content itself is finally listed.
 *
 * <p>Simpler than {@link CategoryPage} on purpose. A category page answers three
 * questions and needs three sections; a topic page answers one — <i>what is
 * available under this topic?</i> — so it is a header and a list.
 *
 * <p>Resources and flyers retain their existing Topic inventory. IM-03 also admits
 * explicitly Category/Topic-classified FAQ and Expert Answers through the same
 * collection service. Category-only Expert content remains on the Category page;
 * broad Topic/help expansion remains IM-05.
 */
public record TopicPage(
        TopicMetadata metadata,
        List<ContentItem> items
) {
}
