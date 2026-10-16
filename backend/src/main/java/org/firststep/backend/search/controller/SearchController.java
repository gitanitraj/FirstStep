package org.firststep.backend.search.controller;

import java.util.List;

import org.firststep.backend.search.dto.SearchResult;
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

    public SearchController(SearchService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SearchResult>>> search(
            @RequestParam String q,
            @RequestParam(required = false) String communityId) {
        return ResponseEntity.ok(ApiResponse.success(service.search(q, communityId)));
    }
}
