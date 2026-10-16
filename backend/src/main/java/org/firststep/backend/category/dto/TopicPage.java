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
