package org.firststep.backend.navigation.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.firststep.backend.category.model.CategoryDefinition;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.navigation.dto.CategoryNavigation;
import org.firststep.backend.navigation.dto.TopicGroup;
import org.firststep.backend.navigation.dto.TopicNavigation;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Turns the editorial taxonomy and already-classified CivicContent into the
 * structure a category page renders.
 *
 * <h2>This is a READ MODEL, not a business model.</h2>
 *
 * <blockquote>
 * It must not classify content, infer relationships, or contain editorial rules.
 * All editorial decisions belong to the taxonomy and the classification pipeline;
 * this service only aggregates, counts, and shapes data for presentation.
 * </blockquote>
 *
 * <p>Concretely, that means this class reads exactly two fields —
 * {@code categoryTags} and {@code subcategory} — and never looks at text, tags,
 * keywords or content type to decide where something goes. Handed an
 * unclassified item, it counts nothing rather than inferring placement. There is
 * no fallback here, deliberately: a fallback would be an editorial rule wearing a
 * convenience disguise, and the moment one exists, "where does this appear?" has
 * two answers in two places.
 *
 * <p>A read model is only possible because <b>classification is an ingestion
 * concern, not a query concern</b> — by the time content arrives here its
 * editorial classification is settled, so this code can be pure aggregation.
 *
 * <p>Slice F3. No endpoint yet; {@code /api/category/{key}} is F4.
 */
@Service
public class NavigationService {

    // ---- navigation.json shape ------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Navigation(List<NavCategory> categories) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NavCategory(String key, List<NavGroup> groups) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NavGroup(String label, List<String> topics) {
    }

    /** categoryKey -> its authored groups. Absent means "render a flat topic list". */
    private final Map<String, List<NavGroup>> groupsByCategory;

    private final TaxonomyService taxonomyService;
    private final ResourceService resourceService;
    private final NewsService newsService;
    private final FlyerService flyerService;
    private final ExpertAnswerService expertAnswerService;
    private final FaqService faqService;
    private final RssFeedSource rssFeedSource;

    public NavigationService(@Value("${app.data.dir:app/data}") String dataDir,
                             TaxonomyService taxonomyService,
                             ResourceService resourceService,
                             NewsService newsService,
                             FlyerService flyerService,
                             ExpertAnswerService expertAnswerService,
                             FaqService faqService,
                             RssFeedSource rssFeedSource) {
        this.groupsByCategory = loadNavigation(dataDir);
        this.taxonomyService = taxonomyService;
        this.resourceService = resourceService;
        this.newsService = newsService;
        this.flyerService = flyerService;
        this.expertAnswerService = expertAnswerService;
        this.faqService = faqService;
        this.rssFeedSource = rssFeedSource;
    }

    /** Every category, in the taxonomy's authored order. */
    public List<CategoryNavigation> getAll(String communityId) {
        List<CivicContent> content = allClassifiedContent(communityId);
        return taxonomyService.getCategories().stream()
                .map(definition -> build(definition, content))
                .toList();
    }

    /** One category, or empty when the key is not in the taxonomy. */
    public Optional<CategoryNavigation> getByKey(String categoryKey, String communityId) {
        return taxonomyService.findByKey(categoryKey)
                .map(definition -> build(definition, allClassifiedContent(communityId)));
    }

    // ---- Aggregation ----------------------------------------------------

    private CategoryNavigation build(CategoryDefinition definition, List<CivicContent> content) {
        List<CivicContent> inCategory = content.stream()
                .filter(item -> taxonomyService.matchesCategoryTags(definition, item.categoryTags))
                .toList();

        List<NavGroup> authoredGroups = groupsByCategory.get(definition.key());

        List<TopicGroup> groups = new ArrayList<>();
        List<TopicNavigation> flatTopics = new ArrayList<>();

        if (authoredGroups != null) {
            for (NavGroup group : authoredGroups) {
                List<TopicNavigation> topics = group.topics().stream()
                        .map(topic -> topicNavigation(topic, inCategory))
                        .toList();
                groups.add(new TopicGroup(group.label(), topics));
            }
        } else {
            definition.subcategories().forEach(topic -> flatTopics.add(topicNavigation(topic, inCategory)));
        }

        return new CategoryNavigation(
                definition.key(), definition.label(), definition.icon(),
                inCategory.size(), countByType(inCategory),
                List.copyOf(groups), List.copyOf(flatTopics));
    }

    /**
     * Counts within a category, so a topic declared under two categories
     * ("Eviction Prevention" under both Housing and Legal) is counted once per
     * category rather than once globally — which is what a category page needs.
     */
    private TopicNavigation topicNavigation(String topic, List<CivicContent> inCategory) {
        List<CivicContent> inTopic = inCategory.stream()
                .filter(item -> topic.equalsIgnoreCase(item.subcategory))
                .toList();
        return new TopicNavigation(topic, TaxonomyService.topicSlug(topic),
                inTopic.size(), countByType(inTopic));
    }

    private Map<ContentType, Integer> countByType(List<CivicContent> items) {
        Map<ContentType, Integer> counts = new EnumMap<>(ContentType.class);
        for (CivicContent item : items) {
            if (item.contentType != null) {
                counts.merge(item.contentType, 1, Integer::sum);
            }
        }
        return counts;
    }

    /**
     * Every content type, so a category page reflects what is actually on it.
     * RSS legislation arrives via {@link RssFeedSource}, which carries only bills
     * that passed relevance assessment — the ungated signed-bill feed belongs to
     * legislation presentation and has no business in discovery counts.
     */
    private List<CivicContent> allClassifiedContent(String communityId) {
        List<CivicContent> all = new ArrayList<>();
        all.addAll(resourceService.getAll());
        all.addAll(newsService.getAll());
        all.addAll(flyerService.getAll());
        all.addAll(expertAnswerService.getAll());
        all.addAll(faqService.getAll());
        all.addAll(rssFeedSource.getRssItems());
        if (communityId == null || communityId.isBlank()) {
            return all;
        }
        return all.stream().filter(item -> communityId.equals(item.communityId)).toList();
    }

    // ---- Loading --------------------------------------------------------

    /**
     * A missing file is NOT an error, unlike a missing taxonomy. Navigation is
     * presentation: without it every category simply renders a flat topic list,
     * which is a valid page. Without a taxonomy there is no vocabulary at all.
     */
    private static Map<String, List<NavGroup>> loadNavigation(String dataDir) {
        ObjectMapper mapper = new ObjectMapper();
        Navigation navigation = read(mapper, dataDir);
        if (navigation == null || navigation.categories() == null) {
            System.out.println("No navigation.json found — all categories render flat topic lists.");
            return Map.of();
        }
        Map<String, List<NavGroup>> result = new LinkedHashMap<>();
        for (NavCategory category : navigation.categories()) {
            if (category.groups() != null && !category.groups().isEmpty()) {
                result.put(category.key(), category.groups());
            }
        }
        System.out.println("Loaded navigation (" + result.size() + " grouped categories)");
        return result;
    }

    private static Navigation read(ObjectMapper mapper, String dataDir) {
        Path external = Path.of(dataDir, "navigation.json");
        try {
            if (Files.exists(external)) {
                return mapper.readValue(external.toFile(), Navigation.class);
            }
            try (InputStream in = NavigationService.class.getResourceAsStream("/navigation.json")) {
                return in == null ? null : mapper.readValue(in, Navigation.class);
            }
        } catch (Exception e) {
            System.err.println("Failed to load navigation.json: " + e.getMessage());
            return null;
        }
    }
}
