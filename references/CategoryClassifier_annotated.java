package org.firststep.backend.shared.classification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.firststep.backend.category.model.CategoryDefinition;
import org.firststep.backend.category.service.TaxonomyService;
import org.springframework.stereotype.Component;

/**
 * Determines the canonical category and subcategory for a piece of content.
 *
 * <p>Two tiers, tried in order.
 *
 * <p><b>Deterministic translation first, inference only after.</b> That ordering
 * is the design, not an optimization: an exact answer should never be displaced
 * by a probabilistic one.
 *
 * <p><b>Tier 1 — source adaptation (deterministic, confidence 1.0).</b> When an
 * item arrives carrying an upstream category, {@link SourceMappingService}
 * translates it and keyword scoring never runs. This is what keeps all 229
 * resources classified exactly as they are. The mapping table is hand-curated
 * with 100% coverage of the DSCYF directory; replacing it with keyword guessing
 * would trade a correct answer for a likely one.
 *
 * <p>The table used to live in {@code taxonomy.json} as {@code matchCategories}.
 * Slice F2.1 moved it to {@code source-mappings.json} because it is a provider's
 * vocabulary rather than First Step's — the editorial taxonomy should not carry
 * any one vendor's words. Two moves, both about layering: F2 took this
 * translation out of the QUERY layer, F2.1 took it out of the DOMAIN model.
 *
 * <p><b>Tier 2 — keyword evidence (scored).</b> For free text — legislation,
 * flyers, expert answers — each category is scored by the distinct
 * keywords/phrases from taxonomy.json that appear in it.
 *
 * <p>Either tier can conclude the content does not belong in First Step at all;
 * that verdict travels on {@link ClassificationResult#relevant()}.
 */
@Component
public class CategoryClassifier {

    /**
     * Minimum evidence before anything is classified at all. Two means either a
     * single two-word phrase or two distinct single-word hits — one stray word is
     * never enough. Set to 1, every bill mentioning "job" once becomes Employment.
     */
    static final int MIN_SCORE = 2;

    /**
     * A category is kept only if it scores at least half the leading category.
     * This is what stops the multi-tagging that made a wetlands bill come back as
     * Housing + Food + Utilities + Benefits + Legal — the weak incidental matches
     * fall away while a genuinely dual-topic item (a bill on both rental
     * assistance and utility shutoffs) still keeps both.
     *
     * <p>Chosen over a hard "max 3 categories" cap because a cap is arbitrary:
     * it would truncate a genuinely four-category item and still admit three bad
     * matches for an item that should have none.
     */
    static final double RELATIVE_FLOOR = 0.5;

    /** Score at which confidence saturates: roughly three solid hits. */
    private static final double CONFIDENT_SCORE = 6.0;

    private final TaxonomyService taxonomyService;
    private final SourceMappingService sourceMappingService;

    public CategoryClassifier(TaxonomyService taxonomyService, SourceMappingService sourceMappingService) {
        this.taxonomyService = taxonomyService;
        this.sourceMappingService = sourceMappingService;
    }

    /**
     * @param sourceId          which upstream provider this content came from, or
     *                          null when it has none (news, flyers, RSS)
     * @param rawSourceCategory the item's upstream category string, or null
     * @param text              the item's classifiable prose
     */
    public ClassificationResult classify(String sourceId, String rawSourceCategory, String text) {
        ClassificationResult bySource = classifyBySourceVocabulary(sourceId, rawSourceCategory);
        if (bySource != null) {
            return bySource;
        }
        return classifyByKeywords(text);
    }

    /**
     * Tier 1 — deterministic source adaptation. Returns null (not a result) to
     * mean "no opinion, try tier 2", so an unmapped value falls through to
     * inference rather than being reported as a confident non-answer.
     */
    private ClassificationResult classifyBySourceVocabulary(String sourceId, String rawSourceCategory) {
        Optional<String> key = sourceMappingService.categoryKeyFor(sourceId, rawSourceCategory);
        if (key.isEmpty()) {
            return null;
        }
        Optional<CategoryDefinition> definition = taxonomyService.findByKey(key.get());
        if (definition.isEmpty()) {
            // The mapping names a category the taxonomy does not have — a data
            // error in source-mappings.json. Fall through to keywords rather
            // than emitting a category that does not exist.
            System.err.println("source-mappings.json maps '" + rawSourceCategory
                    + "' to unknown category key '" + key.get() + "'");
            return null;
        }
        // Evidence is deliberately EMPTY, not the source category. Evidence feeds
        // TagClassifier, and a source category is upstream vocabulary ("Housing
        // Assistance", "Before/After School Care") — putting it in descriptive
        // tags would push a category name into the field that must never hold
        // one, and pollute search with DSCYF's words. Provenance is already
        // preserved on Resource.category.
        return ClassificationResult.relevant(
                List.of(definition.get().label()), null, 1.0,
                "source mapping (" + sourceId + "): " + rawSourceCategory, List.of());
    }

    /** Tier 2 — keyword inference, for content no source mapping covers. */
    private ClassificationResult classifyByKeywords(String text) {
        List<String> tokens = Tokenizer.tokenize(text);
        if (tokens.isEmpty()) {
            return ClassificationResult.irrelevant("no classifiable text");
        }

        Map<CategoryDefinition, Integer> scores = new LinkedHashMap<>();
        Map<CategoryDefinition, List<String>> matched = new LinkedHashMap<>();

        for (CategoryDefinition definition : taxonomyService.getCategories()) {
            int score = 0;
            List<String> hits = new ArrayList<>();
            // A Set because two authored keywords can normalize to the same token
            // ("utility"/"utilities"); counting both would double the evidence.
            Set<String> counted = new LinkedHashSet<>();
            for (String keyword : definition.keywordsOrEmpty()) {
                List<String> normalized = Tokenizer.tokenize(keyword);
                if (normalized.isEmpty() || !counted.add(String.join(" ", normalized))) {
                    continue;
                }
                if (Tokenizer.contains(tokens, keyword)) {
                    score += Tokenizer.weight(keyword);
                    hits.add(keyword);
                }
            }
            if (score > 0) {
                scores.put(definition, score);
                matched.put(definition, hits);
            }
        }

        if (scores.isEmpty()) {
            return ClassificationResult.irrelevant("no category keywords matched");
        }

        int best = scores.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (best < MIN_SCORE) {
            // Evidence exists but is too thin. Declining is the right answer —
            // an unclassified item is honest, a wrongly-classified one is not.
            // The engine is CONSERVATIVE BY DESIGN: accuracy improves by
            // enriching the vocabulary, never by lowering this threshold.
            return ClassificationResult.irrelevant(
                    "evidence below threshold (score " + best + " < " + MIN_SCORE + ")");
        }

        double cutoff = Math.max(MIN_SCORE, best * RELATIVE_FLOOR);
        List<CategoryDefinition> kept = scores.entrySet().stream()
                .filter(e -> e.getValue() >= cutoff)
                .sorted(Map.Entry.<CategoryDefinition, Integer>comparingByValue().reversed()
                        .thenComparing(e -> e.getKey().key()))
                .map(Map.Entry::getKey)
                .toList();

        List<String> labels = kept.stream().map(CategoryDefinition::label).toList();
        List<String> evidence = kept.stream().flatMap(d -> matched.get(d).stream()).distinct().toList();
        double confidence = Math.min(1.0, best / CONFIDENT_SCORE);

        String subcategory = resolveSubcategory(kept, tokens);

        return ClassificationResult.relevant(labels, subcategory, confidence,
                "matched: " + String.join(", ", evidence), evidence);
    }

    /**
     * Topic-level resolution, attempted only within categories the item already
     * belongs to. Returns null when no subcategory keywords are authored or none
     * match — the classifier declines rather than guessing.
     *
     * <p>That reticence is deliberate: a wrongly-categorized item is merely
     * mixed in among hundreds on a category page, while a wrongly-TOPICED item is
     * one of a handful on a specific topic page and looks obviously broken. As of
     * F2 no {@code subcategoryKeywords} are authored, so this returns null for
     * every source except resources (which carry an editorially-assigned
     * subcategory already and never reach here).
     */
    private String resolveSubcategory(List<CategoryDefinition> categories, List<String> tokens) {
        String bestTopic = null;
        int bestScore = 0;
        for (CategoryDefinition definition : categories) {
            for (String topic : definition.subcategories()) {
                int score = 0;
                for (String keyword : definition.subcategoryKeywordsFor(topic)) {
                    if (Tokenizer.contains(tokens, keyword)) {
                        score += Tokenizer.weight(keyword);
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestTopic = topic;
                }
            }
        }
        return bestScore >= MIN_SCORE ? bestTopic : null;
    }

}

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// CategoryClassifier answers "which canonical category does this belong to?" for
// any source, using two tiers of evidence in priority order. It is the piece
// that replaced RssFeedService.classifyLegislation() — which had the same job
// but only for RSS, with its own keyword tables and its own vocabulary.
// =============================================================================

// =============================================================================
// SECTION 1 — WHY matchCategories SURVIVED (the tier-1 argument)
// =============================================================================
// The brief for F2 said the legacy translation logic could be removed once the
// classifier existed. That was right about the QUERY layer and would have been
// wrong about the mapping table, so the two were separated:
//
//   REMOVED   CategoryService filtering resources by matchCategories at request
//             time. That was source-specific translation living in the wrong
//             layer, and it is gone.
//
//   KEPT      matchCategories itself, promoted to tier 1 of this classifier.
//
// The reasoning matters more than the outcome. matchCategories is a hand-curated
// mapping of a KNOWN upstream vocabulary (the DSCYF directory) with 100% coverage
// of all 229 resources. Keyword scoring is probabilistic. Replacing a correct
// deterministic mapping with a probabilistic one, in the name of "using the new
// engine everywhere", would trade a right answer for a likely one and call it
// progress.
//
// LESSON: "legacy" describes where code LIVES, not whether it is correct. The
// same table was legacy cruft in the query layer and exactly the right input to
// the classifier. Ask which layer a thing belongs in before asking whether to
// delete it.
//
// Tier 1 also SHORT-CIRCUITS: a resource whose source category maps exactly is
// never keyword-scored, even if its text screams another category. Locked in by
// shouldShortCircuitKeywordScoringWhenSourceCategoryMatches.
//
// One subtlety worth its own test: tier 1 returns EMPTY evidence, not the source
// category. Evidence feeds TagClassifier, and "Housing Assistance" /
// "Before/After School Care" are upstream category vocabulary — putting them in
// descriptive tags would push a category name into the one field that must never
// hold one, and pollute search with DSCYF's words for things. Provenance is
// already preserved on Resource.category. (This was a genuine bug caught during
// live verification: every one of the 229 resources had gained its raw source
// category as a search tag.)
//
// =============================================================================
// SECTION 2 — THE SCORING RULES, AND WHY EACH NUMBER
// =============================================================================
// MIN_SCORE = 2
//   One stray word is never enough. Set to 1, every bill mentioning "job" once
//   becomes Employment. The cost is real and visible: a bill titled "Relating to
//   the Court of Chancery" scores 1 on "court" and is declined. That is the
//   deliberate trade — the previous classifier had effectively MIN_SCORE = 1 on
//   substring matches, which is how a wetlands bill acquired five categories.
//
// PHRASE WEIGHT = token count (Tokenizer.weight)
//   "manufactured home" is worth 2, "home" is worth 1, because the odds of an
//   unrelated document containing a two-word phrase contiguously are far lower.
//   This lets one specific phrase outrank several vague single words.
//
// RELATIVE_FLOOR = 0.5
//   A category is kept only if it scores at least half the leader. This is what
//   suppresses the incidental-match tail: the wetlands failure mode was many
//   categories each picking up one loose hit beside a real one.
//
//   Chosen over a hard "max 3 categories" cap, which is the obvious alternative
//   and is worse in both directions — it truncates a genuinely four-category
//   item, and it still admits three bad matches for an item that deserves none.
//   A relative floor scales with the evidence instead of guessing a number.
//
//   Live result on 428 real bills: maximum 4 categories on any one item, and
//   that one ("Relating to the Office of Inspector General") plausibly does touch
//   Community Support, Employment, Health and Legal.
//
// CONFIDENT_SCORE = 6.0
//   Confidence saturates at roughly three solid hits. Used for the threshold and
//   the startup report, never persisted — see Classification_annotated.java.
//
// =============================================================================
// SECTION 3 — WHY SUBCATEGORY DECLINES RATHER THAN GUESSES
// =============================================================================
// resolveSubcategory() returns null unless subcategory keywords are authored AND
// score at least MIN_SCORE. As of F2 none are authored, so it always returns
// null for non-resources — resources carry an editorially-assigned subcategory
// from D0.3 and never reach tier 2 at all.
//
// This looks like a gap and is a decision. A wrongly-CATEGORIZED item is one of
// hundreds on a category page and reads as noise; a wrongly-TOPICED item is one
// of a handful on a specific topic page and reads as broken. Topic-level
// precision has to be higher than category-level precision, so topic-level
// evidence has to be better before it is trusted. Authoring keywords for all 58
// topics was deliberately deferred rather than rushed — the schema and the code
// path exist, so it is additive.
//
// =============================================================================
// SECTION 4 — THE DEDUPE THAT IS NOT OBVIOUS
// =============================================================================
// The `counted` LinkedHashSet in classifyByKeywords() guards against authored
// keywords that NORMALIZE to the same token. taxonomy.json lists both "utility"
// and "utilities"; Tokenizer.singularize() maps both to "utility". Without the
// set, a document containing the word once would score 2 instead of 1 — doubling
// the evidence purely because an author was thorough. Vocabulary redundancy
// should not inflate confidence.
//
// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - TaxonomyService supplies the vocabulary (categories, matchCategories,
//   keywords, subcategories). Note the package direction: shared.classification
//   depends on category.service. TaxonomyService is really shared vocabulary
//   infrastructure that happens to live in the category package for historical
//   reasons; moving it was churn F2 did not need, and is noted here rather than
//   hidden.
// - Tokenizer does all matching.
// - CivicContentClassifier is the only caller, and owns the policy about WHEN
//   this runs.
//
// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Word-boundary regex instead of tokenizing. Fixes the same substring bug, but
//   compiles a pattern per keyword per document and gives no phrase matching.
//   Tokenizing once and comparing tokens is cheaper and does more.
// - TF-IDF or embedding similarity. Genuinely better at recall, and rejected as
//   premature: the vocabulary is a few hundred hand-authored terms over a few
//   hundred short documents, and an explainable rule ("it matched these words")
//   is worth more here than a marginal accuracy gain nobody can audit. The
//   evidence list exists precisely so a human can see why something classified.
// - Keeping keywords in Java. Rejected — vocabulary is data, and tuning it
//   should not require a recompile. Putting them in taxonomy.json is what made
//   the mid-slice tuning pass a one-file edit.

// =============================================================================
// SLICE F2.1 UPDATE (Decision 034) — TIER 1 MOVED HOUSE
// =============================================================================
// The tier-1 table did not change. Where it LIVES did.
//
//   before   definition.matchCategories()      // from taxonomy.json
//   after    sourceMappingService.categoryKeyFor(sourceId, raw)
//                                              // from source-mappings.json
//
// Two moves, one year apart in reasoning, both about layering:
//   F2   took this translation out of the QUERY layer (CategoryService was
//        doing it per request).
//   F2.1 took it out of the DOMAIN model (taxonomy.json was carrying DSCYF's
//        vocabulary in the file that defines First Step's own).
//
// It is now where it always belonged: an ingestion-time source adapter inside
// the classification engine. See SourceMappingService_annotated.java Section 1.
//
// WHAT ALSO CHANGED, and why it is more than a rename:
//
//   A SOURCE ID IS NOW REQUIRED. classify() takes sourceId, because mappings are
//   keyed per provider. A raw category only translates for the source that
//   declared it — two directories can use the same word for different things.
//   The cost is that content must carry its provenance; the benefit is that
//   adopting a second provider cannot silently reinterpret the first's data.
//
//   A MAPPING TO AN UNKNOWN CATEGORY FALLS THROUGH rather than emitting it.
//   source-mappings.json is hand-edited and can drift from taxonomy.json; the
//   only options were to emit a category that does not exist, crash, or fall
//   through to keywords with a loud stderr line. The third keeps the app running
//   while making the data error visible — and validate_schema.py now fails hard
//   on the same condition, so CI catches it before a human does.
//
//   THE RESULT TYPE CARRIES RELEVANCE. Both tiers now return
//   ClassificationResult, whose `relevant` flag is the admission decision and
//   whose `reason` explains it ("source mapping (dscyf-directory): Housing
//   Assistance", "evidence below threshold (score 1 < 2)").
//
// TIER 1 STILL RETURNS EMPTY EVIDENCE — the F2 reasoning is unchanged and worth
// restating because it caused a real bug: evidence feeds TagClassifier, and a
// source category is a CATEGORY NAME in a provider's words. Letting it through
// put "Housing Assistance" into the descriptive tags of all 229 resources.
//
// THE INVARIANT THIS TIER UNDERWRITES: EditorialStabilityTest asserts that every
// resource is placed by source mapping and NOT by keyword inference. Removing a
// single mapping during verification did not make 37 resources disappear — it
// silently redistributed them into community-support, health and clothing, all
// plausible-looking. That is the failure mode a deterministic tier exists to
// prevent, and the reason it must never become "mostly deterministic".
