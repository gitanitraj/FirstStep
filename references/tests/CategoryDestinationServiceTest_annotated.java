/*
 * WHAT: Complete annotated mirror of backend/src/test/java/org/firststep/backend/category/service/CategoryDestinationServiceTest.java for IM-08.
 * WHY: Integrate approved selector outcomes with scoped, accessible resident navigation.
 * INTERACTION: Existing eligibility and collection services own admission; the destination
 * projection allowlists cards/links, while React owns responsive presentation and route scope.
 * ALTERNATIVES: Reimplementing selectors or deriving providers from source names would violate
 * predecessor contracts. Tests cover pass-through, actual catalog HTTP, and browser journeys.
 */
package org.firststep.backend.category.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.firststep.backend.category.dto.CategoryCollection;
import org.firststep.backend.category.dto.CollectionLayer;
import org.firststep.backend.category.selection.FeaturedItemSelector;
import org.firststep.backend.category.selection.FeaturedTopicSelector;
import org.firststep.backend.category.selection.FeaturedTopicInventory;
import org.firststep.backend.originals.service.ArticleService;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.model.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoryDestinationServiceTest {
    private FeaturedTopicSelector topics;
    private FeaturedItemSelector items;
    private ArticleService articles;
    private FeaturedTopicInventory inventory;
    private CategoryDestinationService service;
    private final LocalDate today = LocalDate.of(2026, 10, 16);

    @BeforeEach void setUp() {
        topics = mock(FeaturedTopicSelector.class); items = mock(FeaturedItemSelector.class);
        articles = mock(ArticleService.class); inventory = mock(FeaturedTopicInventory.class);
        when(articles.getPublishable()).thenReturn(List.of());
        when(inventory.current(anyString(), any())).thenReturn(new FeaturedTopicInventory.Snapshot(List.of(), List.of()));
        service = new CategoryDestinationService(topics, items, articles, inventory, new TaxonomyService("../app/data"));
    }
    private FeaturedTopicSelector.TopicEvidence topic(String key, String name) {
        return new FeaturedTopicSelector.TopicEvidence(key, name, 1, 1, 0, 0, null,
                List.of(), Map.of(), List.of(), List.of(), List.of());
    }
    private ContentItem card(String id, String url) {
        return new ContentItem(ContentType.RESOURCE, id, id, null, null, null, null, null, null, url, null);
    }
    private FeaturedItemSelector.Position position(String id, FeaturedItemSelector.PositionType type, String url) {
        var candidate = new FeaturedItemSelector.Candidate(card(id, url),
                new FeaturedItemSelector.Provenance("synthetic-community", "source", null, null, url, null),
                true, Set.of(), null, null, List.of("private-evidence"), List.of("private-limitation"));
        return new FeaturedItemSelector.Position(type, candidate, "private-tie-stage");
    }
    private void selected(List<FeaturedTopicSelector.TopicEvidence> selected) {
        when(topics.select("synthetic-community", "food")).thenReturn(new FeaturedTopicSelector.Selection(
                "synthetic-community", "food", today, selected, selected, List.of()));
    }
    private void winners(String name, List<FeaturedItemSelector.Position> positions) {
        when(items.select("synthetic-community", "food", name)).thenReturn(new FeaturedItemSelector.Selection(
                "synthetic-community", "food", name, today, positions, List.of(), List.of()));
    }

    @Test void t02t03PassesThroughInjectedTopicAndItemOrderWithoutHousingRanking() {
        selected(List.of(topic("meal-delivery", "Meal Delivery"), topic("food-pantry", "Food Pantry")));
        winners("Meal Delivery", List.of(position("Z", FeaturedItemSelector.PositionType.DURABLE, "https://example.org/z"),
                position("A", FeaturedItemSelector.PositionType.FALLBACK, null)));
        winners("Food Pantry", List.of(position("X", FeaturedItemSelector.PositionType.TIMELY, "https://example.org/x")));
        var result = service.build("synthetic-community", "food", List.of());
        assertEquals(List.of("meal-delivery", "food-pantry"), result.featuredTopics().stream().map(t -> t.slug()).toList());
        assertEquals(List.of("Z", "A"), result.featuredTopics().get(0).items().stream().map(p -> p.item().id()).toList());
        assertEquals("FALLBACK", result.featuredTopics().get(0).items().get(1).role());
        assertEquals("/category/food/meal-delivery?communityId=synthetic-community", result.featuredTopics().get(0).items().get(1).href());
        verify(items).select("synthetic-community", "food", "Meal Delivery");
    }

    @Test void t06RoleCompleteFixtureRetainsTrueRolesAndDistinctIds() {
        selected(List.of(topic("food-pantry", "Food Pantry")));
        winners("Food Pantry", List.of(position("event", FeaturedItemSelector.PositionType.TIMELY, "https://example.org/event"),
                position("resource", FeaturedItemSelector.PositionType.DURABLE, null),
                position("flyer", FeaturedItemSelector.PositionType.VISUAL, "https://example.org/flyer")));
        var positions = service.build("synthetic-community", "food", List.of()).featuredTopics().get(0).items();
        assertEquals(List.of("TIMELY", "DURABLE", "VISUAL"), positions.stream().map(p -> p.role()).toList());
        assertEquals(3, positions.stream().map(p -> p.item().id()).distinct().count());
    }

    @Test void t15RailRejectsUnprovenLawAndInventsNoExpertOrVideoLinks() {
        selected(List.of());
        var law = new CategoryCollection("law", "collection.law", CollectionLayer.INFORMATION, 1,
                List.of(card("keyword-law", "https://example.org/law")));
        var expert = new CategoryCollection("expert-qa", "collection.expertQa", CollectionLayer.SUPPORTING, 1,
                List.of(card("expert", null)));
        var result = service.build("synthetic-community", "food", List.of(law, expert));
        assertTrue(result.laws().isEmpty()); assertTrue(result.expertLinks().isEmpty()); assertTrue(result.headlines().isEmpty());
    }

    @Test void t07ExecutableUrlCannotBecomeNavigationAndExplicitCommunityIsRequired() {
        selected(List.of(topic("food-pantry", "Food Pantry")));
        winners("Food Pantry", List.of(position("unsafe", FeaturedItemSelector.PositionType.FALLBACK, "javascript:alert(1)")));
        assertEquals("/category/food/food-pantry?communityId=synthetic-community",
                service.build("synthetic-community", "food", List.of()).featuredTopics().get(0).items().get(0).href());
        assertEquals("Explicit Community required", assertThrows(IllegalArgumentException.class,
                () -> service.build(null, "food", List.of())).getMessage());
    }
}
