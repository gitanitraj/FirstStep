package org.firststep.backend.category.model;

import java.util.List;

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
 *   <li>{@code matchCategories} — raw source-data category strings (DSCYF
 *       directory vocabulary, e.g. "Housing Assistance") that map onto this
 *       display category.</li>
 *   <li>{@code matchCategoryTags} — canonical editorial {@code category_tags}
 *       that associate a CivicContent item with this category. These are the
 *       display labels only. The old "Healthcare" alias is GONE: RSS now emits
 *       canonical values at the source, so downstream lists no longer widen to
 *       absorb upstream drift.</li>
 *   <li>{@code subcategories} — the canonical topics beneath this category.</li>
 * </ul>
 *
 * <p>{@code includesFlyers} is also gone. It was a hardcoded boolean that let
 * Community Events sweep in every flyer regardless of what the flyer was about —
 * the last place where content reached a category by special case rather than by
 * editorial classification. Flyers now carry their own {@code category_tags}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoryDefinition(
        String key,
        String label,
        String icon,
        List<String> matchCategories,
        List<String> matchCategoryTags,
        List<String> subcategories
) {
}
