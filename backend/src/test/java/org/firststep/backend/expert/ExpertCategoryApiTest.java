package org.firststep.backend.expert;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.firststep.backend.expert.repository.JsonFaqRepository;
import org.firststep.backend.expert.repository.JsonExpertAnswerRepository;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.shared.classification.ClassifierFixture;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.service.ContentSourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.data.dir=../app/data", "news.rss.initial-delay=86400000"})
@AutoConfigureMockMvc
@Import(ExpertCategoryApiTest.TimeConfiguration.class)
class ExpertCategoryApiTest {
    @TestConfiguration
    static class TimeConfiguration {
        @Bean @Primary Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-10-16T16:00:00Z"), ZoneOffset.UTC);
        }
    }
    @Autowired JsonFaqRepository faqs;
    @Autowired JsonExpertAnswerRepository experts;
    @Autowired TaxonomyService taxonomy;
    @Autowired MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldRetainAllTwelveApprovedCategoryOnlyRecordsAsExpertWithoutDerivedTopics() {
        var all = new java.util.ArrayList<CivicContent>(faqs.findAll());
        all.addAll(experts.findAll());
        assertEquals(12, all.size());
        Map<String, String> expected = Map.ofEntries(
                Map.entry("FAQ-001", "Food"), Map.entry("FAQ-002", "Housing"), Map.entry("FAQ-003", "Legal"),
                Map.entry("FAQ-004", "Utilities"), Map.entry("FAQ-005", "Health"), Map.entry("FAQ-006", "Employment"),
                Map.entry("EA-001", "Housing"), Map.entry("EA-002", "Food"), Map.entry("EA-003", "Employment"),
                Map.entry("EA-004", "Health"), Map.entry("EA-005", "Housing"), Map.entry("EA-006", "Utilities"));
        for (CivicContent item : all) {
            assertEquals(List.of(expected.get(item.id)), item.categoryTags, item.id);
            assertTrue(taxonomy.permitsExpertCategoryParticipation(item), item.id);
            assertEquals(ContentType.EXPERT, item.contentType);
            assertEquals("wilmington-de", item.communityId);
            assertNotNull(item.contentSource.id);
            assertNull(item.subcategory, item.id);
            assertNull(item.expirationDate, item.id);
        }
    }

    @Test
    void shouldServeCorrectedRealFaqAndExpertSupportingCollectionsThroughCategoryApi() throws Exception {
        Map<String, List<String>> expected = Map.of(
                "housing", List.of("EA-001", "EA-005", "FAQ-002"),
                "food", List.of("EA-002", "FAQ-001"), "legal", List.of("FAQ-003"),
                "utilities", List.of("EA-006", "FAQ-004"), "health", List.of("EA-004", "FAQ-005"),
                "employment", List.of("EA-003", "FAQ-006"));
        for (var entry : expected.entrySet()) {
            String body = mvc.perform(get("/api/category/" + entry.getKey()).param("communityId", "wilmington-de"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            List<String> delivered = new java.util.ArrayList<>();
            for (JsonNode collection : mapper.readTree(body).path("data").path("collections")) {
                if (!List.of("faqs", "expert-qa").contains(collection.path("key").asText())) continue;
                assertEquals(collection.path("items").size(), collection.path("count").asInt());
                for (JsonNode item : collection.path("items")) {
                    delivered.add(item.path("id").asText());
                    assertEquals("EXPERT", item.path("contentType").asText());
                }
            }
            assertEquals(entry.getValue(), delivered.stream().sorted().toList(), entry.getKey());
        }
    }

    @Test
    void shouldKeepMissingClassificationVisibleInRawRepositoriesWithoutRuntimeManufacture(@TempDir Path folder) throws Exception {
        String record = """
                {"records":[{"id":"UNCLASSIFIED", "title":"Housing eviction prevention", "question":"Housing rental assistance?",
                "answer":"Tenant eviction prevention", "subcategory":"Eviction Prevention",
                "contentSource":{"id":"first-step"}, "communityId":"test-community"}]}
                """;
        Files.writeString(folder.resolve("faq.json"), record);
        Files.writeString(folder.resolve("expert-answers.json"), record);
        var faqRepository = new JsonFaqRepository(ClassifierFixture.real(), new ContentSourceService("../app/data"));
        var expertRepository = new JsonExpertAnswerRepository(ClassifierFixture.real(), new ContentSourceService("../app/data"));
        for (Object repository : List.of(faqRepository, expertRepository)) {
            ReflectionTestUtils.setField(repository, "dataDir", folder.toString());
        }
        faqRepository.init(); expertRepository.init();
        for (CivicContent loaded : List.of(faqRepository.findAll().get(0), expertRepository.findAll().get(0))) {
            assertNull(loaded.categoryTags);
            assertEquals("test-community", loaded.communityId);
            assertFalse(taxonomy.permitsExpertCategoryParticipation(loaded));
            assertEquals("Eviction Prevention", loaded.subcategory);
        }
    }
}
