package org.firststep.backend.category.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// CategoryDefinition is the fixed, 10-entry taxonomy the homepage redesign's
// sidebar/category previews are built on: a stable key/label/icon plus the
// raw Resource.category strings and News resourceTags that belong to it.
// CategoryDefinition.ALL is the single source of truth CategoryService reads.
// =============================================================================

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
            new CategoryDefinition("housing", "Housing", "🏠",
                    List.of("Housing Assistance", "Housing"), List.of("housing"), false),
            new CategoryDefinition("food", "Food", "🍎",
                    List.of("Food Program"), List.of("food"), false),
            new CategoryDefinition("clothing", "Clothing", "👕",
                    List.of("Clothing & Incidentals"), List.of(), false),
            new CategoryDefinition("health", "Health", "🏥",
                    List.of("Healthcare/Medical", "Mental Health", "Substance Use"), List.of("healthcare"), false),
            new CategoryDefinition("employment", "Employment", "💼",
                    List.of("Employment"), List.of("employment"), false),
            new CategoryDefinition("utilities", "Utilities", "💡",
                    List.of(), List.of("utilities"), false),
            new CategoryDefinition("legal", "Legal", "⚖️",
                    List.of("Advocacy"), List.of("legal"), false),
            new CategoryDefinition("community-events", "Community Events", "🎉",
                    List.of("Recreational"), List.of(), true),
            new CategoryDefinition("furniture-household", "Furniture & Household", "🛋️",
                    List.of("Furniture & Household Items"), List.of(), false),
            new CategoryDefinition("community-support", "Community Support", "🤝",
                    List.of("Resource Information", "Education/Training", "Parenting Education",
                            "Financial Support", "Support Group", "Early Childhood/Pre-K", "Volunteer",
                            "Mentor", "Life Skills", "Transportation", "Child Care",
                            "Before/After School Care", "Entertainment"),
                    List.of(), false)
    );
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// A STATIC REGISTRY, NOT A DATABASE TABLE OR CONFIG FILE: 10 categories,
// rarely changes, and every consumer is server-side Java — same pattern
// RssFeedService already uses for its own fixed TAG_KEYWORDS constant map.
// No CRUD, no admin UI, no reason to externalize this into JSON/DB until a
// real need to edit categories without a deploy actually shows up.
//
// THE TAXONOMY HAD TO BE DISCOVERED, NOT ASSUMED: Resource.category is
// uncontrolled free text. The two live data files use two disjoint
// vocabularies (58 curated records: 3 category strings; 171
// directory-derived records: ~21 strings), and mapping everything onto the
// originally-requested 7 categories (Housing/Food/Clothing/Health/
// Employment/Utilities/Legal) left ~117 of 229 resources (about half)
// unmapped — "Recreational" alone is 53 records, the single largest
// category in the whole directory, second only to Housing Assistance.
// This is the SAME shape of lesson as the county-vs-community finding from
// the multi-tenancy pass: a field that looks like it should carry
// structure (category) turned out to need real investigation before any
// UI could be built on top of it.
//
// TWO CATEGORIES ADDED BEYOND THE ORIGINAL 7, BOTH BY DIRECT INSTRUCTION,
// TO REACH 100% COVERAGE:
// - "community-events" absorbs "Recreational" (the 53-record bucket) AND
//   is the first time Flyer content joins the category taxonomy at all —
//   Flyers were previously only reachable via /api/flyers or /api/search,
//   never through category browsing.
// - "furniture-household" gets its OWN category rather than being folded
//   into a generic catch-all, since it was already a clean, single,
//   real category string ("Furniture & Household Items", 6 records) in
//   the source data — no reason to blend it into a bucket when it didn't
//   need one.
// - "community-support" is the deliberate catch-all for everything left
//   over (Resource Information, Education/Training, Parenting Education,
//   Financial Support, Support Group, Early Childhood/Pre-K, Volunteer,
//   Mentor, Life Skills, Transportation, Child Care, Before/After School
//   Care, Entertainment — 13 leftover category strings, ~64 records) —
//   the frontend is expected to use each resource's own `category`/`tags`
//   for finer filtering within this bucket, per direct instruction.
//
// "utilities" HAS ZERO MATCHING RESOURCES TODAY (matchCategories is
// empty) — accepted deliberately, not a bug. It still has real News
// coverage via matchNewsTags, so CategoryService can still return a
// meaningful latestPolicyUpdate even with resourceCount: 0.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - category/service/CategoryService.getAll() iterates CategoryDefinition.ALL
//   and builds one CategorySummary per entry.
// - matchNewsTags values are lowercase to match NewsItem.resourceTags'
//   casing exactly, as produced by RssFeedService.classifyLegislation()
//   (NewsItem.tags is capitalized for display; resourceTags is the
//   lowercase machine-matching field — see CategoryService_annotated.java
//   for why resourceTags was chosen over tags).
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Normalizing/rewriting the source data's category strings instead of
//   building a mapping layer: rejected — resources.json/
//   resources.communities.json are snapshot data from real external
//   sources (FIRST directory, DSCYF); rewriting their contents to match a
//   UI taxonomy would be editing source-of-truth data to fit a display
//   concern, backwards from the mapping-layer approach here.
// - Leaving ~half the directory uncategorized/search-only (the first
//   proposal): rejected by direct instruction in favor of two additional
//   categories reaching 100% coverage.
// =============================================================================
