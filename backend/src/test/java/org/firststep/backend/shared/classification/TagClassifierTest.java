package org.firststep.backend.shared.classification;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagClassifierTest {

    private final TagClassifier classifier = new TagClassifier();

    @Test
    void shouldAppendEvidenceAfterExistingTags() {
        List<String> merged = classifier.mergeTags(List.of("no-ID-required"), List.of("eviction", "tenant"));

        assertEquals(List.of("no-ID-required", "eviction", "tenant"), merged);
    }

    @Test
    void shouldNotDuplicateATermThatWasAlreadyAuthored() {
        List<String> merged = classifier.mergeTags(List.of("eviction"), List.of("eviction", "tenant"));

        assertEquals(List.of("eviction", "tenant"), merged);
    }

    @Test
    void shouldDeduplicateCaseInsensitively() {
        // A hand-authored "Eviction" should not gain a machine "eviction" beside it.
        List<String> merged = classifier.mergeTags(List.of("Eviction"), List.of("eviction"));

        assertEquals(List.of("Eviction"), merged);
    }

    @Test
    void shouldHandleNullExistingTags() {
        assertEquals(List.of("tenant"), classifier.mergeTags(null, List.of("tenant")));
    }

    @Test
    void shouldPreserveExistingTagsWhenThereIsNoEvidence() {
        assertEquals(List.of("Free", "Community"),
                classifier.mergeTags(List.of("Free", "Community"), List.of()));
    }

    @Test
    void shouldSkipBlankAndNullEntries() {
        List<String> merged = classifier.mergeTags(java.util.Arrays.asList("Free", null, "  "), List.of("tenant"));

        assertEquals(List.of("Free", "tenant"), merged);
    }

    @Test
    void shouldNeverBeHandedCategoryNames() {
        // Contract note rather than behavior: TagClassifier merges whatever it is
        // given, and CategoryClassifier only ever hands it matched KEYWORDS.
        // CivicContentClassifierTest.shouldNotPutCategoryNamesIntoDescriptiveTags
        // proves the end-to-end guarantee; this documents the seam.
        List<String> merged = classifier.mergeTags(List.of(), List.of("eviction", "tenant", "landlord"));

        assertTrue(merged.stream().noneMatch(t -> t.equalsIgnoreCase("Housing")));
    }
}
