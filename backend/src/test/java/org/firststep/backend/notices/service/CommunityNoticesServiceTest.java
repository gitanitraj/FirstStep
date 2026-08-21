package org.firststep.backend.notices.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.notices.dto.CommunityNoticesPage;
import org.firststep.backend.notices.model.NoticeView;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.service.ContentSourceService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Community Notices BFF.
 *
 * <p>Fixtures are hand-built content, but the {@link TaxonomyService} and
 * {@link ContentSourceService} are the REAL ones loaded from {@code app/data} —
 * the same choice UpdatesServiceTest made and for the same reason. The kind
 * vocabulary and the sector registry ARE the behavior under test; stubbing them
 * would leave these tests asserting that a filter loop runs.
 */
class CommunityNoticesServiceTest {

    private static ContentSource source(String id) {
        ContentSource cs = new ContentSource();
        cs.id = id;
        cs.name = id;
        cs.url = "https://example.org/" + id;
        return cs;
    }

    private static Flyer flyer(String id, String sourceId, String eventDate, List<String> tags) {
        Flyer f = new Flyer();
        f.id = id;
        f.title = id + " title";
        f.summary = id + " summary";
        f.contentType = ContentType.FLYER;
        f.organization = "Org " + sourceId;
        f.eventDate = eventDate;
        f.tags = tags;
        f.contentSource = source(sourceId);
        return f;
    }

    private static NewsItem news(String id, String sourceId, String publishDate, List<String> tags) {
        NewsItem n = new NewsItem();
        n.id = id;
        n.title = id + " title";
        n.summary = id + " summary";
        n.contentType = ContentType.NEWS;
        n.publishDate = publishDate;
        n.tags = tags;
        n.contentSource = source(sourceId);
        return n;
    }

    private static CommunityNoticesService service(List<Flyer> flyers, List<NewsItem> news) {
        FlyerRepository flyerRepo = new FlyerRepository() {
            @Override
            public List<Flyer> findAll() {
                return flyers;
            }

            @Override
            public Optional<Flyer> findById(String id) {
                return flyers.stream().filter(f -> f.id.equals(id)).findFirst();
            }
        };
        return new CommunityNoticesService(
                new FlyerService(flyerRepo, new ContentSourceService("../app/data")),
                new NewsService(() -> news),
                new TaxonomyService("../app/data"),
                new ContentSourceService("../app/data"));
    }

    private static List<String> idsIn(CommunityNoticesPage page) {
        return page.items().stream().map(ContentItem::id).toList();
    }

    // ---- view selection ---------------------------------------------------

    @Test
    void shouldSelectEventsByKindTagRegardlessOfContentType() {
        CommunityNoticesService service = service(
                List.of(flyer("FL-1", "ministry-of-caring", "2026-09-01", List.of("event"))),
                List.of(news("NP-1", "united-way-delaware", "2026-09-02", List.of("event"))));

        CommunityNoticesPage page = service.getPage(NoticeView.EVENTS);

        // A flyer and a news item, both tagged "event" — the view is a kind lens,
        // not a contentType filter.
        assertEquals(List.of("FL-1", "NP-1"), idsIn(page));
    }

    @Test
    void shouldSelectFlyersByContentTypeRegardlessOfKind() {
        CommunityNoticesService service = service(
                List.of(flyer("FL-1", "ministry-of-caring", "2026-09-01", List.of("meeting"))),
                List.of(news("NP-1", "united-way-delaware", "2026-09-02", List.of("announcement"))));

        CommunityNoticesPage page = service.getPage(NoticeView.FLYERS);

        // The meeting-tagged flyer is in; the news item never is, whatever its kind.
        assertEquals(List.of("FL-1"), idsIn(page));
    }

    @Test
    void shouldPlaceOneItemInBothEventsAndFlyersBecauseViewsAreLensesNotBuckets() {
        // The central design claim: an event flyer answers two different resident
        // questions and belongs in both answers.
        CommunityNoticesService service = service(
                List.of(flyer("FL-1", "westside-family-healthcare", "2026-09-01", List.of("event"))),
                List.of());

        assertTrue(idsIn(service.getPage(NoticeView.EVENTS)).contains("FL-1"));
        assertTrue(idsIn(service.getPage(NoticeView.FLYERS)).contains("FL-1"));
    }

    @Test
    void shouldExcludeItemCarryingNoNoticeKindFromEveryKindView() {
        CommunityNoticesService service = service(
                List.of(),
                List.of(news("NP-1", "united-way-delaware", "2026-09-02", List.of("housing"))));

        assertTrue(idsIn(service.getPage(NoticeView.EVENTS)).isEmpty());
        assertTrue(idsIn(service.getPage(NoticeView.MEETINGS)).isEmpty());
        assertTrue(idsIn(service.getPage(NoticeView.ANNOUNCEMENTS)).isEmpty());
    }

    @Test
    void shouldExcludeItemCarryingTwoNoticeKindsRatherThanGuessingWhichWins() {
        CommunityNoticesService service = service(
                List.of(),
                List.of(news("NP-1", "united-way-delaware", "2026-09-02", List.of("event", "meeting"))));

        // Two kinds is an authoring error the validator blocks. If one silently won
        // here, the record would look correctly filed on whichever page it landed.
        assertTrue(idsIn(service.getPage(NoticeView.EVENTS)).isEmpty());
        assertTrue(idsIn(service.getPage(NoticeView.MEETINGS)).isEmpty());
    }

    // ---- sector scoping ---------------------------------------------------

    @Test
    void shouldExcludeGovernmentProducedNoticeEvenWhenCorrectlyTagged() {
        // Wilmington Housing Authority is a real, resolvable, correctly tagged
        // producer — and still not a community notice. Latest Updates is its page.
        CommunityNoticesService service = service(
                List.of(flyer("FL-1", "wilmington-housing-authority", "2026-09-01", List.of("event"))),
                List.of());

        assertTrue(idsIn(service.getPage(NoticeView.EVENTS)).isEmpty());
        assertTrue(idsIn(service.getPage(NoticeView.FLYERS)).isEmpty());
    }

    @Test
    void shouldExcludeUnresolvableProducerFromEveryView() {
        CommunityNoticesService service = service(
                List.of(flyer("FL-1", "not-a-registered-producer", "2026-09-01", List.of("event"))),
                List.of());

        // Slice I's failure boundary, unchanged: no sector claim can be made, so
        // no sector page claims it. It stays valid CivicContent everywhere else.
        assertTrue(idsIn(service.getPage(NoticeView.EVENTS)).isEmpty());
        assertTrue(idsIn(service.getPage(NoticeView.FLYERS)).isEmpty());
    }

    // ---- ordering ---------------------------------------------------------

    @Test
    void shouldOrderEventsSoonestFirst() {
        CommunityNoticesService service = service(
                List.of(flyer("LATER", "ministry-of-caring", "2026-12-01", List.of("event")),
                        flyer("SOONER", "ministry-of-caring", "2026-09-01", List.of("event"))),
                List.of());

        assertEquals(List.of("SOONER", "LATER"), idsIn(service.getPage(NoticeView.EVENTS)));
    }

    @Test
    void shouldOrderAnnouncementsNewestFirst() {
        CommunityNoticesService service = service(
                List.of(),
                List.of(news("OLDER", "united-way-delaware", "2026-09-01", List.of("announcement")),
                        news("NEWER", "united-way-delaware", "2026-12-01", List.of("announcement"))));

        // Nothing to attend, so recency is the whole signal — the one view that
        // sorts the other way.
        assertEquals(List.of("NEWER", "OLDER"), idsIn(service.getPage(NoticeView.ANNOUNCEMENTS)));
    }

    // ---- page shape -------------------------------------------------------

    @Test
    void shouldCarryCountsForEveryViewOnEveryResponse() {
        CommunityNoticesService service = service(
                List.of(flyer("FL-1", "ministry-of-caring", "2026-09-01", List.of("event"))),
                List.of(news("NP-1", "united-way-delaware", "2026-09-02", List.of("announcement"))));

        // The four nav cards render on every route, so their counts must arrive
        // with the route's own payload or the nav fills in after the page draws.
        CommunityNoticesPage page = service.getPage(NoticeView.MEETINGS);

        assertEquals(1, page.counts().get(NoticeView.EVENTS));
        assertEquals(0, page.counts().get(NoticeView.MEETINGS));
        assertEquals(1, page.counts().get(NoticeView.ANNOUNCEMENTS));
        assertEquals(1, page.counts().get(NoticeView.FLYERS));
    }

    @Test
    void shouldReturnPreviewsWithFullCountsWhenViewIsOverview() {
        CommunityNoticesService service = service(
                List.of(flyer("FL-1", "ministry-of-caring", "2026-09-01", List.of("event")),
                        flyer("FL-2", "ministry-of-caring", "2026-09-02", List.of("event")),
                        flyer("FL-3", "ministry-of-caring", "2026-09-03", List.of("event")),
                        flyer("FL-4", "ministry-of-caring", "2026-09-04", List.of("event"))),
                List.of());

        CommunityNoticesPage page = service.getPage(NoticeView.OVERVIEW);

        CommunityNoticesPage.NoticePreview events = page.previews().stream()
                .filter(p -> p.view() == NoticeView.EVENTS).findFirst().orElseThrow();
        // The landing route is a destination, not a redirect: it shows a real
        // sample AND the true total, so "see all (4)" means something.
        assertEquals(3, events.items().size());
        assertEquals(4, events.count());
        assertTrue(page.items().isEmpty());
    }

    @Test
    void shouldReturnItemsAndNoPreviewsWhenViewIsNotOverview() {
        CommunityNoticesService service = service(
                List.of(flyer("FL-1", "ministry-of-caring", "2026-09-01", List.of("event"))),
                List.of());

        CommunityNoticesPage page = service.getPage(NoticeView.EVENTS);

        assertEquals(List.of("FL-1"), idsIn(page));
        assertTrue(page.previews().isEmpty());
    }

    @Test
    void shouldResolveImageUrlForFlyersSoTheGalleryHasSomethingToShow() {
        Flyer f = flyer("FL-1", "ministry-of-caring", "2026-09-01", List.of("event"));
        f.image = "food-pantry.png";
        CommunityNoticesService service = service(List.of(f), List.of());

        ContentItem item = service.getPage(NoticeView.FLYERS).items().get(0);

        assertTrue(item.imageUrl().endsWith("food-pantry.png"), "got: " + item.imageUrl());
    }

    @Test
    void shouldLeaveImageUrlNullForNewsBecauseNewsCarriesNoPoster() {
        CommunityNoticesService service = service(
                List.of(),
                List.of(news("NP-1", "united-way-delaware", "2026-09-02", List.of("event"))));

        ContentItem item = service.getPage(NoticeView.EVENTS).items().get(0);

        assertNull(item.imageUrl(), "news should not claim an image");
    }
}
