package org.firststep.backend.category.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.firststep.backend.category.dto.TopicMetadata;
import org.firststep.backend.category.dto.TopicPage;
import org.firststep.backend.category.model.CategoryDefinition;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.model.ContentType;
import org.springframework.stereotype.Service;

/**
 * The topic page's BFF (Slice F6) — the fourth level of the navigation
 * hierarchy, where CivicContent is finally listed.
 *
 * <p>Like every other read path here it reads <b>editorial classification
 * only</b>: an item appears under a topic when its {@code categoryTags} match the
 * category AND its {@code subcategory} equals the topic. No text matching, no
 * tags, no inference. Classification is an ingestion concern.
 *
 * <p><b>Content resolution moved to {@link CategoryContentService} (Decision
 * 051).</b> This service previously held private {@code CivicContent -> ContentItem}
 * mappers and queried ResourceService and FlyerService itself. The category
 * destination needed the same mapping over more sources, which is the second use an
 * abstraction needs before it earns its name. What remains here is what was always
 * this service's own: resolving a slug to a canonical topic, and shaping the page.
 *
 * <p>The behaviour is unchanged by that move — same predicate, same alphabetical
 * order, same items — and TopicPageServiceTest needed no edits, which is the
 * empirical check on the claim.
 */
@Service
public class TopicPageService {

    private final TaxonomyService taxonomyService;
    private final CategoryContentService categoryContentService;

    public TopicPageService(TaxonomyService taxonomyService, CategoryContentService categoryContentService) {
        this.taxonomyService = taxonomyService;
        this.categoryContentService = categoryContentService;
    }

    /**
     * One topic, or empty when either the category key or the topic slug is
     * unknown. Both resolve through the taxonomy, so a topic that is not declared
     * under that category 404s even if another category declares it — which is
     * what makes "Eviction Prevention" two distinct pages under Housing and Legal
     * rather than one ambiguous page.
     */
    public Optional<TopicPage> getByKey(String categoryKey, String topicSlug, String communityId) {
        CategoryDefinition definition = taxonomyService.findByKey(categoryKey).orElse(null);
        if (definition == null) {
            return Optional.empty();
        }
        Optional<String> topic = taxonomyService.findTopicBySlug(categoryKey, topicSlug);
        if (topic.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(build(definition, topic.get(), topicSlug, communityId));
    }

    private TopicPage build(CategoryDefinition definition, String topic, String slug, String communityId) {
        List<ContentItem> items = categoryContentService.forTopic(definition, topic, communityId);

        TopicMetadata metadata = new TopicMetadata(
                definition.key(), definition.label(), definition.icon(),
                topic, slug, items.size(), countByType(items));
        return new TopicPage(metadata, items);
    }

    private static Map<ContentType, Integer> countByType(List<ContentItem> items) {
        Map<ContentType, Integer> counts = new EnumMap<>(ContentType.class);
        for (ContentItem item : items) {
            if (item.contentType() != null) {
                counts.merge(item.contentType(), 1, Integer::sum);
            }
        }
        return counts;
    }
}
