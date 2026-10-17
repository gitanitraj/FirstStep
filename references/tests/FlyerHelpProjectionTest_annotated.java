/*
 * WHAT: Complete IM-09 annotated mirror of backend/src/test/java/org/firststep/backend/category/service/FlyerHelpProjectionTest.java.
 * WHY: Preserve authored placement, canonical identity, truthful dates and private/public boundaries.
 * INTERACTION: Current inventory and Article approval remain independent of exact editorial
 * Expert/FAQ admissions; Category composition intersects Community, taxonomy and membership.
 * ALTERNATIVES: Source/session/verified inference and blanket Expert admission were rejected.
 * Tests cover approved admissions, denied sessions, preserved supporting views and safe links.
 */
package org.firststep.backend.category.service;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.originals.service.ArticleService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.lifecycle.LifecycleFixture;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.service.ContentSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** IM-04: synthetic records, real lifecycle/service/projection, no catalog writes. */
class FlyerHelpProjectionTest {
    private FlyerRepository repository;
    private FlyerService flyers;
    private ResourceService resources;
    private CategoryContentService service;

    @BeforeEach
    void setUp() {
        repository = mock(FlyerRepository.class);
        when(repository.findAll()).thenReturn(List.of());
        flyers = new FlyerService(repository, mock(ContentSourceService.class), LifecycleFixture.fixed());
        resources = mock(ResourceService.class);
        when(resources.getAll()).thenReturn(List.of());
        service = new CategoryContentService(new TaxonomyService("../app/data"), resources,
                mock(NewsService.class), List::of, flyers, mock(ExpertAnswerService.class),
                mock(FaqService.class), mock(ArticleService.class), new org.firststep.backend.category.service.FirstRateNewsMembership("../app/data"));
    }

    private Flyer flyer(String id, String signal) {
        Flyer f = new Flyer();
        f.id = id;
        f.title = "Free housing assistance access event";
        f.summary = "Apply for help; hosted by Community Host, not a permanent service provider";
        f.organization = "Community Host";
        f.categoryTags = List.of("Housing");
        f.subcategory = "Eviction Prevention";
        f.communityId = "second-community";
        f.helpAccess = signal;
        f.image = "Help image.png";
        f.eventDate = "2020-01-01"; // No IM-10 occurrence cutoff is introduced.
        f.contentSource = new ContentSource();
        f.contentSource.id = "independent-producer";
        f.contentSource.url = "https://example.org/original";
        return f;
    }

    private List<ContentItem> help() {
        return service.helpFor("housing", null, "second-community");
    }

    private List<ContentItem> flyerCollection() {
        return service.collectionsFor("housing", "second-community").stream()
                .filter(c -> c.key().equals("flyers")).flatMap(c -> c.items().stream()).toList();
    }

    @Test
    void exactSemanticEnablesDualProjectionWithIdentityMediaAndProducerIntact() throws Exception {
        Flyer f = flyer("SYN-access", "ACTIONABLE_HELP_ACCESS");
        when(repository.findAll()).thenReturn(List.of(f));
        ContentItem item = help().get(0);
        assertEquals(List.of(item), flyerCollection());
        assertEquals("SYN-access", item.id());
        assertEquals(ContentType.FLYER, item.contentType());
        assertEquals("https://example.org/original", item.url());
        assertEquals("/images/seasonal/Help%20image.png", item.imageUrl());
        assertEquals("Community Host", item.organization());
        assertEquals("second-community", f.communityId);
        assertEquals("independent-producer", f.contentSource.id);
        assertSame(f, repository.findAll().get(0));
        assertEquals(1, repository.findAll().size());
        String json = new ObjectMapper().writeValueAsString(item);
        assertFalse(json.contains("helpAccess"));
        assertFalse(json.contains("expirationDate"));
        assertFalse(json.contains("PROVIDER"));
        assertFalse(new ObjectMapper().writeValueAsString(f).contains("helpAccess"));
    }

    @Test
    void absenceAndUnknownValuesCannotUseProseImageCategoryOrTopicToQualify() {
        for (String signal : new String[]{null, "", "unknown", "actionable_help_access", " ACTIONABLE_HELP_ACCESS"}) {
            when(repository.findAll()).thenReturn(List.of(flyer("SYN-ordinary", signal)));
            assertTrue(help().isEmpty(), "must reject " + signal);
            assertEquals(1, flyerCollection().size());
        }
    }

    @Test
    void expiredIsRetainedInRawCatalogAndHistoricalDetailButExcludedFromBoth() {
        Flyer f = flyer("SYN-expired", "ACTIONABLE_HELP_ACCESS");
        f.expirationDate = "2026-10-14";
        when(repository.findAll()).thenReturn(List.of(f));
        when(repository.findById(f.id)).thenReturn(Optional.of(f));
        assertTrue(help().isEmpty());
        assertTrue(flyerCollection().isEmpty());
        assertEquals(List.of(f), repository.findAll());
        assertSame(f, flyers.getById(f.id).orElseThrow());
    }

    @Test
    void approachingAndInclusiveExpiryRemainEligibleWithoutAttentionRanking() {
        Flyer near = flyer("SYN-near", "ACTIONABLE_HELP_ACCESS");
        near.expirationDate = "2026-10-15";
        near.title = "Z approaching";
        Flyer durable = flyer("SYN-active", "ACTIONABLE_HELP_ACCESS");
        durable.title = "A active";
        when(repository.findAll()).thenReturn(List.of(near, durable));
        assertEquals(List.of("SYN-active", "SYN-near"), help().stream().map(ContentItem::id).toList());
        assertEquals(2, flyerCollection().size());
    }

    @Test
    void inactiveAndWithdrawnAreExcludedIndependentlyOfExpiry() {
        for (String status : List.of("inactive", "withdrawn")) {
            Flyer f = flyer("SYN-status", "ACTIONABLE_HELP_ACCESS");
            f.status = status;
            when(repository.findAll()).thenReturn(List.of(f));
            assertTrue(help().isEmpty());
            assertTrue(flyerCollection().isEmpty());
        }
    }

    @Test
    void categoryTopicAndCommunityScopeAreRequired() {
        when(repository.findAll()).thenReturn(List.of(flyer("SYN-scope", "ACTIONABLE_HELP_ACCESS")));
        assertEquals(1, service.helpFor("housing", "Eviction Prevention", "second-community").size());
        assertTrue(service.helpFor("legal", "Eviction Prevention", "second-community").isEmpty());
        assertTrue(service.helpFor("housing", "Rental Assistance", "second-community").isEmpty());
        assertTrue(service.helpFor("housing", "Medical Care", "second-community").isEmpty());
        assertTrue(service.helpFor("housing", null, "wilmington-de").isEmpty());
        assertTrue(service.helpFor("unknown", null, "second-community").isEmpty());
    }

    @Test
    void duplicateMembershipsDoNotDuplicateIdentityWithinACollection() {
        Flyer f = flyer("SYN-once", "ACTIONABLE_HELP_ACCESS");
        when(repository.findAll()).thenReturn(List.of(f, f));
        assertEquals(1, help().size());
        assertEquals(1, flyerCollection().size());
        verify(repository, never()).findById(anyString());
    }

    @Test
    void resourceEligibilityDoesNotRequireOrUseTheNewField() {
        Resource resource = new Resource();
        resource.id = "SYN-resource";
        resource.categoryTags = List.of("Housing");
        resource.communityId = "second-community";
        resource.helpAccess = "INVALID-IGNORED-FOR-RESOURCE";
        when(resources.getAll()).thenReturn(List.of(resource));
        assertEquals(List.of("SYN-resource"), help().stream().map(ContentItem::id).toList());
    }

    @Test
    void jacksonRetainsExactSignalAndNeverPromotesMalformedStrings() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        for (String value : List.of("ACTIONABLE_HELP_ACCESS", "unknown", "actionable_help_access")) {
            Flyer f = mapper.readValue("{\"id\":\"SYN-json\",\"helpAccess\":\"" + value + "\"}", Flyer.class);
            assertEquals(value, f.helpAccess);
            f.categoryTags = List.of("Housing");
            when(repository.findAll()).thenReturn(List.of(f));
            assertEquals(value.equals("ACTIONABLE_HELP_ACCESS") ? 1 : 0,
                    service.helpFor("housing", null, null).size());
        }
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,
                () -> mapper.readValue("{\"helpAccess\":{\"value\":\"ACTIONABLE_HELP_ACCESS\"}}", Flyer.class));
    }
}
