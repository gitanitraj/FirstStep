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
