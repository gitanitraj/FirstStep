package org.firststep.backend.shared.util;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextScoreTest {

    @Test
    void shouldReturnFivePointsWhenFieldContainsQuery() {
        assertEquals(5, TextScore.match("food", "Free food pantry"));
    }

    @Test
    void shouldReturnZeroWhenFieldDoesNotContainQuery() {
        assertEquals(0, TextScore.match("housing", "Free food pantry"));
    }

    @Test
    void shouldMatchCaseInsensitively() {
        assertEquals(5, TextScore.match("FOOD", "free food pantry"));
    }

    @Test
    void shouldReturnZeroWhenQueryIsNullOrBlank() {
        assertEquals(0, TextScore.match(null, "Free food pantry"));
        assertEquals(0, TextScore.match("   ", "Free food pantry"));
    }

    @Test
    void shouldReturnZeroWhenFieldIsNullOrBlank() {
        assertEquals(0, TextScore.match("food", (String) null));
        assertEquals(0, TextScore.match("food", "   "));
    }

    @Test
    void shouldReturnFirstMatchingScoreForListOfFields() {
        assertEquals(5, TextScore.match("housing", List.of("food", "housing assistance", "housing")));
    }

    @Test
    void shouldReturnZeroForListOfFieldsWithNoMatch() {
        assertEquals(0, TextScore.match("clothing", List.of("food", "housing")));
    }

    @Test
    void shouldReturnZeroForNullOrEmptyListOfFields() {
        assertEquals(0, TextScore.match("food", (List<String>) null));
        assertEquals(0, TextScore.match("food", List.of()));
    }

    @Test
    void shouldReturnFirstMatchingScoreForArrayOfFields() {
        assertEquals(5, TextScore.match("youth", new String[] {"eviction", "youth program", "volunteer"}));
    }

    @Test
    void shouldReturnZeroForArrayOfFieldsWithNoMatch() {
        assertEquals(0, TextScore.match("clothing", new String[] {"food", "housing"}));
    }

    @Test
    void shouldLowercaseAndTrimNonNullString() {
        assertEquals("food pantry", TextScore.lower("  Food Pantry  "));
    }

    @Test
    void shouldReturnEmptyStringWhenLoweringNull() {
        assertEquals("", TextScore.lower(null));
    }
}
