package org.firststep.backend.flyer.service;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.firststep.backend.flyer.dto.FlyerCard;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

@Service
public class FlyerService {

    // Seasonal flyer images are served as static resources under this path.
    private static final String IMAGE_BASE = "/images/seasonal/";

    private final FlyerRepository repository;

    public FlyerService(FlyerRepository repository) {
        this.repository = repository;
    }

    public List<Flyer> getAll() {
        return repository.findAll();
    }

    public Optional<Flyer> getById(String id) {
        return repository.findById(id);
    }

    /**
     * Display-ready flyer cards for the homepage Community Information carousel:
     * flyers that have an image, sorted by event date (soonest first, undated
     * last), with a fully-resolved, URL-encoded imageUrl. Aggregation/encoding
     * stays server-side (backend aggregates, frontend displays).
     */
    public List<FlyerCard> getCarouselCards() {
        return getAll().stream()
                .filter(f -> f.image != null && !f.image.isBlank())
                .sorted(Comparator.comparing((Flyer f) -> f.eventDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toCard)
                .toList();
    }

    private FlyerCard toCard(Flyer f) {
        String imageUrl = IMAGE_BASE + UriUtils.encodePathSegment(f.image, StandardCharsets.UTF_8);
        return new FlyerCard(imageUrl, f.title, f.organization, f.eventDate);
    }
}
