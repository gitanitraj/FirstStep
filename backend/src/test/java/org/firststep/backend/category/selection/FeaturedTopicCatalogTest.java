package org.firststep.backend.category.selection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.category.service.CategoryContentService;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.dto.ContentItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import static org.junit.jupiter.api.Assertions.*;

/** Stored catalog integration and optional read-only execution evidence; no public featured endpoint. */
@SpringBootTest(properties = {"app.data.dir=../app/data", "news.rss.initial-delay=86400000"})
@Import(FeaturedTopicCatalogTest.TimeConfiguration.class)
class FeaturedTopicCatalogTest {
    @TestConfiguration
    static class TimeConfiguration {
        @Bean @Primary Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-10-16T16:00:00Z"), ZoneOffset.UTC);
        }
    }
    @Autowired FeaturedTopicSelector selector;
    @Autowired CategoryContentService content;
    @Autowired TaxonomyService taxonomy;
    @Autowired ResourceRepository rawResources;
    @Autowired ResourceService resources;
    @Autowired FlyerService flyers;
    @Autowired ObjectMapper mapper;

    @Test void actualHousingSelectionIsScopedRepeatableNonmutatingAndReconcilesEveryHelpCount() throws Exception {
        String before = mapper.writeValueAsString(rawResources.findAll());
        var result = selector.select("wilmington-de", "housing");
        assertEquals(result, selector.select("wilmington-de", "housing"));
        assertTrue(result.selected().size() <= 4);
        for (var topic : result.candidates()) {
            assertTrue(taxonomy.isTopicOf("housing", topic.name()));
            assertEquals(content.helpFor("housing", topic.name(), "wilmington-de").stream()
                    .map(ContentItem::id).distinct().sorted().toList(), topic.actionableIds());
        }
        assertTrue(result.selected().stream().allMatch(topic -> topic.actionableCount() > 0));
        assertEquals(before, mapper.writeValueAsString(rawResources.findAll()));
        assertTrue(flyers.getAll().stream().noneMatch(flyer -> "ACTIONABLE_HELP_ACCESS".equals(flyer.helpAccess)));
        var eviction = result.candidates().stream().filter(topic -> topic.key().equals("eviction-prevention")).findFirst().orElseThrow();
        assertEquals(content.helpFor("housing", eviction.name(), "wilmington-de").size(), eviction.actionableCount());
        if (eviction.actionableCount() == 0) assertFalse(result.selected().contains(eviction));

        String otherCommunity = resources.getAll().stream()
                .filter(resource -> resource.categoryTags != null && resource.categoryTags.contains("Food"))
                .map(resource -> resource.communityId).sorted().findFirst().orElseThrow();
        var food = selector.select(otherCommunity, "food");
        assertFalse(food.selected().isEmpty());
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("clock", "2026-10-16T16:00:00Z");
        evidence.put("storedResourceCount", rawResources.findAll().size());
        evidence.put("currentResourceCountAcrossCommunities", resources.getAll().size());
        evidence.put("currentWilmingtonHousingHelpCount", content.helpFor("housing", null, "wilmington-de").size());
        evidence.put("housing", result);
        evidence.put("foodWilmington", selector.select("wilmington-de", "food"));
        evidence.put("foodPositiveCommunity", food);
        evidence.put("scope", "Current relevance-admitted RSS cache only; no remote refresh or complete-feed claim");
        String directory = System.getProperty("im06.evidence.dir");
        if (directory != null) {
            Path out = Path.of(directory); Files.createDirectories(out);
            Files.writeString(out.resolve("real-selector-audit.json"), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(evidence) + "\n");
        }
    }
}
