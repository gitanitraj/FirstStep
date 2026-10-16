package org.firststep.backend.category.selection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.shared.classification.SourceMappingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class AuthoredTopicEvidenceTest {
    @TempDir Path directory;
    private final TaxonomyService taxonomy = new TaxonomyService("../app/data");
    private final SourceMappingService mappings = new SourceMappingService("../app/data");

    @Test void runtimeKeywordPlacementIsNotAuthoredNewsOrRssClassification() throws Exception {
        Files.writeString(directory.resolve("news.json"), """
                {"records":[
                 {"id":"explicit","category_tags":["Housing"],"subcategory":"Emergency Shelter"},
                 {"id":"inferred","category_tags":["Housing"],"headline":"Emergency Shelter law"},
                 {"id":"wrong-parent","category_tags":["Food"],"subcategory":"Emergency Shelter"}]}
                """);
        var evidence = new AuthoredTopicEvidence(directory.toString(), taxonomy, mappings);
        var category = taxonomy.findByKey("housing").orElseThrow();
        NewsItem news = new NewsItem(); news.categoryTags = List.of("Housing"); news.subcategory = "Emergency Shelter";
        news.id = "explicit"; assertTrue(evidence.permits(news, category));
        news.id = "inferred"; assertFalse(evidence.permits(news, category));
        news.id = "rss-runtime-id"; assertFalse(evidence.permits(news, category));
        news.id = "wrong-parent"; assertFalse(evidence.permits(news, category));
        news.id = "explicit"; news.subcategory = "Rental Assistance";
        assertFalse(evidence.permits(news, category));
    }

    @Test void resourceSourceMappingCountsAsExplicitCategoryButTopicMustBeStored() throws Exception {
        Files.writeString(directory.resolve("resources.json"), """
                {"records":[
                 {"id":"mapped","category":"Housing Assistance","subcategory":"Emergency Shelter"},
                 {"id":"keyword","category":"Unknown source value","subcategory":"Emergency Shelter"},
                 {"id":"missing-topic","category":"Housing Assistance","description":"Emergency Shelter"}]}
                """);
        var evidence = new AuthoredTopicEvidence(directory.toString(), taxonomy, mappings);
        var category = taxonomy.findByKey("housing").orElseThrow();
        Resource resource = new Resource(); resource.categoryTags = List.of("Housing"); resource.subcategory = "Emergency Shelter";
        resource.id = "mapped"; assertTrue(evidence.permits(resource, category));
        resource.id = "keyword"; assertFalse(evidence.permits(resource, category));
        resource.id = "missing-topic"; assertFalse(evidence.permits(resource, category));
    }

    @Test void conflictingDuplicateAuthoredClassificationDoesNotChooseAnArbitraryWinner() throws Exception {
        Files.writeString(directory.resolve("news.json"), """
                [{"id":"duplicate","category_tags":["Housing"],"subcategory":"Emergency Shelter"},
                 {"id":"duplicate","category_tags":["Housing"],"subcategory":"Rental Assistance"}]
                """);
        var evidence = new AuthoredTopicEvidence(directory.toString(), taxonomy, mappings);
        NewsItem news = new NewsItem(); news.id = "duplicate"; news.subcategory = "Emergency Shelter";
        assertFalse(evidence.permits(news, taxonomy.findByKey("housing").orElseThrow()));
    }
}
