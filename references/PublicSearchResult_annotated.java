/*
 * Annotated reference: backend/src/main/java/org/firststep/backend/search/dto/PublicSearchResult.java
 * WHAT: Preserves the complete source below; see the explicit allowlists and optional exact-owner filters in the source.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Controllers adapt internal models only at the resident boundary; tests and evidence exercise real repositories plus labeled synthetic counterexamples.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
package org.firststep.backend.search.dto;
import org.firststep.backend.shared.dto.ResidentContent;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.dto.FlyerSummary;
import org.firststep.backend.flyer.service.FlyerService;
/** Public counterpart of the internal ranking result. Scores and order are preserved. */
public record PublicSearchResult(String type, int score, Object content) {
    public static PublicSearchResult from(SearchResult r, FlyerService flyers) {
        return new PublicSearchResult(r.type(), r.score(), r.content() instanceof Flyer f
                ? FlyerSummary.from(f, flyers) : ResidentContent.from(r.content()));
    }
}
