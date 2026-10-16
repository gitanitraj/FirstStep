package org.firststep.backend.search.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// SearchService is the /api/search endpoint's implementation: given a query
// string and an optional communityId, it scores every Resource, NewsItem,
// and Flyer the current community can see, keeps the ones that matched at
// all, and returns them as one list sorted by score descending.
// =============================================================================

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.search.dto.SearchResult;
import org.firststep.backend.shared.util.TextScore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private final ResourceService resourceService;
    private final NewsService newsService;
    private final FlyerService flyerService;

    @Value("${app.default-community-id:wilmington-de}")
    private String defaultCommunityId;

    public SearchService(ResourceService resourceService, NewsService newsService, FlyerService flyerService) {
        this.resourceService = resourceService;
        this.newsService = newsService;
        this.flyerService = flyerService;
    }

    public List<SearchResult> search(String query, String communityId) {
        String community = (communityId == null || communityId.isBlank()) ? defaultCommunityId : communityId;

        List<SearchResult> results = new ArrayList<>();

        for (Resource r : resourceService.getAll()) {
            if (!community.equals(r.communityId)) continue;
            int score = TextScore.match(query, r.organization)
                    + TextScore.match(query, r.summary)
                    + TextScore.match(query, r.description)
                    + TextScore.match(query, r.category)
                    + TextScore.match(query, r.subcategory)
                    + TextScore.match(query, r.tags);
            if (score > 0) {
                results.add(new SearchResult("resource", score, r));
            }
        }

        for (NewsItem n : newsService.getAll()) {
            if (!community.equals(n.communityId)) continue;
            int score = TextScore.match(query, n.title)
                    + TextScore.match(query, n.summary)
                    + TextScore.match(query, n.whyItMatters)
                    + TextScore.match(query, n.tags);
            if (score > 0) {
                results.add(new SearchResult("news", score, n));
            }
        }

        for (Flyer f : flyerService.getAll()) {
            if (!community.equals(f.communityId)) continue;
            int score = TextScore.match(query, f.title)
                    + TextScore.match(query, f.summary)
                    + TextScore.match(query, f.organization)
                    + TextScore.match(query, f.tags);
            if (score > 0) {
                results.add(new SearchResult("flyer", score, f));
            }
        }

        results.sort(Comparator.comparingInt(SearchResult::score).reversed());
        return results;
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// DEPENDS ON ResourceService/NewsService/FlyerService, NOT THEIR
// REPOSITORIES: treats each slice's Service as its public API surface,
// matching the layering discipline every other cross-slice dependency in
// this codebase already follows (e.g. DecisionAgentService depends on
// ResourceServiceLike/NewsServiceLike, not on repositories directly).
// SearchService doesn't own any data of its own — it's pure composition.
//
// FIELD SELECTION PER TYPE IS NOT GUESSED: Resource's fields
// (organization/summary/description/category/subcategory/tags) and
// NewsItem's (title/summary/whyItMatters/tags) are EXACTLY what
// DecisionAgentService.selectTopResources/selectTopNews already search —
// a proven, already-tuned field selection, reused rather than reinvented.
// Flyer's fields (title/summary/organization/tags) are the analogous set
// for its simpler shape (Flyer has no richer text fields to draw from).
//
// SUMS TextScore.match() ACROSS DIFFERENT FIELDS, RELIES ON TextScore's
// OWN FIRST-MATCH-WINS SEMANTICS WITHIN A LIST FIELD: e.g. for a Resource,
// this method calls TextScore.match() six separate times (once per field)
// and adds the results — so matching both `organization` AND `tags` scores
// higher than matching only one. But a single call like
// TextScore.match(query, r.tags) still only awards 5 points regardless of
// how many individual tags matched (TextScore's existing, preserved
// behavior — see TextScore_annotated.java). This gets "more matching
// fields ranks higher" without changing TextScore's tested semantics.
//
// COMMUNITY FILTERING IS THE FIRST REAL USE OF communityId AS A FILTER IN
// THIS CODEBASE: every repository (JsonResourceRepository,
// JsonNewsRepository, JsonFlyerRepository) stamps the same default
// (`wilmington-de`) onto every record but nothing had ever read it back
// for filtering before this class. It's inert today (single community, so
// every record always matches), but establishes the community-aware
// plumbing ahead of the later multi-tenancy backlog item — per explicit
// user direction that Search be "community-aware from day one."
//
// A MISSING communityId FALLS BACK TO app.default-community-id, NOT "no
// filter": a search request is always scoped to SOME community context by
// default, rather than defaulting to global/cross-community — consistent
// with every other slice's default-stamping behavior.
//
// NO PAGINATION (PageResponse<T> NOT USED): matches the existing,
// established convention — no other endpoint in this codebase paginates
// (PageResponse has been an unwired shape since the original migration);
// introducing pagination only for Search would be inconsistent, and the
// current dataset sizes (58 resources, single-digit news/flyers) don't
// need it yet.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - search/controller/SearchController is the only caller of search().
// - Depends on resource/service/ResourceService, news/service/NewsService,
//   flyer/service/FlyerService (constructor injection).
// - Uses shared/util/TextScore for all substring-scoring (see that file's
//   annotated reference for why the logic lives there and not here).
// - Produces search/dto/SearchResult instances.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Depending on ResourceRepository/NewsRepository/FlyerRepository
//   directly instead of the Service layer: rejected — would bypass each
//   slice's own service-layer API surface for no benefit, since the
//   services here are pure delegation anyway; going through them keeps the
//   layering consistent with the rest of the app.
// - Building a dedicated inverted index / real search engine (Lucene,
//   etc.): explicitly out of scope — this is an in-memory, small-dataset
//   MVP matching the same "no premature abstraction" philosophy the rest
//   of this codebase follows; revisit if/when dataset size or query
//   complexity actually demands it.
// =============================================================================
