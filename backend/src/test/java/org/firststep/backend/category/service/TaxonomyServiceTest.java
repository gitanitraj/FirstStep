package org.firststep.backend.category.service;

import java.util.List;
import java.util.Set;

import org.firststep.backend.category.model.CategoryDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the loaded taxonomy against the REAL app/data/taxonomy.json rather
 * than a fixture. That is deliberate: this class exists to prove the file is the
 * single source of truth, so a test that read a fixture would prove nothing about
 * the vocabulary the application actually runs on.
 */
class TaxonomyServiceTest {

    private TaxonomyService service;

    @BeforeEach
    void setUp() {
        service = new TaxonomyService("../app/data");
    }

    @Test
    void shouldLoadAllTenCategoriesFromTaxonomyFile() {
        assertEquals(10, service.getCategories().size());
    }

    @Test
    void shouldPreserveAuthoredOrderAsDisplayOrder() {
        List<String> keys = service.getCategories().stream().map(CategoryDefinition::key).toList();

        assertEquals("housing", keys.get(0));
        assertEquals("community-support", keys.get(keys.size() - 1));
    }

    @Test
    void shouldPopulateEveryFieldOfALoadedCategory() {
        CategoryDefinition housing = service.findByKey("housing").orElseThrow();

        assertEquals("Housing", housing.label());
        assertEquals("🏠", housing.icon());
        assertTrue(housing.matchCategories().contains("Housing Assistance"));
        assertEquals(List.of("Housing"), housing.matchCategoryTags());
        assertEquals(9, housing.subcategories().size());
    }

    @Test
    void shouldReturnEmptyWhenCategoryKeyIsUnknown() {
        assertTrue(service.findByKey("nonexistent").isEmpty());
    }

    @Test
    void shouldMatchCategoryTagsCaseInsensitively() {
        CategoryDefinition housing = service.findByKey("housing").orElseThrow();

        assertTrue(service.matchesCategoryTags(housing, List.of("housing")));
        assertTrue(service.matchesCategoryTags(housing, List.of("Food", "Housing")));
    }

    @Test
    void shouldNotMatchWhenCategoryTagsAreNull() {
        CategoryDefinition housing = service.findByKey("housing").orElseThrow();

        assertFalse(service.matchesCategoryTags(housing, null));
    }

    @Test
    void shouldNotMatchDriftedVocabularyThatIsNotInTheTaxonomy() {
        CategoryDefinition health = service.findByKey("health").orElseThrow();

        assertFalse(service.matchesCategoryTags(health, List.of("Healthcare")));
    }

    @Test
    void shouldCollectDistinctSubcategoriesAcrossCategories() {
        Set<String> topics = service.allSubcategories();

        // "Eviction Prevention" is declared under BOTH housing and legal and must
        // appear once; "Thrift Store" and "Vouchers" are likewise dual-placed.
        assertTrue(topics.contains("Eviction Prevention"));
        assertEquals(topics.size(), Set.copyOf(topics).size());
    }

    @Test
    void shouldRecognizeTopicBelongingToCategory() {
        assertTrue(service.isTopicOf("housing", "Emergency Shelter"));
        assertFalse(service.isTopicOf("housing", "Food Pantry"));
    }

    @Test
    void shouldSlugifyTopicsContainingPunctuation() {
        assertEquals("child-care-early-learning",
                TaxonomyService.topicSlug("Child Care & Early Learning"));
        assertEquals("emergency-shelter", TaxonomyService.topicSlug("Emergency Shelter"));
    }

    @Test
    void shouldResolveTopicSlugBackToCanonicalName() {
        assertEquals("Counseling & Therapy",
                service.findTopicBySlug("health", "counseling-therapy").orElseThrow());
    }

    @Test
    void shouldReturnEmptyWhenTopicSlugIsUnknownInCategory() {
        assertTrue(service.findTopicBySlug("health", "emergency-shelter").isEmpty());
    }

    @Test
    void shouldFailFastWhenTaxonomyFileIsMissing() {
        // A missing vocabulary is fatal, not an empty list — ten silently empty
        // categories is far more expensive to diagnose than a startup failure.
        assertThrows(IllegalStateException.class, () -> new TaxonomyService("no/such/dir"));
    }
}
