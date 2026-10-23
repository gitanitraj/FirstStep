/*
 * C-02 annotated test reference — full mirror of
 * backend/src/test/java/org/firststep/backend/audit/FlyerWithdrawalRegressionTest.java
 *
 * WHAT: Regression coverage for the C-02 flyer integrity correction. FL-001..FL-007
 *   carry "status": "withdrawn" in app/data/flyers.json because their poster images
 *   describe different events, organizers, dates or places than their records
 *   (docs/v3-completion-docs/C-02-flyer-integrity-closure.md). FL-008 is the one
 *   verified match.
 *
 * WHY THESE TESTS:
 *   - Retention: the records must stay available for correction, so the repository
 *     still holds 8 records and the seven keep their organization_id and image
 *     (read from the raw JSON, because Flyer has no organization_id field).
 *   - Gate isolation (CLAUDE.md negative-test rule): the seven have no expirationDate
 *     and are expiry-eligible, so status is the ONLY thing that can exclude them. A
 *     green exclusion test therefore proves the withdrawal rule, not expiry.
 *   - Projections: flyer inventory, homepage flyer row, every Community Notices view,
 *     both Updates sectors, and every category page plus its help inventory.
 *     assertNoWithdrawnIds searches recursively (findValuesAsText), which is
 *     over-inclusive and therefore safe for ABSENCE checks; exact-membership checks
 *     read the top-level element id instead, because recursion also returns the
 *     nested contentSource.id.
 *   - Detail links: the established NotFoundException → 404 / NOT_FOUND envelope.
 *   - Precedence: a fixture flyer with a future event and a future expiry appears in
 *     Events and Flyers (control), and disappears entirely when withdrawn.
 *   - Unaffected behavior: FL-008 stays retrievable (and, being upcoming on this
 *     clock, in Events); the twelve approved Housing winners are unchanged.
 *
 * INTERACTION: Real JSON catalog + Spring context on the 2026-10-16 clock used by
 *   CategoryDestinationCatalogTest, so HOUSING_WINNERS is the same approved baseline.
 *   Fixtures reuse the context's lifecycle, occurrence, taxonomy and source registry;
 *   CommunityNoticesService is built as in CommunityNoticesServiceTest.
 *
 * MUTATION EVIDENCE: run against the pre-C-02 flyers.json, 8 of 12 tests fail, each
 *   naming a specific withdrawn ID; the 4 hold-independent tests (two fixtures, FL-008,
 *   featured Housing) pass. See evidence-c02/mutation-check.txt.
 *
 * ALTERNATIVES: An audit-package location follows the Im12 catalog-audit precedent,
 *   since there is no single production class under test. Adding fixture records to
 *   app/data was rejected because it would publish fake content.
 */
package org.firststep.backend.audit;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.firststep.backend.category.service.CategoryPageService;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.notices.model.NoticeView;
import org.firststep.backend.notices.service.CommunityNoticesService;
import org.firststep.backend.notices.service.EventOccurrence;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.lifecycle.EditorialLifecycle;
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

/**
 * C-02 regression: FL-001..FL-007 carry {@code "status": "withdrawn"} because their
 * poster images describe different events than their records. They must stay stored
 * for correction and stay out of every resident projection; FL-008, the one verified
 * match, keeps its normal lifecycle.
 *
 * <p>Real JSON catalog and Spring wiring on the same fixed clock as
 * CategoryDestinationCatalogTest (2026-10-16), so the featured-Housing check compares
 * against the same approved baseline. FL-008's event (2026-10-17) is upcoming here.
 */
@SpringBootTest(properties = {"app.data.dir=../app/data", "news.rss.initial-delay=86400000"})
@AutoConfigureMockMvc
@Import(FlyerWithdrawalRegressionTest.TimeConfiguration.class)
class FlyerWithdrawalRegressionTest {
    @TestConfiguration
    static class TimeConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-10-16T16:00:00Z"), ZoneOffset.UTC);
        }
    }

    /** The withdrawn records and the stored metadata that must survive withdrawal. */
    private static final Map<String, List<String>> WITHDRAWN = new LinkedHashMap<>();
    static {
        WITHDRAWN.put("FL-001", List.of("ORG-056", "Youth.jpg"));
        WITHDRAWN.put("FL-002", List.of("ORG-057", "Eviction Help.png"));
        WITHDRAWN.put("FL-003", List.of("ORG-058", "Volunteer.jpg"));
        WITHDRAWN.put("FL-004", List.of("ORG-026", "Fundraiser.jpg"));
        WITHDRAWN.put("FL-005", List.of("ORG-059", "Disablility Info.jpg"));
        WITHDRAWN.put("FL-006", List.of("ORG-060", "Health Fair.jpg"));
        WITHDRAWN.put("FL-007", List.of("ORG-061", "Furniture.jpg"));
    }

    /** Approved Housing winners, identical to CategoryDestinationCatalogTest t04. */
    private static final List<String> HOUSING_WINNERS = List.of("HA-002", "HA-003", "HA-005", "HA-016",
            "HA-001", "HA-006", "HA-011", "HA-013", "HA-021", "HA-008", "HA-023", "HA-026");

    @Autowired MockMvc mvc;
    @Autowired FlyerRepository flyerRepository;
    @Autowired FlyerService flyerService;
    @Autowired EditorialLifecycle lifecycle;
    @Autowired ContentSourceService contentSources;
    @Autowired TaxonomyService taxonomy;
    @Autowired EventOccurrence occurrence;
    @Autowired CategoryPageService pages;
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode data(String route) throws Exception {
        String body = mvc.perform(get(route)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).path("data");
    }

    private static void assertNoWithdrawnIds(JsonNode payload, String route) {
        List<String> ids = payload.findValuesAsText("id");
        for (String id : WITHDRAWN.keySet()) assertFalse(ids.contains(id), route + " exposes " + id);
    }

    // ---- stored records ---------------------------------------------------

    @Test
    void shouldRetainAllSevenWithdrawnRecordsWithTheirOriginalMetadata() throws Exception {
        JsonNode records = mapper.readTree(Path.of("../app/data/flyers.json").toFile()).path("records");
        Map<String, JsonNode> byId = new LinkedHashMap<>();
        records.forEach(r -> byId.put(r.path("id").asText(), r));

        assertEquals(8, flyerRepository.findAll().size());
        WITHDRAWN.forEach((id, metadata) -> {
            assertEquals("withdrawn", flyerRepository.findById(id).orElseThrow().status, id);
            assertEquals(metadata.get(0), byId.get(id).path("organization_id").asText(), id);
            assertEquals(metadata.get(1), byId.get(id).path("image").asText(), id);
        });
        assertNull(flyerRepository.findById("FL-008").orElseThrow().status);
    }

    @Test
    void shouldLeaveWithdrawalAsTheOnlyGateExcludingTheSevenRecords() {
        // Isolates the rule under test: none of the seven is expired, so a passing
        // exclusion test cannot be explained by editorial expiry.
        for (String id : WITHDRAWN.keySet()) {
            Flyer flyer = flyerRepository.findById(id).orElseThrow();
            assertNull(flyer.expirationDate, id);
            assertTrue(lifecycle.assess(flyer).editorialExpiryEligible(), id);
            assertFalse(lifecycle.isPublicStatus(flyer), id);
        }
    }

    // ---- resident projections ---------------------------------------------

    @Test
    void shouldExcludeWithdrawnFlyersFromResidentFlyerInventory() throws Exception {
        assertEquals(List.of("FL-008"), flyerService.getAll().stream().map(f -> f.id).toList());
        JsonNode delivered = data("/api/flyers");
        assertEquals(1, delivered.size());
        assertEquals("FL-008", delivered.get(0).path("id").asText());
    }

    @Test
    void shouldExcludeWithdrawnFlyersFromEveryCommunityNoticesView() throws Exception {
        for (String route : List.of("/api/community-notices", "/api/community-notices/events",
                "/api/community-notices/meetings", "/api/community-notices/announcements",
                "/api/community-notices/flyers")) {
            assertNoWithdrawnIds(data(route), route);
        }
    }

    @Test
    void shouldShowOnlyTheVerifiedFlyerInTheHomepageFlyerRow() throws Exception {
        // Homepage cards carry no id, so the row is identified by its title.
        JsonNode cards = data("/api/home").path("communityFlyers");
        assertEquals(List.of("United Neighbors 2026"), cards.findValuesAsText("title"));
    }

    @Test
    void shouldExcludeWithdrawnFlyersFromProgramUpdates() throws Exception {
        for (String route : List.of("/api/updates/government", "/api/updates/community")) {
            assertNoWithdrawnIds(data(route), route);
        }
    }

    @Test
    void shouldExcludeWithdrawnFlyersFromEveryCategoryProjection() throws Exception {
        List<String> keys = data("/api/categories").findValuesAsText("key");
        assertFalse(keys.isEmpty());
        for (String key : keys) {
            assertNoWithdrawnIds(data("/api/category/" + key), key);
            assertNoWithdrawnIds(data("/api/category/" + key + "/help"), key + "/help");
        }
    }

    @Test
    void shouldReturnTheEstablishedNotFoundResponseForWithdrawnDetailLinks() throws Exception {
        for (String id : WITHDRAWN.keySet()) {
            String body = mvc.perform(get("/api/flyers/" + id)).andExpect(status().isNotFound())
                    .andReturn().getResponse().getContentAsString();
            JsonNode root = mapper.readTree(body);
            assertFalse(root.path("success").asBoolean(), id);
            assertEquals("NOT_FOUND", root.path("errorCode").asText(), id);
        }
    }

    // ---- withdrawal precedence (controlled fixture) -----------------------

    @Test
    void shouldAdmitTheFixtureFlyerWhenItIsNotWithdrawn() {
        // Control for the next test: the same record is fully eligible on its own.
        Flyer flyer = eligibleFixture();

        CommunityNoticesService notices = notices(flyer);

        assertEquals(List.of("FIX-ELIGIBLE"), ids(notices, NoticeView.EVENTS));
        assertEquals(List.of("FIX-ELIGIBLE"), ids(notices, NoticeView.FLYERS));
    }

    @Test
    void shouldExcludeAWithdrawnFlyerDespiteEligibleEventAndExpiryDates() {
        Flyer flyer = eligibleFixture();
        flyer.status = "withdrawn";
        FlyerService service = new FlyerService(repository(flyer), contentSources, lifecycle);

        assertTrue(service.getAll().isEmpty());
        assertTrue(service.getById("FIX-ELIGIBLE").isEmpty());
        assertTrue(ids(notices(flyer), NoticeView.EVENTS).isEmpty());
        assertTrue(ids(notices(flyer), NoticeView.FLYERS).isEmpty());
    }

    // ---- unaffected behavior ----------------------------------------------

    @Test
    void shouldKeepTheVerifiedFlyerRetrievableUnderItsLifecycle() throws Exception {
        assertEquals("FL-008", data("/api/flyers/FL-008").path("id").asText());
        assertTrue(data("/api/community-notices/flyers").findValuesAsText("id").contains("FL-008"));
        // Upcoming on this clock (event 2026-10-17), so occurrence rules admit it to Events.
        assertTrue(data("/api/community-notices/events").findValuesAsText("id").contains("FL-008"));
    }

    @Test
    void shouldKeepTheApprovedFeaturedHousingSelection() {
        var featured = pages.getByKey("housing", "wilmington-de").orElseThrow().destination().featuredTopics();
        List<String> winners = new ArrayList<>();
        featured.forEach(topic -> topic.items().forEach(card -> winners.add(card.item().id())));

        assertEquals(HOUSING_WINNERS, winners);
    }

    // ---- fixture helpers --------------------------------------------------

    /** Future event, future expiry, community producer, event kind: eligible everywhere. */
    private static Flyer eligibleFixture() {
        ContentSource source = new ContentSource();
        source.id = "ministry-of-caring";
        Flyer flyer = new Flyer();
        flyer.id = "FIX-ELIGIBLE";
        flyer.title = "Fixture event";
        flyer.eventDate = "2026-11-20";
        flyer.expirationDate = "2026-12-31";
        flyer.tags = List.of("event");
        flyer.contentSource = source;
        return flyer;
    }

    private static FlyerRepository repository(Flyer flyer) {
        return new FlyerRepository() {
            @Override public List<Flyer> findAll() { return List.of(flyer); }
            @Override public Optional<Flyer> findById(String id) {
                return flyer.id.equals(id) ? Optional.of(flyer) : Optional.empty();
            }
        };
    }

    private CommunityNoticesService notices(Flyer flyer) {
        return new CommunityNoticesService(new FlyerService(repository(flyer), contentSources, lifecycle),
                new NewsService(List::of, lifecycle), taxonomy, contentSources, occurrence);
    }

    private static List<String> ids(CommunityNoticesService notices, NoticeView view) {
        return notices.getPage(view).items().stream().map(ContentItem::id).toList();
    }
}
