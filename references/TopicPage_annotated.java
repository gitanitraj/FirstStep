/*
 * IM-03 annotated reference — complete mirror of backend/src/main/java/org/firststep/backend/category/dto/TopicPage.java
 * WHAT: Bounded explicit classification enforcement; existing taxonomy and public shapes retained.
 * WHY: Expert/FAQ Category participation requires explicit valid editorial membership;
 * Topic is optional and must have an explicitly assigned parent when present.
 * INTERACTION: the classifier never manufactures Expert placement. TaxonomyService
 * validates relationships before Category/Topic collections, navigation and scoped Updates.
 * IM-01 status/expiry remains independent; IM-02 stored diagnostics remain untouched.
 * ALTERNATIVE: deriving membership from prose, producer or Topic would conceal missing data.
 * See IM-03 execution report for editorial decisions, audit and regression evidence.
 */
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
