package org.firststep.backend.category.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One canonical category, deserialized from {@code app/data/taxonomy.json}.
 *
 * <p><b>This used to be the vocabulary itself</b> — a hardcoded {@code ALL}
 * constant listing all ten categories, hand-mirrored against taxonomy.json and
 * free to drift from it. Slice F1 deleted that constant and made the file the
 * single source of truth; this record is now purely the shape Jackson binds it
 * to, and {@link org.firststep.backend.category.service.TaxonomyService} owns
 * loading it. Any service that classifies or validates CivicContent asks the
 * TaxonomyService rather than carrying its own copy.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code matchCategoryTags} — canonical editorial {@code category_tags}
 *       that associate a CivicContent item with this category. These are the
 *       display labels only. The old "Healthcare" alias is GONE: RSS now emits
 *       canonical values at the source, so downstream lists no longer widen to
 *       absorb upstream drift.</li>
 *   <li>{@code subcategories} — the canonical topics beneath this category.</li>
 * </ul>
 *
 * <p><b>{@code matchCategories} is gone (Slice F2.1).</b> Upstream provider
 * vocabulary — "Housing Assistance", "Before/After School Care" — was DSCYF's
 * words living in First Step's editorial domain model. It moved to
 * {@code app/data/source-mappings.json}, loaded by
 * {@code shared.classification.SourceMappingService}, because translating a
 * source vocabulary is a deterministic <b>source adapter</b> and therefore an
 * ingestion concern. This record now carries only First Step's own vocabulary.
 *
 * <p>{@code includesFlyers} is also gone. It was a hardcoded boolean that let
 * Community Events sweep in every flyer regardless of what the flyer was about —
 * the last place where content reached a category by special case rather than by
 * editorial classification. Flyers now carry their own {@code category_tags}.
 *
 * <p>Slice F2 added the classification vocabulary: {@code keywords} (terms and
 * phrases that are evidence FOR this category in free text) and the optional
 * {@code subcategoryKeywords} map. Both are read by
 * {@code shared.classification.CategoryClassifier}. The two vocabularies left
 * here are distinct and neither substitutes for the other —
 * {@code matchCategoryTags} is canonical editorial classification (authoritative),
 * {@code keywords} is probabilistic evidence used only when nothing has
 * classified the item already.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoryDefinition(
        String key,
        String label,
        String icon,
        List<String> matchCategoryTags,
        List<String> keywords,
        Map<String, List<String>> subcategoryKeywords,
        List<String> subcategories
) {

    /**
     * Null-safe accessors — taxonomy.json is hand-authored and both keyword
     * fields are optional, so a category may legitimately omit them. Callers
     * should never have to null-check a vocabulary.
     */
    public List<String> keywordsOrEmpty() {
        return keywords == null ? List.of() : keywords;
    }

    public List<String> subcategoryKeywordsFor(String subcategory) {
        if (subcategoryKeywords == null) {
            return List.of();
        }
        return subcategoryKeywords.getOrDefault(subcategory, List.of());
    }
}
