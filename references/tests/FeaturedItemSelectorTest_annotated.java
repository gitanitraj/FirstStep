/*
 * WHAT: Complete annotated mirror of FeaturedItemSelectorTest.java.
 * WHY: IM-07 selects distinct item roles without changing resident eligibility or public DTOs.
 * INTERACTION: Current authored inventory intersects existing projections before role matching.
 * ALTERNATIVES: Greedy slots lose role coverage; weighted scores obscure approved tie stages.
 * TEST STRATEGY: Service fixtures exercise gates; exhaustive oracle checks assignments;
 * real catalog integration pins actual collisions. Production remains internal, read-only.
 */
package org.firststep.backend.category.selection;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.firststep.backend.category.service.CategoryContentService;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.repository.ExpertAnswerRepository;
import org.firststep.backend.expert.repository.FaqRepository;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.originals.model.Article;
import org.firststep.backend.originals.model.EditorialReview;
import org.firststep.backend.originals.repository.ArticleRepository;
import org.firststep.backend.originals.service.ArticleService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.lifecycle.EditorialExpiryConfiguration;
import org.firststep.backend.shared.lifecycle.EditorialLifecycle;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.service.ContentSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real services/lifecycle/taxonomy/helpFor/adapter/selector, synthetic raw repository boundaries only. */
class FeaturedItemSelectorTest {
    private static final org.firststep.backend.category.selection.FeaturedRoleQualification.Role TIMELY = org.firststep.backend.category.selection.FeaturedRoleQualification.Role.TIMELY;
    private static final org.firststep.backend.category.selection.FeaturedRoleQualification.Role DURABLE = org.firststep.backend.category.selection.FeaturedRoleQualification.Role.DURABLE;
    private static final org.firststep.backend.category.selection.FeaturedRoleQualification.Role VISUAL = org.firststep.backend.category.selection.FeaturedRoleQualification.Role.VISUAL;

    private final List<Resource> resources = new ArrayList<>();
    private final List<Flyer> flyers = new ArrayList<>();
    private final List<NewsItem> news = new ArrayList<>();
    private final List<NewsItem> rss = new ArrayList<>();
    private final List<Article> articles = new ArrayList<>();
    private final List<FAQ> faqs = new ArrayList<>();
    private final List<ExpertAnswer> experts = new ArrayList<>();
    private FeaturedItemSelector selector;
    private Instant instant = Instant.parse("2026-10-16T23:30:00Z");
    private final java.util.Map<String, String> authoredTopics = new java.util.HashMap<>();
    private final String community = "wilmington-de";

    @BeforeEach void setUp() {
        var dates = new EditorialExpiryConfiguration();
        dates.setCommunityZones(Map.of("second-community", "Asia/Tokyo", community, "America/New_York"));
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
        var lifecycle = new EditorialLifecycle(clock, dates);
        var resourceRepo = mock(ResourceRepository.class);
        var flyerRepo = mock(FlyerRepository.class);
        var newsRepo = mock(NewsRepository.class);
        var articleRepo = mock(ArticleRepository.class);
        var faqRepo = mock(FaqRepository.class);
        var expertRepo = mock(ExpertAnswerRepository.class);
        when(resourceRepo.findAll()).thenAnswer(call -> resources);
        when(flyerRepo.findAll()).thenAnswer(call -> flyers);
        when(newsRepo.findAll()).thenAnswer(call -> news);
        when(articleRepo.findAll()).thenAnswer(call -> articles);
        when(faqRepo.findAll()).thenAnswer(call -> faqs);
        when(expertRepo.findAll()).thenAnswer(call -> experts);
        var resourceService = new ResourceService(resourceRepo, lifecycle);
        var flyerService = new FlyerService(flyerRepo, mock(ContentSourceService.class), lifecycle);
        var newsService = new NewsService(newsRepo, lifecycle);
        var articleService = new ArticleService(articleRepo, lifecycle);
        var faqService = new FaqService(faqRepo, lifecycle);
        var expertService = new ExpertAnswerService(expertRepo, lifecycle);
        RssFeedSource rssSource = () -> rss;
        var taxonomy = new TaxonomyService("../app/data");
        var content = new CategoryContentService(taxonomy, resourceService, newsService, rssSource,
                flyerService, expertService, faqService, articleService);
        AuthoredTopicEvidence authored = mock(AuthoredTopicEvidence.class);
        when(authored.loadLimitations()).thenReturn(List.of());
        when(authored.permits(any(), any())).thenAnswer(call -> {
            org.firststep.backend.shared.model.CivicContent item = call.getArgument(0);
            return item.subcategory != null && item.subcategory.equals(authoredTopics.get(item.id));
        });
        var inventory = new FeaturedTopicInventory(resourceService, flyerService, newsService, rssSource,
                articleService, faqService, expertService, lifecycle, taxonomy, authored);
        selector = new FeaturedItemSelector(taxonomy, content, inventory, clock, dates);
    }

    private Resource resource(String id, String topic) {
        Resource r = new Resource(); r.id = id; r.title = id; r.urgency = "standard";
        r.communityId = community; r.categoryTags = List.of("Housing"); r.subcategory = topic;
        authoredTopics.put(id, topic); resources.add(r); return r;
    }
    private Flyer flyer(String id, String topic) {
        Flyer f = new Flyer(); f.id = id; f.title = id; f.helpAccess = "ACTIONABLE_HELP_ACCESS";
        f.communityId = community; f.categoryTags = List.of("Housing"); f.subcategory = topic;
        f.contentSource = new org.firststep.backend.shared.model.ContentSource();
        f.contentSource.id = "explicit-source"; f.contentSource.url = "https://example.org/original";
        f.image = "original image.png"; f.eventDate = "2026-10-17";
        authoredTopics.put(id, topic); flyers.add(f); return f;
    }
    private NewsItem law(String id, String topic) {
        NewsItem n = new NewsItem(); n.id = id; n.contentType = ContentType.LAW;
        n.communityId = community; n.categoryTags = List.of("Housing"); n.subcategory = topic;
        n.publishDate = "2026-10-16"; authoredTopics.put(id, topic); news.add(n); return n;
    }
    private Article article(String id, String topic, String date) {
        Article a = new Article(); a.id = id; a.communityId = community;
        a.categoryTags = List.of("Housing"); a.subcategory = topic; a.createdDate = date;
        a.editorialReview = new EditorialReview(); a.editorialReview.status = "approved";
        a.editorialReview.reviewedDate = date; authoredTopics.put(id, topic); articles.add(a); return a;
    }
    private FeaturedItemSelector.Selection select() { return selector.select(community, "housing", "Emergency Shelter"); }
    private List<String> ids() { return select().positions().stream().map(p -> p.item().id()).toList(); }
    private FeaturedItemSelector.Candidate candidate(String id) {
        return select().candidates().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }
    private boolean diagnostic(String code) { return select().diagnostics().stream().anyMatch(d -> d.code().equals(code)); }

    @Test void t01ControlledScopeDistinctImmutableResults() {
        for (int i = 0; i < 10; i++) resource("R" + i, "Emergency Shelter");
        assertEquals(3, ids().size()); assertEquals(3, ids().stream().distinct().count());
        assertEquals(select(), selector.select(community, "housing", "emergency-shelter"));
        assertEquals("INVALID_SCOPE", selector.select(community, "housing", "Food Pantry").diagnostics().get(0).code());
        assertEquals("INVALID_SCOPE", selector.select(community, "unknown", "Emergency Shelter").diagnostics().get(0).code());
        assertEquals("Explicit Community required", assertThrows(IllegalArgumentException.class,
                () -> selector.select(null, "housing", "Emergency Shelter")).getMessage());
        assertThrows(UnsupportedOperationException.class, () -> select().positions().clear());
        assertThrows(UnsupportedOperationException.class, () -> candidate("R0").roles().clear());
    }

    @Test void t02CategoryOnlyExpertFaqAndRuntimeInferredRssCannotGainTopicProof() {
        FAQ faq = new FAQ(); faq.id = "FAQ"; faq.communityId = community; faq.categoryTags = List.of("Housing");
        faq.title = "Emergency Shelter"; faqs.add(faq);
        ExpertAnswer expert = new ExpertAnswer(); expert.id = "EA"; expert.communityId = community;
        expert.categoryTags = List.of("Housing"); experts.add(expert);
        NewsItem inferred = law("RSS", "Emergency Shelter"); authoredTopics.remove("RSS"); news.remove(inferred); rss.add(inferred);
        assertTrue(ids().isEmpty());
        assertTrue(select().diagnostics().stream().anyMatch(d -> d.explanation().contains("unproven_authored_category_topic:RSS")));
    }

    @Test void t03LifecycleApprovalAndApproachingWithoutTimelyBoost() {
        resource("active", "Emergency Shelter"); resource("near", "Emergency Shelter").expirationDate = "2026-10-17";
        resource("expired", "Emergency Shelter").expirationDate = "2026-10-15";
        resource("inactive", "Emergency Shelter").status = "inactive";
        resource("withdrawn", "Emergency Shelter").status = "withdrawn";
        article("draft", "Emergency Shelter", "2026-10-16").editorialReview.status = "draft";
        assertEquals(List.of("active", "near"), ids()); assertEquals(java.util.Set.of(DURABLE), candidate("near").roles());
        assertTrue(candidate("near").limitations().contains("no_positive_resident_timely_evidence:near"));
    }

    @Test void t04ExactFlyerHelpAccessControlsAdmissionWithoutMutation() {
        Flyer f = flyer("F", "Emergency Shelter");
        for (String signal : new String[] {"actionable_help_access", " ACTIONABLE_HELP_ACCESS", ""}) {
            f.helpAccess = signal; assertTrue(ids().isEmpty()); assertTrue(diagnostic("FLYER_HELP_ACCESS_REQUIRED"));
            assertEquals(signal, f.helpAccess);
        }
        f.helpAccess = null; assertTrue(ids().isEmpty()); assertNull(f.helpAccess);
        f.helpAccess = "ACTIONABLE_HELP_ACCESS"; assertEquals(List.of("F"), ids());
    }

    @Test void t05DirectLocalWindowBoundariesDoNotExcludeOrdinaryInventory() {
        Flyer f = flyer("F", "Emergency Shelter"); f.image = null;
        for (String date : List.of("2026-10-16", "2026-11-15")) {
            f.eventDate = date; assertTrue(candidate("F").roles().contains(TIMELY));
        }
        for (String date : List.of("2026-10-15", "2026-11-16", "broken", "")) {
            f.eventDate = date; assertEquals(List.of("F"), ids()); assertFalse(candidate("F").roles().contains(TIMELY));
            assertTrue(candidate("F").limitations().contains("no_positive_resident_timely_evidence:F"));
            assertEquals(FeaturedItemSelector.PositionType.FALLBACK, select().positions().get(0).type());
        }
        f.eventDate = null; f.publishDate = "2026-10-16"; f.expirationDate = "2026-10-17";
        f.title = "deadline today"; assertFalse(candidate("F").roles().contains(TIMELY));
    }

    @Test void t06AuthoredContextLocalWindowAndPolicyType() {
        NewsItem n = law("L", "Emergency Shelter");
        for (String date : List.of("2026-10-16", "2026-09-16")) {
            n.publishDate = date; assertTrue(candidate("L").roles().contains(TIMELY)); assertFalse(candidate("L").actionable());
        }
        for (String date : List.of("2026-10-17", "2026-09-15", "broken")) {
            n.publishDate = date; assertFalse(candidate("L").roles().contains(TIMELY));
            assertTrue(candidate("L").limitations().contains("no_positive_resident_timely_evidence:L"));
        }
        n.publishDate = "2026-10-16"; n.contentType = ContentType.NEWS; n.type = "policy-update";
        assertTrue(candidate("L").roles().contains(TIMELY)); n.type = "notice"; assertFalse(candidate("L").roles().contains(TIMELY));
    }

    @Test void t07DurableRequiresAffirmativeExactStandardNotUndated() {
        Resource r = resource("R", "Emergency Shelter"); assertEquals(java.util.Set.of(DURABLE), candidate("R").roles());
        for (String value : List.of("Standard", "emergency", "")) {
            r.urgency = value; assertTrue(candidate("R").roles().isEmpty());
            assertTrue(candidate("R").limitations().contains("no_explicit_standard_urgency:R"));
        }
        r.urgency = null; assertTrue(candidate("R").roles().isEmpty());
    }

    @Test void t08VisualRequiresMediaSourceAndRetainsOriginalPresentation() {
        Flyer f = flyer("F", "Emergency Shelter"); assertTrue(candidate("F").roles().contains(VISUAL));
        assertEquals("original image.png", candidate("F").provenance().originalImage());
        assertEquals("/images/seasonal/original%20image.png", candidate("F").card().imageUrl());
        assertEquals(f.contentSource.url, candidate("F").card().url());
        assertEquals(f.contentSource.id, candidate("F").provenance().sourceId());
        f.contentSource = null; assertFalse(candidate("F").roles().contains(VISUAL));
        assertTrue(candidate("F").limitations().contains("no_positive_visual_media_source_evidence:F"));
        f.image = null; assertFalse(candidate("F").roles().contains(VISUAL));
    }

    @Test void t09MultiRoleFlyerAppearsOnce() {
        flyer("F", "Emergency Shelter"); assertEquals(List.of("F"), ids());
        assertEquals(java.util.Set.of(TIMELY, VISUAL), candidate("F").roles());
        assertEquals(FeaturedItemSelector.PositionType.TIMELY, select().positions().get(0).type()); assertTrue(diagnostic("MISSING_ROLE"));
    }

    @Test void t10IndependentTimelyPreservesFlyerVisualCoverage() {
        flyer("F", "Emergency Shelter"); resource("R", "Emergency Shelter"); law("L", "Emergency Shelter");
        assertEquals(List.of("L", "R", "F"), ids());
        assertEquals(List.of(FeaturedItemSelector.PositionType.TIMELY, FeaturedItemSelector.PositionType.DURABLE,
                FeaturedItemSelector.PositionType.VISUAL), select().positions().stream().map(p -> p.type()).toList());
        assertTrue(diagnostic("COVERAGE_COLLISION")); assertFalse(diagnostic("MISSING_ROLE"));
    }

    @Test void t11CoveragePrecedesHigherRankedDirectTimely() {
        flyer("A-best-direct", "Emergency Shelter").createdDate = "2026-10-16"; law("Z-context", "Emergency Shelter");
        assertEquals(List.of("Z-context", "A-best-direct"), ids());
        assertEquals(FeaturedItemSelector.PositionType.VISUAL, select().positions().get(1).type());
    }

    @Test void t12DirectHelpPrecedesNewerContextAndExpiryCannotQualify() {
        Flyer f = flyer("Z-direct", "Emergency Shelter"); f.image = null; f.eventDate = "2026-11-15";
        law("A-new-law", "Emergency Shelter"); resource("expiry", "Emergency Shelter").expirationDate = "2026-10-17";
        assertEquals("Z-direct", ids().get(0)); assertEquals("ROLE_RELEVANCE_HELP_FIRST", select().positions().get(0).tieStage());
        assertFalse(candidate("expiry").roles().contains(TIMELY));
    }

    @Test void t13MeaningfulDatesPrecedeNewnessAndCommunityLocalDayChanges() {
        Flyer a = flyer("A", "Emergency Shelter"); a.image = null; a.eventDate = "2026-11-15"; a.createdDate = "2026-10-16";
        Flyer b = flyer("B", "Emergency Shelter"); b.image = null; b.eventDate = "2026-10-16"; b.createdDate = "2026-01-01";
        assertEquals("B", ids().get(0)); assertEquals("MEANINGFUL_TEMPORAL_DISTINCTION", select().positions().get(0).tieStage());
        Flyer tokyo = flyer("Tokyo", "Emergency Shelter"); tokyo.communityId = "second-community"; tokyo.eventDate = "2026-11-16";
        var other = selector.select("second-community", "housing", "Emergency Shelter");
        assertEquals("2026-10-17", other.selectionDate().toString()); assertEquals(java.util.Set.of(TIMELY, VISUAL), other.candidates().get(0).roles());
        assertEquals(List.of("Tokyo"), other.positions().stream().map(p -> p.item().id()).toList()); assertFalse(ids().contains("Tokyo"));
    }

    @Test void t14GenuineVisualNewnessButCopiedResourceDatesCannotDiscriminate() {
        Flyer a = flyer("A", "Emergency Shelter"); a.eventDate = null; a.createdDate = "2026-01-01";
        Flyer b = flyer("B", "Emergency Shelter"); b.eventDate = null; b.createdDate = "2026-10-16";
        assertEquals("B", ids().get(0)); assertEquals("CATALOG_OR_APPROVAL_NEWNESS", select().positions().get(0).tieStage());
        flyers.clear(); resource("A", "Emergency Shelter").createdDate = "2020-01-01";
        resource("B", "Emergency Shelter").createdDate = "2026-10-16";
        assertEquals("A", ids().get(0)); assertNull(candidate("B").catalogOrApprovalDate());
        assertEquals("STABLE_CANONICAL_ID", select().positions().get(0).tieStage());
    }

    @Test void t15StableIdDiagnosticsSurviveRepositoryReorder() {
        for (String id : List.of("C", "A", "B", "D")) resource(id, "Emergency Shelter");
        var expected = select(); assertEquals(List.of("A", "B", "C"), ids()); assertTrue(diagnostic("STABLE_ITEM_ID_FALLBACK"));
        for (int seed = 0; seed < 20; seed++) { Collections.shuffle(resources, new Random(seed)); assertEquals(expected, select()); }
        assertTrue(select().diagnostics().stream().allMatch(d -> d.communityId().equals(community)
                && d.categoryKey().equals("housing") && d.topic().equals("Emergency Shelter")));
    }

    @Test void t16FallbackTruthfulAndHelpPrecedesNewerContext() {
        resource("Z-help", "Emergency Shelter").urgency = null; article("A-new-context", "Emergency Shelter", "2026-10-16");
        assertEquals(List.of("Z-help", "A-new-context"), ids());
        assertTrue(select().positions().stream().allMatch(p -> p.type() == FeaturedItemSelector.PositionType.FALLBACK));
        assertFalse(candidate("A-new-context").actionable()); assertTrue(diagnostic("MISSING_ROLE"));
    }

    @Test void t17EmptyOneTwoAndLargeInventoryWithoutCap() {
        assertTrue(ids().isEmpty()); resource("Z", "Emergency Shelter"); assertEquals(1, ids().size());
        resource("Y", "Emergency Shelter"); assertEquals(2, ids().size());
        for (int i = 0; i < 1000; i++) resource(String.format("R%04d", i), "Emergency Shelter");
        assertEquals(1002, select().candidates().size()); assertEquals(List.of("R0000", "R0001", "R0002"), ids());
    }

    @Test void t18DuplicateProjectionsAndCommunityCannotInflate() {
        Resource r = resource("R", "Emergency Shelter"); resources.add(r);
        Flyer f = flyer("F", "Emergency Shelter"); flyers.add(f);
        resource("other", "Emergency Shelter").communityId = "second-community";
        assertEquals(2, select().candidates().size()); assertEquals(2, ids().size());
        assertEquals(2, ids().stream().distinct().count()); assertFalse(ids().contains("other"));
    }

    @Test void otherCategoryAndContextDateDirection() {
        Resource food = resource("FOOD", "Food Pantry"); food.categoryTags = List.of("Food");
        assertEquals("FOOD", selector.select(community, "food", "food-pantry").positions().get(0).item().id());
        law("A-old", "Emergency Shelter").publishDate = "2026-09-16"; law("B-new", "Emergency Shelter").publishDate = "2026-10-16";
        assertEquals("B-new", ids().get(0)); assertEquals("MEANINGFUL_TEMPORAL_DISTINCTION", select().positions().get(0).tieStage());
    }

    @Test void approvalNewnessFallbackAndFutureDatesUnknown() {
        article("A", "Emergency Shelter", "2026-01-01"); article("B", "Emergency Shelter", "2026-10-16");
        assertEquals(List.of("B", "A"), ids()); assertEquals("2026-10-16", candidate("B").catalogOrApprovalDate().toString());
        article("C", "Emergency Shelter", "2026-10-17"); assertNull(candidate("C").catalogOrApprovalDate());
    }
    @Test void t13DstFallBackRepeatedHourKeepsLocalCalendarBoundaries() {
        for (String time : List.of("2026-11-01T05:30:00Z", "2026-11-01T06:30:00Z")) {
            resources.clear(); flyers.clear(); news.clear(); instant = Instant.parse(time); setUp();
            Flyer f = flyer("DST", "Emergency Shelter"); f.eventDate = "2026-12-01";
            assertEquals("2026-11-01", select().selectionDate().toString());
            assertTrue(candidate("DST").roles().contains(TIMELY));
            f.eventDate = "2026-12-02"; assertFalse(candidate("DST").roles().contains(TIMELY));
            law("DST-law", "Emergency Shelter").publishDate = "2026-10-02";
            assertTrue(candidate("DST-law").roles().contains(TIMELY));
        }
    }

    @Test void t13ContextWindowDiffersAcrossCommunitiesAtSameInstant() {
        law("NY", "Emergency Shelter").publishDate = "2026-09-16";
        NewsItem other = law("Tokyo", "Emergency Shelter"); other.communityId = "second-community"; other.publishDate = "2026-09-16";
        assertTrue(candidate("NY").roles().contains(TIMELY));
        var result = selector.select("second-community", "housing", "Emergency Shelter");
        assertFalse(result.candidates().get(0).roles().contains(TIMELY));
        assertTrue(result.candidates().get(0).limitations().contains("no_positive_resident_timely_evidence:Tokyo"));
    }

    @Test void t03RejectionReasonsAreIndependentlyLifecycleStatusAndApproval() {
        var config = new EditorialExpiryConfiguration();
        var lifecycle = new EditorialLifecycle(Clock.fixed(instant, ZoneOffset.UTC), config);
        Resource expired = resource("expired", "Emergency Shelter"); expired.expirationDate = "2026-10-15";
        assertTrue(lifecycle.isPublicStatus(expired));
        assertEquals(EditorialLifecycle.State.EXPIRED, lifecycle.assess(expired).state());
        Resource inactive = resource("inactive", "Emergency Shelter"); inactive.status = "inactive";
        assertTrue(lifecycle.assess(inactive).editorialExpiryEligible()); assertFalse(lifecycle.isPublicStatus(inactive));
        Article draft = article("draft", "Emergency Shelter", "2026-10-16"); draft.editorialReview.status = "draft";
        assertTrue(lifecycle.isCurrent(draft)); assertFalse(draft.isPublishable()); assertTrue(ids().isEmpty());
    }

}
