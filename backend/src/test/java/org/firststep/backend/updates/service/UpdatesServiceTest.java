package org.firststep.backend.updates.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.repository.ExpertAnswerRepository;
import org.firststep.backend.expert.repository.FaqRepository;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.updates.dto.UpdateItem;
import org.firststep.backend.shared.service.ContentSourceService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdatesServiceTest {

    private static NewsItem news(String id, String title, String published, String urgency) {
        NewsItem n = new NewsItem();
        n.id = id;
        n.title = title;
        n.summary = title + " summary";
        n.publishDate = published;
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

    private static ExpertAnswer expert(String id, String title, String sessionDate, List<String> categoryTags) {
        ExpertAnswer e = new ExpertAnswer();
        e.id = id;
        e.title = title;
        e.summary = title + " summary";
        e.sessionDate = sessionDate;
        e.expertOrganization = "Legal Aid";
        e.categoryTags = categoryTags;
        return e;
    }

    private static UpdatesService service(List<NewsItem> curated, List<NewsItem> rss, List<Flyer> flyers) {
        return service(curated, rss, flyers, List.of(), List.of());
    }

    private static UpdatesService service(List<NewsItem> curated, List<NewsItem> rss, List<Flyer> flyers,
            List<ExpertAnswer> experts, List<FAQ> faqs) {
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
        ExpertAnswerRepository expertRepo = new ExpertAnswerRepository() {
            @Override
            public List<ExpertAnswer> findAll() {
                return experts;
            }

            @Override
            public Optional<ExpertAnswer> findById(String id) {
                return Optional.empty();
            }
        };
        FaqRepository faqRepo = new FaqRepository() {
            @Override
            public List<FAQ> findAll() {
                return faqs;
            }

            @Override
            public Optional<FAQ> findById(String id) {
                return Optional.empty();
            }
        };
        // The real taxonomy: category scoping IS the taxonomy's matchCategoryTags
        // rule, so a fixture would prove only that the filter loop runs.
        return new UpdatesService(newsService, rssSource, new FlyerService(flyerRepo, new ContentSourceService("../app/data")),
                new ExpertAnswerService(expertRepo), new FaqService(faqRepo),
                new TaxonomyService("../app/data"), new ContentSourceService("../app/data"));
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

        assertEquals(ContentType.NEWS, item.contentType());
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
        n.categoryTags = List.of("Housing", "Utilities");
        UpdatesService service = service(List.of(n), List.of(), List.of());

        assertEquals(List.of("Housing", "Utilities"), service.getUpdates().get(0).categoryTags());
    }

    @Test
    void shouldCarryEditorialCategoryTagsForFlyers() {
        // Decision 031 left this null because a Flyer had no editorial
        // classification field at all. Decision 032 gave flyers category_tags
        // like every other CivicContent type, so the feed carries them through.
        Flyer f = flyer("F1", "Community day", "2026-06-15", "2026-06-01");
        f.categoryTags = List.of("Community Events");
        UpdatesService service = service(List.of(), List.of(), List.of(f));

        assertEquals(List.of("Community Events"), service.getUpdates().get(0).categoryTags());
    }

    @Test
    void shouldLeaveCategoryTagsNullForUnclassifiedFlyer() {
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
        assertEquals(ContentType.FLYER, withEvent.contentType());
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

    // ---- getForCategory — a category page's "Stay Informed" (Slice F5a) ------

    @Test
    void shouldScopeCategoryFeedToItsEditorialClassification() {
        NewsItem housing = news("N1", "Housing news", "2026-05-01", "standard");
        housing.categoryTags = List.of("Housing");
        NewsItem food = news("N2", "Food news", "2026-05-02", "standard");
        food.categoryTags = List.of("Food");
        UpdatesService service = service(List.of(housing, food), List.of(), List.of());

        List<UpdateItem> updates = service.getForCategory("housing", null, 10);

        assertEquals(1, updates.size());
        assertEquals("N1", updates.get(0).id());
    }

    @Test
    void shouldIncludeEveryNonResourceContentTypeInACategoryFeed() {
        NewsItem curated = news("N1", "Curated news", "2026-05-01", "standard");
        curated.categoryTags = List.of("Housing");
        NewsItem bill = news("L1", "A signed bill", "2026-05-02", null);
        bill.categoryTags = List.of("Housing");
        bill.contentType = ContentType.LAW;
        Flyer f = flyer("F1", "Housing fair", "2026-05-03", "2026-05-01");
        f.categoryTags = List.of("Housing");
        ExpertAnswer e = expert("E1", "Counselor session", "2026-05-04", List.of("Housing"));

        List<UpdateItem> updates = service(List.of(curated), List.of(bill), List.of(f), List.of(e), List.of())
                .getForCategory("housing", null, 10);

        assertEquals(4, updates.size());
        assertEquals(List.of(ContentType.EXPERT, ContentType.FLYER, ContentType.LAW, ContentType.NEWS),
                updates.stream().map(UpdateItem::contentType).toList());
    }

    @Test
    void shouldDistinguishSignedLegislationFromCuratedNews() {
        // `type` reports "news" for both, which is why contentType was added: a
        // category page has to badge a law differently from a news item.
        NewsItem bill = news("L1", "A signed bill", "2026-05-02", null);
        bill.categoryTags = List.of("Housing");
        bill.contentType = ContentType.LAW;

        UpdateItem item = service(List.of(), List.of(bill), List.of())
                .getForCategory("housing", null, 10).get(0);

        // The whole point of this test, now that the legacy `type` is gone: the
        // feed carries ONE identifier, and it says LAW. The old String type
        // reported "news" for signed legislation AND curated news, so a resident
        // could not tell a change in the law from an announcement — the conflation
        // Decision 036 existed to end.
        assertEquals(ContentType.LAW, item.contentType());
    }

    @Test
    void shouldNotIncludeUnclassifiedContentInACategoryFeed() {
        // The mirror of NavigationService's rule: no category, no placement. The
        // feed does not fall back to text or tags to rescue an unclassified item.
        NewsItem unclassified = news("N1", "Eviction and rent and landlords", "2026-05-01", "standard");
        unclassified.tags = List.of("Housing");

        assertTrue(service(List.of(unclassified), List.of(), List.of())
                .getForCategory("housing", null, 10).isEmpty());
    }

    @Test
    void shouldReturnEmptyFeedForUnknownCategoryKey() {
        NewsItem n = news("N1", "Housing news", "2026-05-01", "standard");
        n.categoryTags = List.of("Housing");

        assertTrue(service(List.of(n), List.of(), List.of())
                .getForCategory("nonexistent", null, 10).isEmpty());
    }

    @Test
    void shouldRespectTheCallersLimitOnACategoryFeed() {
        List<NewsItem> many = new java.util.ArrayList<>();
        for (int i = 0; i < 12; i++) {
            NewsItem n = news("N" + i, "News " + i, String.format("2026-01-%02d", i + 1), "standard");
            n.categoryTags = List.of("Housing");
            many.add(n);
        }

        List<UpdateItem> updates = service(many, List.of(), List.of()).getForCategory("housing", null, 6);

        assertEquals(6, updates.size());
        assertEquals("N11", updates.get(0).id(), "newest first, then capped");
    }

    @Test
    void shouldFilterCategoryFeedByCommunityWhenRequested() {
        NewsItem local = news("N1", "Local", "2026-05-01", "standard");
        local.categoryTags = List.of("Housing");
        local.communityId = "wilmington-de";
        NewsItem elsewhere = news("N2", "Elsewhere", "2026-05-02", "standard");
        elsewhere.categoryTags = List.of("Housing");
        elsewhere.communityId = "newark-de";

        List<UpdateItem> updates = service(List.of(local, elsewhere), List.of(), List.of())
                .getForCategory("housing", "wilmington-de", 10);

        assertEquals(1, updates.size());
        assertEquals("N1", updates.get(0).id());
    }

    @Test
    void shouldNotIncludeExpertContentInTheHomepageFeed() {
        // The two feeds answer different questions: the homepage is an urgency
        // feed, a category page's is "what changed in this category".
        ExpertAnswer e = expert("E1", "Counselor session", "2026-05-04", List.of("Housing"));
        UpdatesService service = service(List.of(), List.of(), List.of(), List.of(e), List.of());

        assertTrue(service.getUpdates().isEmpty());
        assertFalse(service.getForCategory("housing", null, 10).isEmpty());
    }
}
