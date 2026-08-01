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

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// NavigationService turns the editorial taxonomy plus already-classified
// CivicContent into the structure a category page renders: groups, topics,
// counts, and a per-content-type breakdown. Slice F3.
// =============================================================================

// =============================================================================
// SECTION 1 — "READ MODEL, NOT BUSINESS MODEL" IS THE DESIGN CONSTRAINT
// =============================================================================
// The governing rule, stated before this class was written:
//
//     It must not classify content, infer relationships, or contain editorial
//     rules. All editorial decisions belong to the taxonomy and the
//     classification pipeline; NavigationService only aggregates, counts, and
//     shapes data for presentation.
//
// Concretely: this class reads exactly TWO fields, categoryTags and subcategory.
// It never looks at title, summary, description, tags, keywords or contentType
// to decide where something goes. Handed an unclassified item it counts it
// nowhere.
//
// The temptation this forbids is real and would look reasonable in review. A
// resource with `category: "Housing Assistance"` and no categoryTags is
// OBVIOUSLY housing — three lines here would place it, and a category page would
// look more complete. Those three lines would be an editorial rule living in a
// read model, and "where does this content appear?" would then have two answers
// in two places that drift independently.
//
// The correct fix for such an item is upstream: classify it at ingestion. That
// is what NavigationServiceTest.shouldNotClassifyUnclassifiedContent pins, using
// a resource deliberately stuffed with housing language.
//
// A second test, shouldNotUseDescriptiveTagsToPlaceContent, covers the subtler
// version: an item whose descriptive tags happen to include "Emergency Shelter"
// is counted in its category but NOT under that topic. Descriptive metadata that
// coincidentally matches a topic name is still descriptive metadata.
//
// SECTION 2 — WHY A READ MODEL IS EVEN POSSIBLE HERE
// -----------------------------------------------------------------------------
// Worth naming, because it is the payoff of two prior slices:
//
//     Classification is an INGESTION concern, not a QUERY concern.
//
// Because every item's editorial classification is settled before it reaches
// any service, this class can be pure aggregation. Before Slice F2,
// CategoryService had to translate raw DSCYF categories at request time — a
// query-layer service that knew a vendor's vocabulary. Had that still been true,
// NavigationService would have needed the same knowledge, and "read model" would
// have been aspirational.
//
// SECTION 3 — GROUPED vs FLAT, ENFORCED RATHER THAN DOCUMENTED
// -----------------------------------------------------------------------------
// Decision 029 established: "a category absent from navigation.json renders a
// flat topic list." Until now that was a sentence in a note and a rule in a
// Python validator. Here it is structural — build() returns either groups or
// topics, never both, and CategoryNavigation.isGrouped() is the single place a
// caller asks.
//
// Housing (9 topics) and community-support (12) are grouped; the other eight
// are flat, because a group header above one topic is noise rather than
// hierarchy.
//
// Loading navigation.json is likewise NOT fatal when absent — unlike the
// taxonomy. Navigation is presentation: without it every category renders a flat
// topic list, which is a valid page. Without a taxonomy there is no vocabulary at
// all. Same asymmetry, same reasoning, as SourceMappingService.
//
// SECTION 4 — WHY TOPIC COUNTS ARE SCOPED TO A CATEGORY
// -----------------------------------------------------------------------------
// topicNavigation() filters within `inCategory`, not across all content. This is
// not an optimization — it is the only correct answer for a dual-declared topic.
//
// "Eviction Prevention" is a subcategory of BOTH housing and legal. A
// housing-only resource under that topic must show on the Housing page and NOT
// on the Legal one; flyer FL-002, editorially classified as both, must show on
// both. Counting by topic name globally would give both pages the same number
// and quietly overstate one of them. Two tests pin exactly this pair of cases.
//
// SECTION 5 — WHY EMPTY TOPICS ARE RETURNED
// -----------------------------------------------------------------------------
// A topic with count 0 is still in the response. Filtering them out would make
// pages look tidier and would hide the thing validate_navigation.py exists to
// surface: a canonical topic that no content can reach. An empty topic is
// information — either content is missing or the taxonomy has a branch nobody
// uses — and both are worth seeing. Presentation may choose to dim it; the read
// model should not decide that.
//
// SECTION 6 — WHICH RSS FEED THIS READS, AND WHY IT MATTERS
// -----------------------------------------------------------------------------
// It injects RssFeedSource (relevance-gated) and NOT SignedLegislationSource
// (every signed bill). Category pages are discovery, so they count the ~175
// bills admitted as CivicContent, never the ~253 the engine turned away. Reading
// the wrong interface here would drag pet-store and state-flag legislation into
// residents' category pages — which is precisely the outcome the feed split was
// introduced to make impossible.
//
// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - TaxonomyService supplies categories, subcategories and topicSlug().
// - Six content sources supply already-classified CivicContent.
// - Slice F4's GET /api/category/{key} will be a thin controller over getByKey();
//   there is no endpoint in F3 on purpose, per the standing BFF pattern.
// - /api/home is untouched: CategorySummary keeps its resources+flyers count
//   until F4 reshapes the BFF, which is what keeps the Editorial Stability
//   Invariant trivially checkable across this slice.
//
// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Fold this into CategoryService. Rejected: that service answers "summarize
//   every category for the homepage"; this one answers "shape one category for
//   its page". Merging them would produce a service with two response shapes and
//   two audiences.
// - Precompute counts at load. Rejected: counts must reflect live content
//   (RSS refreshes hourly), and Decision 029 already established that counts are
//   computed at request time rather than stored — storing them in navigation.json
//   was explicitly rejected there.
// - Let NavigationService fall back to classification for unclassified items.
//   Rejected — Section 1. This is the whole constraint.
