/*
 * Annotated reference: backend/src/main/java/org/firststep/backend/search/controller/SearchController.java
 * WHAT: Preserves the complete source below; see the explicit allowlists and optional exact-owner filters in the source.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Controllers adapt internal models only at the resident boundary; tests and evidence exercise real repositories plus labeled synthetic counterexamples.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
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
