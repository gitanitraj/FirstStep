package org.firststep.backend.category.selection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Isolated seven-stage lexicographic counterexamples; metadata joins are separately integrated. */
class FeaturedTopicOrderingTest {
    private final LocalDate day = LocalDate.of(2026, 10, 16);
    private FeaturedTopicSelector.TopicEvidence e(String key, int help, int coverage, int direct, int context, LocalDate date) {
        return new FeaturedTopicSelector.TopicEvidence(key, key, help, coverage, direct, context, date,
                List.of(), Map.of(), List.of(), List.of(), List.of());
    }
    private FeaturedTopicSelector.Selection order(List<FeaturedTopicSelector.TopicEvidence> input) {
        return FeaturedTopicSelector.order("second-community", "housing", day, input);
    }
    private void before(FeaturedTopicSelector.TopicEvidence winner, FeaturedTopicSelector.TopicEvidence loser) {
        assertEquals(List.of(winner.key(), loser.key()), order(List.of(loser, winner)).selected().stream().map(FeaturedTopicSelector.TopicEvidence::key).toList());
    }
    @Test void actionableCountPrecedesEveryLaterSignalIncludingContextVolume() {
        before(e("z-help", 2, 0, 0, 0, null), e("a-context", 1, 3, 0, 500, day));
    }
    @Test void distinctRoleCoveragePrecedesDirectActionsContextAndNewness() {
        before(e("z-complete", 2, 3, 0, 0, null), e("a-action", 2, 2, 2, 500, day));
    }
    @Test void directActionPrecedesContextAndNewness() {
        before(e("z-action", 2, 3, 1, 1, null), e("a-context", 2, 3, 0, 500, day));
    }
    @Test void relevantContextIsLateButPrecedesNewness() {
        before(e("z-context", 2, 2, 1, 1, null), e("a-new", 2, 2, 1, 0, day));
    }
    @Test void authenticNewnessIsConsideredOnlyAfterFourStagesTieAndMissingFallsThrough() {
        before(e("z-new", 2, 2, 1, 1, day), e("a-old", 2, 2, 1, 1, day.minusDays(1)));
        before(e("z-known", 2, 2, 1, 1, day), e("a-missing", 2, 2, 1, 1, null));
    }
    @Test void stableKeyTieEmitsScopedDiagnosticAndDoesNotBlockDelivery() {
        var result = order(List.of(e("z", 1, 0, 0, 0, null), e("a", 1, 0, 0, 0, null)));
        assertEquals(List.of("a", "z"), result.selected().stream().map(FeaturedTopicSelector.TopicEvidence::key).toList());
        assertEquals(1, result.diagnostics().size());
        assertEquals("STABLE_TOPIC_KEY_FALLBACK", result.diagnostics().get(0).code());
        assertEquals("second-community", result.diagnostics().get(0).communityId());
        assertEquals(List.of("a", "z"), result.diagnostics().get(0).topicKeys());
    }
    @Test void shuffledInputsProduceExactlyEqualEvidenceAndCutoffDiagnostics() {
        var input = new ArrayList<>(List.of(e("a", 1, 0, 0, 0, null), e("b", 1, 0, 0, 0, null),
                e("c", 1, 0, 0, 0, null), e("d", 1, 0, 0, 0, null), e("e", 1, 0, 0, 0, null)));
        var expected = order(input);
        for (int seed = 0; seed < 30; seed++) {
            Collections.shuffle(input, new Random(seed));
            assertEquals(expected, order(input));
        }
        assertEquals(4, expected.selected().size());
        assertEquals(5, expected.diagnostics().get(0).topicKeys().size());
    }
    @Test void oneMultiTopicFixtureExercisesTheEntireHierarchyAndTechnicalDiagnostic() {
        var result = order(List.of(e("z-help", 3, 0, 0, 0, null), e("y-roles", 2, 3, 0, 0, null),
                e("x-actions", 2, 2, 2, 0, null), e("w-context", 2, 2, 1, 2, null),
                e("v-new", 2, 2, 1, 1, day), e("u-old", 2, 2, 1, 1, day.minusDays(1)),
                e("b-missing", 2, 2, 1, 1, null), e("a-missing", 2, 2, 1, 1, null)));
        assertEquals(List.of("z-help", "y-roles", "x-actions", "w-context", "v-new", "u-old", "a-missing", "b-missing"),
                result.candidates().stream().map(FeaturedTopicSelector.TopicEvidence::key).toList());
        assertEquals(List.of("a-missing", "b-missing"), result.diagnostics().get(0).topicKeys());
        assertEquals(4, result.selected().size());
    }

    @Test void fewerThanFourNonzeroHelpTopicsAreNeverPaddedWithContextOnlyTopics() {
        var result = order(List.of(e("real", 1, 0, 0, 0, null), e("context-only", 0, 3, 0, 100, day)));
        assertEquals(List.of("real"), result.selected().stream().map(FeaturedTopicSelector.TopicEvidence::key).toList());
        assertEquals(2, result.candidates().size());
    }
}
