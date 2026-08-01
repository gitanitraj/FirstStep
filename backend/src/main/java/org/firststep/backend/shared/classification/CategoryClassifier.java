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
