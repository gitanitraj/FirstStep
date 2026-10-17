/*
 * WHAT: Complete IM-11 annotated mirror of backend/src/test/java/org/firststep/backend/category/selection/Im11RealCatalogAuditTest.java.
 * WHY: Lock actual catalog collisions and preserve deterministic selection under explicit evidence.
 * INTERACTION: Existing selectors consume unchanged current authored inventory and controlled projections.
 * The sole production correction rejects unsafe URLs as positive VISUAL source evidence; matching,
 * comparator order, role windows, fallback, occurrence and diagnostics remain independent.
 * ALTERNATIVES: Re-ranking, fabricated catalog dates, Flyer activation and a new model were rejected.
 */
package org.firststep.backend.category.selection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.category.service.CategoryContentService;
import org.firststep.backend.category.service.CategoryPageService;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.expert.repository.ExpertAnswerRepository;
import org.firststep.backend.expert.repository.FaqRepository;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.originals.model.Article;
import org.firststep.backend.originals.repository.ArticleRepository;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.shared.lifecycle.EditorialLifecycle;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.notices.service.CommunityNoticesService;
import org.firststep.backend.notices.service.EventOccurrence;
import org.firststep.backend.notices.model.NoticeView;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Actual repository facts and actual selectors; the audit never writes to a catalog. */
@SpringBootTest(properties={"app.data.dir=../app/data","news.rss.initial-delay=86400000"})
@AutoConfigureMockMvc
@Import(Im11RealCatalogAuditTest.TimeConfiguration.class)
class Im11RealCatalogAuditTest {
    @TestConfiguration static class TimeConfiguration {
        @Bean @Primary Clock auditClock() {
            String value=System.getProperty("im11.clock","2026-10-16T16:00:00Z");
            return Clock.fixed("runtime".equals(value)?Instant.now():Instant.parse(value),ZoneOffset.UTC);
        }
    }
    @Autowired Clock clock;
    @Autowired ObjectMapper mapper;
    @Autowired MockMvc mvc;
    @Autowired FeaturedTopicSelector topics;
    @Autowired FeaturedItemSelector items;
    @Autowired FeaturedTopicInventory inventory;
    @Autowired AuthoredTopicEvidence authored;
    @Autowired TaxonomyService taxonomy;
    @Autowired CategoryContentService content;
    @Autowired CategoryPageService pages;
    @Autowired EditorialLifecycle lifecycle;
    @Autowired ResourceRepository resources;
    @Autowired FlyerRepository flyers;
    @Autowired NewsRepository news;
    @Autowired ArticleRepository articles;
    @Autowired FaqRepository faqs;
    @Autowired ExpertAnswerRepository experts;
    @Autowired RssFeedSource rss;
    @Autowired CommunityNoticesService notices;
    @Autowired EventOccurrence occurrence;

    private List<CivicContent> raw() {
        List<CivicContent> rows=new ArrayList<>();rows.addAll(resources.findAll());rows.addAll(flyers.findAll());
        rows.addAll(news.findAll());rows.addAll(articles.findAll());rows.addAll(faqs.findAll());rows.addAll(experts.findAll());
        return rows;
    }
    private List<String> origins(String id) throws Exception {
        List<String> files=new ArrayList<>();
        for(String name:List.of("resources.json","resources.communities.json","flyers.json","news.json","articles.json","faq.json","expert-answers.json")) {
            JsonNode root=mapper.readTree(Path.of("../app/data",name).toFile());
            JsonNode rows=root.isArray()?root:null;
            if(rows==null)for(String key:List.of("records","resources","flyers","news","articles","faqs","expertAnswers"))if(root.path(key).isArray()){rows=root.path(key);break;}
            if(rows==null) rows=mapper.createArrayNode().add(root);
            for(JsonNode row:rows)if(id.equals(row.path("id").asText()))files.add("app/data/"+name);
        }
        return files;
    }
    @Test void im11T02T04T05T17ReadOnlyAllHousingCandidatesFixedOrRuntimeAudit() throws Exception {
        String before=mapper.writeValueAsString(raw());
        var definition=taxonomy.findByKey("housing").orElseThrow();
        var topicSelection=topics.select("wilmington-de","housing");
        Map<String,FeaturedItemSelector.Selection> itemSelections=new LinkedHashMap<>();
        for(String topic:definition.subcategories())itemSelections.put(topic,items.select("wilmington-de","housing",topic));
        Set<String> admitted=new TreeSet<>();itemSelections.values().forEach(s->s.candidates().forEach(c->admitted.add(c.id())));
        Set<String> helpIds=new TreeSet<>(content.helpFor("housing",null,"wilmington-de").stream().map(c->c.id()).toList());
        List<Map<String,Object>> rows=new ArrayList<>();
        LocalDate today=LocalDate.now(clock.withZone(ZoneId.of("America/New_York")));
        for(CivicContent item:raw())if(taxonomy.matchesCategoryTags(definition,item.categoryTags)) {
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("id",item.id);row.put("concreteKind",item.getClass().getSimpleName());row.put("contentType",item.contentType);
            row.put("originFiles",origins(item.id));row.put("communityId",item.communityId);row.put("source",item.contentSource);
            row.put("categoryTags",item.categoryTags);row.put("topic",item.subcategory);row.put("lifecycle",lifecycle.assess(item));
            row.put("publicStatus",lifecycle.isPublicStatus(item));row.put("authoredTopicProof",authored.permits(item,definition));
            row.put("scopedActionable",helpIds.contains(item.id));row.put("featuredItemAdmitted",admitted.contains(item.id));
            row.put("topicProxyFactsNotItemAdmission",FeaturedRoleQualification.assess(item,today));
            row.put("rawCreatedDateNotNecessarilyCatalogNewness",item.createdDate);row.put("publishDate",item.publishDate);
            row.put("articleApproved",item instanceof Article a?a.isPublishable():null);
            row.put("helpAccess",item.helpAccess);row.put("eventDate",item instanceof Flyer f?f.eventDate:null);
            row.put("startsAt",item instanceof Flyer f?f.startsAt:null);row.put("endsAt",item instanceof Flyer f?f.endsAt:null);
            List<String> excluded=new ArrayList<>();
            if(!"wilmington-de".equals(item.communityId))excluded.add("different_community");
            if(!lifecycle.isCurrent(item))excluded.add("not_current_editorial_inventory");
            if(item instanceof Article a&&!a.isPublishable())excluded.add("article_not_approved");
            if(!authored.permits(item,definition))excluded.add("no_matching_authored_topic_proof");
            if(item instanceof Flyer&&!"ACTIONABLE_HELP_ACCESS".equals(item.helpAccess))excluded.add("flyer_help_access_required");
            row.put("itemAdmissionExclusions",excluded);rows.add(row);
        }
        Map<String,Object> audit=new LinkedHashMap<>();
        audit.put("clock",clock.instant().toString());audit.put("clockMode",System.getProperty("im11.clock","fixed"));
        audit.put("communityZone","America/New_York");audit.put("storedResources",resources.findAll().size());
        audit.put("storedFlyers",flyers.findAll().size());audit.put("housingHelpIds",helpIds);
        audit.put("topics",topicSelection);audit.put("itemsByTopic",itemSelections);audit.put("storedHousingRecords",rows);
        audit.put("currentAuthoredHousingInventory",inventory.current("wilmington-de",definition));
        audit.put("category",pages.getByKey("housing","wilmington-de").orElseThrow());
        audit.put("foodWilmington",topics.select("wilmington-de","food"));audit.put("foodBear",topics.select("bear-de","food"));
        audit.put("housingBear",topics.select("bear-de","housing"));
        audit.put("foodBearItems",items.select("bear-de","food","Food Pantry"));
        audit.put("communityEvents",notices.getPage(NoticeView.EVENTS,"wilmington-de"));
        audit.put("communityFlyers",notices.getPage(NoticeView.FLYERS,"wilmington-de"));
        audit.put("fl008Occurrence",occurrence.assess(flyers.findById("FL-008").orElseThrow()));
        audit.put("rssAdmittedItems",rss.getRssItems().size());audit.put("coverageLimitation","Local catalog and admitted cache only; no remote RSS completeness or source reachability claim");
        String directory=System.getProperty("im11.evidence.dir");
        if(directory!=null)Files.writeString(Path.of(directory,"real-catalog-"+System.getProperty("im11.audit.phase","after")+".json"),mapper.writerWithDefaultPrettyPrinter().writeValueAsString(audit)+"\n");
        assertEquals(before,mapper.writeValueAsString(raw()));
        assertEquals(9,topicSelection.candidates().size());
        assertEquals(List.of("transitional-housing","emergency-shelter","homeownership","rental-assistance"),topicSelection.selected().stream().map(t->t.key()).toList());
        assertEquals(36,helpIds.size());assertEquals(8,flyers.findAll().size());assertTrue(flyers.findAll().stream().allMatch(f->f.helpAccess==null));
        assertTrue(itemSelections.values().stream().flatMap(s->s.candidates().stream()).noneMatch(c->c.roles().contains(FeaturedRoleQualification.Role.VISUAL)));
        assertTrue(content.helpFor("housing","Eviction Prevention","wilmington-de").isEmpty());
        assertEquals(List.of("OR-003","FAQ-002","EA-001","EA-005"),content.collectionsFor("housing","wilmington-de").stream().filter(c->c.key().equals("first-rate-news")).flatMap(c->c.items().stream()).map(c->c.id()).toList());
        assertFalse(topics.select("bear-de","food").selected().isEmpty());
    }
    @Test void im11T06PersistedRealCollisionCandidatesMatchTheThenCurrentCatalog() throws Exception {
        try(var input=getClass().getResourceAsStream("/im11/real-housing-collisions.json")) {
            assertNotNull(input);var fixture=mapper.readTree(input);
            for(String topic:List.of("Transitional Housing","Emergency Shelter","Homeownership","Rental Assistance")) {
                var recorded=mapper.treeToValue(fixture.path("itemsByTopic").path(topic),FeaturedItemSelector.Selection.class);
                assertEquals(recorded.candidates(),items.select("wilmington-de","housing",topic).candidates(),topic);
            }
        }
    }

    @Test void im11T14ActualMockMvcCategoryPreservesExactSelectorOrderAndRolesWithoutPrivateEvidence() throws Exception {
        var response=mvc.perform(get("/api/category/housing").param("communityId","wilmington-de")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var page=mapper.readTree(response).path("data");
        var selected=topics.select("wilmington-de","housing").selected();
        assertEquals(4,page.path("destination").path("featuredTopics").size());
        for(int i=0;i<selected.size();i++) {
            var column=page.path("destination").path("featuredTopics").get(i);
            assertEquals(selected.get(i).key(),column.path("slug").asText());
            var positions=items.select("wilmington-de","housing",selected.get(i).name()).positions();
            for(int j=0;j<positions.size();j++) {
                var card=column.path("items").get(j);assertEquals(positions.get(j).item().id(),card.path("item").path("id").asText());
                assertEquals(positions.get(j).type().name(),card.path("role").asText());
                assertFalse(card.path("href").asText().isBlank());
            }
        }
        for(String field:List.of("diagnostics","tieStage","limitations","helpAccess","editorialReview","provenance","catalogOrApprovalDate"))assertFalse(response.contains("\""+field+"\""),field);
    }
}
