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
