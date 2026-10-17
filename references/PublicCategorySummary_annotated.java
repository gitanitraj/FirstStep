/*
 * Annotated reference: backend/src/main/java/org/firststep/backend/category/dto/PublicCategorySummary.java
 * WHAT: Preserves the complete source below; see the explicit allowlists and optional exact-owner filters in the source.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Controllers adapt internal models only at the resident boundary; tests and evidence exercise real repositories plus labeled synthetic counterexamples.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
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
