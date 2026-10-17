package org.firststep.backend.category.dto;

import java.util.List;
import org.firststep.backend.shared.dto.ContentItem;

/** Public IM-08 allowlist: presentation only, never internal selector/editorial evidence. */
public record CategoryDestination(String communityId, List<FeaturedTopic> featuredTopics,
        List<ContextLink> headlines, List<ContextLink> expertLinks, List<ContextLink> laws) {
    public record FeaturedTopic(String name, String slug, List<FeaturedCard> items) {
        public FeaturedTopic { items = List.copyOf(items); }
    }
    public record FeaturedCard(String role, ContentItem item, String href) {}
    public record ContextLink(String id, String title, String href, String date) {}
    public CategoryDestination {
        featuredTopics = List.copyOf(featuredTopics); headlines = List.copyOf(headlines);
        expertLinks = List.copyOf(expertLinks); laws = List.copyOf(laws);
    }
}
