/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../category/dto/TopicPage.java  (Slice F6)
 * See references/decisions.md Decision 040.
 * NB: references/TopicPage_annotated.tsx is the FRONTEND page of the same name.
 * ============================================================================= */

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
 * <p><b>Only resources and flyers reach a topic page</b>, because they are the
 * only content types that carry a {@code subcategory} (229/229 and 7/7; news,
 * legislation and expert answers carry none). That is not a limitation of this
 * endpoint — it is the same fact that made the category page an aggregate, seen
 * from the other side.
 */
public record TopicPage(
        TopicMetadata metadata,
        List<ContentItem> items
) {
}
