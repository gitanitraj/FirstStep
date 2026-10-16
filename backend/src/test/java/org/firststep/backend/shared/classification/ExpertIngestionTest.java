package org.firststep.backend.shared.classification;

import java.util.List;
import java.util.stream.Stream;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.shared.model.CivicContent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class ExpertIngestionTest {
    static Stream<CivicContent> expertClasses() { return Stream.of(new FAQ(), new ExpertAnswer()); }

    @ParameterizedTest
    @MethodSource("expertClasses")
    void shouldNotManufactureExpertCategoryFromProseOrTopic(CivicContent item) {
        item.title = "Housing eviction prevention and rental assistance";
        item.subcategory = "Eviction Prevention";
        ClassifierFixture.real().classify(item);
        assertNull(item.categoryTags, "Topic/prose are not editorial Category authorization");
    }

    @ParameterizedTest
    @MethodSource("expertClasses")
    void shouldPreserveExplicitCategoryWithoutInventingATopic(CivicContent item) {
        item.title = "Housing eviction prevention and rental assistance";
        item.categoryTags = List.of("Housing");
        ClassifierFixture.real().classify(item);
        assertEquals(List.of("Housing"), item.categoryTags);
        assertNull(item.subcategory, "Category-only expert records are valid without inferred Topics");
    }
}
