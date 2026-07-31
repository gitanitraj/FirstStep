package org.firststep.backend.shared.classification;

import org.firststep.backend.category.service.TaxonomyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs against the REAL taxonomy so the authored keyword vocabulary is under
 * test, not just the scoring code. A fixture vocabulary would prove the
 * algorithm works and tell us nothing about whether classification works.
 */
class CategoryClassifierTest {

    private CategoryClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new CategoryClassifier(new TaxonomyService("../app/data"));
    }

    // ---- Tier 1: source vocabulary -----------------------------------------

    @Test
    void shouldClassifyByExactSourceCategoryWithFullConfidence() {
        Classification result = classifier.classify("Housing Assistance", "");

        assertEquals(java.util.List.of("Housing"), result.categoryTags());
        assertEquals(1.0, result.confidence());
    }

    @Test
    void shouldMatchSourceCategoryCaseInsensitivelyAndTrimmed() {
        assertEquals(java.util.List.of("Food"),
                classifier.classify("  food program  ", "").categoryTags());
    }

    @Test
    void shouldShortCircuitKeywordScoringWhenSourceCategoryMatches() {
        // Text screaming "housing" must not override an exact source mapping —
        // the deterministic tier wins outright.
        Classification result = classifier.classify(
                "Food Program", "eviction tenant landlord rental housing shelter");

        assertEquals(java.util.List.of("Food"), result.categoryTags());
    }

    @Test
    void shouldFallThroughToKeywordsWhenSourceCategoryIsUnknown() {
        Classification result = classifier.classify(
                "Some Vendor Category We Have Never Seen", "eviction tenant landlord");

        assertTrue(result.categoryTags().contains("Housing"));
    }

    // ---- Tier 2: keyword evidence ------------------------------------------

    @Test
    void shouldClassifyLegislationTextIntoCanonicalCategory() {
        Classification result = classifier.classify(
                null, "AN ACT RELATING TO EVICTION PROTECTIONS FOR TENANTS AND LANDLORDS.");

        assertEquals(java.util.List.of("Housing"), result.categoryTags());
        assertFalse(result.evidence().isEmpty());
    }

    @Test
    void shouldEmitOnlyCanonicalLabelsNeverDriftedVocabulary() {
        Classification result = classifier.classify(
                null, "AN ACT RELATING TO MEDICAID DENTAL COVERAGE AND CLINIC ACCESS.");

        assertTrue(result.categoryTags().contains("Health"));
        assertFalse(result.categoryTags().contains("Healthcare"));
        assertFalse(result.categoryTags().contains("Delaware Legislation"));
    }

    @Test
    void shouldClassifyNothingWhenTextHasNoRelevantKeywords() {
        Classification result = classifier.classify(null, "AN ACT CONCERNING THE STATE FLAG DESIGN.");

        assertTrue(result.categoryTags().isEmpty());
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldDeclineRatherThanClassifyOnASingleWeakHit() {
        // One single-word match scores 1, below MIN_SCORE. Declining is correct:
        // an unclassified item is honest, a wrongly-classified one is not.
        Classification result = classifier.classify(null, "the job");

        assertTrue(result.categoryTags().isEmpty());
    }

    @Test
    void shouldSuppressWeakIncidentalCategoriesBesideAStrongOne() {
        // The wetlands-bill failure mode: many categories picking up one loose
        // hit each. The relative floor keeps only what is genuinely competitive.
        Classification result = classifier.classify(null,
                "AN ACT RELATING TO EVICTION, TENANTS, LANDLORDS, RENTAL ASSISTANCE "
                        + "AND PUBLIC HOUSING, which mentions one library.");

        assertEquals(java.util.List.of("Housing"), result.categoryTags());
    }

    @Test
    void shouldKeepBothCategoriesWhenEvidenceIsGenuinelyBalanced() {
        Classification result = classifier.classify(null,
                "eviction tenant landlord rental assistance and utility bill "
                        + "electric energy shutoff weatherization");

        assertTrue(result.categoryTags().contains("Housing"));
        assertTrue(result.categoryTags().contains("Utilities"));
    }

    @Test
    void shouldRankStrongestCategoryFirst() {
        Classification result = classifier.classify(null,
                "eviction tenant landlord mortgage foreclosure homeownership plus one clinic");

        assertEquals("Housing", result.categoryTags().get(0));
    }

    @Test
    void shouldReturnNullSubcategoryWhenNoSubcategoryKeywordsAreAuthored() {
        // As of F2 no subcategoryKeywords exist, so topic resolution declines
        // rather than guessing. Locked in so adding them later is a visible change.
        Classification result = classifier.classify(
                null, "AN ACT RELATING TO EVICTION PROTECTIONS FOR TENANTS.");

        assertNull(result.subcategory());
    }

    @Test
    void shouldReturnNoneForEmptyText() {
        assertTrue(classifier.classify(null, "").isEmpty());
        assertTrue(classifier.classify(null, null).isEmpty());
    }

    @Test
    void shouldReportConfidenceProportionalToEvidence() {
        Classification weak = classifier.classify(null, "eviction tenant");
        Classification strong = classifier.classify(null,
                "eviction tenant landlord mortgage foreclosure public housing rental assistance");

        assertTrue(strong.confidence() > weak.confidence());
        assertTrue(strong.confidence() <= 1.0);
    }
}
