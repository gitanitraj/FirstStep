package org.firststep.backend.flyer.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.flyer.dto.FlyerCard;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.service.ContentSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlyerServiceTest {

    private FlyerRepository repository;
    private FlyerService service;

    @BeforeEach
    void setUp() {
        repository = mock(FlyerRepository.class);
        service = new FlyerService(repository, new ContentSourceService("../app/data"));
    }

    @Test
    void shouldReturnAllFlyersFromRepositoryWhenGetAllIsCalled() {
        Flyer flyer = new Flyer();
        flyer.id = "FL-001";
        when(repository.findAll()).thenReturn(List.of(flyer));

        List<Flyer> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals("FL-001", result.get(0).id);
    }

    @Test
    void shouldReturnFlyerByIdFromRepositoryWhenGetByIdIsCalled() {
        Flyer flyer = new Flyer();
        flyer.id = "FL-002";
        when(repository.findById("FL-002")).thenReturn(Optional.of(flyer));

        Optional<Flyer> result = service.getById("FL-002");

        assertTrue(result.isPresent());
        assertEquals("FL-002", result.get().id);
    }

    private static Flyer flyer(String title, String image, String eventDate) {
        return flyer(title, image, eventDate, "ministry-of-caring");
    }

    /** The carousel is the homepage's Community Notices preview, so every fixture
     *  needs a producer — an unattributed flyer is in no sector and shows nowhere. */
    private static Flyer flyer(String title, String image, String eventDate, String sourceId) {
        Flyer f = new Flyer();
        f.title = title;
        f.image = image;
        f.eventDate = eventDate;
        ContentSource cs = new ContentSource();
        cs.id = sourceId;
        cs.name = sourceId;
        f.contentSource = cs;
        return f;
    }

    @Test
    void shouldResolveAndUrlEncodeImagePathForCarouselCards() {
        when(repository.findAll()).thenReturn(List.of(flyer("Health Fair", "Health Fair.jpg", "2026-08-05")));

        FlyerCard card = service.getCarouselCards().get(0);

        assertEquals("/images/seasonal/Health%20Fair.jpg", card.imageUrl());
        assertEquals("Health Fair", card.title());
    }

    @Test
    void shouldSortCarouselByEventDateSoonestFirst() {
        when(repository.findAll()).thenReturn(List.of(
                flyer("Later", "b.jpg", "2026-08-15"),
                flyer("Sooner", "a.jpg", "2026-07-20")));

        List<FlyerCard> cards = service.getCarouselCards();

        assertEquals("Sooner", cards.get(0).title());
        assertEquals("Later", cards.get(1).title());
    }

    @Test
    void shouldSkipGovernmentFlyersBecauseTheCarouselIsTheCommunityNoticesPreview() {
        // The homepage row is labeled Community Notices and links to a destination
        // scoped to the community sector. A government flyer here would be visible
        // under that heading and absent from the page it links to.
        when(repository.findAll()).thenReturn(List.of(
                flyer("Community fundraiser", "a.jpg", "2026-08-01", "ministry-of-caring"),
                flyer("Housing authority giveaway", "b.jpg", "2026-07-01", "wilmington-housing-authority")));

        List<FlyerCard> cards = service.getCarouselCards();

        assertEquals(1, cards.size());
        assertEquals("Community fundraiser", cards.get(0).title());
    }

    @Test
    void shouldSkipFlyerWhoseProducerCannotBeResolved() {
        // Slice I's failure boundary: no sector claim can be made, so no
        // sector-scoped surface claims it. It stays valid CivicContent elsewhere.
        when(repository.findAll()).thenReturn(List.of(
                flyer("Known producer", "a.jpg", "2026-08-01", "ministry-of-caring"),
                flyer("Mystery producer", "b.jpg", "2026-07-01", "not-in-the-registry")));

        List<FlyerCard> cards = service.getCarouselCards();

        assertEquals(1, cards.size());
        assertEquals("Known producer", cards.get(0).title());
    }

    @Test
    void shouldSkipFlyersWithoutAnImage() {
        when(repository.findAll()).thenReturn(List.of(
                flyer("Has image", "a.jpg", "2026-08-01"),
                flyer("No image", null, "2026-07-01")));

        List<FlyerCard> cards = service.getCarouselCards();

        assertEquals(1, cards.size());
        assertEquals("Has image", cards.get(0).title());
    }
}
