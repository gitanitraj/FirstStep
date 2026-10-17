/*
 * WHAT: Complete annotated mirror of backend/src/main/java/org/firststep/backend/category/service/CategoryDestinationService.java for IM-08.
 * WHY: Integrate approved selector outcomes with scoped, accessible resident navigation.
 * INTERACTION: Existing eligibility and collection services own admission; the destination
 * projection allowlists cards/links, while React owns responsive presentation and route scope.
 * ALTERNATIVES: Reimplementing selectors or deriving providers from source names would violate
 * predecessor contracts. Tests cover pass-through, actual catalog HTTP, and browser journeys.
 */
package org.firststep.backend.category.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.firststep.backend.category.dto.CategoryCollection;
import org.firststep.backend.category.dto.CategoryDestination;
import org.firststep.backend.category.dto.CategoryDestination.ContextLink;
import org.firststep.backend.category.selection.FeaturedItemSelector;
import org.firststep.backend.category.selection.FeaturedTopicSelector;
import org.firststep.backend.category.selection.FeaturedTopicInventory;
import org.firststep.backend.originals.service.ArticleService;
import org.firststep.backend.shared.dto.ContentItem;
import org.springframework.stereotype.Service;

/** Composes already selected/projection-admitted content. No ranking or raw repository access. */
@Service
public class CategoryDestinationService {
    private final FeaturedTopicSelector topics;
    private final FeaturedItemSelector items;
    private final ArticleService articles;
    private final FeaturedTopicInventory inventory;
    private final TaxonomyService taxonomy;

    public CategoryDestinationService(FeaturedTopicSelector topics, FeaturedItemSelector items,
            ArticleService articles, FeaturedTopicInventory inventory, TaxonomyService taxonomy) {
        this.topics = topics; this.items = items; this.articles = articles;
        this.inventory = inventory; this.taxonomy = taxonomy;
    }

    public CategoryDestination build(String community, String category, List<CategoryCollection> collections) {
        if (community == null || community.isBlank()) throw new IllegalArgumentException("Explicit Community required");
        Set<String> approved = articles.getPublishable().stream().filter(a -> community.equals(a.communityId))
                .map(a -> a.id).collect(java.util.stream.Collectors.toSet());
        String query = "?communityId=" + URLEncoder.encode(community, StandardCharsets.UTF_8);
        var featured = topics.select(community, category).selected().stream().map(topic ->
                new CategoryDestination.FeaturedTopic(topic.name(), topic.key(),
                        items.select(community, category, topic.name()).positions().stream().map(position -> {
                            ContentItem card = position.item().card();
                            String href = destination(card, approved, query);
                            if (href == null) href = "/category/" + category + "/" + topic.key() + query;
                            return new CategoryDestination.FeaturedCard(position.type().name(), card, href);
                        }).toList())).toList();
        // Rail law relevance requires authored scope, never RSS headline-derived classification.
        Set<String> authored = taxonomy.findByKey(category).map(definition -> inventory.current(community, definition)
                .items().stream().map(i -> i.id).collect(java.util.stream.Collectors.toSet())).orElse(Set.of());
        return new CategoryDestination(community, featured,
                links(collections, "first-rate-news", approved, query, null),
                links(collections, "expert-qa", approved, query, null),
                links(collections, "law", approved, query, authored));
    }

    private static List<ContextLink> links(List<CategoryCollection> collections, String key,
            Set<String> approved, String query, Set<String> permittedIds) {
        return collections.stream().filter(c -> c.key().equals(key)).flatMap(c -> c.items().stream())
                .filter(card -> permittedIds == null || permittedIds.contains(card.id()))
                .map(card -> new ContextLink(card.id(), card.title(), destination(card, approved, query), card.date()))
                .filter(link -> link.href() != null).limit(3).toList();
    }

    private static String destination(ContentItem card, Set<String> approved, String query) {
        if (approved.contains(card.id())) return "/originals/" + URLEncoder.encode(card.id(), StandardCharsets.UTF_8) + query;
        String external = safeExternal(card.url());
        if (external != null) return external;
        return card.imageUrl() != null && card.imageUrl().startsWith("/images/seasonal/") ? card.imageUrl() : null;
    }

    /** A source value cannot turn a public card into an executable URL. */
    private static String safeExternal(String value) {
        if (value == null) return null;
        try {
            URI uri = URI.create(value);
            return Set.of("https", "http", "tel").contains(uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(java.util.Locale.ROOT)) ? value : null;
        } catch (IllegalArgumentException ignored) { return null; }
    }
}
