package org.firststep.backend.category.service;

import java.util.List;

import org.firststep.backend.category.dto.CategoryPage;
import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.navigation.service.NavigationService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.organization.model.Organization;
import org.firststep.backend.organization.repository.OrganizationRepository;
import org.firststep.backend.organization.service.OrganizationService;
import org.firststep.backend.originals.service.ArticleService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.updates.dto.UpdateItem;
import org.firststep.backend.updates.service.UpdatesService;
import org.firststep.backend.shared.service.ContentSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Composition tests for the category page aggregate.
 *
 * <p>Wired with the REAL taxonomy, navigation, updates and organization services
 * over mocked content — the thing under test is how the three collaborators
 * compose, and stubbing them would test only that a record constructor runs.
 */
class CategoryPageServiceTest {

    private ResourceService resourceService;
    private NewsService newsService;
    private FlyerService flyerService;
    private ExpertAnswerService expertAnswerService;
    private FaqService faqService;
    private RssFeedSource rssFeedSource;
    private ArticleService articleService;
    private CategoryPageService service;

    @BeforeEach
    void setUp() {
        resourceService = mock(ResourceService.class);
        newsService = mock(NewsService.class);
        flyerService = mock(FlyerService.class);
        expertAnswerService = mock(ExpertAnswerService.class);
        faqService = mock(FaqService.class);
        articleService = mock(ArticleService.class);
        when(articleService.getPublishable()).thenReturn(List.of());
        rssFeedSource = List::of;
        rebuild();
    }

    /** Rebuilt after stubbing, because both services read their sources eagerly. */
    private void rebuild() {
        TaxonomyService taxonomy = new TaxonomyService("../app/data");
        NavigationService navigation = new NavigationService("../app/data", taxonomy,
                resourceService, newsService, flyerService, expertAnswerService, faqService,
                rssFeedSource, articleService);
        UpdatesService updates = new UpdatesService(newsService, rssFeedSource, flyerService,
                expertAnswerService, faqService, taxonomy, new ContentSourceService("../app/data"));
        OrganizationService organizations = new OrganizationService(resourceService, taxonomy,
                registryFor(resourceService.getAll()));
        CategoryContentService content = new CategoryContentService(taxonomy, resourceService,
                newsService, rssFeedSource, flyerService, expertAnswerService, faqService, articleService);
        service = new CategoryPageService(navigation, updates, organizations, content, mock(CategoryDestinationService.class));
    }

    private static Resource resource(String id, String org, List<String> categoryTags, String subcategory) {
        Resource r = new Resource();
        r.id = id;
        r.organization = org;
        r.categoryTags = categoryTags;
        r.subcategory = subcategory;
        return r;
    }

    private static NewsItem news(String id, String date, ContentType type, List<String> categoryTags) {
        NewsItem n = new NewsItem();
        n.id = id;
        n.title = "Item " + id;
        n.publishDate = date;
        n.contentType = type;
        n.categoryTags = categoryTags;
        return n;
    }

    private CategoryPage housing() {
        return service.getByKey("housing", null).orElseThrow();
    }

    // ---- Structure ---------------------------------------------------------

    @Test
    void shouldReturnEmptyForUnknownCategoryKey() {
        assertTrue(service.getByKey("nonexistent", null).isEmpty());
    }

    @Test
    void shouldCarryGroupedNavigationThroughToThePage() {
        CategoryPage page = housing();

        assertTrue(page.isGrouped());
        assertEquals("Need Help Right Away", page.groups().get(0).label());
        assertTrue(page.topics().isEmpty(), "a grouped category returns no flat topic list");
    }

    @Test
    void shouldCarryFlatNavigationThroughToThePage() {
        CategoryPage page = service.getByKey("food", null).orElseThrow();

        assertFalse(page.isGrouped());
        assertEquals(4, page.topics().size());
        assertTrue(page.groups().isEmpty());
    }

    @Test
    void shouldBuildMetadataFromTheNavigationReadModel() {
        when(resourceService.getAll()).thenReturn(List.of(
                resource("R1", "Housing Alliance", List.of("Housing"), "Emergency Shelter")));
        rebuild();

        CategoryPage page = housing();

        assertEquals("housing", page.metadata().key());
        assertEquals("Housing", page.metadata().label());
        assertEquals(1, page.metadata().totalCount());
        assertEquals(1, page.metadata().countsByType().get(ContentType.RESOURCE));
    }

    // ---- The two halves ----------------------------------------------------

    @Test
    void shouldReachTopiclessContentThroughUpdatesRatherThanTopics() {
        // THE point of the slice. A law is classified into Housing and has no
        // subcategory, so no topic tile can reach it — but the page still shows it.
        when(newsService.getAll()).thenReturn(List.of());
        rssFeedSource = () -> List.of(news("L1", "2026-05-02", ContentType.LAW, List.of("Housing")));
        rebuild();

        CategoryPage page = housing();

        assertEquals(1, page.metadata().totalCount());
        assertEquals(0, page.groups().stream().flatMap(g -> g.topics().stream())
                .mapToInt(t -> t.count()).sum(), "no topic can hold it");
        assertEquals(List.of("L1"), page.updates().stream().map(UpdateItem::id).toList());
    }

    @Test
    void shouldNotPutResourcesInTheUpdatesFeed() {
        // Resources are the Discover half. A standing service is not an event.
        when(resourceService.getAll()).thenReturn(List.of(
                resource("R1", "Housing Alliance", List.of("Housing"), "Emergency Shelter")));
        rebuild();

        CategoryPage page = housing();

        assertEquals(1, page.metadata().totalCount());
        assertTrue(page.updates().isEmpty());
    }

    @Test
    void shouldComposeEveryUpdateContentTypeOntoOnePage() {
        when(newsService.getAll()).thenReturn(List.of(
                news("N1", "2026-05-01", ContentType.NEWS, List.of("Housing"))));
        Flyer flyer = new Flyer();
        flyer.id = "F1";
        flyer.title = "Housing fair";
        flyer.eventDate = "2026-05-03";
        flyer.categoryTags = List.of("Housing");
        when(flyerService.getAll()).thenReturn(List.of(flyer));
        ExpertAnswer expert = new ExpertAnswer();
        expert.id = "E1";
        expert.title = "Counselor session";
        expert.sessionDate = "2026-05-04";
        expert.categoryTags = List.of("Housing");
        when(expertAnswerService.getAll()).thenReturn(List.of(expert));
        rssFeedSource = () -> List.of(news("L1", "2026-05-02", ContentType.LAW, List.of("Housing")));
        rebuild();

        List<ContentType> types = housing().updates().stream().map(UpdateItem::contentType).toList();

        assertEquals(List.of(ContentType.EXPERT, ContentType.FLYER, ContentType.LAW, ContentType.NEWS), types);
    }

    @Test
    void shouldCapUpdatesToKeepThePageADashboard() {
        List<NewsItem> many = new java.util.ArrayList<>();
        for (int i = 0; i < 12; i++) {
            many.add(news("N" + i, String.format("2026-01-%02d", i + 1), ContentType.NEWS, List.of("Housing")));
        }
        when(newsService.getAll()).thenReturn(many);
        rebuild();

        CategoryPage page = housing();

        assertEquals(6, page.updates().size());
        assertEquals(12, page.metadata().totalCount(), "the count still reports everything present");
    }

    // ---- Metadata ----------------------------------------------------------

    @Test
    void shouldReportMostRecentUpdateDate() {
        when(newsService.getAll()).thenReturn(List.of(
                news("N1", "2026-05-01", ContentType.NEWS, List.of("Housing")),
                news("N2", "2026-06-15", ContentType.NEWS, List.of("Housing"))));
        rebuild();

        assertEquals("2026-06-15", housing().metadata().lastUpdated());
    }

    @Test
    void shouldNotReportALastUpdatedDateWhenNothingHasChanged() {
        // A category holding only resources has no editorial dates to report.
        // Resource.updatedDate is a load-date proxy and must never be shown as one.
        when(resourceService.getAll()).thenReturn(List.of(
                resource("R1", "Housing Alliance", List.of("Housing"), "Emergency Shelter")));
        rebuild();

        assertNull(housing().metadata().lastUpdated());
    }

    // ---- Connect -----------------------------------------------------------

    @Test
    void shouldListOrganizationsScopedToTheCategory() {
        when(resourceService.getAll()).thenReturn(List.of(
                resource("R1", "Housing Alliance", List.of("Housing"), "Emergency Shelter"),
                resource("R2", "Food Bank", List.of("Food"), "Food Pantry")));
        rebuild();

        assertEquals(List.of("Housing Alliance"),
                housing().organizations().stream().map(o -> o.name()).toList());
    }

    @Test
    void shouldReturnAUsablePageForACategoryWithNoResources() {
        // utilities has 0 resources and 0 declared subcategories, and must still
        // render — its content arrives entirely through updates.
        when(newsService.getAll()).thenReturn(List.of(
                news("N1", "2026-05-01", ContentType.NEWS, List.of("Utilities"))));
        rebuild();

        CategoryPage page = service.getByKey("utilities", null).orElseThrow();

        assertEquals("Utilities", page.metadata().label());
        assertTrue(page.topics().isEmpty());
        assertTrue(page.groups().isEmpty());
        assertTrue(page.organizations().isEmpty());
        assertEquals(1, page.updates().size());
    }

    @Test
    void shouldFilterEveryPillarByCommunityWhenRequested() {
        Resource local = resource("R1", "Housing Alliance", List.of("Housing"), "Emergency Shelter");
        local.communityId = "wilmington-de";
        Resource elsewhere = resource("R2", "Newark Housing", List.of("Housing"), "Emergency Shelter");
        elsewhere.communityId = "newark-de";
        when(resourceService.getAll()).thenReturn(List.of(local, elsewhere));
        NewsItem localNews = news("N1", "2026-05-01", ContentType.NEWS, List.of("Housing"));
        localNews.communityId = "wilmington-de";
        NewsItem otherNews = news("N2", "2026-05-02", ContentType.NEWS, List.of("Housing"));
        otherNews.communityId = "newark-de";
        when(newsService.getAll()).thenReturn(List.of(localNews, otherNews));
        rebuild();

        CategoryPage page = service.getByKey("housing", "wilmington-de").orElseThrow();

        assertEquals(2, page.metadata().totalCount());
        assertEquals(List.of("N1"), page.updates().stream().map(UpdateItem::id).toList());
    }

    /**
     * A registry containing exactly the organizations these FIXTURES name.
     *
     * <p>An earlier version loaded the real app/data registry, reasoning that
     * category tests use the real taxonomy. That was wrong here: these fixtures
     * invent organizations ("Housing Alliance") that the real registry has never
     * heard of, so every one was filtered out and the assertion saw an empty
     * list. The registry is the fixture's peer, not the environment.
     */
    static OrganizationRepository registryFor(List<org.firststep.backend.resource.model.Resource> resources) {
        List<Organization> orgs = resources.stream()
                .map(r -> r.organization)
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .map(name -> {
                    Organization o = new Organization();
                    o.name = name;
                    o.slug = name.toLowerCase(java.util.Locale.ROOT)
                            .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
                    o.id = "ORG-" + String.format("%03d", Math.abs(name.hashCode()) % 1000);
                    return o;
                })
                .toList();
        return new OrganizationRepository() {
            @Override
            public List<Organization> findAll() {
                return orgs;
            }

            @Override
            public java.util.Optional<Organization> findById(String id) {
                return orgs.stream().filter(o -> id.equals(o.id)).findFirst();
            }

            @Override
            public java.util.Optional<Organization> findBySlug(String slug) {
                return orgs.stream().filter(o -> slug.equals(o.slug)).findFirst();
            }
        };
    }

}
