package org.firststep.backend.notices.service;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.flyer.controller.FlyerController;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.notices.controller.CommunityNoticesController;
import org.firststep.backend.notices.model.NoticeView;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.shared.lifecycle.EditorialExpiryConfiguration;
import org.firststep.backend.shared.lifecycle.EditorialLifecycle;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.service.ContentSourceService;
import org.firststep.backend.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EventProjectionTest {
    private final ObjectMapper mapper = new ObjectMapper();
    @org.junit.jupiter.api.io.TempDir Path directory;
    private Flyer real() throws Exception {
        JsonNode root = mapper.readTree(Path.of("../app/data/flyers.json").toFile());
        JsonNode rows = root.isArray() ? root : root.has("flyers") ? root.get("flyers") : root.get("records");
        for (JsonNode row : rows) if (row.path("id").asText().equals("FL-008")) return mapper.treeToValue(row, Flyer.class);
        throw new AssertionError("FL-008 missing");
    }
    private record Fixture(CommunityNoticesService notices, FlyerService flyers, FlyerRepository repository, EditorialLifecycle lifecycle) {}
    private Fixture fixture(List<Flyer> rows, List<NewsItem> news, String now) {
        var config = new EditorialExpiryConfiguration();
        config.setCommunityZones(Map.of("wilmington-de","America/New_York", "synthetic-west","America/Los_Angeles"));
        Clock clock = Clock.fixed(Instant.parse(now), ZoneOffset.UTC);
        var lifecycle = new EditorialLifecycle(clock,config);
        var sources = new ContentSourceService("../app/data");
        FlyerRepository repository = new FlyerRepository() {
            public List<Flyer> findAll() { return rows; }
            public Optional<Flyer> findById(String id) { return rows.stream().filter(f -> id.equals(f.id)).findFirst(); }
        };
        var flyers = new FlyerService(repository,sources,lifecycle);
        var notices = new CommunityNoticesService(flyers,new NewsService(() -> news,lifecycle),
                new TaxonomyService("../app/data"),sources,new EventOccurrence(clock,config));
        return new Fixture(notices,flyers,repository,lifecycle);
    }
    private List<String> ids(Fixture f, NoticeView view) { return f.notices.getPage(view,"wilmington-de").items().stream().map(i -> i.id()).toList(); }

    @Test void t02t03t10t15t17SyntheticScheduledEndKeepsSameIdentityMediaSourceAndHistory() throws Exception {
        Flyer event = real(); assertNull(event.startsAt); assertNull(event.endsAt);
        event.startsAt = "2026-10-17T13:00:00-04:00"; event.endsAt = "2026-10-17T16:00:00-04:00";
        var exact = fixture(List.of(event),List.of(),"2026-10-17T20:00:00Z");
        var events = exact.notices.getPage(NoticeView.EVENTS,"wilmington-de");
        var posters = exact.notices.getPage(NoticeView.FLYERS,"wilmington-de");
        assertEquals(1, events.items().size()); assertEquals(1,posters.items().size());
        assertEquals(events.items().get(0).id(),posters.items().get(0).id());
        assertEquals(ContentType.FLYER,events.items().get(0).contentType());
        assertEquals(events.items().get(0).imageUrl(),posters.items().get(0).imageUrl());
        assertEquals("/images/seasonal/United%20Neighbors%202026.jpg",events.items().get(0).imageUrl());
        assertEquals("2026-10-17T13:00:00-04:00 – 2026-10-17T16:00:00-04:00",events.items().get(0).date());
        var after = fixture(List.of(event),List.of(),"2026-10-17T20:00:00.000000001Z");
        assertTrue(ids(after,NoticeView.EVENTS).isEmpty()); assertEquals(List.of("FL-008"),ids(after,NoticeView.FLYERS));
        assertSame(event,after.repository.findById("FL-008").orElseThrow());
        assertSame(event,after.flyers.getById("FL-008").orElseThrow());
        assertEquals("westside-grows-together",event.contentSource.id); assertEquals("wilmington-de",event.communityId);
        assertNull(event.helpAccess); assertNull(event.expirationDate);
        assertEquals(1,after.repository.findAll().size());
    }
    @Test void t17RealFl008HasNoStructuredEndAndUsesItsActualCalendarBoundary() throws Exception {
        Flyer event=real(); assertNull(event.startsAt); assertNull(event.endsAt);
        var day=fixture(List.of(event),List.of(),"2026-10-18T03:59:59.999999999Z");
        assertEquals(List.of("FL-008"),ids(day,NoticeView.EVENTS));
        var next=fixture(List.of(event),List.of(),"2026-10-18T04:00:00Z");
        assertTrue(ids(next,NoticeView.EVENTS).isEmpty());
        assertEquals(List.of("FL-008"),ids(next,NoticeView.FLYERS));
        assertEquals("2026-10-17",event.eventDate); assertNull(event.endsAt);
    }

    @Test void t02OptionalTimestampFieldsBindInExistingFlyerAndNewsParsers() throws Exception {
        Flyer event=real(); event.startsAt="2026-10-17T13:00:00-04:00"; event.endsAt="2026-10-17T16:00:00-04:00";
        mapper.writeValue(directory.resolve("flyers.json").toFile(),List.of(event));
        var repository=new org.firststep.backend.flyer.repository.JsonFlyerRepository(
                org.mockito.Mockito.mock(org.firststep.backend.shared.classification.CivicContentClassifier.class));
        org.springframework.test.util.ReflectionTestUtils.setField(repository,"dataDir",directory.toString());
        repository.init();
        var loaded=repository.findById("FL-008").orElseThrow();
        assertEquals(event.startsAt,loaded.startsAt); assertEquals(event.endsAt,loaded.endsAt);
        assertEquals(ContentType.FLYER,loaded.contentType);
        var n=mapper.readValue("{\"id\":\"notice\",\"startsAt\":\"2026-10-17T13:00:00-04:00\",\"endsAt\":\"2026-10-17T16:00:00-04:00\"}",NewsItem.class);
        assertEquals(event.startsAt,n.startsAt); assertEquals(event.endsAt,n.endsAt);
    }

    @Test void t09EditorialExpiryIsIndependentOfOccurrenceAndStewardshipState() throws Exception {
        Flyer event = real(); event.endsAt = "2027-01-01T20:00:00Z";
        var f = fixture(List.of(event),List.of(),"2026-10-17T20:00:00Z");
        assertEquals(EditorialLifecycle.State.ACTIVE,f.lifecycle.assess(event).state());
        assertEquals(List.of("FL-008"),ids(f,NoticeView.EVENTS));
        event.expirationDate = "2026-10-18";
        assertEquals(EditorialLifecycle.State.APPROACHING_EXPIRY,f.lifecycle.assess(event).state());
        assertTrue(f.lifecycle.assess(event).editorialAttentionRequired()); assertEquals(List.of("FL-008"),ids(f,NoticeView.EVENTS));
        event.expirationDate = "2026-10-16";
        assertEquals(EditorialLifecycle.State.EXPIRED,f.lifecycle.assess(event).state());
        assertTrue(ids(f,NoticeView.EVENTS).isEmpty()); assertTrue(ids(f,NoticeView.FLYERS).isEmpty());
        assertTrue(f.flyers.getById("FL-008").isPresent());
        event.expirationDate = "2026-10-18"; event.endsAt = "2026-10-17T19:00:00Z";
        assertTrue(ids(f,NoticeView.EVENTS).isEmpty()); assertEquals(List.of("FL-008"),ids(f,NoticeView.FLYERS));
        assertEquals(EditorialLifecycle.State.APPROACHING_EXPIRY,f.lifecycle.assess(event).state());
        event.status="withdrawn"; assertTrue(f.flyers.getById("FL-008").isEmpty());
    }
    @Test void t11t16CommunityProducerAndControlledKindAreIndependentGates() throws Exception {
        Flyer event=real(); var f=fixture(List.of(event),List.of(),"2026-10-17T20:00:00Z");
        assertEquals(1,ids(f,NoticeView.EVENTS).size());
        assertTrue(f.notices.getPage(NoticeView.EVENTS,"synthetic-west").items().isEmpty());
        event.communityId="synthetic-west";
        assertTrue(ids(f,NoticeView.EVENTS).isEmpty()); assertEquals(1,f.notices.getPage(NoticeView.EVENTS,"synthetic-west").items().size());
        event.communityId="wilmington-de"; event.contentSource.id="wilmington-housing-authority";
        assertTrue(ids(f,NoticeView.EVENTS).isEmpty()); assertTrue(ids(f,NoticeView.FLYERS).isEmpty());
        event.contentSource.id="unknown-producer"; assertTrue(ids(f,NoticeView.EVENTS).isEmpty());
        event.contentSource.id="westside-grows-together"; event.tags=List.of("festival");
        assertTrue(ids(f,NoticeView.EVENTS).isEmpty()); assertEquals(1,ids(f,NoticeView.FLYERS).size());
        event.tags=List.of("event","meeting"); assertTrue(ids(f,NoticeView.EVENTS).isEmpty());
        event.tags=List.of("event"); event.contentType=ContentType.RESOURCE; assertTrue(ids(f,NoticeView.EVENTS).isEmpty());
    }
    @Test void t08t13UnknownOccurrenceDoesNotMassFilterOtherViews() throws Exception {
        Flyer event=real(); event.eventDate=null;
        var f=fixture(List.of(event),List.of(),"2026-10-17T20:00:00Z");
        assertTrue(ids(f,NoticeView.EVENTS).isEmpty()); assertEquals(1,ids(f,NoticeView.FLYERS).size());
        event.tags=List.of("meeting"); assertEquals(1,ids(f,NoticeView.MEETINGS).size());
        event.tags=List.of("announcement"); assertEquals(1,ids(f,NoticeView.ANNOUNCEMENTS).size());
        event.tags=List.of("event"); event.eventDate="2026-02-30";
        assertTrue(ids(f,NoticeView.EVENTS).isEmpty()); assertEquals(1,ids(f,NoticeView.FLYERS).size());
    }
    @Test void t14CountsPreviewsDedupAndInstantOrderingAgree() throws Exception {
        Flyer event=real(); event.eventDate=null; event.startsAt="2026-10-18T09:00:00+02:00";
        Flyer second=real(); second.id="EARLIER"; second.eventDate=null; second.startsAt="2026-10-18T06:00:00Z";
        var f=fixture(List.of(event,second,event),List.of(),"2026-10-17T20:00:00Z");
        assertEquals(List.of("EARLIER","FL-008"),ids(f,NoticeView.EVENTS));
        for (NoticeView view:NoticeView.values()) {
            var page=f.notices.getPage(view,"wilmington-de"); assertEquals(2,page.counts().get(NoticeView.EVENTS));
            if(view==NoticeView.OVERVIEW) assertEquals(ids(f,NoticeView.EVENTS),page.previews().stream().filter(p->p.view()==NoticeView.EVENTS).findFirst().orElseThrow().items().stream().map(i->i.id()).toList());
        }
        second.startsAt="2026-10-18T07:00:00Z";
        assertEquals(List.of("EARLIER","FL-008"),ids(f,NoticeView.EVENTS));
    }
    @Test void t08NewsPublicationCannotBecomeEventOccurrence() throws Exception {
        NewsItem n=new NewsItem(); n.id="notice"; n.title="Next year"; n.communityId="wilmington-de";
        n.contentSource=real().contentSource; n.tags=List.of("event"); n.publishDate="2027-01-01";
        var f=fixture(List.of(),List.of(n),"2026-10-17T20:00:00Z"); assertTrue(ids(f,NoticeView.EVENTS).isEmpty());
        n.startsAt="2027-01-02T13:00:00-05:00"; assertEquals(List.of("notice"),ids(f,NoticeView.EVENTS));
        assertEquals(n.startsAt,f.notices.getPage(NoticeView.EVENTS).items().get(0).date());
    }
    @Test void t12OnlyFiveCanonicalContentTypesAndNoEventConversion() {
        assertEquals(List.of("RESOURCE","NEWS","FLYER","LAW","EXPERT"),java.util.Arrays.stream(ContentType.values()).map(Enum::name).toList());
    }
    @Test void t15UnsafeSourceIsAbsentWhileOriginalMediaAndSafeSourceSurvive() throws Exception {
        Flyer event=real(); var f=fixture(List.of(event),List.of(),"2026-10-17T20:00:00Z");
        event.contentSource.url="javascript:alert(1)";
        assertNull(org.firststep.backend.flyer.dto.FlyerSummary.from(event,f.flyers).contentSource().url());
        event.contentSource.url="https://example.org/original-event";
        var resident=org.firststep.backend.flyer.dto.FlyerSummary.from(event,f.flyers);
        assertEquals(event.contentSource.url,resident.contentSource().url());
        assertEquals("/images/seasonal/United%20Neighbors%202026.jpg",resident.imageUrl());
    }

    @Test void t18PublicHttpFactsKeepHistoricalFlyerButOmitPrivateFields() throws Exception {
        Flyer event=real(); event.endsAt="2026-10-17T19:00:00Z"; event.status="active";
        var f=fixture(List.of(event),List.of(),"2026-10-17T20:00:00Z");
        var mvc=MockMvcBuilders.standaloneSetup(new CommunityNoticesController(f.notices),new FlyerController(f.flyers))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/community-notices/events").param("communityId","wilmington-de")).andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(0)).andExpect(jsonPath("$.data.counts.EVENTS").value(0));
        mvc.perform(get("/api/community-notices/flyers").param("communityId","synthetic-west")).andExpect(jsonPath("$.data.items.length()").value(0));
        for(String path:List.of("/api/flyers","/api/flyers/FL-008","/api/community-notices/flyers","/api/community-notices")) {
            var response=mvc.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertPrivateAbsent(mapper.readTree(response));
        }
        mvc.perform(get("/api/flyers/FL-008")).andExpect(jsonPath("$.data.endsAt").value(event.endsAt)).andExpect(jsonPath("$.data.contentSource.id").value("westside-grows-together")).andExpect(jsonPath("$.data.imageUrl").value("/images/seasonal/United%20Neighbors%202026.jpg"));
    }
    private void assertPrivateAbsent(JsonNode node) {
        if(node.isObject()) node.fields().forEachRemaining(e->{
            assertFalse(List.of("diagnostic","diagnostics","occurrenceAssessment","status","helpAccess","verified","createdDate","updatedDate","expirationDate","editorialReview","retrieved").contains(e.getKey()),e.getKey());
            assertPrivateAbsent(e.getValue());
        }); else if(node.isArray()) node.forEach(this::assertPrivateAbsent);
    }
}
