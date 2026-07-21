package org.firststep.backend.legislation.service;

import java.util.Comparator;
import java.util.List;

import org.firststep.backend.legislation.dto.LawItem;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.shared.model.ContentSource;
import org.springframework.stereotype.Service;

/**
 * Exposes the most recently signed Delaware bills for the homepage rotator.
 * The RSS feed (RssFeedService) is configured to the GovernorSignedLegislation
 * feed, so its items ARE the signed bills; this just sorts newest-first,
 * caps at the rotator size, and maps to the display DTO — aggregation stays
 * server-side (backend aggregates, frontend displays).
 */
@Service
public class LegislationService {

    private static final int MAX_BILLS = 7;

    private final RssFeedSource rssFeedSource;

    public LegislationService(RssFeedSource rssFeedSource) {
        this.rssFeedSource = rssFeedSource;
    }

    public List<LawItem> getRecentSignedBills() {
        return rssFeedSource.getRssItems().stream()
                // Newest first; bills without a date sort last (yyyy-MM-dd is
                // lexically sortable).
                .sorted(Comparator.comparing((NewsItem n) -> n.published,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_BILLS)
                .map(this::toLawItem)
                .toList();
    }

    private LawItem toLawItem(NewsItem n) {
        ContentSource cs = n.contentSource;
        return new LawItem(n.title, cs != null ? cs.url : null, n.published);
    }
}
