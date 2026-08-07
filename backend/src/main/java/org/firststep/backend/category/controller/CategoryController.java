package org.firststep.backend.category.controller;

import java.util.List;

import org.firststep.backend.category.dto.CategoryPage;
import org.firststep.backend.category.dto.CategorySummary;
import org.firststep.backend.category.dto.TopicPage;
import org.firststep.backend.category.service.CategoryPageService;
import org.firststep.backend.category.service.CategoryService;
import org.firststep.backend.category.service.TopicPageService;
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
    private final CategoryPageService categoryPageService;
    private final TopicPageService topicPageService;

    public CategoryController(CategoryService service, CategoryPageService categoryPageService,
            TopicPageService topicPageService) {
        this.service = service;
        this.categoryPageService = categoryPageService;
        this.topicPageService = topicPageService;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategorySummary>>> getAll(
            @RequestParam(required = false) String communityId) {
        return ResponseEntity.ok(ApiResponse.success(service.getAll(communityId)));
    }

    /**
     * The category page's BFF. Returns the whole page in one request — header,
     * what has changed, what to browse, and who to contact — so the client
     * displays what it is given rather than fetching everything and filtering.
     *
     * <p>F4 returned navigation alone; F5a made the page an aggregate, and
     * {@link CategoryPageService} now owns the composition.
     */
    @GetMapping("/category/{key}")
    public ResponseEntity<ApiResponse<CategoryPage>> getByKey(
            @PathVariable String key,
            @RequestParam(required = false) String communityId) {
        CategoryPage page = categoryPageService.getByKey(key, communityId)
                .orElseThrow(() -> new NotFoundException("Category not found: " + key));
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * The topic page's BFF (Slice F6) — the fourth level of the hierarchy, where
     * CivicContent is finally listed.
     *
     * <p>A single 404 covers both an unknown category and a topic not declared
     * under it. That is deliberate: to a resident holding a bad URL the two are
     * the same event, and distinguishing them would leak which halves of the
     * taxonomy exist.
     */
    @GetMapping("/category/{key}/{topic}")
    public ResponseEntity<ApiResponse<TopicPage>> getTopic(
            @PathVariable String key,
            @PathVariable String topic,
            @RequestParam(required = false) String communityId) {
        TopicPage page = topicPageService.getByKey(key, topic, communityId)
                .orElseThrow(() -> new NotFoundException("Topic not found: " + key + "/" + topic));
        return ResponseEntity.ok(ApiResponse.success(page));
    }
}
