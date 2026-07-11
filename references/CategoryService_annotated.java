package org.firststep.backend.category.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// CategoryService implements /api/categories: for each of the 10 fixed
// CategoryDefinitions, it counts matching Resource/Flyer records, picks the
// 3 most recently updated as "latest items," and finds the most recent
// News item whose resourceTags overlap the category's matchNewsTags.
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

        NewsItem latestPolicyUpdate = definition.matchNewsTags().isEmpty()
                ? null
                : news.stream()
                        .filter(n -> matchesAnyTag(n.resourceTags, definition.matchNewsTags()))
                        .max(Comparator.comparing(n -> n.published, Comparator.nullsFirst(Comparator.naturalOrder())))
                        .orElse(null);

        return new CategorySummary(definition.key(), definition.label(), definition.icon(),
                resourceCount, latestItems, latestPolicyUpdate);
    }

    private boolean matchesAnyTag(List<String> resourceTags, List<String> matchNewsTags) {
        if (resourceTags == null) return false;
        for (String tag : resourceTags) {
            for (String match : matchNewsTags) {
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
// MATCHING AGAINST NewsItem.resourceTags, NOT .tags: RssFeedService's
// classifyLegislation() produces BOTH fields from the same matched-bucket
// list, differing only in casing — tags gets Capitalized names ("Housing")
// for display, resourceTags gets the same keys lowercase ("housing") —
// see RssFeedService.java's classifyLegislation(). CategoryDefinition's
// matchNewsTags values are lowercase specifically to line up with
// resourceTags exactly, with an additional equalsIgnoreCase comparison as
// a defensive safety net (costs nothing, guards against future casing
// drift). Matching against .tags would require the mapping table to carry
// display-cased strings instead, coupling this class to a formatting
// choice made for a different purpose.
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
// matchNewsTags.isEmpty() SHORT-CIRCUITS TO null RATHER THAN SCANNING ALL
// NEWS: categories like "clothing"/"furniture-household"/
// "community-support" have no News-tag linkage defined at all (no
// RssFeedService bucket maps onto them) — skipping the scan for these is
// both a minor efficiency win and, more importantly, correct: an empty
// matchNewsTags list would otherwise vacuously match every News item via
// matchesAnyTag's inner loop, wrongly making every category's
// latestPolicyUpdate resolve to "whatever is most recent overall."
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
