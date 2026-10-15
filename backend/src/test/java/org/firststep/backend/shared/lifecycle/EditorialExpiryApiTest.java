package org.firststep.backend.shared.lifecycle;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.shared.model.CivicContent;
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
        assertEquals(List.of("FL-002"), ids(meetings.path("items")));
        assertEquals(1, meetings.path("items").size());
        assertEquals(1, meetings.path("counts").path("MEETINGS").asInt(-1));
        assertFalse(ids(data("/api/updates/community")).contains("NP-009"));
        assertFalse(ids(data("/api/updates/community")).contains("NP-010"));
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
        List<String> delivered = ids(data("/api/flyers"));
        for (int i = 1; i <= 8; i++) {
            String id = "FL-00" + i;
            assertTrue(delivered.contains(id), id);
            assertEquals(id, data("/api/flyers/" + id).path("id").asText());
        }
        assertTrue(flyerRepository.findAll().stream().allMatch(f -> f.expirationDate == null));
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
