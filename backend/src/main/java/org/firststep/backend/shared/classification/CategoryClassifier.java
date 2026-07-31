package org.firststep.backend.shared.classification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.firststep.backend.category.model.CategoryDefinition;
import org.firststep.backend.category.service.TaxonomyService;
import org.springframework.stereotype.Component;

/**
 * Determines the canonical category and subcategory for a piece of content.
 *
 * <p>Two tiers, tried in order.
 *
 * <p><b>Tier 1 — source-vocabulary mapping (deterministic, confidence 1.0).</b>
 * When an item arrives carrying a raw source category, an exact match against a
 * category's {@code matchCategories} settles it and keyword scoring never runs.
 * This is what keeps all 229 resources classified exactly as they are today.
 * The instinct to delete {@code matchCategories} as "legacy translation" was
 * resisted deliberately: it is a hand-curated mapping of a known upstream
 * vocabulary with 100% coverage, and replacing it with keyword guessing would
 * trade a correct answer for a probabilistic one. What F2 removes is that
 * translation happening in the QUERY layer (CategoryService); as classifier
 * input it is exactly the right kind of evidence.
 *
 * <p><b>Tier 2 — keyword evidence (scored).</b> For free text — legislation,
 * flyers, expert answers — each category is scored by the distinct
 * keywords/phrases from taxonomy.json that appear in it.
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

    public CategoryClassifier(TaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }

    /**
     * @param rawSourceCategory the item's upstream category string, or null when
     *                          the source has no such concept (news, flyers, RSS)
     * @param text              the item's classifiable prose
     */
    public Classification classify(String rawSourceCategory, String text) {
        Classification bySource = classifyBySourceVocabulary(rawSourceCategory);
        if (bySource != null) {
            return bySource;
        }
        return classifyByKeywords(text);
    }

    /** Tier 1. Returns null (not Classification.none()) to mean "no opinion, try tier 2". */
    private Classification classifyBySourceVocabulary(String rawSourceCategory) {
        if (rawSourceCategory == null || rawSourceCategory.isBlank()) {
            return null;
        }
        for (CategoryDefinition definition : taxonomyService.getCategories()) {
            for (String source : definition.matchCategories()) {
                if (source.equalsIgnoreCase(rawSourceCategory.trim())) {
                    // Evidence is deliberately EMPTY, not the source category.
                    // Evidence feeds TagClassifier, and a source category is
                    // upstream vocabulary ("Housing Assistance", "Before/After
                    // School Care") — putting it in descriptive tags would push a
                    // category name into the field that must never hold one, and
                    // pollute search with DSCYF's words. Provenance is already
                    // preserved on Resource.category.
                    return new Classification(List.of(definition.label()), null, 1.0, List.of());
                }
            }
        }
        return null;
    }

    /** Tier 2. */
    private Classification classifyByKeywords(String text) {
        List<String> tokens = Tokenizer.tokenize(text);
        if (tokens.isEmpty()) {
            return Classification.none();
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
            return Classification.none();
        }

        int best = scores.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (best < MIN_SCORE) {
            // Evidence exists but is too thin. Declining is the right answer —
            // an unclassified item is honest, a wrongly-classified one is not.
            return Classification.none();
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

        return new Classification(labels, subcategory, confidence, evidence);
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
