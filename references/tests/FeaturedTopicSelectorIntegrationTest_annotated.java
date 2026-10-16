/* IM-06 annotated reference.
 * What: scoped current inventory, authored classification proof, approved role facts and lexicographic Topic selection.
 * Why: retain identity and all seven P5 stages without accepting keyword inference or counting one ID in two roles.
 * Interaction: current services and helpFor supply eligibility; metadata proof establishes stored classification;
 * qualification and maximum matching feed ordered internal evidence and nonblocking diagnostics.
 * Alternatives rejected: raw-record eligibility, inferred Topic relevance, persisted scores/flags,
 * retrieval as catalog newness, final item selection and public featured UI owned by IM-07/08.
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
class FeaturedTopicSelectorIntegrationTest {
    private final List<Resource> resources = new ArrayList<>();
    private final List<Flyer> flyers = new ArrayList<>();
    private final List<NewsItem> news = new ArrayList<>();
    private final List<NewsItem> rss = new ArrayList<>();
    private final List<Article> articles = new ArrayList<>();
    private final List<FAQ> faqs = new ArrayList<>();
    private final List<ExpertAnswer> experts = new ArrayList<>();
    private FeaturedTopicSelector selector;
    private final java.util.Map<String, String> authoredTopics = new java.util.HashMap<>();
    private final String community = "wilmington-de";

    @BeforeEach void setUp() {
        var dates = new EditorialExpiryConfiguration();
        dates.setCommunityZones(Map.of("second-community", "Asia/Tokyo", community, "America/New_York"));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T23:30:00Z"), ZoneOffset.UTC);
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
        selector = new FeaturedTopicSelector(taxonomy, content, inventory, clock, dates);
    }

    private Resource resource(String id, String topic) {
        Resource r = new Resource(); r.id = id; r.title = id; r.urgency = "standard";
        r.communityId = community; r.categoryTags = List.of("Housing"); r.subcategory = topic;
        authoredTopics.put(id, topic); resources.add(r); return r;
    }
    private Flyer flyer(String id, String topic) {
        Flyer f = new Flyer(); f.id = id; f.title = id; f.helpAccess = "ACTIONABLE_HELP_ACCESS";
        f.communityId = community; f.categoryTags = List.of("Housing"); f.subcategory = topic;
        f.image = "original.png"; f.eventDate = "2026-10-17";
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
    private FeaturedTopicSelector.Selection select() { return selector.select(community, "housing"); }
    private FeaturedTopicSelector.TopicEvidence topic(String key) {
        return select().candidates().stream().filter(t -> t.key().equals(key)).findFirst().orElseThrow();
    }

    @Test void helpHeavyBeatsContextHeavyEvenWithStrongerRoleCoverageAndMoreRecentLaws() {
        resource("R1", "Emergency Shelter"); resource("R2", "Emergency Shelter");
        resource("R3", "Transitional Housing");
        for (int i = 0; i < 20; i++) law("LAW-" + i, "Transitional Housing");
        assertEquals("emergency-shelter", select().selected().get(0).key());
        assertEquals(20, topic("transitional-housing").recentContextCount());
    }

    @Test void roleCoverageMatchesDistinctIdsAndDoesNotCountTheFlyerTwice() {
        resource("R1", "Emergency Shelter"); flyer("F1", "Emergency Shelter");
        resource("R2", "Transitional Housing"); resource("R3", "Transitional Housing");
        assertEquals(2, topic("emergency-shelter").distinctRoleCoverage());
        law("L1", "Emergency Shelter");
        assertEquals(3, topic("emergency-shelter").distinctRoleCoverage());
        assertEquals("emergency-shelter", select().selected().get(0).key());
        assertEquals(List.of("F1", "R1"), topic("emergency-shelter").actionableIds());
        assertEquals(2, topic("emergency-shelter").actionableCount());
    }

    @Test void directActionDiscriminatesBeforeContextWithoutChangingEventEligibility() {
        resource("R1", "Emergency Shelter"); flyer("F1", "Emergency Shelter");
        resource("R2", "Transitional Housing"); flyer("F2", "Transitional Housing").eventDate = "2026-11-16";
        assertEquals(2, topic("transitional-housing").actionableCount()); // Day31 remains Help; no occurrence cutoff.
        assertEquals(2, topic("transitional-housing").distinctRoleCoverage());
        assertEquals(1, topic("emergency-shelter").directActionCount());
        assertEquals("emergency-shelter", select().selected().get(0).key());
    }

    @Test void explicitRecentContextBreaksTieOnlyAfterEqualActionAndRoleEvidence() {
        resource("R1", "Emergency Shelter"); flyer("F1", "Emergency Shelter"); law("L1", "Emergency Shelter");
        resource("R2", "Transitional Housing"); flyer("F2", "Transitional Housing"); law("L2", "Transitional Housing");
        law("L3", "Transitional Housing");
        assertEquals("transitional-housing", select().selected().get(0).key());
        assertEquals(3, topic("emergency-shelter").distinctRoleCoverage());
        assertEquals(3, topic("transitional-housing").distinctRoleCoverage());
    }

    @Test void approvedNewnessBreaksLateTieButDraftOrCopiedSourceDatesCannot() {
        resource("R1", "Emergency Shelter").createdDate = "2026-10-16";
        resource("R2", "Transitional Housing").createdDate = "2026-10-01";
        Article draft = article("A-draft", "Emergency Shelter", "2026-10-16"); draft.editorialReview.status = "draft";
        article("A-approved", "Transitional Housing", "2026-10-15");
        assertEquals("transitional-housing", select().selected().get(0).key());
        assertNull(topic("emergency-shelter").catalogOrApprovalNewness());
    }

    @Test void realLifecycleRejectsExpiredInactiveWithdrawnButApproachingIsNotPriority() {
        resource("active", "Emergency Shelter");
        resource("near", "Emergency Shelter").expirationDate = "2026-10-17";
        resource("expired", "Emergency Shelter").expirationDate = "2026-10-15";
        resource("inactive", "Emergency Shelter").status = "inactive";
        resource("withdrawn", "Emergency Shelter").status = "withdrawn";
        assertEquals(List.of("active", "near"), topic("emergency-shelter").actionableIds());
        assertEquals(1, topic("emergency-shelter").distinctRoleCoverage());
        assertEquals(0, topic("emergency-shelter").directActionCount());
    }

    @Test void explicitParentsTopicsAndExpertGuardBlockNarrativeAndInvalidMembership() {
        resource("good", "Emergency Shelter");
        resource("bad-parent", "Food Pantry");
        resource("no-category", "Emergency Shelter").categoryTags = null;
        resource("unknown", "Invented Topic");
        law("unclassified-law", null).title = "Emergency Shelter policy news";
        FAQ faq = new FAQ(); faq.id = "FAQ-prose"; faq.title = "Emergency Shelter";
        faq.communityId = community; faq.subcategory = "Emergency Shelter"; faqs.add(faq);
        ExpertAnswer expert = new ExpertAnswer(); expert.id = "EA-category-only";
        expert.communityId = community; expert.categoryTags = List.of("Housing"); experts.add(expert);
        assertEquals(List.of("good"), topic("emergency-shelter").actionableIds());
        assertEquals(0, topic("emergency-shelter").recentContextCount());
        assertEquals(1, select().selected().size());
    }

    @Test void twoCommunitiesHaveIndependentLocalCalendarWindowsAndNoInventoryLeakage() {
        Flyer ny = flyer("F-NY", "Emergency Shelter"); ny.eventDate = "2026-11-16";
        Flyer tokyo = flyer("F-Tokyo", "Emergency Shelter"); tokyo.communityId = "second-community";
        tokyo.eventDate = ny.eventDate;
        NewsItem nyLaw = law("L-NY", "Emergency Shelter"); nyLaw.publishDate = "2026-09-16";
        NewsItem tokyoLaw = law("L-Tokyo", "Emergency Shelter"); tokyoLaw.publishDate = nyLaw.publishDate;
        tokyoLaw.communityId = "second-community";
        assertEquals(0, topic("emergency-shelter").directActionCount());
        assertEquals(1, topic("emergency-shelter").recentContextCount());
        var other = selector.select("second-community", "housing");
        assertEquals("2026-10-17", other.selectionDate().toString());
        assertEquals(List.of("F-Tokyo"), other.selected().get(0).actionableIds());
        assertEquals(1, other.selected().get(0).directActionCount());
        assertEquals(0, other.selected().get(0).recentContextCount());
        assertEquals(List.of("F-NY"), topic("emergency-shelter").actionableIds());
    }

    @Test void duplicateProjectionMembershipAndInputShufflesDoNotChangeSelectionOrDiagnostics() {
        Resource a = resource("R1", "Emergency Shelter"); resources.add(a);
        resource("R2", "Transitional Housing");
        Flyer f = flyer("F1", "Emergency Shelter"); flyers.add(f);
        var expected = select();
        for (int seed = 0; seed < 20; seed++) {
            Collections.shuffle(resources, new Random(seed)); Collections.shuffle(flyers, new Random(seed));
            assertEquals(expected, select());
        }
        assertEquals(2, topic("emergency-shelter").actionableCount());
    }

    @Test void nonHousingAndEmptyCategoryUseSameContractAndRequireExplicitCommunity() {
        Resource food = resource("FOOD", "Food Pantry"); food.categoryTags = List.of("Food");
        var result = selector.select(community, "food");
        assertEquals("food-pantry", result.selected().get(0).key());
        assertTrue(selector.select("empty-community", "food").selected().isEmpty());
        assertTrue(selector.select(community, "unknown").selected().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> selector.select(null, "food"));
        assertThrows(IllegalArgumentException.class, () -> selector.select(" ", "food"));
    }

    @Test void rawRssFreshnessWithoutExplicitTopicNeverCreatesRelevanceOrNewness() {
        resource("R", "Emergency Shelter");
        NewsItem irrelevant = law("RSS", null); news.remove(irrelevant); rss.add(irrelevant);
        irrelevant.title = "Emergency Shelter Housing law"; irrelevant.createdDate = "2026-10-16";
        assertEquals(0, topic("emergency-shelter").recentContextCount());
        assertNull(topic("emergency-shelter").catalogOrApprovalNewness());
        irrelevant.subcategory = "Emergency Shelter";
        assertEquals(0, topic("emergency-shelter").recentContextCount());
        assertTrue(select().diagnostics().stream().anyMatch(d -> d.code().equals("CLASSIFICATION_EVIDENCE_LIMITATION")));
        irrelevant.expirationDate = "2026-10-15";
        assertEquals(0, topic("emergency-shelter").recentContextCount());
    }
}
