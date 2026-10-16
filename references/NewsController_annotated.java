package org.firststep.backend.news.controller;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// NewsController exposes the news slice's REST endpoints: GET /api/news
// (static, JSON-file-backed) and GET /api/news/rss (live, RSS-feed-backed).
// Moved here unchanged in behavior from the flat controller/ package, aside
// from the ApiResponse<T> wiring already introduced in Step 2.
// =============================================================================

import java.util.List;

import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class NewsController {

    private final NewsService service;
    private final RssFeedSource rssFeedService;

    public NewsController(NewsService service, RssFeedSource rssFeedService) {
        this.service = service;
        this.rssFeedService = rssFeedService;
    }

    @GetMapping("/news")
    public ResponseEntity<ApiResponse<List<NewsItem>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/news/rss")
    public ResponseEntity<ApiResponse<List<NewsItem>>> getRssNews() {
        return ResponseEntity.ok(ApiResponse.success(rssFeedService.getRssItems()));
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Purely a package move plus updated imports for NewsItem/NewsService/
// RssFeedSource's new locations. No endpoint, method signature, or
// controller-level behavior changed here — the wire shape of each NewsItem
// changed (as a result of the model migration), but that's documented in
// NewsItem_annotated.java, not a controller-level decision.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Depends on both NewsService (static news) and RssFeedSource (live RSS
//   news) — two independent data paths behind two endpoints.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - None specific to this step — see ResourceController_annotated.java for
//   the equivalent reasoning applied to the sibling resource slice.
// =============================================================================
