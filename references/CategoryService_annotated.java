package org.firststep.backend.category.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// CategoryService implements /api/categories: for each of the 10 fixed
// CategoryDefinitions, it counts matching Resource/Flyer records, picks the
// 3 most recently updated as "latest items," and finds the most recent
// News item whose EDITORIAL category_tags overlap the category's
// matchCategoryTags (Decision 031 — resource_tags are descriptive metadata
// and are never consulted for categorization).
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

    private final ResourceService resourceService;
    private final NewsService newsService;
    private final FlyerService flyerService;

    public CategoryService(ResourceService resourceService, NewsService newsService, FlyerService flyerService) {
        this.resourceService = resourceService;
        this.newsService = newsService;
        this.flyerService = flyerService;
    }

    public List<CategorySummary> getAll(String communityId) {
        List<Resource> resources = filterByCommunity(resourceService.getAll(), communityId);
        List<Flyer> flyers = filterByCommunity(flyerService.getAll(), communityId);
        List<NewsItem> news = filterByCommunity(newsService.getAll(), communityId);

        List<CategorySummary> summaries = new ArrayList<>();
        for (CategoryDefinition definition : CategoryDefinition.ALL) {
            summaries.add(summarize(definition, resources, flyers, news));
        }
        return summaries;
    }

    private CategorySummary summarize(CategoryDefinition definition, List<Resource> resources,
                                       List<Flyer> flyers, List<NewsItem> news) {
        List<Resource> matchedResources = resources.stream()
                .filter(r -> definition.matchCategories().contains(r.category))
                .toList();
        List<Flyer> matchedFlyers = definition.includesFlyers() ? flyers : List.of();

        int resourceCount = matchedResources.size() + matchedFlyers.size();

        List<SearchResult> combined = new ArrayList<>();
        matchedResources.forEach(r -> combined.add(new SearchResult("resource", 0, r)));
        matchedFlyers.forEach(f -> combined.add(new SearchResult("flyer", 0, f)));
        combined.sort(Comparator.comparing(
                (SearchResult sr) -> sr.content().updatedDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        List<SearchResult> latestItems = combined.stream().limit(MAX_LATEST_ITEMS).toList();

        NewsItem latestPolicyUpdate = news.stream()
                .filter(n -> matchesAnyTag(n.tags, definition.matchCategoryTags()))
                .max(Comparator.comparing(n -> n.published, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);

        return new CategorySummary(definition.key(), definition.label(), definition.icon(),
                resourceCount, latestItems, latestPolicyUpdate);
    }

    private boolean matchesAnyTag(List<String> categoryTags, List<String> matchCategoryTags) {
        if (categoryTags == null) return false;
        for (String tag : categoryTags) {
            for (String match : matchCategoryTags) {
                if (match.equalsIgnoreCase(tag)) return true;
            }
        }
        return false;
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
// - Depends on resource/service/ResourceService, news/service/NewsService,
//   flyer/service/FlyerService (constructor injection).
// - Reads category/model/CategoryDefinition.ALL as its fixed taxonomy.
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
