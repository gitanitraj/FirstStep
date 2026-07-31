package org.firststep.backend.shared.classification;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenizerTest {

    @Test
    void shouldSplitOnNonAlphanumericsAndLowercase() {
        assertEquals(List.of("eviction", "protection", "tenant"),
                Tokenizer.tokenize("EVICTION PROTECTIONS, tenant!"));
    }

    @Test
    void shouldDropStopwords() {
        assertEquals(List.of("act", "relating", "housing"),
                Tokenizer.tokenize("An Act relating to the housing"));
    }

    @Test
    void shouldReturnEmptyForNullOrBlankText() {
        assertTrue(Tokenizer.tokenize(null).isEmpty());
        assertTrue(Tokenizer.tokenize("   ").isEmpty());
    }

    // ---- The bug this class exists to remove -------------------------------

    @Test
    void shouldNotMatchKeywordAsSubstringOfAnotherWord() {
        // The whole reason for tokenizing: text.contains("aid") matched "said"
        // and "paid", which is how a wetlands bill picked up five categories.
        List<String> tokens = Tokenizer.tokenize("The board said it paid the maid");

        assertFalse(Tokenizer.contains(tokens, "aid"));
    }

    @Test
    void shouldNotMatchCareInsideCareful() {
        assertFalse(Tokenizer.contains(Tokenizer.tokenize("a careful review"), "care"));
    }

    @Test
    void shouldNotMatchFarmInsideFarmer() {
        assertFalse(Tokenizer.contains(Tokenizer.tokenize("the farmer arrived"), "farm"));
    }

    @Test
    void shouldMatchWholeWord() {
        assertTrue(Tokenizer.contains(Tokenizer.tokenize("legal aid is available"), "legal aid"));
    }

    // ---- Phrases -----------------------------------------------------------

    @Test
    void shouldMatchMultiWordPhraseOnlyWhenContiguous() {
        assertTrue(Tokenizer.contains(Tokenizer.tokenize("a mental health clinic"), "mental health"));
    }

    @Test
    void shouldNotMatchPhraseWhenWordsAppearSeparately() {
        List<String> tokens = Tokenizer.tokenize("mental illness and physical health");

        assertFalse(Tokenizer.contains(tokens, "mental health"));
    }

    @Test
    void shouldMatchPhraseAcrossDroppedStopwords() {
        // "information and referral" tokenizes identically on both sides because
        // the stopword is removed from the keyword and the text alike.
        assertTrue(Tokenizer.contains(
                Tokenizer.tokenize("provides information and referral services"),
                "information and referral"));
    }

    @Test
    void shouldWeightPhrasesHigherThanSingleWords() {
        assertEquals(1, Tokenizer.weight("housing"));
        assertEquals(2, Tokenizer.weight("manufactured home"));
    }

    // ---- Singularization ---------------------------------------------------

    @Test
    void shouldMatchPluralTextAgainstSingularKeyword() {
        // Real case: a bill says "TENANTS AND LANDLORDS", the taxonomy says
        // "tenant" and "landlord".
        List<String> tokens = Tokenizer.tokenize("AN ACT RELATING TO TENANTS AND LANDLORDS");

        assertTrue(Tokenizer.contains(tokens, "tenant"));
        assertTrue(Tokenizer.contains(tokens, "landlord"));
    }

    @Test
    void shouldSingularizeIesToY() {
        assertEquals("library", Tokenizer.singularize("libraries"));
        assertEquals("grocery", Tokenizer.singularize("groceries"));
    }

    @Test
    void shouldStripEsAfterSibilant() {
        assertEquals("dish", Tokenizer.singularize("dishes"));
    }

    @Test
    void shouldNotOverStemShortOrNonPluralWords() {
        // Guards against the failure mode this class was built to avoid —
        // manufacturing matches by mangling words.
        assertEquals("gas", Tokenizer.singularize("gas"));
        assertEquals("bus", Tokenizer.singularize("bus"));
        assertEquals("business", Tokenizer.singularize("business"));
        assertEquals("status", Tokenizer.singularize("status"));
        assertEquals("analysis", Tokenizer.singularize("analysis"));
        assertEquals("housing", Tokenizer.singularize("housing"));
    }
}
