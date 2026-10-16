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

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// LegislationService feeds the homepage's "New Delaware Laws" rotator: the 7
// most recently signed bills, newest first, mapped to a display DTO.
// =============================================================================

// =============================================================================
// WHY IT READS SignedLegislationSource AND NOT RssFeedSource
// =============================================================================
// This is the only interesting thing about the class, and it is a one-word
// difference that decides what a resident sees.
//
// Slice F2.1 split the RSS feed in two: a relevance-gated CivicContent feed for
// DISCOVERY, and the complete signed-bill feed for legislation PRESENTATION.
// This service reads the second.
//
// The rotator's job is to report what the Governor signed. It is titled "New
// Delaware Laws", not "laws that might help you" — so a bill about pet stores or
// animal cruelty belongs there, even though the classification engine correctly
// judges it irrelevant to a resident looking for housing or food assistance.
//
// Had this kept reading the gated feed, the rotator would have quietly lost
// roughly half its content (253 of 428 bills) and become an editorial selection
// that nobody chose to make. The failure would have been invisible: seven bills
// would still appear, just a different seven.
//
// THE GENERAL SHAPE, worth carrying to other features: when one upstream feed
// serves two purposes, the purposes will eventually want different filters. The
// interfaces are the place to name that, not a boolean parameter — a caller
// naming the interface it depends on is stating which question it is asking.
//
// Both interfaces stay single-method so tests can supply a lambda.
//
// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - RssFeedService implements both RssFeedSource and SignedLegislationSource.
// - HomeService composes this into HomePayload.delawareLaws.
// - NavigationService (Slice F3) reads the OTHER interface, RssFeedSource,
//   because category pages are discovery — the mirror image of this decision.
