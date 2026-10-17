/*
 * WHAT: Complete IM-09 annotated mirror of backend/src/main/java/org/firststep/backend/news/controller/NewsController.java.
 * WHY: Preserve authored placement, canonical identity, truthful dates and private/public boundaries.
 * INTERACTION: Current inventory and Article approval remain independent of exact editorial
 * Expert/FAQ admissions; Category composition intersects Community, taxonomy and membership.
 * ALTERNATIVES: Source/session/verified inference and blanket Expert admission were rejected.
 * Tests cover approved admissions, denied sessions, preserved supporting views and safe links.
 */
package org.firststep.backend.news.controller;

import java.util.List;

import org.firststep.backend.news.dto.NewsSummary;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

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
    public ResponseEntity<ApiResponse<List<NewsSummary>>> getAll(@RequestParam(required = false) String communityId) {
        return ResponseEntity.ok(ApiResponse.success(service.getAll().stream().filter(item -> communityId == null || communityId.equals(item.communityId)).map(NewsSummary::from).toList()));
    }

    @GetMapping("/news/rss")
    public ResponseEntity<ApiResponse<List<NewsSummary>>> getRssNews(@RequestParam(required = false) String communityId) {
        return ResponseEntity.ok(ApiResponse.success(rssFeedService.getRssItems().stream().filter(item -> communityId == null || communityId.equals(item.communityId)).map(NewsSummary::from).toList()));
    }
}
