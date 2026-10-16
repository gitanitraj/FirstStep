package org.firststep.backend.search.service;

import java.util.List;

import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.search.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchServiceTest {

    private ResourceService resourceService;
    private NewsService newsService;
    private FlyerService flyerService;
    private SearchService service;

    @BeforeEach
    void setUp() {
        resourceService = mock(ResourceService.class);
        newsService = mock(NewsService.class);
        flyerService = mock(FlyerService.class);
        service = new SearchService(resourceService, newsService, flyerService);
        ReflectionTestUtils.setField(service, "defaultCommunityId", "wilmington-de");

        when(resourceService.getAll()).thenReturn(List.of());
        when(newsService.getAll()).thenReturn(List.of());
        when(flyerService.getAll()).thenReturn(List.of());
    }

    private Resource resource(String id, String communityId, String organization) {
        Resource r = new Resource();
        r.id = id;
        r.communityId = communityId;
        r.organization = organization;
        return r;
    }

    private NewsItem newsItem(String id, String communityId, String title) {
        NewsItem n = new NewsItem();
        n.id = id;
        n.communityId = communityId;
        n.title = title;
        return n;
    }

    private Flyer flyer(String id, String communityId, String title) {
        Flyer f = new Flyer();
        f.id = id;
        f.communityId = communityId;
        f.title = title;
        return f;
    }

    @Test
    void shouldReturnMatchingResourceWhenQueryMatchesOrganization() {
        when(resourceService.getAll()).thenReturn(List.of(resource("CI-001", "wilmington-de", "Beautiful Gate Outreach Center")));

        List<SearchResult> results = service.search("beautiful", null);

        assertEquals(1, results.size());
        assertEquals("resource", results.get(0).type());
        assertEquals("CI-001", results.get(0).content().id);
    }

    @Test
    void shouldReturnMatchingNewsItemWhenQueryMatchesTitle() {
        when(newsService.getAll()).thenReturn(List.of(newsItem("NW-001", "wilmington-de", "New Housing Voucher Program Announced")));

        List<SearchResult> results = service.search("voucher", null);

        assertEquals(1, results.size());
        assertEquals("news", results.get(0).type());
        assertEquals("NW-001", results.get(0).content().id);
    }

    @Test
    void shouldReturnMatchingFlyerWhenQueryMatchesTitle() {
        when(flyerService.getAll()).thenReturn(List.of(flyer("FL-001", "wilmington-de", "Summer Youth Enrichment Program")));

        List<SearchResult> results = service.search("youth", null);

        assertEquals(1, results.size());
        assertEquals("flyer", results.get(0).type());
        assertEquals("FL-001", results.get(0).content().id);
    }

    @Test
    void shouldReturnUnifiedListSortedByScoreDescendingAcrossTypes() {
        Resource r = resource("CI-001", "wilmington-de", "Eviction Help Center");
        r.summary = "Eviction help";
        r.description = "Eviction help";
        NewsItem n = newsItem("NW-001", "wilmington-de", "Eviction Rules Update");
        Flyer f = flyer("FL-002", "wilmington-de", "Know Your Rights: Eviction Prevention");

        when(resourceService.getAll()).thenReturn(List.of(r));
        when(newsService.getAll()).thenReturn(List.of(n));
        when(flyerService.getAll()).thenReturn(List.of(f));

        List<SearchResult> results = service.search("eviction", null);

        assertEquals(3, results.size());
        assertEquals("resource", results.get(0).type());
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).score() >= results.get(i).score());
        }
    }

    @Test
    void shouldExcludeRecordsFromNonMatchingCommunity() {
        when(resourceService.getAll()).thenReturn(List.of(resource("CI-001", "other-city", "Beautiful Gate Outreach Center")));

        List<SearchResult> results = service.search("beautiful", "wilmington-de");

        assertTrue(results.isEmpty());
    }

    @Test
    void shouldFallBackToDefaultCommunityWhenCommunityIdNotProvided() {
        when(resourceService.getAll()).thenReturn(List.of(resource("CI-001", "wilmington-de", "Beautiful Gate Outreach Center")));

        List<SearchResult> results = service.search("beautiful", null);

        assertEquals(1, results.size());
    }

    @Test
    void shouldReturnEmptyListWhenQueryIsBlank() {
        when(resourceService.getAll()).thenReturn(List.of(resource("CI-001", "wilmington-de", "Beautiful Gate Outreach Center")));

        List<SearchResult> results = service.search("", null);

        assertTrue(results.isEmpty());
    }
}
