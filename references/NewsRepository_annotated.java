package org.firststep.backend.news.repository;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// NewsRepository is the news slice's persistence seam — one method
// (findAll) that NewsService depends on instead of knowing how/where static
// news data is stored. (RSS-derived news has its own separate path — see
// RssFeedSource/RssFeedService — since it's a live feed, not stored data.)
// =============================================================================

import java.util.List;

import org.firststep.backend.news.model.NewsItem;

public interface NewsRepository {
    List<NewsItem> findAll();
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Mirrors ResourceRepository's shape (see ResourceRepository_annotated.java)
// — a small, slice-owned interface with only the method NewsService actually
// calls, per the same "per-slice repositories, not one generic Repository<T>"
// decision. No findById here since NewsController/NewsService never look up
// a single news item by id today (unlike Resource, which has GET
// /api/resources/{id}).
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - JsonNewsRepository is the only implementation, backed by
//   app/data/news.json.
// - NewsService depends on this interface, not JsonNewsRepository directly.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Adding a findById method to match ResourceRepository's shape exactly:
//   rejected — no current caller needs it; adding an unused method would be
//   speculative.
// =============================================================================
