/*
 * WHAT: Complete IM-09 annotated mirror of backend/src/test/java/org/firststep/backend/category/service/CivicCollectionCatalogTest.java.
 * WHY: Preserve authored placement, canonical identity, truthful dates and private/public boundaries.
 * INTERACTION: Current inventory and Article approval remain independent of exact editorial
 * Expert/FAQ admissions; Category composition intersects Community, taxonomy and membership.
 * ALTERNATIVES: Source/session/verified inference and blanket Expert admission were rejected.
 * Tests cover approved admissions, denied sessions, preserved supporting views and safe links.
 */
package org.firststep.backend.category.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.shared.lifecycle.EditorialLifecycle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.data.dir=../app/data", "news.rss.initial-delay=86400000"})
@AutoConfigureMockMvc
@Import(CivicCollectionCatalogTest.TimeConfiguration.class)
class CivicCollectionCatalogTest {
    @TestConfiguration static class TimeConfiguration {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-10-16T16:00:00Z"), ZoneOffset.UTC); }
    }
    @Autowired CategoryContentService content;
    @Autowired CategoryPageService pages;
    @Autowired NewsRepository rawNews;
    @Autowired FlyerRepository flyers;
    @Autowired RssFeedSource rss;
    @Autowired EditorialLifecycle lifecycle;
    @Autowired ObjectMapper mapper;
    @Autowired MockMvc mvc;

    @Test void t01t08t11t12RealFourCollectionAndInclusiveMembershipAudit() throws Exception {
        Map<String,Object> audit = new LinkedHashMap<>(); audit.put("clock", "2026-10-16T16:00:00Z");
        for (var scope : List.of(List.of("housing", "wilmington-de"), List.of("food", "wilmington-de"),
                List.of("food", "bear-de"), List.of("legal", "wilmington-de"))) {
            var collections = content.collectionsFor(scope.get(0), scope.get(1));
            var four = new LinkedHashMap<String,Object>();
            for (String key : List.of("policy-info", "law", "flyers", "first-rate-news"))
                four.put(key, collections.stream().filter(c -> c.key().equals(key)).flatMap(c -> c.items().stream()).toList());
            four.put("supporting", collections.stream().filter(c -> c.layer() == org.firststep.backend.category.dto.CollectionLayer.SUPPORTING).toList());
            four.put("destination", pages.getByKey(scope.get(0), scope.get(1)).orElseThrow().destination());
            audit.put(String.join("/", scope), four);
        }
        var housing = content.collectionsFor("housing", "wilmington-de");
        assertEquals(List.of("OR-003", "FAQ-002", "EA-001", "EA-005"), ids(housing, "first-rate-news"), "explicit record admissions retain independent identities");
        assertEquals(List.of("FAQ-002"), ids(housing, "faqs"));
        assertEquals(List.of("EA-001", "EA-005"), ids(housing, "expert-qa"));
        // C-02: FL-002 is withdrawn (poster/record mismatch), so the real Housing flyers
        // collection is empty while the record stays stored. Positive placement of an
        // eligible Housing flyer is proven by the CategoryContentServiceTest fixture.
        /*
         * C-02 ANNOTATION
         * WHAT: The real-catalog audit now expects no Housing flyer.
         * WHY: FL-002's poster is an ongoing legal-help referral, not the info session the
         *   record describes, so it is withdrawn rather than shown with false details.
         * INTERACTION: Withdrawal acts in FlyerService.getAll() via EditorialLifecycle
         *   .isPublicStatus; the repository still returns all 8 records (asserted here).
         * ALTERNATIVE: A synthetic Housing flyer injected into this real-catalog audit was
         *   rejected — it would stop the audit describing the real catalog. The positive
         *   case lives in CategoryContentServiceTest
         *   .shouldPlaceFlyersInTheirOwnCollectionRatherThanFoldingThemIntoPolicyInfo.
         */
        assertEquals(List.of(), ids(housing, "flyers")); assertEquals(8, flyers.findAll().size());
        assertTrue(flyers.findAll().stream().noneMatch(f -> "ACTIONABLE_HELP_ACCESS".equals(f.helpAccess)));
        audit.put("storedNews", rawNews.findAll().stream().map(n -> Map.of("id", n.id,
                "current", lifecycle.isCurrent(n), "lifecycle", lifecycle.assess(n).state().name())).toList());
        audit.put("rssCurrentlyAdmitted", rss.getRssItems().size());
        audit.put("rssCoverage", "Refresh delayed; local empty cache is not evidence that Delaware has no laws");
        audit.put("firstRateNewsDecision", "User-approved exact Housing admissions: EA-001, EA-005, FAQ-002; no blanket Expert/FAQ rule");
        String directory = System.getProperty("im09.evidence.dir");
        if (directory != null) {
            Files.createDirectories(Path.of(directory));
            Files.writeString(Path.of(directory, "collection-audit-" + System.getProperty("im09.audit.phase", "after") + ".json"),
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(audit) + "\n");
        }
    }

    @Test void t12t13ApprovedAdmissionsDoNotAdmitOtherSessionsOrRemoveSupportingMembership() {
        for (var row : List.of(List.of("food", "EA-002"), List.of("employment", "EA-003"),
                List.of("health", "EA-004"), List.of("utilities", "EA-006"))) {
            var collections = content.collectionsFor(row.get(0), "wilmington-de");
            assertFalse(ids(collections, "first-rate-news").contains(row.get(1)));
            assertTrue(ids(collections, "expert-qa").contains(row.get(1)));
        }
        assertEquals(List.of("OR-003"), ids(content.collectionsFor("legal", "wilmington-de"), "first-rate-news"));
        assertTrue(ids(content.collectionsFor("housing", "bear-de"), "first-rate-news").isEmpty());
    }

    @Test void t04RealExpiredNewsStoredButNotCurrentPolicyAndCommunityIsolation() throws Exception {
        var expired = rawNews.findAll().stream().filter(n -> lifecycle.assess(n).state() == EditorialLifecycle.State.EXPIRED).map(n -> n.id).toList();
        assertFalse(expired.isEmpty());
        var residentIds = content.collectionsFor("housing", "wilmington-de").stream().flatMap(c -> c.items().stream()).map(c -> c.id()).toList();
        assertTrue(expired.stream().noneMatch(residentIds::contains));
        assertTrue(content.collectionsFor("housing", "not-a-community").isEmpty());
        assertTrue(content.collectionsFor("not-a-category", "wilmington-de").isEmpty());
        mvc.perform(get("/api/category/housing/eviction-prevention").param("communityId", "wilmington-de"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test void t16PublicCategoryAndArticle404ParityProtectInternals() throws Exception {
        var category = mvc.perform(get("/api/category/housing").param("communityId", "wilmington-de"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        noPrivate(mapper.readTree(category));
        var approved = mvc.perform(get("/api/originals/OR-003")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        noPrivate(mapper.readTree(approved));
        for (String id : List.of("OR-001", "OR-002", "not-an-article"))
            mvc.perform(get("/api/originals/" + id)).andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
    private List<String> ids(List<org.firststep.backend.category.dto.CategoryCollection> rows, String key) {
        return rows.stream().filter(c -> c.key().equals(key)).flatMap(c -> c.items().stream()).map(c -> c.id()).toList();
    }
    private void noPrivate(JsonNode node) {
        if (node.isObject()) node.fields().forEachRemaining(e -> {
            assertFalse(List.of("editorialReview", "generatedBy", "reviewer", "flags", "disposition", "helpAccess", "diagnostics", "limitations", "verified", "status", "createdDate", "authoredCategoryTags").contains(e.getKey()), e.getKey());
            noPrivate(e.getValue());
        }); else if (node.isArray()) node.forEach(this::noPrivate);
    }
}
