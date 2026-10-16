package org.firststep.backend.category.service;

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
        // ONE rule for every content type. Slice F2 moved the raw-source-category
        // translation into the classifier (shared/classification), which runs at
        // ingestion — so by the time anything reaches this service it already
        // carries canonical categoryTags, and this service no longer knows or
        // cares that resources arrive from a directory with its own vocabulary.
        List<Resource> matchedResources = resources.stream()
                .filter(r -> taxonomyService.matchesCategoryTags(definition, r.categoryTags))
                .toList();

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
