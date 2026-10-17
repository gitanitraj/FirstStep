/*
 * WHAT: Complete IM-11 annotated mirror of backend/src/test/java/org/firststep/backend/category/selection/Im11InteractionRegressionTest.java.
 * WHY: Lock actual catalog collisions and preserve deterministic selection under explicit evidence.
 * INTERACTION: Existing selectors consume unchanged current authored inventory and controlled projections.
 * The sole production correction rejects unsafe URLs as positive VISUAL source evidence; matching,
 * comparator order, role windows, fallback, occurrence and diagnostics remain independent.
 * ALTERNATIVES: Re-ranking, fabricated catalog dates, Flyer activation and a new model were rejected.
 */
package org.firststep.backend.category.selection;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.firststep.backend.category.service.CategoryContentService;
import org.firststep.backend.category.service.FirstRateNewsMembership;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.notices.model.NoticeView;
import org.firststep.backend.notices.service.CommunityNoticesService;
import org.firststep.backend.notices.service.EventOccurrence;
import org.firststep.backend.originals.service.ArticleService;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.lifecycle.EditorialExpiryConfiguration;
import org.firststep.backend.shared.lifecycle.EditorialLifecycle;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.service.ContentSourceService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Synthetic positives are explicitly approved test facts; no production Flyer is activated. */
class Im11InteractionRegressionTest {
    private final List<Flyer> flyers=new ArrayList<>();
    private record Fixture(FeaturedItemSelector items,FeaturedTopicSelector topics,CategoryContentService content,CommunityNoticesService notices,EventOccurrence occurrence) {}
    private Flyer flyer(String id) {
        Flyer f=new Flyer();f.id=id;f.title=id;f.communityId="wilmington-de";f.categoryTags=List.of("Housing");f.subcategory="Emergency Shelter";
        f.tags=List.of("event");f.helpAccess="ACTIONABLE_HELP_ACCESS";f.eventDate="2026-10-17";f.image="original poster.png";
        f.contentSource=new ContentSource();f.contentSource.id="ministry-of-caring";f.contentSource.url="https://example.org/original";
        flyers.add(f);return f;
    }
    private Fixture at(String now) {
        Clock clock=Clock.fixed(Instant.parse(now),ZoneOffset.UTC);
        var dates=new EditorialExpiryConfiguration();dates.setCommunityZones(Map.of("wilmington-de","America/New_York","synthetic-tokyo","Asia/Tokyo"));
        var lifecycle=new EditorialLifecycle(clock,dates);var sources=new ContentSourceService("../app/data");
        FlyerRepository repository=new FlyerRepository(){
            public List<Flyer> findAll(){return flyers;}
            public Optional<Flyer> findById(String id){return flyers.stream().filter(f->id.equals(f.id)).findFirst();}
        };
        var flyerService=new FlyerService(repository,sources,lifecycle);
        var news=new NewsService(List::of,lifecycle);var resources=mock(ResourceService.class);
        var experts=mock(ExpertAnswerService.class);var faqs=mock(FaqService.class);var articles=mock(ArticleService.class);
        var taxonomy=new TaxonomyService("../app/data");
        var content=new CategoryContentService(taxonomy,resources,news,List::of,flyerService,experts,faqs,articles,new FirstRateNewsMembership("../app/data"));
        var proof=mock(AuthoredTopicEvidence.class);when(proof.loadLimitations()).thenReturn(List.of());
        when(proof.permits(any(),any())).thenAnswer(call->{Flyer f=call.getArgument(0);return f.categoryTags.equals(List.of("Housing"))&&"Emergency Shelter".equals(f.subcategory);});
        var inventory=new FeaturedTopicInventory(resources,flyerService,news,List::of,articles,faqs,experts,lifecycle,taxonomy,proof);
        var occurrence=new EventOccurrence(clock,dates);
        return new Fixture(new FeaturedItemSelector(taxonomy,content,inventory,clock,dates),new FeaturedTopicSelector(taxonomy,content,inventory,clock,dates),content,
                new CommunityNoticesService(flyerService,news,taxonomy,sources,occurrence),occurrence);
    }
    private FeaturedItemSelector.Candidate candidate(Fixture f,String id) {return f.items.select("wilmington-de","housing","Emergency Shelter").candidates().stream().filter(c->c.id().equals(id)).findFirst().orElseThrow();}

    @Test void im11T12UnsafeSourceCannotSupplyPositiveVisualEvidence() {
        Flyer f=flyer("synthetic-visual");var fixture=at("2026-10-17T20:00:00Z");
        for(String url:List.of("javascript:alert(1)","data:text/html,unsafe","//example.org/original","not a URI","https:missing-host")) {
            f.contentSource.url=url;var candidate=candidate(fixture,f.id);
            assertTrue(candidate.actionable());assertTrue(candidate.roles().contains(FeaturedRoleQualification.Role.TIMELY));
            assertFalse(candidate.roles().contains(FeaturedRoleQualification.Role.VISUAL),"unsafe original source must not confer VISUAL: "+url);
            assertTrue(candidate.limitations().contains("no_positive_visual_media_source_evidence:"+f.id));
        }
        for(String url:List.of("https://example.org/original","http://example.org/original")) {
            f.contentSource.url=url;assertTrue(candidate(fixture,f.id).roles().contains(FeaturedRoleQualification.Role.VISUAL));
        }
    }
    @Test void im11T15ExplicitEndConclusionDoesNotGloballyDeleteHelpOrOverrideIndependentDateTimelyRule() {
        Flyer f=flyer("synthetic-ended");f.startsAt="2026-10-17T13:00:00-04:00";f.endsAt="2026-10-17T16:00:00-04:00";
        var exact=at("2026-10-17T20:00:00Z");assertEquals(1,exact.notices.getPage(NoticeView.EVENTS,"wilmington-de").items().size());
        var after=at("2026-10-17T20:00:00.000000001Z");assertEquals(EventOccurrence.State.CONCLUDED,after.occurrence.assess(f).state());
        assertTrue(after.notices.getPage(NoticeView.EVENTS,"wilmington-de").items().isEmpty());
        assertEquals(1,after.notices.getPage(NoticeView.FLYERS,"wilmington-de").items().size());
        assertEquals(List.of(f.id),after.content.helpFor("housing","Emergency Shelter","wilmington-de").stream().map(c->c.id()).toList());
        assertEquals(java.util.Set.of(FeaturedRoleQualification.Role.TIMELY,FeaturedRoleQualification.Role.VISUAL),candidate(after,f.id).roles());
        assertEquals(1,after.topics.select("wilmington-de","housing").selected().get(0).actionableCount());
        var tomorrow=at("2026-10-18T04:00:00Z");assertFalse(candidate(tomorrow,f.id).roles().contains(FeaturedRoleQualification.Role.TIMELY));
        assertTrue(candidate(tomorrow,f.id).roles().contains(FeaturedRoleQualification.Role.VISUAL));
        f.helpAccess=null;assertTrue(tomorrow.items.select("wilmington-de","housing","Emergency Shelter").positions().isEmpty());
        assertEquals(1,tomorrow.notices.getPage(NoticeView.FLYERS,"wilmington-de").items().size());
    }
    @Test void im11T09T16IndependentCommunityDaysControlDayThirtyWithoutChangingHelpOrOccurrence() {
        Flyer ny=flyer("synthetic-ny");ny.eventDate="2026-11-17";
        Flyer tokyo=flyer("synthetic-tokyo");tokyo.communityId="synthetic-tokyo";tokyo.eventDate=ny.eventDate;
        var fixture=at("2026-10-18T02:00:00Z");
        assertFalse(candidate(fixture,ny.id).roles().contains(FeaturedRoleQualification.Role.TIMELY));
        var other=fixture.items.select("synthetic-tokyo","housing","Emergency Shelter");
        assertEquals("2026-10-18",other.selectionDate().toString());assertEquals(1,other.candidates().size());
        assertEquals(tokyo.id,other.candidates().get(0).id());assertTrue(other.candidates().get(0).roles().contains(FeaturedRoleQualification.Role.TIMELY));
        assertEquals(1,fixture.content.helpFor("housing","Emergency Shelter","wilmington-de").size());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "2026-03-08T05:00:00Z,2026-03-08,2026-04-07,2026-04-08",
        "2026-03-09T03:59:59Z,2026-03-08,2026-04-07,2026-04-08",
        "2026-11-01T04:00:00Z,2026-11-01,2026-12-01,2026-12-02",
        "2026-11-02T04:59:59Z,2026-11-01,2026-12-01,2026-12-02"})
    void im11T09T16SelectorWindowUsesCalendarDaysAcrossBothDstTransitions(String now,String localDay,String dayThirty,String dayThirtyOne) {
        Flyer f=flyer("synthetic-dst");var fixture=at(now);
        f.eventDate=dayThirty;
        assertEquals(localDay,fixture.items.select("wilmington-de","housing","Emergency Shelter").selectionDate().toString());
        assertTrue(candidate(fixture,f.id).roles().contains(FeaturedRoleQualification.Role.TIMELY));
        f.eventDate=dayThirtyOne;assertFalse(candidate(fixture,f.id).roles().contains(FeaturedRoleQualification.Role.TIMELY));
        f.eventDate=localDay;assertTrue(candidate(fixture,f.id).roles().contains(FeaturedRoleQualification.Role.TIMELY));
        f.eventDate=java.time.LocalDate.parse(localDay).minusDays(1).toString();
        assertFalse(candidate(fixture,f.id).roles().contains(FeaturedRoleQualification.Role.TIMELY));
    }

    @Test void im11T18UnknownOccurrenceAndMissingVisualFactsProduceDeterministicSeparateDiagnostics() {
        Flyer f=flyer("synthetic-unknown");f.eventDate=null;f.contentSource=null;
        var fixture=at("2026-10-17T20:00:00Z");
        assertEquals(new EventOccurrence.Assessment(EventOccurrence.State.UNKNOWN,"missing_occurrence"),fixture.occurrence.assess(f));
        var candidate=candidate(fixture,f.id);assertTrue(candidate.roles().isEmpty());
        assertTrue(candidate.limitations().contains("missing_or_invalid_event_date:"+f.id));
        assertTrue(candidate.limitations().contains("no_positive_visual_media_source_evidence:"+f.id));
        assertEquals(FeaturedItemSelector.PositionType.FALLBACK,fixture.items.select("wilmington-de","housing","Emergency Shelter").positions().get(0).type());
        assertTrue(fixture.notices.getPage(NoticeView.EVENTS,"wilmington-de").items().isEmpty());
    }
}
