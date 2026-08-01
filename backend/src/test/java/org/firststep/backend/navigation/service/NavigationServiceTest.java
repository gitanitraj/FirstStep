package org.firststep.backend.navigation.service;

import java.util.List;

import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.navigation.dto.CategoryNavigation;
import org.firststep.backend.navigation.dto.TopicNavigation;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.model.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Runs against the REAL taxonomy and navigation.json — the structure under test
 * IS those files, so a fixture would prove only that the aggregation loop works.
 * Content is mocked so each test controls exactly what is being counted.
 */
class NavigationServiceTest {

    private ResourceService resourceService;
    private NewsService newsService;
    private FlyerService flyerService;
    private ExpertAnswerService expertAnswerService;
    private FaqService faqService;
    private RssFeedSource rssFeedSource;
    private NavigationService service;

    @BeforeEach
    void setUp() {
        resourceService = mock(ResourceService.class);
        newsService = mock(NewsService.class);
        flyerService = mock(FlyerService.class);
        expertAnswerService = mock(ExpertAnswerService.class);
        faqService = mock(FaqService.class);
        rssFeedSource = List::of;

        when(resourceService.getAll()).thenReturn(List.of());
        when(newsService.getAll()).thenReturn(List.of());
        when(flyerService.getAll()).thenReturn(List.of());
        when(expertAnswerService.getAll()).thenReturn(List.of());
        when(faqService.getAll()).thenReturn(List.of());

        service = new NavigationService("../app/data", new TaxonomyService("../app/data"),
                resourceService, newsService, flyerService, expertAnswerService, faqService, rssFeedSource);
    }

    private Resource resource(String id, List<String> categoryTags, String subcategory) {
        Resource r = new Resource();
        r.id = id;
        r.categoryTags = categoryTags;
        r.subcategory = subcategory;
        return r;
    }

    private CategoryNavigation find(String key) {
        return service.getAll(null).stream().filter(c -> c.key().equals(key)).findFirst().orElseThrow();
    }

    private TopicNavigation topic(CategoryNavigation category, String name) {
        return category.groups().stream().flatMap(g -> g.topics().stream())
                .filter(t -> t.name().equals(name)).findFirst()
                .orElseGet(() -> category.topics().stream()
                        .filter(t -> t.name().equals(name)).findFirst().orElseThrow());
    }

    // ---- Structure: grouped vs flat ----------------------------------------

    @Test
    void shouldReturnEveryCategoryInTaxonomyOrder() {
        List<CategoryNavigation> all = service.getAll(null);

        assertEquals(10, all.size());
        assertEquals("housing", all.get(0).key());
    }

    @Test
    void shouldGroupTopicsForCategoriesDeclaredInNavigationFile() {
        CategoryNavigation housing = find("housing");

        assertTrue(housing.isGrouped());
        assertEquals("Need Help Right Away", housing.groups().get(0).label());
        assertTrue(housing.topics().isEmpty(), "a grouped category returns no flat topic list");
    }

    @Test
    void shouldReturnFlatTopicListForCategoriesAbsentFromNavigationFile() {
        // Decision 029's rule, now enforced in code rather than only documented.
        CategoryNavigation food = find("food");

        assertFalse(food.isGrouped());
        assertEquals(4, food.topics().size());
        assertEquals("Food Pantry", food.topics().get(0).name());
    }

    @Test
    void shouldCoverEverySubcategoryOfAGroupedCategory() {
        CategoryNavigation housing = find("housing");
        long grouped = housing.groups().stream().mapToLong(g -> g.topics().size()).sum();

        assertEquals(9, grouped);
    }

    @Test
    void shouldExposeSlugsForTopicRouting() {
        assertEquals("child-care-early-learning",
                topic(find("community-support"), "Child Care & Early Learning").slug());
    }

    @Test
    void shouldReturnEmptyForUnknownCategoryKey() {
        assertTrue(service.getByKey("nonexistent", null).isEmpty());
    }

    // ---- Counting ----------------------------------------------------------

    @Test
    void shouldCountAllClassifiedContentTypesNotJustResources() {
        when(resourceService.getAll()).thenReturn(List.of(
                resource("R1", List.of("Housing"), "Emergency Shelter")));
        NewsItem news = new NewsItem();
        news.categoryTags = List.of("Housing");
        when(newsService.getAll()).thenReturn(List.of(news));
        Flyer flyer = new Flyer();
        flyer.categoryTags = List.of("Housing");
        flyer.subcategory = "Eviction Prevention";
        when(flyerService.getAll()).thenReturn(List.of(flyer));

        CategoryNavigation housing = find("housing");

        assertEquals(3, housing.totalCount());
        assertEquals(1, housing.countsByType().get(ContentType.RESOURCE));
        assertEquals(1, housing.countsByType().get(ContentType.NEWS));
        assertEquals(1, housing.countsByType().get(ContentType.FLYER));
    }

    @Test
    void shouldCountRelevantLawsFromTheDiscoveryFeed() {
        NewsItem bill = new NewsItem();
        bill.categoryTags = List.of("Housing");
        bill.contentType = ContentType.LAW;
        service = new NavigationService("../app/data", new TaxonomyService("../app/data"),
                resourceService, newsService, flyerService, expertAnswerService, faqService,
                () -> List.of(bill));

        assertEquals(1, find("housing").countsByType().get(ContentType.LAW));
    }

    @Test
    void shouldCountDualClassifiedContentUnderEveryCategoryItBelongsTo() {
        // FL-002's shape: one flyer, editorially classified as both Housing and
        // Legal, under a topic both categories declare.
        Flyer flyer = new Flyer();
        flyer.categoryTags = List.of("Housing", "Legal");
        flyer.subcategory = "Eviction Prevention";
        when(flyerService.getAll()).thenReturn(List.of(flyer));

        assertEquals(1, topic(find("housing"), "Eviction Prevention").count());
        assertEquals(1, topic(find("legal"), "Eviction Prevention").count());
    }

    @Test
    void shouldScopeTopicCountsToTheirOwnCategory() {
        // A Housing-only item under a dual-declared topic must not appear in Legal.
        when(resourceService.getAll()).thenReturn(List.of(
                resource("R1", List.of("Housing"), "Eviction Prevention")));

        assertEquals(1, topic(find("housing"), "Eviction Prevention").count());
        assertEquals(0, topic(find("legal"), "Eviction Prevention").count());
    }

    @Test
    void shouldReturnTopicsWithNoContentRatherThanHidingThem() {
        // Suppressing empty topics would conceal exactly what
        // validate_navigation.py exists to surface — a canonical topic nothing
        // can reach.
        assertEquals(0, topic(find("housing"), "Emergency Shelter").count());
        assertTrue(find("utilities").topics().isEmpty(), "utilities declares no subcategories");
    }

    @Test
    void shouldFilterByCommunityWhenRequested() {
        Resource local = resource("R1", List.of("Housing"), "Emergency Shelter");
        local.communityId = "wilmington-de";
        Resource elsewhere = resource("R2", List.of("Housing"), "Emergency Shelter");
        elsewhere.communityId = "newark-de";
        when(resourceService.getAll()).thenReturn(List.of(local, elsewhere));

        CategoryNavigation housing = service.getByKey("housing", "wilmington-de").orElseThrow();

        assertEquals(1, housing.totalCount());
    }

    // ---- Read-model discipline ---------------------------------------------

    @Test
    void shouldNotClassifyUnclassifiedContent() {
        // THE read-model constraint. This item is obviously about housing, and
        // NavigationService must count it nowhere — inferring placement here
        // would put an editorial rule in a read model and give "where does this
        // appear?" two answers in two places.
        Resource unclassified = resource("R1", null, null);
        unclassified.category = "Housing Assistance";
        unclassified.title = "Emergency shelter for families facing eviction";
        unclassified.description = "Tenant and landlord housing assistance.";
        when(resourceService.getAll()).thenReturn(List.of(unclassified));

        assertTrue(service.getAll(null).stream().allMatch(c -> c.totalCount() == 0));
    }

    @Test
    void shouldNotUseDescriptiveTagsToPlaceContent() {
        Resource r = resource("R1", List.of("Housing"), null);
        r.tags = List.of("Emergency Shelter", "eviction");
        when(resourceService.getAll()).thenReturn(List.of(r));

        CategoryNavigation housing = find("housing");

        assertEquals(1, housing.totalCount(), "counted in the category by its editorial classification");
        assertEquals(0, topic(housing, "Emergency Shelter").count(),
                "but NOT placed under a topic by a descriptive tag that happens to match");
    }
}
