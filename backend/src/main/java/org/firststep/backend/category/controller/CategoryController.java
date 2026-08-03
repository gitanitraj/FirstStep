package org.firststep.backend.category.controller;

import java.util.List;

import org.firststep.backend.category.dto.CategorySummary;
import org.firststep.backend.category.service.CategoryService;
import org.firststep.backend.navigation.dto.CategoryNavigation;
import org.firststep.backend.navigation.service.NavigationService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.firststep.backend.shared.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService service;
    private final NavigationService navigationService;

    public CategoryController(CategoryService service, NavigationService navigationService) {
        this.service = service;
        this.navigationService = navigationService;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategorySummary>>> getAll(
            @RequestParam(required = false) String communityId) {
        return ResponseEntity.ok(ApiResponse.success(service.getAll(communityId)));
    }

    /**
     * The category page's BFF (Slice F4). Returns the whole page shape in one
     * request — header, topic groups or flat topics, and counts — so the client
     * displays what it is given rather than fetching everything and filtering.
     *
     * <p>Thin by design: {@link NavigationService} is already the aggregator, so
     * an intervening service would only forward a call.
     */
    @GetMapping("/category/{key}")
    public ResponseEntity<ApiResponse<CategoryNavigation>> getByKey(
            @PathVariable String key,
            @RequestParam(required = false) String communityId) {
        CategoryNavigation category = navigationService.getByKey(key, communityId)
                .orElseThrow(() -> new NotFoundException("Category not found: " + key));
        return ResponseEntity.ok(ApiResponse.success(category));
    }
}
