package org.firststep.backend.updates.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.updates.dto.UpdateItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdatesServiceTest {

    private static NewsItem news(String id, String title, String published, String urgency) {
        NewsItem n = new NewsItem();
        n.id = id;
        n.title = title;
        n.summary = title + " summary";
        n.published = published;
        n.urgency = urgency;
        ContentSource cs = new ContentSource();
        cs.name = "Delaware Legislature";
        cs.url = "https://example.gov/" + id;
        n.contentSource = cs;
        return n;
    }

    private static Flyer flyer(String id, String title, String eventDate, String updatedDate) {
        Flyer f = new Flyer();
        f.id = id;
        f.title = title;
        f.summary = title + " summary";
        f.eventDate = eventDate;
        f.updatedDate = updatedDate;
        f.organization = "Community Center";
        return f;
    }

    private static UpdatesService service(List<NewsItem> curated, List<NewsItem> rss, List<Flyer> flyers) {
        NewsService newsService = new NewsService(() -> curated);
        RssFeedSource rssSource = () -> rss;
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
        FlyerService flyerService = new FlyerService(flyerRepo);
        return new UpdatesService(newsService, rssSource, flyerService);
    }

    @Test
    void shouldMergeNewsAndFlyersSortedByDateDescending() {
        UpdatesService service = service(
                List.of(news("N1", "Older news", "2026-01-01", "standard")),
                List.of(),
                List.of(flyer("F1", "Newer flyer", "2026-06-15", "2026-06-01")));

        List<UpdateItem> updates = service.getUpdates();

        assertEquals(2, updates.size());
        assertEquals("F1", updates.get(0).id()); // 2026-06-15 is newest → first
        assertEquals("N1", updates.get(1).id());
    }

    @Test
    void shouldNormalizeNewsItemFields() {
        UpdatesService service = service(
                List.of(news("N1", "A law passed", "2026-05-01", "high")),
                List.of(),
                List.of());

        UpdateItem item = service.getUpdates().get(0);

        assertEquals("news", item.type());
        assertEquals("A law passed", item.title());
        assertEquals("2026-05-01", item.date());
        assertEquals("Delaware Legislature", item.source());
        assertEquals("https://example.gov/N1", item.url());
        assertEquals("high", item.urgency());
    }

    @Test
    void shouldCarryEditorialCategoryTagsForNewsItems() {
        // The Weekly Updates page groups by editorial classification, so the feed
        // has to carry category_tags through (Decision 031).
        NewsItem n = news("N1", "A law passed", "2026-05-01", "high");
        n.tags = List.of("Housing", "Utilities");
        UpdatesService service = service(List.of(n), List.of(), List.of());

        assertEquals(List.of("Housing", "Utilities"), service.getUpdates().get(0).categoryTags());
    }

    @Test
    void shouldLeaveCategoryTagsNullForFlyers() {
        // A Flyer has no editorial classification field; its own tags are content
        // descriptors, not navigation.
        UpdatesService service = service(
                List.of(),
                List.of(),
                List.of(flyer("F1", "Community day", "2026-06-15", "2026-06-01")));

        assertNull(service.getUpdates().get(0).categoryTags());
    }

    @Test
    void shouldUseEventDateForFlyerAndFallBackToUpdatedDate() {
        UpdatesService service = service(
                List.of(),
                List.of(),
                List.of(flyer("F1", "Has event date", "2026-06-15", "2026-06-01"),
                        flyer("F2", "No event date", null, "2026-06-10")));

        List<UpdateItem> updates = service.getUpdates();

        UpdateItem withEvent = updates.stream().filter(u -> u.id().equals("F1")).findFirst().orElseThrow();
        UpdateItem noEvent = updates.stream().filter(u -> u.id().equals("F2")).findFirst().orElseThrow();
        assertEquals("2026-06-15", withEvent.date());
        assertEquals("2026-06-10", noEvent.date()); // fell back to updatedDate
        assertEquals("flyer", withEvent.type());
        assertEquals("Community Center", withEvent.source());
        assertNull(withEvent.url());
        assertNull(withEvent.urgency());
    }

    @Test
    void shouldDedupeNewsByIdAcrossCuratedAndRss() {
        UpdatesService service = service(
                List.of(news("N1", "Curated version", "2026-05-01", "standard")),
                List.of(news("N1", "RSS duplicate", "2026-05-01", "standard")),
                List.of());

        List<UpdateItem> updates = service.getUpdates();

        assertEquals(1, updates.size());
        assertEquals("Curated version", updates.get(0).title()); // curated wins
    }

    @Test
    void shouldCapAtEightItems() {
        List<NewsItem> many = new java.util.ArrayList<>();
        for (int i = 0; i < 12; i++) {
            many.add(news("N" + i, "News " + i, String.format("2026-01-%02d", i + 1), "standard"));
        }
        UpdatesService service = service(many, List.of(), List.of());

        assertEquals(8, service.getUpdates().size());
    }

    @Test
    void shouldSortNullDatesLast() {
        UpdatesService service = service(
                List.of(news("N1", "Dated", "2026-05-01", "standard"),
                        news("N2", "Undated", null, "standard")),
                List.of(),
                List.of());

        List<UpdateItem> updates = service.getUpdates();

        assertEquals("N1", updates.get(0).id());
        assertNull(updates.get(1).date());
        assertTrue(updates.get(1).id().equals("N2"));
    }
}
