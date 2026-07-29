package org.firststep.backend.category.model;

import java.util.List;

/**
 * A display category and the source values that map onto it.
 *
 * <p>{@code matchCategories} matches a Resource's raw source {@code category};
 * {@code matchCategoryTags} matches a NewsItem's editorial {@code category_tags}.
 * The two tag fields on a NewsItem have deliberately separate jobs:
 * <b>category_tags is the editorial classification that drives navigation and
 * category association</b>, while {@code resource_tags} stays descriptive
 * metadata for search, filtering and AI retrieval — it is never consulted for
 * categorization (see references/decisions.md Decision 031).
 *
 * <p>Each entry holds its display label plus any aliases an upstream source
 * emits. RSS-classified legislation uses "Healthcare" where the taxonomy says
 * "Health", so health carries both.
 */
public record CategoryDefinition(
        String key,
        String label,
        String icon,
        List<String> matchCategories,
        List<String> matchCategoryTags,
        boolean includesFlyers
) {

    public static final List<CategoryDefinition> ALL = List.of(
            new CategoryDefinition(
                    "housing", "Housing", "🏠",
                    List.of("Housing Assistance", "Housing"),
                    List.of("Housing"),
                    false),
            new CategoryDefinition(
                    "food", "Food", "🍎",
                    List.of("Food Program"),
                    List.of("Food"),
                    false),
            new CategoryDefinition(
                    "clothing", "Clothing", "👕",
                    List.of("Clothing & Incidentals"),
                    List.of("Clothing"),
                    false),
            new CategoryDefinition(
                    "health", "Health", "🏥",
                    List.of("Healthcare/Medical", "Mental Health", "Substance Use"),
                    List.of("Health", "Healthcare"),
                    false),
            new CategoryDefinition(
                    "employment", "Employment", "💼",
                    List.of("Employment"),
                    List.of("Employment"),
                    false),
            new CategoryDefinition(
                    "utilities", "Utilities", "💡",
                    List.of(),
                    List.of("Utilities"),
                    false),
            new CategoryDefinition(
                    "legal", "Legal", "⚖️",
                    List.of("Advocacy"),
                    List.of("Legal"),
                    false),
            new CategoryDefinition(
                    "community-events", "Community Events", "🎉",
                    List.of("Recreational"),
                    List.of("Community Events"),
                    true),
            new CategoryDefinition(
                    "furniture-household", "Furniture & Household", "🛋️",
                    List.of("Furniture & Household Items"),
                    List.of("Furniture & Household"),
                    false),
            new CategoryDefinition(
                    "community-support", "Community Support", "🤝",
                    List.of("Resource Information", "Education/Training", "Parenting Education",
                            "Financial Support", "Support Group", "Early Childhood/Pre-K", "Volunteer",
                            "Mentor", "Life Skills", "Transportation", "Child Care",
                            "Before/After School Care", "Entertainment"),
                    List.of("Community Support"),
                    false)
    );
}
