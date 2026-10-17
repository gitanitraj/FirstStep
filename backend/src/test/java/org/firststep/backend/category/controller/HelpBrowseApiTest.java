package org.firststep.backend.category.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.Website;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full HTTP -> taxonomy -> IM-04 projection -> real lifecycle, synthetic raw repositories only. */
@SpringBootTest(properties = {"app.data.dir=../app/data", "news.rss.initial-delay=86400000"})
@AutoConfigureMockMvc
@Import(HelpBrowseApiTest.TimeConfiguration.class)
class HelpBrowseApiTest {
    @TestConfiguration
    static class TimeConfiguration {
        @Bean @Primary Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-10-16T16:00:00Z"), ZoneOffset.UTC);
        }
    }
    @Autowired MockMvc mvc;
    @MockitoBean ResourceRepository resources;
    @MockitoBean FlyerRepository flyers;
    private final ObjectMapper mapper = new ObjectMapper();

    private Resource resource(String id, String topic) {
        Resource r = new Resource();
        r.id = id; r.title = "Same title"; r.categoryTags = List.of("Housing");
        r.subcategory = topic; r.communityId = "synthetic-community";
        Website site = new Website(); site.url = "https://example.org/" + id;
        r.websites = List.of(site);
        return r;
    }
    private Flyer flyer(String id, String signal) {
        Flyer f = new Flyer();
        f.id = id; f.title = "Acceso: información"; f.categoryTags = List.of("Housing");
        f.subcategory = "Emergency Shelter"; f.communityId = "synthetic-community";
        f.helpAccess = signal; f.image = "Eviction Help.png";
        f.organization = "Host label only";
        f.contentSource = new ContentSource(); f.contentSource.id = "community-legal-aid";
        f.contentSource.url = "https://example.org/source";
        return f;
    }
    @BeforeEach void setUp() {
        when(resources.findAll()).thenReturn(List.of());
        when(flyers.findAll()).thenReturn(List.of());
    }
    private JsonNode page(String path) throws Exception {
        return mapper.readTree(mvc.perform(get(path)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("data");
    }
    private String all() { return "/api/category/housing/help?communityId=synthetic-community"; }

    @Test void categoryWideRetainsTopiclessResourcesAndCompleteDeduplicatedStableInventory() throws Exception {
        List<Resource> many = IntStream.range(0, 55).mapToObj(i -> resource(String.format("SYN-%02d", i),
                i == 0 ? null : "Emergency Shelter")).toList();
        var duplicate = new java.util.ArrayList<>(many); duplicate.add(many.get(0));
        java.util.Collections.reverse(duplicate);
        when(resources.findAll()).thenReturn(duplicate);
        JsonNode data = page(all());
        assertEquals(55, data.path("metadata").path("totalCount").asInt());
        assertEquals(55, data.path("items").size());
        assertEquals("SYN-00", data.path("items").get(0).path("id").asText());
        assertEquals(54, page("/api/category/housing/emergency-shelter?communityId=synthetic-community").path("items").size());
        assertEquals(9, data.path("topics").size());
    }
    @Test void exactSignalPreservesFlyerIdentityMediaAndSourceWhileUnknownAndAbsentStayOut() throws Exception {
        when(flyers.findAll()).thenReturn(List.of(flyer("SYN-yes", "ACTIONABLE_HELP_ACCESS"),
                flyer("SYN-no", null), flyer("SYN-invalid", "unknown")));
        JsonNode data = page(all());
        assertEquals(1, data.path("items").size());
        JsonNode item = data.path("items").get(0);
        assertEquals("SYN-yes", item.path("id").asText());
        assertEquals("FLYER", item.path("contentType").asText());
        assertEquals("/images/seasonal/Eviction%20Help.png", item.path("imageUrl").asText());
        assertEquals("https://example.org/source", item.path("url").asText());
        assertFalse(item.has("helpAccess"));
        assertFalse(item.has("expirationDate"));
        assertEquals(item, page("/api/category/housing/emergency-shelter?communityId=synthetic-community").path("items").get(0));
    }
    @Test void lifecycleExcludesExpiredInactiveWithdrawnAndKeepsApproachingWithoutPriority() throws Exception {
        Resource active = resource("SYN-a", "Emergency Shelter");
        Resource near = resource("SYN-z", "Emergency Shelter"); near.expirationDate = "2026-10-17";
        Resource expired = resource("SYN-expired", "Emergency Shelter"); expired.expirationDate = "2026-10-15";
        Resource inactive = resource("SYN-inactive", "Emergency Shelter"); inactive.status = "inactive";
        Resource withdrawn = resource("SYN-withdrawn", "Emergency Shelter"); withdrawn.status = "withdrawn";
        when(resources.findAll()).thenReturn(List.of(near, active, expired, inactive, withdrawn));
        JsonNode data = page(all());
        assertEquals(2, data.path("items").size());
        assertEquals("SYN-a", data.path("items").get(0).path("id").asText());
        assertEquals("SYN-z", data.path("items").get(1).path("id").asText());
    }
    @Test void communityCategoryAndControlledTopicAreIndependentGates() throws Exception {
        when(resources.findAll()).thenReturn(List.of(resource("SYN-local", "Emergency Shelter")));
        assertEquals(1, page(all()).path("items").size());
        assertEquals(0, page("/api/category/housing/help?communityId=wilmington-de").path("items").size());
        assertEquals(0, page("/api/category/food/help?communityId=synthetic-community").path("items").size());
        assertEquals(0, page("/api/category/housing/rental-assistance?communityId=synthetic-community").path("items").size());
        for (String path : List.of("/api/category/unknown/help", "/api/category/housing/unknown", "/api/category/housing/food-pantry")) {
            mvc.perform(get(path)).andExpect(status().isNotFound());
        }
    }
    @Test void realCategoryOnlyExpertRecordsNeverBecomeHelpAndEmptyTopicRemainsValid() throws Exception {
        assertEquals(0, page("/api/category/housing/help").path("items").size());
        assertEquals(0, page("/api/category/housing/eviction-prevention").path("metadata").path("totalCount").asInt());
        assertEquals(0, page("/api/category/food/food-pantry").path("items").size());
    }
    @Test void missingWebsiteUsesOnlyExplicitStoredTelephoneWithoutInventingDetailRoute() throws Exception {
        Resource r = resource("SYN-phone", "Emergency Shelter");
        r.websites = List.of();
        org.firststep.backend.shared.model.Phone phone = new org.firststep.backend.shared.model.Phone();
        phone.number = "302-658-7161";
        r.phones = List.of(phone);
        when(resources.findAll()).thenReturn(List.of(r));
        when(resources.findById(r.id)).thenReturn(java.util.Optional.of(r));
        assertEquals("tel:3026587161", page(all()).path("items").get(0).path("url").asText());
    }

    @Test void categoryNavigationTopicCountMatchesItsHelpDestination() throws Exception {
        when(resources.findAll()).thenReturn(List.of(resource("SYN-resource", "Emergency Shelter")));
        when(flyers.findAll()).thenReturn(List.of(flyer("SYN-ordinary", null)));
        JsonNode category = page("/api/category/housing?communityId=synthetic-community");
        JsonNode navTopic = category.path("groups").get(0).path("topics").get(0);
        assertEquals(1, navTopic.path("count").asInt());
        assertEquals(page(all()).path("items").size(), navTopic.path("count").asInt());
    }
}
