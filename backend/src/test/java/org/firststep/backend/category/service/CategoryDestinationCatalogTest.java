package org.firststep.backend.category.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.category.selection.FeaturedItemSelector;
import org.firststep.backend.category.selection.FeaturedTopicSelector;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.data.dir=../app/data", "news.rss.initial-delay=86400000"})
@AutoConfigureMockMvc
@Import(CategoryDestinationCatalogTest.TimeConfiguration.class)
class CategoryDestinationCatalogTest {
    @TestConfiguration static class TimeConfiguration {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-10-16T16:00:00Z"), ZoneOffset.UTC); }
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired CategoryPageService pages;
    @Autowired FeaturedTopicSelector topics;
    @Autowired FeaturedItemSelector items;
    @Autowired FlyerRepository flyers;

    @Test void t04RealHousingHttpHasExactTwelveWinnersAndNoPrivateFields() throws Exception {
        var response = mvc.perform(get("/api/category/housing").param("communityId", "wilmington-de"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var page = pages.getByKey("housing", "wilmington-de").orElseThrow();
        var featured = page.destination().featuredTopics();
        assertEquals(List.of("transitional-housing", "emergency-shelter", "homeownership", "rental-assistance"),
                featured.stream().map(t -> t.slug()).toList());
        assertEquals(List.of("HA-002", "HA-003", "HA-005", "HA-016", "HA-001", "HA-006", "HA-011", "HA-013", "HA-021", "HA-008", "HA-023", "HA-026"),
                featured.stream().flatMap(t -> t.items().stream()).map(p -> p.item().id()).toList());
        for (var topic : featured) {
            assertEquals(List.of("DURABLE", "FALLBACK", "FALLBACK"), topic.items().stream().map(p -> p.role()).toList());
            assertEquals(items.select("wilmington-de", "housing", topic.name()).positions().stream().map(p -> p.item().card()).toList(),
                    topic.items().stream().map(p -> p.item()).toList());
            assertTrue(topic.items().stream().allMatch(p -> p.href() != null && !p.href().isBlank()));
        }
        assertEquals(topics.select("wilmington-de", "housing").selected().stream().map(t -> t.key()).toList(),
                featured.stream().map(t -> t.slug()).toList());
        assertNoPrivate(mapper.readTree(response));
        assertTrue(flyers.findAll().stream().noneMatch(f -> "ACTIONABLE_HELP_ACCESS".equals(f.helpAccess)));
        assertEquals(8, flyers.findAll().size());
        // C-02: FL-002 is withdrawn, so the real Housing page has no flyers collection.
        // Flyer placement and the never-featured rule are proven with fixtures in
        // CategoryContentServiceTest and FeaturedItemSelectorTest.
        assertTrue(page.collections().stream().noneMatch(c -> c.key().equals("flyers")));
        assertTrue(featured.stream().flatMap(t -> t.items().stream()).noneMatch(p -> p.item().id().startsWith("FL-")));
        String directory = System.getProperty("im08.evidence.dir");
        if (directory != null) { Files.createDirectories(Path.of(directory)); Files.writeString(Path.of(directory, "real-category-api.json"), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(response)) + "\n"); }
    }

    @Test void t09t10ScopeDefaultsZeroHelpAndOtherCategoryAreIsolated() throws Exception {
        mvc.perform(get("/api/category/housing")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.destination.communityId").value("wilmington-de"));
        mvc.perform(get("/api/category/housing/eviction-prevention").param("communityId", "wilmington-de"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.metadata.totalCount").value(0));
        mvc.perform(get("/api/category/housing/help").param("communityId", "wilmington-de"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(36));
        mvc.perform(get("/api/category/food").param("communityId", "bear-de"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.destination.featuredTopics[0].slug").value("food-pantry"));
        var empty = pages.getByKey("housing", "synthetic-empty-community").orElseThrow();
        assertTrue(empty.destination().featuredTopics().isEmpty()); assertTrue(empty.organizations().isEmpty());
        assertTrue(empty.collections().isEmpty()); assertTrue(empty.destination().headlines().isEmpty());
        mvc.perform(get("/api/category/unknown")).andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test void t13t14t15ApprovedHeadlinesAndScopedOrganizationLinksOnly() {
        var page = pages.getByKey("housing", "wilmington-de").orElseThrow();
        assertEquals(List.of("OR-003"), page.destination().headlines().stream().map(h -> h.id()).toList());
        assertEquals("/originals/OR-003?communityId=wilmington-de", page.destination().headlines().get(0).href());
        assertTrue(page.organizations().size() <= 4); assertFalse(page.organizations().isEmpty());
        assertTrue(page.destination().expertLinks().isEmpty(), "real experts have no original source URL; do not fabricate YouTube");
        assertTrue(page.destination().laws().isEmpty(), "no authored scoped current law in local cache");
    }

    private void assertNoPrivate(JsonNode node) {
        if (node.isObject()) node.fields().forEachRemaining(entry -> {
            assertFalse(List.of("diagnostics", "limitations", "metadataLimitations", "helpAccess", "editorialReview", "generatedBy", "verified", "catalogOrApprovalDate", "selectionDate", "provenance", "tieStage").contains(entry.getKey()), entry.getKey());
            assertNoPrivate(entry.getValue());
        });
        else if (node.isArray()) node.forEach(this::assertNoPrivate);
    }
}
