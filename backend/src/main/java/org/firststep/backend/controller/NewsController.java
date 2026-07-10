package org.firststep.backend.controller;

import java.util.List;

import org.firststep.backend.model.NewsItem;
import org.firststep.backend.service.NewsService;
import org.firststep.backend.service.RssFeedSource;
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
