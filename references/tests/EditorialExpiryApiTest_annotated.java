/*
 * IM-01 annotated test reference — full mirror of backend/src/test/java/org/firststep/backend/shared/lifecycle/EditorialExpiryApiTest.java
 * WHAT: Lifecycle/projection regression evidence.
 * WHY: prove behavior using deterministic time and preserve unrelated test expectations.
 * INTERACTION: see individual test names and fixtures; only lifecycle dependencies were
 * added to existing service construction. No assertion was weakened to hide expiry failures.
 * ALTERNATIVE: machine-clock fixtures would make future runs depend on the date of execution.
 *
 * C-02 FLYER WITHDRAWAL (2026-10-23)
 * WHAT: Two real-catalog tests lost their only live positive example when FL-001..FL-007
 *   were withdrawn for poster/record mismatch. Each keeps its live assertions on the
 *   corrected catalog (meetings now empty; /api/flyers now FL-008 only) and gains a
 *   controlled fixture proving the original behavior.
 * WHY: The authorization requires each test's behavioral purpose to survive, and the
 *   corrected catalog has no current meeting and — on this test's 2026-10-15 clock — no
 *   past-event flyer (FL-008's event is 2026-10-17, i.e. upcoming here).
 * INTERACTION: Fixtures are built from the Spring context's own fixed-clock
 *   EditorialLifecycle, EventOccurrence, TaxonomyService and ContentSourceService, so
 *   only the records are hand-made; ministry-of-caring and united-way-delaware are real
 *   community-sector registry entries. CommunityNoticesService is constructed the same
 *   way CommunityNoticesServiceTest constructs it.
 * ALTERNATIVE: Adding test-only records to app/data was rejected (it would publish fake
 *   content); moving the cases to another class was rejected because these tests own
 *   the expiry-vs-occurrence purpose they protect.
 */
package org.firststep.backend.shared.lifecycle;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.notices.dto.CommunityNoticesPage;
import org.firststep.backend.notices.model.NoticeView;
import org.firststep.backend.notices.service.CommunityNoticesService;
import org.firststep.backend.notices.service.EventOccurrence;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.service.ContentSourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real JSON repositories, Spring wiring, fixed Community date and resident API projections. */
@SpringBootTest(properties = {"app.data.dir=../app/data", "news.rss.initial-delay=86400000",
        "app.editorial-expiry.community-zones.test-community=Asia/Tokyo"})
@AutoConfigureMockMvc
@Import(EditorialExpiryApiTest.TimeConfiguration.class)
class EditorialExpiryApiTest {
    @TestConfiguration
    static class TimeConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-10-15T16:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired MockMvc mvc;
    @Autowired NewsRepository newsRepository;
    @Autowired NewsService newsService;
    @Autowired FlyerRepository flyerRepository;
    @Autowired EditorialLifecycle lifecycle;
    @Autowired org.firststep.backend.originals.service.ArticleService articleService;
    @Autowired ContentSourceService contentSources;
    @Autowired TaxonomyService taxonomy;
    @Autowired EventOccurrence occurrence;
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode data(String route) throws Exception {
        String body = mvc.perform(get(route)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode root = mapper.readTree(body);
        assertTrue(root.path("success").asBoolean(), body);
        return root.path("data");
    }

    private List<String> ids(JsonNode root) {
        return root.findValuesAsText("id");
    }

    @Test
    void shouldRetainAllTenStoredNewsButDeliverFiveCurrentlyEligibleItems() throws Exception {
        assertEquals(10, newsRepository.findAll().size());
        assertEquals(5, newsService.getAll().size());
        assertEquals(5, data("/api/news").size());
    }

    @Test
    void shouldExcludeRealExpiredHousingPolicyFixturesFromCategoryAndGovernmentUpdates() throws Exception {
        for (String id : List.of("NP-001", "NP-002", "NP-006")) {
            NewsItem stored = newsRepository.findAll().stream().filter(n -> id.equals(n.id)).findFirst().orElseThrow();
            assertEquals(EditorialLifecycle.State.EXPIRED, lifecycle.assess(stored).state(), id);
            assertFalse(ids(data("/api/category/housing")).contains(id), id);
            assertFalse(ids(data("/api/updates/government")).contains(id), id);
        }
    }

    @Test
    void shouldExcludeExpiredCommunityMeetingsAndUpdateTheirCurrentCounts() throws Exception {
        JsonNode meetings = data("/api/community-notices/meetings");
        assertFalse(ids(meetings).contains("NP-009"));
        assertFalse(ids(meetings).contains("NP-010"));
        // C-02: FL-002, the catalog's only current meeting, is withdrawn.
        assertEquals(List.of(), ids(meetings.path("items")));
        assertEquals(0, meetings.path("counts").path("MEETINGS").asInt(-1));
        assertFalse(ids(data("/api/updates/community")).contains("NP-009"));
        assertFalse(ids(data("/api/updates/community")).contains("NP-010"));

        // Controlled fixture keeps the positive half: a current meeting is listed and
        // counted while an expired one is not, on the same clock and real registries.
        NewsItem expired = new NewsItem();
        expired.id = "FIX-EXPIRED-MEETING"; expired.title = "Expired meeting";
        expired.publishDate = "2026-09-01"; expired.expirationDate = "2026-10-01";
        expired.tags = List.of("meeting"); expired.contentSource = source("united-way-delaware");
        CommunityNoticesPage page = notices(List.of(flyer("FIX-MEETING", null, List.of("meeting"))), List.of(expired))
                .getPage(NoticeView.MEETINGS);
        assertEquals(List.of("FIX-MEETING"), page.items().stream().map(ContentItem::id).toList());
        assertEquals(1, page.counts().get(NoticeView.MEETINGS));
    }

    @Test
    void shouldCountOnlyCurrentHousingNewsInNavigationAndCollections() throws Exception {
        JsonNode housing = data("/api/category/housing");
        JsonNode navigation = housing.path("metadata");
        long expected = newsService.getAll().stream()
                .filter(n -> n.categoryTags != null && n.categoryTags.contains("Housing")).count()
                + articleService.getPublishable().stream()
                    .filter(a -> a.categoryTags != null && a.categoryTags.contains("Housing")).count();
        assertEquals(expected, navigation.path("countsByType").path("NEWS").asInt());
        for (JsonNode collection : housing.path("collections")) {
            assertEquals(collection.path("items").size(), collection.path("count").asInt());
        }
    }

    @Test
    void shouldPreservePastEventFlyersAndTheirHistoricalDetailLinks() throws Exception {
        assertEquals(8, flyerRepository.findAll().size());
        assertTrue(flyerRepository.findAll().stream().allMatch(f -> f.expirationDate == null));
        // C-02: FL-001..FL-007 are withdrawn; FL-008 (event 2026-10-17) is upcoming on
        // this clock, so the live catalog has no past-event example. FL-008 still
        // proves delivery and its detail link.
        JsonNode delivered = data("/api/flyers");
        assertEquals(1, delivered.size());
        assertEquals("FL-008", delivered.get(0).path("id").asText());
        assertEquals("FL-008", data("/api/flyers/FL-008").path("id").asText());

        // Controlled fixture: an event date in the past does not expire a flyer — it
        // stays in inventory, its detail lookup still resolves, and it stays in Flyers.
        Flyer past = flyer("FIX-PAST-EVENT", "2026-08-01", List.of("event"));
        FlyerService service = new FlyerService(repository(List.of(past)), contentSources, lifecycle);
        assertEquals(List.of(past), service.getAll());
        assertSame(past, service.getById("FIX-PAST-EVENT").orElseThrow());
        assertEquals(List.of("FIX-PAST-EVENT"), notices(List.of(past), List.of()).getPage(NoticeView.FLYERS)
                .items().stream().map(ContentItem::id).toList());
    }

    private static ContentSource source(String id) {
        ContentSource source = new ContentSource();
        source.id = id;
        return source;
    }

    private static Flyer flyer(String id, String eventDate, List<String> tags) {
        Flyer flyer = new Flyer();
        flyer.id = id; flyer.title = id; flyer.eventDate = eventDate; flyer.tags = tags;
        flyer.contentSource = source("ministry-of-caring");
        return flyer;
    }

    private static FlyerRepository repository(List<Flyer> flyers) {
        return new FlyerRepository() {
            @Override public List<Flyer> findAll() { return flyers; }
            @Override public Optional<Flyer> findById(String id) {
                return flyers.stream().filter(f -> f.id.equals(id)).findFirst();
            }
        };
    }

    private CommunityNoticesService notices(List<Flyer> flyers, List<NewsItem> news) {
        return new CommunityNoticesService(new FlyerService(repository(flyers), contentSources, lifecycle),
                new NewsService(() -> news, lifecycle), taxonomy, contentSources, occurrence);
    }

    @Test
    void shouldApplyBoundCommunityTimezoneConfiguration() {
        CivicContent item = new NewsItem();
        item.expirationDate = "2026-10-15";
        item.communityId = "wilmington-de";
        assertEquals(EditorialLifecycle.State.APPROACHING_EXPIRY, lifecycle.assess(item).state());
        item.communityId = "test-community";
        assertEquals(EditorialLifecycle.State.EXPIRED, lifecycle.assess(item).state());
    }
}
