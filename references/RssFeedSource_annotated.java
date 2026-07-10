package org.firststep.backend.news.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// RssFeedSource is a one-method interface (getRssItems) that decouples
// NewsController from the concrete RssFeedService, so a test can swap in a
// fake feed source without needing Mockito on a concrete class holding a
// @Scheduled method and live network calls.
// =============================================================================

import org.firststep.backend.news.model.NewsItem;

import java.util.List;

public interface RssFeedSource {
    List<NewsItem> getRssItems();
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Moved unchanged from org.firststep.backend.service to
// org.firststep.backend.news.service — package move only, no behavior or
// shape change. Single-method interface, so tests can implement it with a
// lambda (see NewsControllerTest: `() -> List.of(testItem())`).
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - RssFeedService implements this.
// - NewsController depends on this interface (constructor-injected), not on
//   RssFeedService directly.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - None — this is a straightforward package move of an already-minimal
//   interface.
// =============================================================================
