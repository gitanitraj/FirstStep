package org.firststep.backend.category.dto;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// CategorySummary is the per-category response shape /api/categories
// returns: the category's identity (key/label/icon), a resource count, up
// to 3 "latest" items, and the most recent linked policy update.
// =============================================================================

import java.util.List;

import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.search.dto.SearchResult;

public record CategorySummary(
        String key,
        String label,
        String icon,
        int resourceCount,
        List<SearchResult> latestItems,
        NewsItem latestPolicyUpdate
) {
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// `latestItems: List<SearchResult>` REUSES search/dto/SearchResult RATHER
// THAN INVENTING A NEW WRAPPER: a category preview needs a polymorphic
// list mixing Resource and Flyer (specifically for "community-events",
// which includes both) with a type discriminator so the client knows how
// to render each item. SearchResult{type, score, content} already solves
// exactly this problem. `score` is unused here (always 0) — it's a
// vestigial field from SearchResult's original search-ranking purpose,
// harmless to carry along rather than forking a near-identical record
// type just to drop one field.
//
// `latestPolicyUpdate: NewsItem` IS SINGULAR, NOT A LIST: the redesign
// brief's category preview example shows one linked policy item per
// category section, not a list — matches that spec directly rather than
// over-building.
//
// NO PAGINATION FIELDS: matches the same "PageResponse<T> stays unwired"
// precedent used by every other endpoint in this codebase (Resource,
// News, Flyer, Search) — 8-10 categories, 3 items each, is not a
// pagination-scale response.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Built exclusively by category/service/CategoryService.
// - Returned wrapped in ApiResponse<List<CategorySummary>> by
//   category/controller/CategoryController — same envelope every other
//   endpoint in the app uses.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - A new CategoryItem{type, content} record without the unused `score`
//   field: rejected as needless duplication of SearchResult's shape for a
//   one-field difference that costs nothing to ignore.
// =============================================================================
