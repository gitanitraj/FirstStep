// =============================================================================
// ANNOTATED REFERENCE — backend/.../flyer/service/FlyerService.java
// Flyer slice originally; carousel cards in Slice E (Decision 025);
// imageUrlFor made public in Slice J (Decision 046).
// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// FlyerService is the flyer slice's service layer: thin delegation to
// FlyerRepository for getAll()/getById(), plus the two operations that belong to
// flyers specifically — building homepage carousel cards, and resolving a
// flyer's image URL.
// =============================================================================

package org.firststep.backend.flyer.service;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.firststep.backend.flyer.dto.FlyerCard;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.shared.model.Sector;
import org.firststep.backend.shared.service.ContentSourceService;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

@Service
public class FlyerService {

    // Seasonal flyer images are served as static resources under this path.
    private static final String IMAGE_BASE = "/images/seasonal/";

    private final FlyerRepository repository;
    private final ContentSourceService contentSources;

    public FlyerService(FlyerRepository repository, ContentSourceService contentSources) {
        this.repository = repository;
        this.contentSources = contentSources;
    }

    public List<Flyer> getAll() {
        return repository.findAll();
    }

    public Optional<Flyer> getById(String id) {
        return repository.findById(id);
    }

    /**
     * Display-ready flyer cards for the homepage Community Notices row:
     * COMMUNITY-produced flyers that have an image, sorted by event date
     * (soonest first, undated last), with a fully-resolved, URL-encoded
     * imageUrl. Aggregation/encoding stays server-side (backend aggregates,
     * frontend displays).
     *
     * <p><b>The sector filter is what makes the row's label true.</b> The
     * homepage section is a PREVIEW of /community-notices, and that destination
     * is scoped to {@link Sector#COMMUNITY}. Without this clause the row showed
     * government-produced flyers under a heading that says "Community" and a
     * link to a page that correctly excludes them — a resident who clicked
     * through to find the flyer they just saw would not find it. Nothing is
     * lost: government flyers appear in Latest Updates, which is their
     * destination.
     */
    public List<FlyerCard> getCarouselCards() {
        return getAll().stream()
                .filter(f -> contentSources.isInSector(f.contentSource, Sector.COMMUNITY))
                .filter(f -> f.image != null && !f.image.isBlank())
                .sorted(Comparator.comparing((Flyer f) -> f.eventDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toCard)
                .toList();
    }

    /**
     * The resolved, URL-encoded path for a flyer's image, or null when it has
     * none.
     *
     * <p>Public because the Community Notices gallery needs the same URL and this
     * is the ONE place the rule lives: seasonal images only serve from that
     * static path when the filename is %20-encoded (Decision 025). A second
     * service re-deriving it would be a second place to get the encoding wrong.
     */
    public String imageUrlFor(Flyer f) {
        if (f == null || f.image == null || f.image.isBlank()) {
            return null;
        }
        return IMAGE_BASE + UriUtils.encodePathSegment(f.image, StandardCharsets.UTF_8);
    }

    private FlyerCard toCard(Flyer f) {
        return new FlyerCard(imageUrlFor(f), f.title, f.organization, f.eventDate);
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// It began as a direct mirror of ResourceService's shape — thin delegation, no
// logic of its own. It has since acquired exactly two responsibilities, and both
// are things only this class can correctly do:
//
//   getCarouselCards()  aggregation for the homepage's Community Notices row,
//                       done server-side because the backend aggregates and the
//                       frontend displays. Sector-scoped — see the last section.
//
//   imageUrlFor(Flyer)  the ONE place the image-path rule lives.
//
// Unlike ResourceService/NewsService, it does NOT implement a
// DecisionAgentService.*ServiceLike marker interface — Flyer still isn't wired
// into the AI decision-aid's retrieval. Adding one would be speculative.
// =============================================================================

// =============================================================================
// SLICE J — WHY imageUrlFor BECAME PUBLIC
// =============================================================================
// The Community Notices flyer gallery needs the same URL the homepage carousel
// needs. There were three ways to give it one:
//
//   1. Duplicate the IMAGE_BASE + encode logic in CommunityNoticesService.
//   2. Reuse getCarouselCards() and map FlyerCard onto ContentItem.
//   3. Make the existing private helper public.
//
// (1) is a second place to get the encoding wrong, and it WOULD go wrong: the
// rule is not obvious. Seasonal images only serve from the static path when the
// filename is %20-encoded (Decision 025) — a bug found by a broken image, not by
// a test.
//
// (2) looks like reuse but is not: getCarouselCards() FILTERS OUT flyers with no
// image and sorts for the homepage. The gallery must keep image-less flyers (a
// count of 5 above a grid of 4 is a bug) and sorts for its own view. Reusing it
// would have meant inheriting two decisions that belong to a different page.
//
// (3) shares the rule and nothing else. The method was already written, already
// correct, and had no carousel-specific behavior in it — the extraction had
// effectively already happened when toCard() started delegating to it.
//
// This is the "an abstraction earns its name on the second use" rule (F4→F5a)
// applied to a method rather than a component: the second caller is what turned
// a private helper into part of the class's contract.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - FlyerController calls getAll()/getById(id).
// - HomeService calls getCarouselCards() for the homepage Community Notices row.
// - UpdatesService reads getAll() for the community sector feed.
// - CommunityNoticesService (Slice J) reads getAll() and calls imageUrlFor(f)
//   when flattening a Flyer into a ContentItem for the gallery.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - A FlyerServiceLike interface anticipating future AI integration: rejected as
//   speculative — add it when DecisionAgentService actually retrieves flyers.
// - Moving imageUrlFor to a shared ImageUrlResolver utility: rejected. Only
//   flyers have images today, so the utility would have one implementation and
//   one caller-type. It earns its own class when a second content type gets
//   images, not before.
// =============================================================================

// =============================================================================
// SLICE J — THE BUG THE RENAME EXPOSED, AND WHY THE FILTER LIVES HERE
// =============================================================================
// Renaming the homepage row from "Community Information" to "Community Notices"
// and pointing it at /community-notices made an existing bug VISIBLE and, more
// importantly, made it a bug at all.
//
// getCarouselCards() had never been sector-scoped. On live data it was returning
// the three soonest-dated flyers with images, which happened to be:
//
//     FL-005  Disability Services & Benefits Info Fair   GOVERNMENT
//     FL-007  Free Furniture Giveaway                    GOVERNMENT
//     FL-004  Back-to-School Supply Drive Fundraiser      community
//
// Two thirds of a row labeled "Community Notices" was government-produced — and
// the destination it now links to correctly excludes both. A resident who saw
// the furniture giveaway on the homepage and clicked "See all community notices"
// would not find it there. The label was false and the link was a dead end for
// the very items it advertised.
//
// This was only found by LOOKING AT THE RENDERED PAGE. Every test passed, the
// API returned exactly what it was asked for, and the section had looked
// correct for two slices under a vaguer name. Worth remembering: a rename can
// convert working code into wrong code without touching it, because the label is
// part of the contract.
//
// WHY THE FILTER IS HERE AND NOT IN HomeService
// ---------------------------------------------
// getCarouselCards() already owned three quarters of this row's content rule —
// has an image, sorted soonest-first, mapped to a card. "Community-produced" is
// the fourth clause of the same rule, and splitting one rule across two classes
// so that HomeService re-filters what this method just built would leave neither
// class able to state what the row contains.
//
// The cost, stated plainly: FlyerService now depends on ContentSourceService,
// and nine test files gained a constructor argument. That was the honest price.
// The alternative considered was a getCarouselCards(Predicate<Flyer>) overload
// that kept this class sector-ignorant — rejected as configurability nobody
// asked for, invented purely to avoid touching test constructors.
//
// NOTHING IS LOST. Government flyers appear in Latest Updates (/updates), which
// is scoped to their sector and already carried both — verified on live data
// before the filter was added, so this removes a wrong placement rather than a
// resident's only route to that content.
// =============================================================================
