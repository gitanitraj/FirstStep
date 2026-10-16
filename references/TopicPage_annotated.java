/* IM-05 annotated mirror.
 * What: controlled help-only Category/Topic browsing and its resident contract.
 * Why: consume IM-01/03/04 eligibility without duplicating classification or identity.
 * Interaction: shared helpFor, lifecycle-filtered services, HTTP DTOs and React navigation.
 * Alternatives: rejected all-content Topic counts, capped inventory and inferred Flyer activation.
 */
package org.firststep.backend.category.dto;

import java.util.List;

import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.navigation.dto.TopicNavigation;

/** Help-only browse response; topics are controlled narrowing choices with help-only counts. */
public record TopicPage(
        TopicMetadata metadata,
        List<ContentItem> items,
        List<TopicNavigation> topics
) {
    public TopicPage(TopicMetadata metadata, List<ContentItem> items) {
        this(metadata, items, List.of());
    }
}
