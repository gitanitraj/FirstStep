package org.firststep.backend.category.service;

import java.util.List;

import org.firststep.backend.category.dto.CategorySummary;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryServiceTest {

    private ResourceService resourceService;
    private NewsService newsService;
    private FlyerService flyerService;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        resourceService = mock(ResourceService.class);
        newsService = mock(NewsService.class);
        flyerService = mock(FlyerService.class);
        service = new CategoryService(resourceService, newsService, flyerService);

        when(resourceService.getAll()).thenReturn(List.of());
        when(newsService.getAll()).thenReturn(List.of());
        when(flyerService.getAll()).thenReturn(List.of());
    }

    private Resource resource(String id, String communityId, String category, String updatedDate) {
        Resource r = new Resource();
        r.id = id;
        r.communityId = communityId;
        r.category = category;
        r.updatedDate = updatedDate;
        return r;
    }

    private Flyer flyer(String id, String communityId, String updatedDate) {
        Flyer f = new Flyer();
        f.id = id;
        f.communityId = communityId;
        f.updatedDate = updatedDate;
        return f;
    }

    /** categoryTags is the editorial classification — the only field categorization reads. */
    private NewsItem newsItem(String id, String communityId, List<String> categoryTags, String published) {
        NewsItem n = new NewsItem();
        n.id = id;
        n.communityId = communityId;
        n.tags = categoryTags;
        n.published = published;
        return n;
    }

    private CategorySummary find(List<CategorySummary> summaries, String key) {
        return summaries.stream().filter(s -> s.key().equals(key)).findFirst().orElseThrow();
    }

    @Test
    void shouldReturnAllTenCategories() {
        List<CategorySummary> summaries = service.getAll(null);
        assertEquals(10, summaries.size());
    }

    @Test
    void shouldCountResourcesMatchingCategoryString() {
        when(resourceService.getAll()).thenReturn(List.of(
                resource("CI-001", "wilmington-de", "Housing Assistance", "2026-01-01"),
                resource("CI-002", "wilmington-de", "Housing", "2026-01-02"),
                resource("CI-003", "wilmington-de", "Food Program", "2026-01-01")));

        CategorySummary housing = find(service.getAll(null), "housing");

        assertEquals(2, housing.resourceCount());
    }

    @Test
    void shouldIncludeFlyersInCommunityEventsCount() {
        when(resourceService.getAll()).thenReturn(List.of(
                resource("CI-001", "wilmington-de", "Recreational", "2026-01-01")));
        when(flyerService.getAll()).thenReturn(List.of(
                flyer("FL-001", "wilmington-de", "2026-01-05"),
                flyer("FL-002", "wilmington-de", "2026-01-06")));

        CategorySummary communityEvents = find(service.getAll(null), "community-events");

        assertEquals(3, communityEvents.resourceCount());
    }

    @Test
    void shouldNotIncludeFlyersInCategoriesThatDontIncludeFlyers() {
        when(flyerService.getAll()).thenReturn(List.of(flyer("FL-001", "wilmington-de", "2026-01-05")));

        CategorySummary housing = find(service.getAll(null), "housing");

        assertEquals(0, housing.resourceCount());
    }

    @Test
    void shouldCapLatestItemsAtThreeSortedByUpdatedDateDescending() {
        when(resourceService.getAll()).thenReturn(List.of(
                resource("CI-001", "wilmington-de", "Employment", "2026-01-01"),
                resource("CI-002", "wilmington-de", "Employment", "2026-01-04"),
                resource("CI-003", "wilmington-de", "Employment", "2026-01-02"),
                resource("CI-004", "wilmington-de", "Employment", "2026-01-03")));

        CategorySummary employment = find(service.getAll(null), "employment");

        assertEquals(4, employment.resourceCount());
        assertEquals(3, employment.latestItems().size());
        assertEquals("CI-002", employment.latestItems().get(0).content().id);
        assertEquals("CI-004", employment.latestItems().get(1).content().id);
        assertEquals("CI-003", employment.latestItems().get(2).content().id);
    }

    @Test
    void shouldFindMostRecentMatchingPolicyUpdate() {
        when(newsService.getAll()).thenReturn(List.of(
                newsItem("NW-001", "wilmington-de", List.of("Housing"), "2026-01-01"),
                newsItem("NW-002", "wilmington-de", List.of("Housing"), "2026-03-01"),
                newsItem("NW-003", "wilmington-de", List.of("Food"), "2026-05-01")));

        CategorySummary housing = find(service.getAll(null), "housing");

        assertEquals("NW-002", housing.latestPolicyUpdate().id);
    }

    @Test
    void shouldReturnNullPolicyUpdateWhenNoNewsMatches() {
        when(newsService.getAll()).thenReturn(List.of(
                newsItem("NW-001", "wilmington-de", List.of("Food"), "2026-01-01")));

        CategorySummary housing = find(service.getAll(null), "housing");

        assertNull(housing.latestPolicyUpdate());
    }

    @Test
    void shouldIgnoreResourceTagsWhenAssociatingNewsWithCategory() {
        // resource_tags are descriptive metadata for search, filtering and AI
        // retrieval — they must never pull a news item into a category.
        NewsItem n = newsItem("NW-001", "wilmington-de", List.of("Food"), "2026-01-01");
        n.resourceTags = List.of("housing", "rental-assistance", "eviction");
        when(newsService.getAll()).thenReturn(List.of(n));

        List<CategorySummary> summaries = service.getAll(null);

        assertNull(find(summaries, "housing").latestPolicyUpdate());
        assertEquals("NW-001", find(summaries, "food").latestPolicyUpdate().id);
    }

    @Test
    void shouldMatchCategoryTagAliasWhenSourceUsesDifferentLabel() {
        // RSS-classified legislation says "Healthcare" where the taxonomy says "Health".
        when(newsService.getAll()).thenReturn(List.of(
                newsItem("NW-001", "wilmington-de", List.of("Healthcare"), "2026-01-01")));

        CategorySummary health = find(service.getAll(null), "health");

        assertEquals("NW-001", health.latestPolicyUpdate().id);
    }

    @Test
    void shouldReturnNullPolicyUpdateWhenOnlySubcategoryTagIsPresent() {
        // A topic-level tag is valid editorially but does not by itself reach a
        // category — the item needs its category label too.
        when(newsService.getAll()).thenReturn(List.of(
                newsItem("NW-001", "wilmington-de", List.of("Rental Assistance"), "2026-01-01")));

        CategorySummary housing = find(service.getAll(null), "housing");

        assertNull(housing.latestPolicyUpdate());
    }

    @Test
    void shouldReturnZeroCountAndNullPolicyUpdateForUtilitiesWithNoData() {
        CategorySummary utilities = find(service.getAll(null), "utilities");

        assertEquals(0, utilities.resourceCount());
        assertTrue(utilities.latestItems().isEmpty());
        assertNull(utilities.latestPolicyUpdate());
    }

    @Test
    void shouldCatchLeftoverCategoriesInCommunitySupport() {
        when(resourceService.getAll()).thenReturn(List.of(
                resource("CI-001", "wilmington-de", "Volunteer", "2026-01-01"),
                resource("CI-002", "wilmington-de", "Entertainment", "2026-01-01"),
                resource("CI-003", "wilmington-de", "Life Skills", "2026-01-01")));

        CategorySummary communitySupport = find(service.getAll(null), "community-support");

        assertEquals(3, communitySupport.resourceCount());
    }

    @Test
    void shouldExcludeResourcesFromNonMatchingCommunity() {
        when(resourceService.getAll()).thenReturn(List.of(
                resource("CI-001", "newark-de", "Housing Assistance", "2026-01-01")));

        CategorySummary housing = find(service.getAll("wilmington-de"), "housing");

        assertEquals(0, housing.resourceCount());
    }
}
