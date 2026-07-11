package org.firststep.backend.category.model;

import java.util.List;

public record CategoryDefinition(
        String key,
        String label,
        String icon,
        List<String> matchCategories,
        List<String> matchNewsTags,
        boolean includesFlyers
) {

    public static final List<CategoryDefinition> ALL = List.of(
            new CategoryDefinition(
                    "housing", "Housing", "🏠",
                    List.of("Housing Assistance", "Housing"),
                    List.of("housing"),
                    false),
            new CategoryDefinition(
                    "food", "Food", "🍎",
                    List.of("Food Program"),
                    List.of("food"),
                    false),
            new CategoryDefinition(
                    "clothing", "Clothing", "👕",
                    List.of("Clothing & Incidentals"),
                    List.of(),
                    false),
            new CategoryDefinition(
                    "health", "Health", "🏥",
                    List.of("Healthcare/Medical", "Mental Health", "Substance Use"),
                    List.of("healthcare"),
                    false),
            new CategoryDefinition(
                    "employment", "Employment", "💼",
                    List.of("Employment"),
                    List.of("employment"),
                    false),
            new CategoryDefinition(
                    "utilities", "Utilities", "💡",
                    List.of(),
                    List.of("utilities"),
                    false),
            new CategoryDefinition(
                    "legal", "Legal", "⚖️",
                    List.of("Advocacy"),
                    List.of("legal"),
                    false),
            new CategoryDefinition(
                    "community-events", "Community Events", "🎉",
                    List.of("Recreational"),
                    List.of(),
                    true),
            new CategoryDefinition(
                    "furniture-household", "Furniture & Household", "🛋️",
                    List.of("Furniture & Household Items"),
                    List.of(),
                    false),
            new CategoryDefinition(
                    "community-support", "Community Support", "🤝",
                    List.of("Resource Information", "Education/Training", "Parenting Education",
                            "Financial Support", "Support Group", "Early Childhood/Pre-K", "Volunteer",
                            "Mentor", "Life Skills", "Transportation", "Child Care",
                            "Before/After School Care", "Entertainment"),
                    List.of(),
                    false)
    );
}
