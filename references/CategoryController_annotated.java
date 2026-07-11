package org.firststep.backend.category.controller;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// CategoryController exposes GET /api/categories?communityId=..., wrapped
// in ApiResponse<T> — mirroring every other controller in the app exactly.
// =============================================================================

import java.util.List;

import org.firststep.backend.category.dto.CategorySummary;
import org.firststep.backend.category.service.CategoryService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategorySummary>>> getAll(
            @RequestParam(required = false) String communityId) {
        return ResponseEntity.ok(ApiResponse.success(service.getAll(communityId)));
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// SAME ApiResponse<T> WIRING AS EVERY OTHER CONTROLLER: no new response
// pattern introduced.
//
// `communityId` IS OPTIONAL (unlike SearchController's required `q`):
// there's no equivalent "meaningless without it" argument here — browsing
// categories with no community filter (all communities combined) is a
// perfectly sensible default response, unlike a blank search query. No
// GlobalExceptionHandler gap exists here the way SearchController's
// required `q` surfaced one.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Depends on CategoryService (constructor-injected).
// - Relies on GlobalExceptionHandler (shared.web) for any unexpected
//   failure -> 500 ApiResponse envelope.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Making communityId required, mirroring SearchController: rejected —
//   see WHY section; there's no analogous "meaningless without it" case
//   for browsing categories the way there is for a free-text search query.
// =============================================================================
