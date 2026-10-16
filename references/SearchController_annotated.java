package org.firststep.backend.search.controller;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// SearchController exposes GET /api/search?q=...&communityId=..., wrapped
// in ApiResponse<T> — mirroring every other controller in the app exactly.
// =============================================================================

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

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// SAME ApiResponse<T> WIRING AS EVERY OTHER CONTROLLER: no new response
// pattern introduced.
//
// `q` IS A REQUIRED @RequestParam (no default, no `required = false`): a
// search request without a query string isn't a meaningful search request.
// Spring's default behavior for a missing required @RequestParam is a 400
// (MissingServletRequestParameterException) — this surfaced a real,
// pre-existing gap in GlobalExceptionHandler (see that class's annotated
// reference): no other endpoint in the app had a required @RequestParam
// before this one, so a missing-param request had never been exercised,
// and the catch-all Exception -> 500 handler was silently swallowing what
// should have been a 400. Fixed by adding a specific
// MissingServletRequestParameterException -> 400 handler — found by a
// failing SearchControllerTest case, not anticipated in advance.
//
// A BLANK `q` (e.g. `?q=`) IS ALLOWED, NOT REJECTED: no special-case
// validation needed — TextScore.match() already returns 0 for a blank
// query against any field, so a blank q naturally produces an empty result
// list through the normal scoring path.
//
// `communityId` IS OPTIONAL: absence falls back to
// app.default-community-id inside SearchService, matching every other
// slice's default-stamping convention (see SearchService_annotated.java).
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Depends on SearchService (constructor-injected).
// - Relies on GlobalExceptionHandler (shared.web) to turn a missing `q`
//   into a 400 ApiResponse envelope, and any unexpected failure into a 500.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Making `q` optional (defaulting to empty string, returning all
//   content when absent): rejected — conflates "search for nothing" with
//   "browse everything," which is a different feature (already covered by
//   the existing /api/resources, /api/news, /api/flyers endpoints).
// - Wiring a search box into the current app.js demo in this same pass:
//   explicitly deferred — matches how the Flyer slice was done (backend
//   first), and a real search UI is better built once in the upcoming
//   React frontend than twice.
// =============================================================================
