package org.firststep.backend.search.controller;

import java.util.List;

import org.firststep.backend.search.dto.PublicSearchResult;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.search.service.SearchService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchService service;
    private final FlyerService flyers;

    public SearchController(SearchService service, FlyerService flyers) {
        this.service = service;
        this.flyers = flyers;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PublicSearchResult>>> search(
            @RequestParam String q,
            @RequestParam(required = false) String communityId) {
        return ResponseEntity.ok(ApiResponse.success(service.search(q, communityId).stream().map(r -> PublicSearchResult.from(r, flyers)).toList()));
    }
}
