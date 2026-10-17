package org.firststep.backend.category.dto;
import java.util.List;
import org.firststep.backend.news.dto.NewsSummary;
import org.firststep.backend.search.dto.PublicSearchResult;
import org.firststep.backend.flyer.service.FlyerService;
/** Public adapter, leaving category selection, counts and ranking internal and unchanged. */
public record PublicCategorySummary(String key, String label, String icon, int resourceCount,
        List<PublicSearchResult> latestItems, NewsSummary latestPolicyUpdate) {
    public static PublicCategorySummary from(CategorySummary c, FlyerService flyers) {
        return new PublicCategorySummary(c.key(), c.label(), c.icon(), c.resourceCount(),
                c.latestItems().stream().map(r -> PublicSearchResult.from(r, flyers)).toList(),
                c.latestPolicyUpdate() == null ? null : NewsSummary.from(c.latestPolicyUpdate()));
    }
}
