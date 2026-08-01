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
        classifier = new CategoryClassifier(new TaxonomyService("../app/data"),
                new SourceMappingService("../app/data"));
    }

    // ---- Tier 1: source vocabulary -----------------------------------------

    @Test
    void shouldClassifyByExactSourceCategoryWithFullConfidence() {
        ClassificationResult result = classifier.classify("dscyf-directory", "Housing Assistance", "");

        assertEquals(java.util.List.of("Housing"), result.categoryTags());
        assertEquals(1.0, result.confidence());
    }

    @Test
    void shouldMatchSourceCategoryCaseInsensitivelyAndTrimmed() {
        assertEquals(java.util.List.of("Food"),
                classifier.classify("dscyf-directory", "  food program  ", "").categoryTags());
    }

    @Test
    void shouldShortCircuitKeywordScoringWhenSourceCategoryMatches() {
        // Text screaming "housing" must not override an exact source mapping —
        // the deterministic tier wins outright.
        ClassificationResult result = classifier.classify("dscyf-directory", "Food Program", "eviction tenant landlord rental housing shelter");

        assertEquals(java.util.List.of("Food"), result.categoryTags());
    }

    @Test
    void shouldFallThroughToKeywordsWhenSourceCategoryIsUnknown() {
        ClassificationResult result = classifier.classify("dscyf-directory", "Some Vendor Category We Have Never Seen", "eviction tenant landlord");

        assertTrue(result.categoryTags().contains("Housing"));
    }

    // ---- Tier 2: keyword evidence ------------------------------------------

    @Test
    void shouldClassifyLegislationTextIntoCanonicalCategory() {
        ClassificationResult result = classifier.classify(null, null, "AN ACT RELATING TO EVICTION PROTECTIONS FOR TENANTS AND LANDLORDS.");

        assertEquals(java.util.List.of("Housing"), result.categoryTags());
        assertFalse(result.evidence().isEmpty());
    }

    @Test
    void shouldEmitOnlyCanonicalLabelsNeverDriftedVocabulary() {
        ClassificationResult result = classifier.classify(null, null, "AN ACT RELATING TO MEDICAID DENTAL COVERAGE AND CLINIC ACCESS.");

        assertTrue(result.categoryTags().contains("Health"));
        assertFalse(result.categoryTags().contains("Healthcare"));
        assertFalse(result.categoryTags().contains("Delaware Legislation"));
    }

    @Test
    void shouldClassifyNothingWhenTextHasNoRelevantKeywords() {
        ClassificationResult result = classifier.classify(null, null, "AN ACT CONCERNING THE STATE FLAG DESIGN.");

        assertTrue(result.categoryTags().isEmpty());
        assertFalse(result.relevant());
        assertEquals("no category keywords matched", result.reason());
    }

    @Test
    void shouldDeclineRatherThanClassifyOnASingleWeakHit() {
        // One single-word match scores 1, below MIN_SCORE. Declining is correct:
        // an unclassified item is honest, a wrongly-classified one is not. The
        // engine is CONSERVATIVE BY DESIGN — the fix for a miss like this is a
        // richer vocabulary, never a lower threshold.
        ClassificationResult result = classifier.classify(null, null, "the job");

        assertFalse(result.relevant());
        assertTrue(result.reason().contains("below threshold"));
    }

    @Test
    void shouldSuppressWeakIncidentalCategoriesBesideAStrongOne() {
        // The wetlands-bill failure mode: many categories picking up one loose
        // hit each. The relative floor keeps only what is genuinely competitive.
        ClassificationResult result = classifier.classify(null, null,
                "AN ACT RELATING TO EVICTION, TENANTS, LANDLORDS, RENTAL ASSISTANCE "
                        + "AND PUBLIC HOUSING, which mentions one library.");

        assertEquals(java.util.List.of("Housing"), result.categoryTags());
    }

    @Test
    void shouldKeepBothCategoriesWhenEvidenceIsGenuinelyBalanced() {
        ClassificationResult result = classifier.classify(null, null,
                "eviction tenant landlord rental assistance and utility bill "
                        + "electric energy shutoff weatherization");

        assertTrue(result.categoryTags().contains("Housing"));
        assertTrue(result.categoryTags().contains("Utilities"));
    }

    @Test
    void shouldRankStrongestCategoryFirst() {
        ClassificationResult result = classifier.classify(null, null,
                "eviction tenant landlord mortgage foreclosure homeownership plus one clinic");

        assertEquals("Housing", result.categoryTags().get(0));
    }

    @Test
    void shouldReturnNullSubcategoryWhenNoSubcategoryKeywordsAreAuthored() {
        // As of F2 no subcategoryKeywords exist, so topic resolution declines
        // rather than guessing. Locked in so adding them later is a visible change.
        ClassificationResult result = classifier.classify(null, null, "AN ACT RELATING TO EVICTION PROTECTIONS FOR TENANTS.");

        assertNull(result.subcategory());
    }

    @Test
    void shouldReturnIrrelevantForEmptyText() {
        assertFalse(classifier.classify(null, null, "").relevant());
        assertFalse(classifier.classify(null, null, null).relevant());
    }

    @Test
    void shouldMarkSourceMappedAndKeywordClassifiedContentRelevant() {
        assertTrue(classifier.classify("dscyf-directory", "Housing Assistance", "").relevant());
        assertTrue(classifier.classify(null, null, "eviction tenant landlord").relevant());
    }

    @Test
    void shouldExplainItselfInTheReason() {
        // `reason` is what makes the conservative principle auditable — a
        // declined item must say why so the vocabulary can be improved.
        assertTrue(classifier.classify("dscyf-directory", "Housing Assistance", "").reason()
                .contains("source mapping"));
        assertTrue(classifier.classify(null, null, "eviction tenant landlord").reason()
                .startsWith("matched:"));
    }

    @Test
    void shouldIgnoreSourceMappingsFromAnUnknownSource() {
        // A raw category only translates for the source that declares it —
        // two providers may use the same word for different things.
        ClassificationResult result = classifier.classify("some-other-provider", "Housing Assistance", "");

        assertFalse(result.relevant());
    }

    @Test
    void shouldReportConfidenceProportionalToEvidence() {
        ClassificationResult weak = classifier.classify(null, null, "eviction tenant");
        ClassificationResult strong = classifier.classify(null, null,
                "eviction tenant landlord mortgage foreclosure public housing rental assistance");

        assertTrue(strong.confidence() > weak.confidence());
        assertTrue(strong.confidence() <= 1.0);
    }
}
