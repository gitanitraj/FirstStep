/*
 * IM-01 annotated test reference — full mirror of backend/src/test/java/org/firststep/backend/shared/lifecycle/EditorialProjectionTest.java
 * WHAT: Lifecycle/projection regression evidence.
 * WHY: prove behavior using deterministic time and preserve unrelated test expectations.
 * INTERACTION: see individual test names and fixtures; only lifecycle dependencies were
 * added to existing service construction. No assertion was weakened to hide expiry failures.
 * ALTERNATIVE: machine-clock fixtures would make future runs depend on the date of execution.
 */
package org.firststep.backend.shared.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.firststep.backend.category.service.CategoryContentService;
import org.firststep.backend.category.service.CategoryService;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.repository.ExpertAnswerRepository;
import org.firststep.backend.expert.repository.FaqRepository;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.findhelp.service.FindHelpService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.home.service.HomeService;
import org.firststep.backend.home.service.PathwayService;
import org.firststep.backend.legislation.service.LegislationService;
import org.firststep.backend.navigation.service.NavigationService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedService;
import org.firststep.backend.notices.model.NoticeView;
import org.firststep.backend.notices.service.CommunityNoticesService;
import org.firststep.backend.organization.model.Organization;
import org.firststep.backend.organization.repository.OrganizationRepository;
import org.firststep.backend.organization.service.OrganizationService;
import org.firststep.backend.originals.model.Article;
import org.firststep.backend.originals.model.EditorialReview;
import org.firststep.backend.originals.repository.ArticleRepository;
import org.firststep.backend.originals.service.ArticleService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.search.service.SearchService;
import org.firststep.backend.shared.classification.ClassifierFixture;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.model.Sector;
import org.firststep.backend.shared.service.ContentSourceService;
import org.firststep.backend.updates.service.UpdatesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Synthetic inventory exercises every existing current-availability path before DTO mapping. */
class EditorialProjectionTest {
    private final EditorialLifecycle lifecycle = LifecycleFixture.fixed();
    private final TaxonomyService taxonomy = new TaxonomyService("../app/data");
    private final ContentSourceService sources = new ContentSourceService("../app/data");
    private final List<CivicContent> stored = new ArrayList<>();
    private ResourceService resources;
    private NewsService news;
    private FlyerService flyers;
    private FaqService faqs;
    private ExpertAnswerService experts;
    private ArticleService articles;
    private RssFeedService rss;
    private CategoryContentService categories;
    private NavigationService navigation;
    private OrganizationRepository organizations;

    private <T extends CivicContent> List<T> inventory(java.util.function.Supplier<T> factory, String prefix) {
        List<T> items = new ArrayList<>();
        for (String state : List.of("active", "approaching", "expired", "inactive", "withdrawn")) {
            T item = factory.get();
            item.id = prefix + "-" + state;
            item.title = item.id;
            item.summary = "housing help";
            item.categoryTags = List.of("Housing");
            item.subcategory = "Eviction Prevention";
            item.communityId = "wilmington-de";
            item.contentSource = new ContentSource();
            item.contentSource.id = "first-step";
            item.tags = List.of("event");
            item.status = state.equals("inactive") || state.equals("withdrawn") ? state : "active";
            item.expirationDate = switch (state) {
                case "approaching" -> "2026-11-14";
                case "expired" -> "2026-10-14";
                default -> null;
            };
            if (item instanceof Resource r) { r.organization = item.title; r.organizationId = "ORG-test"; }
            if (item instanceof Flyer f) { f.image = "Sample.png"; f.eventDate = "2000-01-01"; }
            if (item instanceof Article a) {
                a.editorialReview = new EditorialReview();
                a.editorialReview.status = "approved";
            }
            items.add(item);
            stored.add(item);
        }
        return items;
    }

    @BeforeEach
    void setUp() {
        ResourceRepository resourceRepository = mock(ResourceRepository.class);
        List<Resource> resourceItems = inventory(Resource::new, "resource");
        when(resourceRepository.findAll()).thenReturn(resourceItems);
        when(resourceRepository.findById(anyString())).thenAnswer(i -> resourceItems.stream()
                .filter(r -> r.id.equals(i.getArgument(0))).findFirst());
        resources = new ResourceService(resourceRepository, lifecycle);
        NewsRepository newsRepository = () -> inventoryNews;
        inventoryNews = inventory(NewsItem::new, "news");
        // External producer admits the NEWS collection and government updates.
        inventoryNews.forEach(n -> n.contentSource.id = "city-of-wilmington");
        news = new NewsService(newsRepository, lifecycle);
        FlyerRepository flyerRepository = mock(FlyerRepository.class);
        List<Flyer> flyerItems = inventory(Flyer::new, "flyer");
        flyerItems.forEach(f -> f.contentSource.id = "ministry-of-caring");
        when(flyerRepository.findAll()).thenReturn(flyerItems);
        flyers = new FlyerService(flyerRepository, sources, lifecycle);
        FaqRepository faqRepository = mock(FaqRepository.class);
        when(faqRepository.findAll()).thenReturn(inventory(FAQ::new, "faq"));
        faqs = new FaqService(faqRepository, lifecycle);
        ExpertAnswerRepository expertRepository = mock(ExpertAnswerRepository.class);
        when(expertRepository.findAll()).thenReturn(inventory(ExpertAnswer::new, "expert"));
        experts = new ExpertAnswerService(expertRepository, lifecycle);
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        List<Article> articleItems = inventory(Article::new, "article");
        Article draft = new Article();
        draft.id = "draft";
        articleItems.add(draft);
        when(articleRepository.findAll()).thenReturn(articleItems);
        when(articleRepository.findById(anyString())).thenAnswer(i -> articleItems.stream()
                .filter(a -> a.id.equals(i.getArgument(0))).findFirst());
        articles = new ArticleService(articleRepository, lifecycle);
        rss = new RssFeedService(ClassifierFixture.real(), sources, lifecycle);
        List<NewsItem> laws = inventory(NewsItem::new, "law");
        laws.forEach(n -> n.contentType = ContentType.LAW);
        ReflectionTestUtils.setField(rss, "rssItems", laws);
        ReflectionTestUtils.setField(rss, "signedBills", laws);
        categories = new CategoryContentService(taxonomy, resources, news, rss, flyers, experts, faqs, articles);
        navigation = new NavigationService("../app/data", taxonomy, resources, news, flyers, experts, faqs, rss, articles);
        organizations = mock(OrganizationRepository.class);
        Organization org = new Organization();
        org.id = "ORG-test"; org.name = "Test organization"; org.slug = "test";
        when(organizations.findAll()).thenReturn(List.of(org));
        when(organizations.findBySlug("test")).thenReturn(Optional.of(org));
    }

    private List<NewsItem> inventoryNews;

    private void assertCurrentIds(List<String> ids) {
        assertFalse(ids.isEmpty());
        assertTrue(ids.stream().allMatch(id -> id.endsWith("-active") || id.endsWith("-approaching")), ids.toString());
        assertTrue(ids.stream().anyMatch(id -> id.endsWith("-approaching")), ids.toString());
    }

    @Test
    void shouldFilterAllCategoryCollectionsAndCountsIncludingLawAndApprovedArticles() {
        var collections = categories.collectionsFor("housing", "wilmington-de");
        assertEquals(7, collections.size());
        for (var collection : collections) {
            assertEquals(2, collection.count(), collection.key());
            assertCurrentIds(collection.items().stream().map(ContentItem::id).toList());
        }
        assertEquals(14, navigation.getByKey("housing", "wilmington-de").orElseThrow().totalCount());
        assertEquals(2, navigation.getByKey("housing", "wilmington-de").orElseThrow().countsByType().get(ContentType.RESOURCE));
    }

    @Test
    void shouldFilterTopicInventoryAndDynamicTopicCounts() {
        var items = categories.forTopic(taxonomy.findByKey("housing").orElseThrow(), "Eviction Prevention", "wilmington-de");
        assertEquals(4, items.size()); // Existing Topic inventory is RESOURCE + FLYER only.
        assertCurrentIds(items.stream().map(ContentItem::id).toList());
        var nav = navigation.getByKey("housing", "wilmington-de").orElseThrow();
        var topic = nav.groups().stream().flatMap(g -> g.topics().stream())
                .filter(t -> t.name().equals("Eviction Prevention")).findFirst().orElseThrow();
        assertEquals(14, topic.count()); // Navigation already counts all classified content.
    }

    @Test
    void shouldFilterUpdatesNoticesAndTheirCounts() {
        var updates = new UpdatesService(news, rss, flyers, experts, faqs, taxonomy, sources);
        assertCurrentIds(updates.getUpdates().stream().map(i -> i.id()).toList());
        assertCurrentIds(updates.getForCategory("housing", "wilmington-de", 100).stream().map(i -> i.id()).toList());
        var page = updates.getPage(Sector.GOVERNMENT);
        assertEquals(2, page.totalCount());
        var notices = new CommunityNoticesService(flyers, news, taxonomy, sources).getPage(NoticeView.FLYERS);
        assertEquals(2, notices.items().size());
        assertEquals(2, notices.counts().get(NoticeView.FLYERS));
        assertCurrentIds(notices.items().stream().map(ContentItem::id).toList());
    }

    @Test
    void shouldFilterSearchAndCategorySummaryPreviews() {
        assertCurrentIds(new SearchService(resources, news, flyers).search("housing", "wilmington-de")
                .stream().map(r -> r.content().id).toList());
        var housing = new CategoryService(taxonomy, resources, news, flyers).getAll("wilmington-de").stream()
                .filter(c -> c.key().equals("housing")).findFirst().orElseThrow();
        assertEquals(4, housing.resourceCount()); // Existing summary counts RESOURCE + FLYER.
        assertCurrentIds(housing.latestItems().stream().map(r -> r.content().id).toList());
        assertTrue(housing.latestPolicyUpdate().id.endsWith("-active") || housing.latestPolicyUpdate().id.endsWith("-approaching"));
    }

    @Test
    void shouldFilterFindHelpAndOrganizationOfferingsWhileRetainingOrganizationIdentity() {
        var pathways = new PathwayService(taxonomy, "../app/data");
        var page = new FindHelpService(pathways, organizations, resources).getPage();
        assertCurrentIds(page.openToEveryone().stream().map(ContentItem::id).toList());
        assertEquals(2, page.directory().get(0).services().size());
        var organization = new OrganizationService(resources, taxonomy, organizations).getBySlug("test").orElseThrow();
        assertEquals("ORG-test", organization.id());
        assertCurrentIds(organization.resources().stream().map(ContentItem::id).toList());
    }

    @Test
    void shouldFilterHomepageOriginalsLawsAndFlyerPreviews() {
        var page = new HomeService(new PathwayService(taxonomy, "../app/data"), faqs,
                new LegislationService(rss), flyers, articles).getHome();
        assertEquals(4, page.originals().size());
        assertCurrentIds(page.originals().stream().map(ContentItem::id).toList());
        assertEquals(2, page.delawareLaws().size());
        assertEquals(2, page.communityFlyers().size());
        assertTrue(page.communityFlyers().stream().allMatch(f -> f.title().endsWith("-active") || f.title().endsWith("-approaching")));
    }

    @Test
    void shouldRetainHistoricalRecordsAndKeepPublicationStatusIndependent() {
        assertEquals(35, stored.size());
        assertTrue(resources.getById("resource-expired").isPresent());
        assertTrue(resources.getById("resource-inactive").isEmpty());
        assertTrue(articles.getPublishableById("article-expired").isPresent());
        assertTrue(articles.getPublishableById("article-withdrawn").isEmpty());
        assertTrue(articles.getPublishableById("draft").isEmpty());
        assertCurrentIds(articles.getPublishable().stream().map(a -> a.id).toList());
        assertEquals(5, stored.stream().filter(c -> c.contentType == ContentType.RESOURCE).count());
    }
}
