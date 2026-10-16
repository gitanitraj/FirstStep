package org.firststep.backend.search.service;

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
