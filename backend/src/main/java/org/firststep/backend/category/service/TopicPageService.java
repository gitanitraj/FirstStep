package org.firststep.backend.category.service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.firststep.backend.category.dto.TopicMetadata;
import org.firststep.backend.category.dto.TopicPage;
import org.firststep.backend.category.model.CategoryDefinition;
import org.firststep.backend.navigation.dto.TopicNavigation;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.model.ContentType;
import org.springframework.stereotype.Service;
import org.firststep.backend.resource.service.ResourceService;

/** IM-05: controlled Topic and Category-wide help browsing over the IM-04 seam. */
@Service
public class TopicPageService {

    private final TaxonomyService taxonomyService;
    private final CategoryContentService categoryContentService;
    private final ResourceService resourceService;

    public TopicPageService(TaxonomyService taxonomyService, CategoryContentService categoryContentService, ResourceService resourceService) {
        this.resourceService = resourceService;
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

    /** Complete inventory, never capped; the client progressively discloses the list. */
    public Optional<TopicPage> getAllHelp(String categoryKey, String communityId) {
        return taxonomyService.findByKey(categoryKey)
                .map(definition -> build(definition, null, "help", communityId));
    }

    private TopicPage build(CategoryDefinition definition, String topic, String slug, String communityId) {
        List<ContentItem> items = categoryContentService.helpFor(definition.key(), topic, communityId).stream()
                // Browse alphabetically as before; equal titles use stable identity, never attention/ranking.
                .sorted(Comparator.comparing(ContentItem::title,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(ContentItem::id, Comparator.nullsLast(String::compareTo)))
                .map(this::withAvailableContact)
                .toList();

        TopicMetadata metadata = new TopicMetadata(
                definition.key(), definition.label(), definition.icon(),
                topic == null ? definition.label() : topic, slug, items.size(), countByType(items));
        List<TopicNavigation> topics = definition.subcategories().stream()
                .map(name -> {
                    List<ContentItem> help = categoryContentService.helpFor(definition.key(), name, communityId);
                    return new TopicNavigation(name,
                            TaxonomyService.topicSlug(name), help.size(), countByType(help));
                }).toList();
        return new TopicPage(metadata, items, topics);
    }

    /** A missing website is not a license to invent a URL: use an explicitly stored phone. */
    private ContentItem withAvailableContact(ContentItem item) {
        if (item.url() != null || item.contentType() != ContentType.RESOURCE) return item;
        String telephone = resourceService.getById(item.id()).stream()
                .flatMap(resource -> resource.phones == null ? java.util.stream.Stream.empty() : resource.phones.stream())
                .filter(phone -> phone != null && phone.number != null && !phone.number.isBlank())
                .map(phone -> "tel:" + phone.number.replaceAll("[^+0-9]", ""))
                .filter(value -> !value.equals("tel:"))
                .findFirst().orElse(null);
        return new ContentItem(item.contentType(), item.id(), item.title(), item.summary(), item.organization(),
                item.location(), item.cost(), item.urgency(), item.date(), telephone, item.imageUrl());
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
