package org.firststep.backend.shared.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Turns free text into a list of normalized word tokens, and answers whether a
 * keyword or multi-word phrase appears in it.
 *
 * <p><b>Whole-word matching by construction.</b> The classifier this feeds
 * replaced raw {@code text.contains(keyword)}, which matched on substrings:
 * {@code "aid"} matched <i>said</i> and <i>paid</i>, {@code "care"} matched
 * <i>careful</i>, {@code "farm"} matched <i>farmer</i>. That is why a wetlands
 * bill once classified into five categories at once. Here a keyword matches only
 * when it equals a whole token, so the class of bug is gone rather than patched
 * — a word-boundary regex would fix the same symptom, but tokenizing once and
 * comparing tokens is cheaper per keyword and gives phrase matching for free.
 *
 * <p><b>Phrases match as phrases.</b> A multi-word keyword ("mental health",
 * "manufactured home") matches only where its tokens appear <i>contiguously</i>,
 * so a document mentioning mental illness and physical health separately does
 * not match "mental health".
 */
public final class Tokenizer {

    /**
     * Words carrying no classifying signal. Kept deliberately short: this is not
     * a general-purpose stopword list, it only needs to stop noise words from
     * ever being authored as accidental single-token keywords.
     */
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "in", "is",
            "it", "its", "of", "on", "or", "that", "the", "this", "to", "was", "were", "with");

    private Tokenizer() {
    }

    /**
     * Lowercase, split on any run of non-alphanumerics, drop stopwords, and
     * singularize. Order is preserved because phrase matching depends on it.
     */
    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String raw : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (!raw.isEmpty() && !STOPWORDS.contains(raw)) {
                tokens.add(singularize(raw));
            }
        }
        return tokens;
    }

    /**
     * Minimal plural stripping — NOT a stemmer.
     *
     * <p>This exists because real text and authored keywords disagree about
     * number constantly: a bill says "TENANTS AND LANDLORDS", the taxonomy says
     * "tenant" and "landlord". Without it the vocabulary would have to list every
     * word twice and would still miss whatever an author forgot.
     *
     * <p>The rules are deliberately conservative, because over-stemming
     * manufactures false matches — the exact failure mode this class was built to
     * remove. Guards: nothing under four characters is touched (gas, bus), and
     * -ss / -us / -is endings are left alone (business, status, analysis).
     *
     * <p>The same normalization is applied to keywords and to text, so the two
     * meet in the middle rather than either side having to anticipate the other.
     * A full stemmer (Porter/Snowball) was rejected: it would pull in a
     * dependency and stem aggressively enough ("housing" → "hous") to need its
     * own tuning pass, for a vocabulary of a few hundred hand-authored terms.
     */
    static String singularize(String token) {
        if (token.length() <= 3) {
            return token;
        }
        if (token.endsWith("ss") || token.endsWith("us") || token.endsWith("is")) {
            return token;
        }
        if (token.length() > 4 && token.endsWith("ies")) {
            return token.substring(0, token.length() - 3) + "y";   // libraries -> library
        }
        if (token.length() > 4 && token.endsWith("es")
                && endsWithSibilantBefore(token)) {
            return token.substring(0, token.length() - 2);          // dishes -> dish
        }
        if (token.endsWith("s")) {
            return token.substring(0, token.length() - 1);          // tenants -> tenant
        }
        return token;
    }

    /** True when the "es" is doing real plural work: -ches, -shes, -xes, -zes, -ses. */
    private static boolean endsWithSibilantBefore(String token) {
        String stem = token.substring(0, token.length() - 2);
        return stem.endsWith("ch") || stem.endsWith("sh")
                || stem.endsWith("x") || stem.endsWith("z") || stem.endsWith("s");
    }

    /**
     * True when {@code keyword} — a single word or a phrase — appears in
     * {@code tokens}. The keyword is tokenized by the same rules as the text, so
     * "information and referral" and "information referral" both work: the
     * stopword drops out of each side identically.
     */
    public static boolean contains(List<String> tokens, String keyword) {
        List<String> phrase = tokenize(keyword);
        if (phrase.isEmpty() || phrase.size() > tokens.size()) {
            return false;
        }
        for (int i = 0; i <= tokens.size() - phrase.size(); i++) {
            if (tokens.subList(i, i + phrase.size()).equals(phrase)) {
                return true;
            }
        }
        return false;
    }

    /**
     * How much evidence a matched keyword is worth: its token count. A two-word
     * phrase is stronger evidence than a single word, because the odds of an
     * unrelated document containing "manufactured home" contiguously are far
     * lower than of it containing "home". This is what lets a specific phrase
     * outrank several vague single-word hits.
     */
    public static int weight(String keyword) {
        return tokenize(keyword).size();
    }
}
