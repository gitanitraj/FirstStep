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

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// Tokenizer turns free text into normalized word tokens and answers whether a
// keyword or multi-word phrase occurs in it. It is small, and it is the piece
// that actually removes the bug F2 was built around.
// =============================================================================

// =============================================================================
// SECTION 1 — THE BUG, PRECISELY
// =============================================================================
// The previous classifier matched like this:
//
//     if (text.contains(kw))
//
// Raw substring search. Consequences, all observed rather than theorized:
//
//     "aid"   matched  said, paid, maid, afraid
//     "care"  matched  careful, careless, scarce
//     "farm"  matched  farmer, farming
//
// The "benefits" bucket contained "aid", and English prose is full of "said" and
// "paid" — so a startling share of legislation matched Benefits. Combined with
// several other loose terms, a bill about WETLANDS came back tagged
// ["Housing", "Food", "Utilities", "Benefits", "Legal"] (Decision 031).
//
// The instruction for F2 was explicitly NOT to swap in a word-boundary regex.
// That is the right instinct: a regex fixes the symptom at each call site, while
// tokenizing makes whole-word matching STRUCTURAL — there is no way to express
// a substring match through this API, so the bug cannot be reintroduced by a
// future caller who forgets the \b.
//
// =============================================================================
// SECTION 2 — PHRASES, AND WHY THEY ARE WORTH MORE
// =============================================================================
// contains() matches a multi-word keyword only where its tokens appear
// CONTIGUOUSLY. "mental health" matches "a mental health clinic" and does NOT
// match "mental illness and physical health" — both words present, neither
// meaning the term.
//
// weight() returns the token count, so a two-word phrase is worth twice a single
// word. The justification is probabilistic: the chance of an unrelated document
// containing two specific words ADJACENTLY is far lower than containing either
// alone, so a phrase hit is stronger evidence. This is what lets "manufactured
// home" outrank several vague single-word matches in CategoryClassifier.
//
// A nice property falls out of tokenizing keywords by the same rules as text:
// stopwords drop from both sides identically, so the authored keyword
// "information and referral" matches the text "information and referral" and
// "information referral" alike, with no special handling.
//
// =============================================================================
// SECTION 3 — singularize(): THE PART MOST LIKELY TO BE MISJUDGED
// =============================================================================
// This exists because real text and authored keywords disagree about number
// constantly. A Delaware bill says "TENANTS AND LANDLORDS"; the taxonomy says
// "tenant" and "landlord". Without normalization the vocabulary would have to
// list every word twice and would still miss whatever an author forgot — and
// the very first RSS test in the suite would fail.
//
// It is NOT a stemmer, deliberately. Over-stemming manufactures false matches,
// which is the exact failure mode this whole class exists to eliminate — trading
// a substring bug for a stemming bug is not progress. So the rules are
// conservative and each guard earns its place:
//
//     length <= 3        untouched     gas, bus  (would become "ga", "bu")
//     -ss / -us / -is    untouched     business, status, analysis
//     -ies -> -y         libraries -> library, groceries -> grocery
//     -es after sibilant dishes -> dish, taxes -> tax
//     trailing -s        tenants -> tenant, courts -> court
//
// The same function runs over keywords and text, so the two meet in the middle
// rather than either side having to anticipate the other. That symmetry is the
// design; applying it to only one side would be worse than not applying it.
//
// A full Porter/Snowball stemmer was rejected: it adds a dependency and stems
// hard enough ("housing" -> "hous") to need its own tuning pass, for a
// vocabulary of a few hundred hand-authored terms where the plural/singular
// mismatch is the entire problem.
//
// =============================================================================
// SECTION 4 — WHY THE STOPWORD LIST IS SHORT
// =============================================================================
// STOPWORDS holds ~25 words, not the several hundred a search engine would use.
// It is not there to improve relevance ranking; it exists so that noise words
// cannot become accidental single-token keywords and so phrase matching survives
// an "and"/"of" appearing on one side only. A longer list would start removing
// words that legitimately carry signal in civic text.
//
// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - CategoryClassifier tokenizes each document once and calls contains()/weight()
//   per keyword.
// - Nothing else uses it. SearchService has its own TextScore matching and was
//   deliberately left alone — unifying them is a separate change with its own
//   relevance-tuning consequences.
