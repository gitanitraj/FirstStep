package org.firststep.backend.category.service;

import java.util.List;

import org.firststep.backend.category.dto.TopicPage;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.model.Location;
import org.firststep.backend.shared.model.Website;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Wired with the REAL taxonomy over mocked content — the thing under test is how
 * editorial classification selects items for a topic, and the taxonomy IS that
 * vocabulary.
 */
class TopicPageServiceTest {

    private ResourceService resourceService;
    private FlyerService flyerService;
    private TopicPageService service;

    @BeforeEach
    void setUp() {
        resourceService = mock(ResourceService.class);
        flyerService = mock(FlyerService.class);
        when(resourceService.getAll()).thenReturn(List.of());
        when(flyerService.getAll()).thenReturn(List.of());
        service = new TopicPageService(new TaxonomyService("../app/data"), resourceService, flyerService);
    }

    private static Resource resource(String id, String title, List<String> categoryTags, String subcategory) {
        Resource r = new Resource();
        r.id = id;
        r.title = title;
        r.summary = title + " summary";
        r.categoryTags = categoryTags;
        r.subcategory = subcategory;
        return r;
    }

    private TopicPage housingShelter() {
        return service.getByKey("housing", "emergency-shelter", null).orElseThrow();
    }

    // ---- Resolution --------------------------------------------------------

    @Test
    void shouldResolveTheSlugToItsCanonicalTopicName() {
        TopicPage page = housingShelter();

        assertEquals("Emergency Shelter", page.metadata().name());
        assertEquals("emergency-shelter", page.metadata().slug());
    }

    @Test
    void shouldCarryTheParentCategoryForABreadcrumb() {
        TopicPage page = housingShelter();

        assertEquals("housing", page.metadata().categoryKey());
        assertEquals("Housing", page.metadata().categoryLabel());
    }

    @Test
    void shouldReturnEmptyForUnknownCategory() {
        assertTrue(service.getByKey("nonexistent", "emergency-shelter", null).isEmpty());
    }

    @Test
    void shouldReturnEmptyForUnknownTopicSlug() {
        assertTrue(service.getByKey("housing", "not-a-topic", null).isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenTheTopicBelongsToADifferentCategory() {
        // "Food Pantry" is a food topic. Requesting it under housing must 404
        // rather than quietly returning an empty housing page.
        assertTrue(service.getByKey("housing", "food-pantry", null).isEmpty());
    }

    // ---- Selection ---------------------------------------------------------

    @Test
    void shouldListOnlyContentClassifiedIntoThisCategoryAndTopic() {
        when(resourceService.getAll()).thenReturn(List.of(
                resource("R1", "Shelter A", List.of("Housing"), "Emergency Shelter"),
                resource("R2", "Wrong topic", List.of("Housing"), "Rental Assistance"),
                resource("R3", "Wrong category", List.of("Food"), "Emergency Shelter")));

        assertEquals(List.of("R1"), housingShelter().items().stream().map(ContentItem::id).toList());
    }

    @Test
    void shouldNotListUnclassifiedContent() {
        // The read-model rule, again: no editorial classification, no placement.
        Resource r = resource("R1", "Emergency shelter for families", null, null);
        r.description = "Emergency shelter beds and housing support.";
        when(resourceService.getAll()).thenReturn(List.of(r));

        assertTrue(housingShelter().items().isEmpty());
    }

    @Test
    void shouldScopeADualDeclaredTopicToTheCategoryAsked() {
        // "Eviction Prevention" is declared by both Housing and Legal. An item
        // classified Housing-only must not surface on the Legal topic page.
        when(resourceService.getAll()).thenReturn(List.of(
                resource("R1", "Housing only", List.of("Housing"), "Eviction Prevention")));

        assertEquals(1, service.getByKey("housing", "eviction-prevention", null).orElseThrow().items().size());
        assertEquals(0, service.getByKey("legal", "eviction-prevention", null).orElseThrow().items().size());
    }

    @Test
    void shouldIncludeFlyersAlongsideResources() {
        when(resourceService.getAll()).thenReturn(List.of(
                resource("R1", "A shelter", List.of("Housing"), "Emergency Shelter")));
        Flyer f = new Flyer();
        f.id = "F1";
        f.title = "B flyer";
        f.categoryTags = List.of("Housing");
        f.subcategory = "Emergency Shelter";
        f.eventDate = "2026-08-01";
        when(flyerService.getAll()).thenReturn(List.of(f));

        TopicPage page = housingShelter();

        assertEquals(2, page.metadata().totalCount());
        assertEquals(1, page.metadata().countsByType().get(ContentType.RESOURCE));
        assertEquals(1, page.metadata().countsByType().get(ContentType.FLYER));
    }

    @Test
    void shouldFilterByCommunityWhenRequested() {
        Resource local = resource("R1", "Local", List.of("Housing"), "Emergency Shelter");
        local.communityId = "wilmington-de";
        Resource other = resource("R2", "Elsewhere", List.of("Housing"), "Emergency Shelter");
        other.communityId = "newark-de";
        when(resourceService.getAll()).thenReturn(List.of(local, other));

        TopicPage page = service.getByKey("housing", "emergency-shelter", "wilmington-de").orElseThrow();

        assertEquals(List.of("R1"), page.items().stream().map(ContentItem::id).toList());
    }

    @Test
    void shouldReturnAValidPageForATopicWithNoContent() {
        TopicPage page = housingShelter();

        assertEquals(0, page.metadata().totalCount());
        assertTrue(page.items().isEmpty());
        assertEquals("Emergency Shelter", page.metadata().name());
    }

    // ---- Normalization -----------------------------------------------------

    @Test
    void shouldSortAlphabeticallyIgnoringCase() {
        when(resourceService.getAll()).thenReturn(List.of(
                resource("R1", "zebra house", List.of("Housing"), "Emergency Shelter"),
                resource("R2", "Alpha House", List.of("Housing"), "Emergency Shelter"),
                resource("R3", "middle house", List.of("Housing"), "Emergency Shelter")));

        assertEquals(List.of("Alpha House", "middle house", "zebra house"),
                housingShelter().items().stream().map(ContentItem::title).toList());
    }

    @Test
    void shouldNormalizeAResourceOntoTheCard() {
        Resource r = resource("R1", "Shelter", List.of("Housing"), "Emergency Shelter");
        r.organization = "Ministry of Caring";
        r.cost = "free";
        r.urgency = "emergency";
        Location loc = new Location();
        loc.city = "Wilmington";
        loc.address = "123 Main St";
        r.locations = List.of(loc);
        Website w = new Website();
        w.url = "https://example.org";
        r.websites = List.of(w);
        when(resourceService.getAll()).thenReturn(List.of(r));

        ContentItem item = housingShelter().items().get(0);

        assertEquals(ContentType.RESOURCE, item.contentType());
        assertEquals("Ministry of Caring", item.organization());
        assertEquals("free", item.cost());
        assertEquals("emergency", item.urgency());
        assertEquals("https://example.org", item.url());
        assertEquals("Wilmington", item.location());
        assertNull(item.date(), "a resource has no editorial date");
    }

    @Test
    void shouldExposeTheCityButNeverTheStreetAddress() {
        Resource r = resource("R1", "Shelter", List.of("Housing"), "Emergency Shelter");
        Location loc = new Location();
        loc.city = "Newark";
        loc.address = "500 Confidential Way";
        r.locations = List.of(loc);
        when(resourceService.getAll()).thenReturn(List.of(r));

        ContentItem item = housingShelter().items().get(0);

        assertEquals("Newark", item.location());
        assertTrue(housingShelter().items().stream()
                        .noneMatch(i -> String.valueOf(i.location()).contains("500")),
                "a browse card must not carry a street address");
    }

    @Test
    void shouldFallBackToTheDescriptionWhenAResourceHasNoSummary() {
        Resource r = resource("R1", "Shelter", List.of("Housing"), "Emergency Shelter");
        r.summary = null;
        r.description = "Full description text.";
        when(resourceService.getAll()).thenReturn(List.of(r));

        assertEquals("Full description text.", housingShelter().items().get(0).summary());
    }
}
