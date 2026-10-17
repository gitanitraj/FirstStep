package org.firststep.backend.notices.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.notices.model.NoticeView;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.category.service.CategoryContentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"app.data.dir=../app/data", "news.rss.initial-delay=86400000"})
@Import(EventOccurrenceCatalogTest.TimeConfiguration.class)
class EventOccurrenceCatalogTest {
    @TestConfiguration static class TimeConfiguration {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-10-18T04:00:00Z"), ZoneOffset.UTC); }
    }
    @Autowired CommunityNoticesService notices;
    @Autowired FlyerRepository repository;
    @Autowired FlyerService flyers;
    @Autowired CategoryContentService categories;
    @Autowired ObjectMapper mapper;
    @Autowired EventOccurrence occurrence;
    @Autowired org.firststep.backend.news.repository.NewsRepository rawNews;
    @Test void t01t10t17RealFl008ConcludesAtNextLocalMidnightButRetainsIndependentFlyersAndHistory() throws Exception {
        var event = repository.findById("FL-008").orElseThrow();
        assertEquals("2026-10-17", event.eventDate);
        assertEquals("wilmington-de", event.communityId);
        assertEquals("westside-grows-together", event.contentSource.id);
        assertEquals("United Neighbors 2026.jpg", event.image);
        assertNull(event.startsAt); assertNull(event.endsAt);
        assertNull(event.expirationDate); assertNull(event.helpAccess);
        var events = notices.getPage(NoticeView.EVENTS);
        var gallery = notices.getPage(NoticeView.FLYERS);
        var audit = new LinkedHashMap<String,Object>();
        audit.put("occurrenceDiagnostics",repository.findAll().stream().map(f -> Map.of("id",f.id,"occurrence",occurrence.assess(f))).toList());
        audit.put("newsEventDiagnostics",rawNews.findAll().stream().filter(n -> n.tags != null && n.tags.contains("event")).map(n -> Map.of("id",n.id,"occurrence",occurrence.assess(n))).toList());
        audit.put("clock", "2026-10-18T04:00:00Z"); audit.put("events",events); audit.put("flyers",gallery);
        audit.put("overview", notices.getPage(NoticeView.OVERVIEW));
        audit.put("communitySupport", categories.collectionsFor("community-support", "wilmington-de"));
        String directory = System.getProperty("im10.evidence.dir");
        if(directory != null) Files.writeString(Path.of(directory,"catalog-"+System.getProperty("im10.audit.phase","after")+".json"),mapper.writerWithDefaultPrettyPrinter().writeValueAsString(audit));
        assertTrue(gallery.items().stream().anyMatch(i -> i.id().equals("FL-008") && i.imageUrl().equals("/images/seasonal/United%20Neighbors%202026.jpg")));
        assertTrue(flyers.getById("FL-008").isPresent()); assertEquals(8,repository.findAll().size());
        assertTrue(categories.collectionsFor("community-support","wilmington-de").stream().filter(c -> c.key().equals("flyers")).flatMap(c -> c.items().stream()).anyMatch(i -> i.id().equals("FL-008")));
        assertTrue(categories.helpFor("community-support",null,"wilmington-de").stream().noneMatch(i -> i.id().equals("FL-008")));
        assertTrue(events.items().stream().noneMatch(i -> i.id().equals("FL-008")), "date-only FL-008 must conclude at Wilmington midnight");
    }
}
