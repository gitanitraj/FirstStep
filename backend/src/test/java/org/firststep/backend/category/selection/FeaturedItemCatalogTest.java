package org.firststep.backend.category.selection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.category.service.CategoryContentService;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

/** Actual current repositories and approved fixed clock; pinned real collisions, no catalog writes. */
@SpringBootTest(properties = {"app.data.dir=../app/data", "news.rss.initial-delay=86400000"})
@Import(FeaturedTopicCatalogTest.TimeConfiguration.class)
class FeaturedItemCatalogTest {
    @Autowired FeaturedItemSelector selector;
    @Autowired FeaturedTopicSelector topics;
    @Autowired FeaturedTopicInventory inventory;
    @Autowired CategoryContentService content;
    @Autowired TaxonomyService taxonomy;
    @Autowired ResourceRepository resources;
    @Autowired FlyerRepository flyers;
    @Autowired ObjectMapper mapper;

    @Test void t19ActualAllNineHousingTopicsAndFourFeaturedWinnersRemainTruthfulAndNonmutating() throws Exception {
        String before = mapper.writeValueAsString(resources.findAll()) + mapper.writeValueAsString(flyers.findAll());
        var featured = topics.select("wilmington-de", "housing");
        assertEquals(List.of("transitional-housing", "emergency-shelter", "homeownership", "rental-assistance"),
                featured.selected().stream().map(t -> t.key()).toList());
        Map<String, List<String>> pinned = Map.of(
                "Transitional Housing", List.of("HA-002", "HA-003", "HA-005"),
                "Emergency Shelter", List.of("HA-016", "HA-001", "HA-006"),
                "Homeownership", List.of("HA-011", "HA-013", "HA-021"),
                "Rental Assistance", List.of("HA-008", "HA-023", "HA-026"));
        Map<String, Object> evidence = new LinkedHashMap<>();
        Map<String, FeaturedItemSelector.Selection> housing = new LinkedHashMap<>();
        for (String topic : taxonomy.findByKey("housing").orElseThrow().subcategories()) {
            var selection = selector.select("wilmington-de", "housing", topic);
            assertEquals(selection, selector.select("wilmington-de", "housing", topic));
            housing.put(topic, selection);
            assertTrue(selection.positions().size() <= 3);
            assertEquals(selection.positions().size(), selection.positions().stream().map(p -> p.item().id()).distinct().count());
            assertTrue(selection.positions().stream().allMatch(p -> p.item().provenance().communityId().equals("wilmington-de")));
            assertTrue(selection.candidates().stream().noneMatch(c -> c.roles().contains(FeaturedRoleQualification.Role.VISUAL)));
            assertTrue(selection.candidates().stream().filter(FeaturedItemSelector.Candidate::actionable).allMatch(c -> c.catalogOrApprovalDate() == null));
            if (pinned.containsKey(topic)) {
                assertEquals(pinned.get(topic), selection.positions().stream().map(p -> p.item().id()).toList(), topic);
                assertEquals(List.of(FeaturedItemSelector.PositionType.DURABLE, FeaturedItemSelector.PositionType.FALLBACK,
                        FeaturedItemSelector.PositionType.FALLBACK), selection.positions().stream().map(p -> p.type()).toList());
            }
        }
        var eviction = housing.get("Eviction Prevention");
        assertEquals(List.of("OR-003"), eviction.positions().stream().map(p -> p.item().id()).toList());
        assertEquals(FeaturedItemSelector.PositionType.FALLBACK, eviction.positions().get(0).type());
        assertFalse(eviction.positions().get(0).item().actionable());
        assertTrue(content.helpFor("housing", "Eviction Prevention", "wilmington-de").isEmpty());
        assertTrue(housing.get("Eviction Prevention").diagnostics().stream().anyMatch(d ->
                d.code().equals("FLYER_HELP_ACCESS_REQUIRED") && d.itemIds().contains("FL-002")));
        assertEquals(8, flyers.findAll().size());
        assertTrue(flyers.findAll().stream().noneMatch(f -> "ACTIONABLE_HELP_ACCESS".equals(f.helpAccess)));
        assertEquals(before, mapper.writeValueAsString(resources.findAll()) + mapper.writeValueAsString(flyers.findAll()));
        var food = selector.select("bear-de", "food", "Food Pantry"); assertFalse(food.positions().isEmpty());
        assertTrue(food.positions().stream().allMatch(p -> p.item().provenance().communityId().equals("bear-de")));
        evidence.put("clock", "2026-10-16T16:00:00Z"); evidence.put("storedResources", resources.findAll().size());
        evidence.put("storedFlyers", flyers.findAll().size()); evidence.put("approvedFlyers", 0);
        evidence.put("featuredTopics", featured.selected().stream().map(t -> t.name()).toList());
        evidence.put("housing", housing); evidence.put("foodBear", food);
        evidence.put("foodWilmington", selector.select("wilmington-de", "food", "Food Pantry"));
        evidence.put("currentAuthoredHousingIds", inventory.current("wilmington-de", taxonomy.findByKey("housing").orElseThrow())
                .items().stream().map(i -> i.id).sorted().toList());
        evidence.put("housingHelpIds", content.helpFor("housing", null, "wilmington-de").stream().map(c -> c.id()).sorted().toList());
        evidence.put("scope", "Local stored catalog and admitted RSS cache; no remote feed or URL reachability claim");
        String directory = System.getProperty("im07.evidence.dir");
        if (directory != null) {
            Path out = Path.of(directory); Files.createDirectories(out);
            Files.writeString(out.resolve("real-item-audit.json"), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(evidence) + "\n");
        }
    }

    @Test void t19RealHomeownershipDurableCollisionUsesStableIdentityAndMissingNewnessDiagnostic() {
        var selection = selector.select("wilmington-de", "housing", "Homeownership");
        assertEquals("HA-011", selection.positions().get(0).item().id());
        assertEquals("STABLE_CANONICAL_ID", selection.positions().get(0).tieStage());
        assertTrue(selection.diagnostics().stream().anyMatch(d -> d.code().equals("STABLE_ITEM_ID_FALLBACK")
                && d.explanation().startsWith("DURABLE") && d.itemIds().equals(List.of("HA-011", "HA-013", "HA-021", "HA-022"))));
        assertTrue(selection.candidates().stream().allMatch(c -> c.limitations().contains("no_authentic_catalog_date:" + c.id())));
    }

    @Test void t20ResidentCardSerializationContainsNoInternalDiagnosticsOrReviewFields() throws Exception {
        var selection = selector.select("wilmington-de", "housing", "Homeownership");
        var tree = mapper.readTree(mapper.writeValueAsString(selection.positions().get(0).item().card()));
        for (String field : List.of("diagnostics", "roles", "helpAccess", "limitations", "editorialReview", "verified", "tieStage"))
            assertFalse(tree.has(field), field);
    }
}
