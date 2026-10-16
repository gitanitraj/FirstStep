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
//   GET /api/category/{key}    -> CategoryPage            (the CATEGORY PAGE's
//                                 BFF: metadata + updates + topics + orgs, in
//                                 ONE response. F4 shipped it returning
//                                 navigation alone; F5a made it an aggregate.)
//
//   GET /api/category/{key}/{topic} -> TopicPage          (the TOPIC PAGE's BFF,
//                                 Slice F6 — the fourth and last level of the
//                                 hierarchy, where CivicContent is finally
//                                 listed)
//
// The three together are the navigation hierarchy in URL form: tiles -> a
// category -> a topic. All are wrapped in ApiResponse<T>, mirroring every other
// controller.
// =============================================================================

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
// F4 → F5a: WHEN ORCHESTRATION EARNS A SERVICE
// -----------------------------------------------------------------------------
// F4 deliberately had NO CategoryPageService, and said so at length: the BFF
// pattern (Decisions 019/020) gives a page one page-shaped endpoint so the client
// can be a thin display layer — it does NOT oblige every endpoint to have a
// service. NavigationService WAS the aggregator, so a second service whose only
// method forwarded a call would have been an abstraction over a single use, which
// "Simplicity First" rejects. The prediction recorded there:
//
//     "When the category page eventually needs more than navigation (featured
//      content, related organizations), the service appears then — at the point
//      it has something to compose."
//
// F5a IS that point. The page now composes three sources — navigation, a
// category-scoped updates feed, and category-scoped organizations — so
// CategoryPageService exists and this controller delegates to it. The rule did
// not change; the facts did. Keep the pair as the worked example: the same class
// was correctly refused and correctly accepted, on evidence rather than taste.
//
// The response type moved with it, for the same reason. F4 returned
// CategoryNavigation because that record was ALREADY the whole page and a
// one-field wrapper would have been ceremony. F5a's page has four branches
// (metadata, updates, browse, organizations), so CategoryPage is a real
// aggregate rather than a wrapper — and CategoryNavigation stayed untouched,
// because it belongs to the read model, not to a page.
//
// RESHAPING A SHIPPED ENDPOINT WAS SAFE HERE, and it is worth being explicit
// about why rather than treating it as luck: F4 shipped /api/category/{key} but
// /category/:key was still StubPage, so the endpoint had no consumer. Once F5b
// builds the page, a change of this size stops being free.
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
// CategoryPageService.getByKey() returns Optional.empty() for a key that is not
// in the taxonomy (it asks the navigation read model, which owns the taxonomy
// lookup). Translating that to NotFoundException matches
// ResourceController/FlyerController/ExpertAnswerController exactly, and
// GlobalExceptionHandler turns it into a 404 with the standard error envelope.
//
// The alternative — returning an empty CategoryPage with zero counts — would
// make "/category/hosuing" (typo) render a real-looking but permanently empty
// page. A category that EXISTS and is empty and a category that DOES NOT EXIST
// are different facts and must not produce the same response. Note this is the
// mirror image of TopicNavigation's rule that empty topics are returned rather
// than hidden: both come from the same principle — never let "nothing here" and
// "no such thing" look alike.
//
// F5a sharpened this, incidentally. Utilities used to BE the empty page — 0
// resources, 0 topics — and is now a live page with 22 signed bills reachable
// through its updates feed. The "exists but empty" case it once illustrated has
// largely stopped existing, which is the slice working as intended.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Depends on CategoryService (constructor-injected) for /api/categories.
// - Depends on CategoryPageService (constructor-injected) for
//   /api/category/{key}. That service composes three aggregators, each of which
//   reads ONLY editorial classification — which is what keeps this controller
//   pure routing: classification is an INGESTION concern, so by the time a
//   request arrives there is nothing left to decide, only to shape.
// - Relies on GlobalExceptionHandler (shared.web) for NotFoundException -> 404
//   and for any unexpected failure -> 500, both in the ApiResponse envelope.
//
// CALL PATH:
//   GET /api/category/housing
//     -> CategoryPageService.getByKey("housing", null)
//        -> NavigationService.getByKey        (exists? + metadata + groups/topics)
//        -> UpdatesService.getForCategory     (news + law + flyer + expert, cap 6)
//        -> OrganizationService.getForCategory(orgs ranked within the category)
//     -> ApiResponse.success(CategoryPage)
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Making communityId required, mirroring SearchController: rejected —
//   see WHY section; there's no analogous "meaningless without it" case
//   for browsing categories the way there is for a free-text search query.
//
// - A CategoryPageService between controller and read model: rejected in F4 as an
//   abstraction over a single use, INTRODUCED in F5a once there were three
//   sources to compose. See the F4 -> F5a section above.
//
// - Returning the category's RESOURCE ITEMS alongside its topics: still NOT done,
//   and F5a did not change this. The four-level nav hierarchy is Category ->
//   topic-group -> topic -> CivicContent (Decision 021), so the resource list
//   belongs to the TOPIC page (F6) and its own BFF. What F5a added is the
//   category's DATED content — news, laws, flyers, expert answers — which has no
//   topic to live under and would otherwise be unreachable. The two are different
//   halves of the page, not the same concession made twice.
//   A category page that shipped every resource item
//   would defeat the hierarchy it exists to present, and would make the payload
//   grow with the largest category (community-support, 61 items) for data the
//   page does not render.
//
// - Serving this from /api/navigation/{key}: rejected. The URL should name the
//   RESOURCE the client asked for (a category), not the internal service that
//   happens to assemble it. Clients should not have to know the read model's
//   name.
// =============================================================================
