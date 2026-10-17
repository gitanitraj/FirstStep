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
