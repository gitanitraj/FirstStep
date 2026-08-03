package org.firststep.backend.category.controller;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// CategoryController owns the /api/category* URL family. It exposes two
// endpoints that look similar and serve two completely different pages:
//
//   GET /api/categories        -> List<CategorySummary>   (the HOMEPAGE's
//                                 Resource Discovery column: 10 tiles, each
//                                 with a count and a few latest items)
//
//   GET /api/category/{key}    -> CategoryNavigation      (the CATEGORY PAGE's
//                                 BFF, Slice F4: header + topic groups or flat
//                                 topics + counts, in ONE response)
//
// Both are wrapped in ApiResponse<T>, mirroring every other controller.
// =============================================================================

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

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// SAME ApiResponse<T> WIRING AS EVERY OTHER CONTROLLER: no new response
// pattern introduced.
//
// `communityId` IS OPTIONAL on both endpoints (unlike SearchController's
// required `q`): there's no equivalent "meaningless without it" argument
// here — browsing categories with no community filter (all communities
// combined) is a perfectly sensible default response, unlike a blank
// search query. No GlobalExceptionHandler gap exists here the way
// SearchController's required `q` surfaced one.
//
// -----------------------------------------------------------------------------
// SLICE F4: THE CATEGORY PAGE BFF IS A PASS-THROUGH, ON PURPOSE
// -----------------------------------------------------------------------------
// There is deliberately NO CategoryPageService between this controller and
// NavigationService. The BFF pattern (Decisions 019/020) says a page gets one
// page-shaped endpoint so the client can be a thin display layer — it does NOT
// say every endpoint needs its own service layer. NavigationService IS the
// aggregator; wrapping it in a second service whose only method forwards a call
// would be an abstraction over a single use, which the project's "Simplicity
// First" rule explicitly rejects.
//
// Compare HomeController -> HomeService: that service exists because it composes
// FIVE aggregators plus static AI config. Here there is exactly one source, so
// the composition step is empty. When the category page eventually needs more
// than navigation (featured content, related organizations), the service appears
// then — at the point it has something to compose.
//
// -----------------------------------------------------------------------------
// WHY THE RESPONSE IS CategoryNavigation RATHER THAN A NEW CategoryPagePayload
// -----------------------------------------------------------------------------
// HomePayload exists because the homepage needs six unrelated things stitched
// together. CategoryNavigation is ALREADY page-shaped — key, label, icon,
// totalCount, countsByType, groups, topics is exactly the category page. Adding
// a wrapper record with one field would be ceremony that makes the JSON deeper
// for no gain. If a second top-level field is ever needed, introducing the
// wrapper THEN is a small, honest change; introducing it now is speculation.
//
// -----------------------------------------------------------------------------
// WHY THIS LIVES IN CategoryController RATHER THAN A NEW NavigationController
// -----------------------------------------------------------------------------
// The alternative was navigation/controller/NavigationController. Rejected for
// two reasons:
//
//   1. URL-FAMILY COHESION. The codebase's existing convention is one controller
//      per URL family — ResourceController owns /resources, /health and
//      /seasonal-images. Someone looking for /api/category/{key} looks in
//      category/controller. A package named `navigation` serving a URL named
//      `category` makes the reader translate between two vocabularies.
//
//   2. THE PACKAGE NAME WOULD LIE. `navigation` is the READ MODEL's package. Its
//      job is aggregation, and F3 was careful to keep it free of delivery
//      concerns. Putting an HTTP controller in it would mix the two.
//
// The cost is that CategoryController now serves two pages (home tiles, category
// page). That is acceptable: controllers here are pure routing, and routing by
// URL is exactly what they are for.
//
// -----------------------------------------------------------------------------
// 404 RATHER THAN AN EMPTY PAYLOAD FOR AN UNKNOWN KEY
// -----------------------------------------------------------------------------
// NavigationService.getByKey() returns Optional.empty() for a key that is not in
// the taxonomy. Translating that to NotFoundException matches
// ResourceController/FlyerController/ExpertAnswerController exactly, and
// GlobalExceptionHandler turns it into a 404 with the standard error envelope.
//
// The alternative — returning an empty CategoryNavigation with zero counts —
// would make "/category/hosuing" (typo) render a real-looking but permanently
// empty page. A category that EXISTS and is empty (utilities, count 0) and a
// category that DOES NOT EXIST are different facts and must not produce the same
// response. Note this is the mirror image of TopicNavigation's rule that empty
// topics are returned rather than hidden: both come from the same principle —
// never let "nothing here" and "no such thing" look alike.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Depends on CategoryService (constructor-injected) for /api/categories.
// - Depends on NavigationService (constructor-injected) for /api/category/{key}.
//   That service reads ONLY editorial classification (categoryTags and
//   subcategory), which is what makes this endpoint safe to be so thin:
//   classification is an INGESTION concern, so by the time a request arrives
//   there is nothing left to decide, only to shape.
// - Relies on GlobalExceptionHandler (shared.web) for NotFoundException -> 404
//   and for any unexpected failure -> 500, both in the ApiResponse envelope.
//
// CALL PATH:
//   GET /api/category/housing
//     -> NavigationService.getByKey("housing", null)
//        -> TaxonomyService.findByKey  (does this category exist?)
//        -> ResourceService/NewsService/FlyerService/ExpertAnswerService/
//           FaqService/RssFeedSource  (all classified CivicContent)
//        -> filter by categoryTags, group by navigation.json, count by subcategory
//     -> ApiResponse.success(CategoryNavigation)
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Making communityId required, mirroring SearchController: rejected —
//   see WHY section; there's no analogous "meaningless without it" case
//   for browsing categories the way there is for a free-text search query.
//
// - A CategoryPageService between controller and read model: rejected as an
//   abstraction over a single use. See the F4 section above.
//
// - Returning the category's CONTENT ITEMS alongside its topics: deliberately
//   NOT in F4. The four-level nav hierarchy is Category -> topic-group -> topic
//   -> CivicContent (Decision 021), so the content list belongs to the TOPIC
//   page (F6) and its own BFF. A category page that also shipped every item
//   would defeat the hierarchy it exists to present, and would make the payload
//   grow with the largest category (community-support, 61 items) for data the
//   page does not render.
//
// - Serving this from /api/navigation/{key}: rejected. The URL should name the
//   RESOURCE the client asked for (a category), not the internal service that
//   happens to assemble it. Clients should not have to know the read model's
//   name.
// =============================================================================
