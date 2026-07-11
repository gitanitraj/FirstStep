package org.firststep.backend.category.dto;

import java.util.List;

import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.search.dto.SearchResult;

public record CategorySummary(
        String key,
        String label,
        String icon,
        int resourceCount,
        List<SearchResult> latestItems,
        NewsItem latestPolicyUpdate
) {
}
