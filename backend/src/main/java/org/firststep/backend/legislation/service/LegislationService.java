package org.firststep.backend.legislation.service;

import java.util.Comparator;
import java.util.List;

import org.firststep.backend.legislation.dto.LawItem;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.SignedLegislationSource;
import org.firststep.backend.shared.model.ContentSource;
import org.springframework.stereotype.Service;

/**
 * Exposes the most recently signed Delaware bills for the homepage rotator.
 * The RSS feed (RssFeedService) is configured to the GovernorSignedLegislation
 * feed, so its items ARE the signed bills; this just sorts newest-first,
 * caps at the rotator size, and maps to the display DTO — aggregation stays
 * server-side (backend aggregates, frontend displays).
 *
 * <p><b>Reads {@link SignedLegislationSource}, deliberately NOT
 * {@code RssFeedSource}.</b> This is legislation PRESENTATION: the section is
 * titled "New Delaware Laws" and its job is to show what the Governor signed,
 * whether or not any of it is relevant to a resident looking for help. Reading
 * the relevance-gated discovery feed instead would silently drop every bill the
 * classifier could not categorize — roughly half of them — turning a factual
 * legislative feed into an editorial selection without anyone deciding to
 * (Slice F2.1).
 */
@Service
public class LegislationService {

    private static final int MAX_BILLS = 7;

    private final SignedLegislationSource signedLegislationSource;

    public LegislationService(SignedLegislationSource signedLegislationSource) {
        this.signedLegislationSource = signedLegislationSource;
    }

    public List<LawItem> getRecentSignedBills() {
        return signedLegislationSource.getSignedBills().stream()
                // Newest first; bills without a date sort last (yyyy-MM-dd is
                // lexically sortable).
                .sorted(Comparator.comparing((NewsItem n) -> n.publishDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_BILLS)
                .map(this::toLawItem)
                .toList();
    }

    private LawItem toLawItem(NewsItem n) {
        ContentSource cs = n.contentSource;
        return new LawItem(n.title, cs != null ? cs.url : null, n.publishDate);
    }
}
