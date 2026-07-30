package org.firststep.backend.category.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// CategoryService implements /api/categories: for each canonical category
// LOADED FROM taxonomy.json, it counts matching Resource/Flyer records, picks
// the 3 most recently updated as "latest items," and finds the most recent News
// item whose EDITORIAL category_tags match the category.
//
// Slice F1 (Decision 032) changed where this class gets its vocabulary and how
// flyers reach a category. See the SLICE F1 UPDATE section below.
// =============================================================================

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.firststep.backend.category.dto.CategorySummary;
import org.firststep.backend.category.model.CategoryDefinition;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.search.dto.SearchResult;
import org.firststep.backend.shared.model.CivicContent;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private static final int MAX_LATEST_ITEMS = 3;

    private final TaxonomyService taxonomyService;
    private final ResourceService resourceService;
    private final NewsService newsService;
    private final FlyerService flyerService;

    public CategoryService(TaxonomyService taxonomyService, ResourceService resourceService,
                           NewsService newsService, FlyerService flyerService) {
        this.taxonomyService = taxonomyService;
        this.resourceService = resourceService;
        this.newsService = newsService;
        this.flyerService = flyerService;
    }

    public List<CategorySummary> getAll(String communityId) {
        List<Resource> resources = filterByCommunity(resourceService.getAll(), communityId);
        List<Flyer> flyers = filterByCommunity(flyerService.getAll(), communityId);
        List<NewsItem> news = filterByCommunity(newsService.getAll(), communityId);

        List<CategorySummary> summaries = new ArrayList<>();
        for (CategoryDefinition definition : taxonomyService.getCategories()) {
            summaries.add(summarize(definition, resources, flyers, news));
        }
        return summaries;
    }

    private CategorySummary summarize(CategoryDefinition definition, List<Resource> resources,
                                       List<Flyer> flyers, List<NewsItem> news) {
        // Resources still match on their RAW source category via matchCategories.
        // Normalizing that into canonical categoryTags is the classifier's job
        // (Slice F2); doing it here would put a second classifier in this service.
        List<Resource> matchedResources = resources.stream()
                .filter(r -> definition.matchCategories().contains(r.category))
                .toList();

        // Flyers are classified editorially like every other content type. This
        // replaces the old includesFlyers boolean, under which Community Events
        // swept in all seven flyers regardless of subject while a furniture
        // giveaway or an eviction-rights session reached no relevant category.
        List<Flyer> matchedFlyers = flyers.stream()
                .filter(f -> taxonomyService.matchesCategoryTags(definition, f.categoryTags))
                .toList();

        int resourceCount = matchedResources.size() + matchedFlyers.size();

        List<SearchResult> combined = new ArrayList<>();
        matchedResources.forEach(r -> combined.add(new SearchResult("resource", 0, r)));
        matchedFlyers.forEach(f -> combined.add(new SearchResult("flyer", 0, f)));
        combined.sort(Comparator.comparing(
                (SearchResult sr) -> sr.content().updatedDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        List<SearchResult> latestItems = combined.stream().limit(MAX_LATEST_ITEMS).toList();

        // Categorization reads a news item's EDITORIAL classification
        // (category_tags), never its descriptive tags — see the CivicContent
        // contract and decisions.md Decisions 031/032.
        NewsItem latestPolicyUpdate = news.stream()
                .filter(n -> taxonomyService.matchesCategoryTags(definition, n.categoryTags))
                .max(Comparator.comparing(n -> n.publishDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);

        return new CategorySummary(definition.key(), definition.label(), definition.icon(),
                resourceCount, latestItems, latestPolicyUpdate);
    }

    private <T extends CivicContent> List<T> filterByCommunity(List<T> items, String communityId) {
        if (communityId == null || communityId.isBlank()) return items;
        return items.stream().filter(i -> communityId.equals(i.communityId)).toList();
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// DEPENDS ON ResourceService/NewsService/FlyerService, NOT REPOSITORIES:
// same composition discipline as SearchService — treats each slice's
// Service as its public API surface.
//
// MATCHING AGAINST NewsItem.tags (category_tags), NOT .resourceTags —
// REVERSED IN DECISION 031, and the reversal is the important lesson here.
// The original implementation matched resourceTags because RssFeedService's
// classifyLegislation() emits both fields from the same bucket list and the
// lowercase one lined up with a lowercase mapping table. That was a
// convenience of the prototype's plumbing, not a model decision, and it cost
// real correctness: curated news carries HAND-WRITTEN resourceTags that are
// fine-grained descriptors ("rental-assistance", "SRAP", "WHA"), so 4 of 8
// curated items matched no category at all and silently never appeared —
// Health showed no policy update despite having a Medicaid dental item.
//
// The V2 CivicContent model separates three concerns, and this class must
// only ever read the first:
//   category_tags   editorial classification -> navigation and categorization
//   resource_tags   descriptive metadata     -> search, filtering, AI retrieval
//   status/expires  content lifecycle
// Overloading resource_tags with category meaning would collapse two of those
// into one field. So matchCategoryTags now carries display-cased editorial
// values ("Housing"), plus any alias an upstream source emits — RSS says
// "Healthcare" where the taxonomy says "Health", so health holds both.
// equalsIgnoreCase remains as a casing safety net.
//
// SORTING BY updatedDate FOR "LATEST ITEMS" — HONEST LIMITATION, NOTED
// FOR THE FRONTEND: Resource.updatedDate is set from the JSON load/
// retrieved date (see JsonResourceRepository), not real edit-history
// tracking. It's a reasonable best-effort recency proxy for internal
// sorting, but per direct instruction, THE FRONTEND MUST NOT DISPLAY IT
// TO USERS AS "last updated" — that would imply a freshness guarantee
// the data doesn't actually have. This is a UI guideline for the later
// frontend steps (roadmap step 6/8), not a backend behavior change.
//
// THE isEmpty() SHORT-CIRCUIT IS GONE (Decision 031). It guarded the four
// categories that had no news linkage at all. Worth correcting the original
// note here: it claimed an empty match list would "vacuously match every
// News item," which is backwards — matchesAnyTag's inner loop never runs
// over an empty list, so it returns false and nothing matches. The guard was
// only ever an efficiency nicety. Now every category owns at least its own
// label as an editorial tag, so no list is ever empty and the branch is dead
// code — removed rather than left as decoration.
//
// COMMUNITY FILTERING MIRRORS SearchService EXACTLY: same
// filterByCommunity generic helper shape, same "empty/null communityId
// means no filter" semantics — kept consistent across the two aggregation
// endpoints rather than inventing a second convention.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - category/controller/CategoryController is the only caller of getAll().
// - Depends on category/service/TaxonomyService, resource/service/ResourceService,
//   news/service/NewsService, flyer/service/FlyerService (constructor injection).
// - Gets its vocabulary from TaxonomyService.getCategories(), which loads
//   app/data/taxonomy.json. (Before Slice F1 this read a hardcoded
//   CategoryDefinition.ALL constant — see the SLICE F1 UPDATE section.)
// - Produces category/dto/CategorySummary instances, reusing
//   search/dto/SearchResult for latestItems.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - A single combined pass building all 10 summaries in one stream
//   traversal instead of one summarize() call per definition: rejected —
//   with only 10 categories and ~230 resources, the O(categories ×
//   resources) approach here is trivially fast, and the per-definition
//   method is far easier to read/test than a fused multi-accumulator loop.
// - Unifying with SearchService (making category browsing a special case
//   of search): rejected — the two have different shapes (search: one
//   flat ranked list from a free-text query; categories: N fixed,
//   pre-defined groups with counts) and forcing them into one abstraction
//   would be premature generalization for a two-call-site "similarity."
// =============================================================================

// =============================================================================
// SLICE F1 UPDATE (Decision 032) — TWO CHANGES
// =============================================================================
// SECTION A — THE VOCABULARY IS INJECTED, NOT COMPILED IN
// -----------------------------------------------------------------------------
// Before:  for (CategoryDefinition d : CategoryDefinition.ALL)   // static constant
// After:   for (CategoryDefinition d : taxonomyService.getCategories())
//
// CategoryDefinition.ALL was a hardcoded ten-entry list in Java, hand-mirrored
// against app/data/taxonomy.json. It is gone; TaxonomyService loads the file and
// this service asks for it. The constructor gained a TaxonomyService parameter,
// which is the only signature change.
//
// Two knock-on effects worth noting:
//   - Category ORDER is now the file's authored order rather than the Java
//     constant's. They matched, so nothing moved — but the file is now the
//     thing to edit to reorder the homepage's category column.
//   - The tag-matching loop moved OUT of this class into
//     TaxonomyService.matchesCategoryTags(). It is vocabulary behavior, not
//     category-summarizing behavior, and NavigationService (Slice F3) needs the
//     same rule. Leaving a private copy here would have guaranteed two
//     implementations of "does this item belong to this category".
//
// SECTION B — FLYERS CLASSIFY LIKE EVERYTHING ELSE
// -----------------------------------------------------------------------------
// Before:  List<Flyer> matchedFlyers = definition.includesFlyers() ? flyers : List.of();
// After:   List<Flyer> matchedFlyers = flyers.stream()
//              .filter(f -> taxonomyService.matchesCategoryTags(definition, f.categoryTags))
//              .toList();
//
// The old line is worth staring at: a category declared, via a boolean, that it
// would accept ALL flyers. Community Events had includesFlyers = true, so all
// seven flyers counted toward it and toward nothing else — the health fair, the
// furniture giveaway and the eviction-rights session included.
//
// Now each flyer says what it is about (flyers.json gained category_tags), and
// the SAME predicate that places a news item places a flyer. Note the symmetry
// in summarize(): news and flyers now go through one identical call. That is
// what "every CivicContent source classifies the same way" buys — the method
// gets shorter as the model gets more uniform.
//
// VERIFIED LIVE (Docker, /api/home) — the counts moved exactly as predicted:
//   community-events    60 -> 54   (53 resources + FL-001 only)
//   housing             44 -> 45   (+ FL-002)
//   legal                3 ->  5   (+ FL-002, FL-005 — both dual-classified)
//   health              32 -> 33   (+ FL-006)
//   furniture-household  6 ->  7   (+ FL-007)
//   community-support   58 -> 61   (+ FL-003, FL-004, FL-005)
//   TOTAL              229 resources + 9 flyer placements = 238
// latestPolicyUpdate was unchanged for every category (housing NP-006, food
// NP-003, health NP-008, utilities NP-004), confirming the news path did not
// regress while the flyer path changed.
//
// =============================================================================
// WHAT DID NOT CHANGE, AND WHY
// =============================================================================
// Resources still match on their RAW source category:
//
//     .filter(r -> definition.matchCategories().contains(r.category))
//
// So resources are the one content type NOT yet using canonical categoryTags.
// This is a deliberate seam. Translating a raw directory string into a canonical
// category is CLASSIFICATION, and Slice F2 builds shared/classification/ to do
// that for every source at once. Doing it inline here would mean writing a
// second classifier that F2 deletes.
//
// Also unchanged: `resourceCount` still counts resources + flyers only, not news.
// The user's direction is that TOPIC pages count all classified CivicContent;
// this DTO field is the homepage's category tile count and is renamed/reshaped
// in Slice F4 when the category BFF endpoint lands. Changing its meaning here
// would alter the homepage without any page needing it yet.
//
// =============================================================================
// ALTERNATIVES CONSIDERED (Slice F1)
// =============================================================================
// - Keep matchesAnyTag() private here and have NavigationService duplicate it.
//   Rejected: two implementations of the central classification predicate is
//   exactly the drift this slice exists to remove.
// - Have CategoryService hold the loaded taxonomy itself (load in its own
//   constructor) rather than injecting a TaxonomyService. Rejected: the
//   vocabulary is needed by NavigationService, the future classifier and the
//   validators' Java counterparts. Ownership belongs to a service whose only
//   job is the vocabulary.
// - Filter flyers by subcategory instead of categoryTags, on the theory that a
//   flyer's topic is more specific. Rejected: category membership is a
//   category-level question, and FL-005 is classified under two categories with
//   a single subcategory — matching on subcategory would drop it from one of
//   them.
// =============================================================================
