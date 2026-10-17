/*
 * WHAT: Complete annotated mirror of backend/src/main/java/org/firststep/backend/category/dto/CategoryDestination.java for IM-08.
 * WHY: Integrate approved selector outcomes with scoped, accessible resident navigation.
 * INTERACTION: Existing eligibility and collection services own admission; the destination
 * projection allowlists cards/links, while React owns responsive presentation and route scope.
 * ALTERNATIVES: Reimplementing selectors or deriving providers from source names would violate
 * predecessor contracts. Tests cover pass-through, actual catalog HTTP, and browser journeys.
 */
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
