/*
 * IM-02 annotated reference — complete mirror of backend/src/test/java/org/firststep/backend/shared/lifecycle/EditorialExpiryDiagnosticsTest.java
 * WHAT: Fixed-clock diagnostic/command acceptance evidence.
 * WHY: stewardship must inspect retained records without changing resident eligibility.
 * INTERACTION: diagnostics reuse IM-01 assessment and its Community calendar; status and
 * Article approval remain separate. The CLI is outside resident component scanning.
 * ALTERNATIVES: a second expiry algorithm, filtered resident inputs, live feed fetching,
 * or an Admin dashboard would violate this package's narrow contract.
 * See docs/v2-completion-plan/IM-02 Execution Report.md and the individual method comments.
 */
package org.firststep.backend.shared.lifecycle;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.repository.ExpertAnswerRepository;
import org.firststep.backend.expert.repository.FaqRepository;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedService;
import org.firststep.backend.originals.model.Article;
import org.firststep.backend.originals.model.EditorialReview;
import org.firststep.backend.originals.repository.ArticleRepository;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.shared.classification.ClassifierFixture;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.service.ContentSourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EditorialExpiryDiagnosticsTest {
    private final List<Resource> resources = new ArrayList<>();
    private final List<NewsItem> news = new ArrayList<>();
    private final List<Flyer> flyers = new ArrayList<>();
    private final List<FAQ> faqs = new ArrayList<>();
    private final List<ExpertAnswer> experts = new ArrayList<>();
    private final List<Article> articles = new ArrayList<>();
    private final List<NewsItem> laws = new ArrayList<>();

    private <T extends CivicContent> T item(T item, String id, String expiry) {
        item.id = id;
        item.title = "Catalog " + id;
        item.communityId = "wilmington-de";
        item.expirationDate = expiry;
        item.status = "active";
        item.contentSource = new ContentSource();
        item.contentSource.id = "first-step";
        return item;
    }

    private Clock clock(String instant) { return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC); }

    private RssFeedService rss(EditorialLifecycle lifecycle) {
        RssFeedService rss = new RssFeedService(ClassifierFixture.real(), new ContentSourceService("../app/data"), lifecycle);
        ReflectionTestUtils.setField(rss, "rssItems", laws);
        return rss;
    }

    private EditorialExpiryDiagnostics diagnostics(String instant, EditorialExpiryConfiguration configuration) {
        var lifecycle = new EditorialLifecycle(clock(instant), configuration);
        ResourceRepository resourceRepository = mock(ResourceRepository.class);
        when(resourceRepository.findAll()).thenReturn(resources);
        FlyerRepository flyerRepository = mock(FlyerRepository.class);
        when(flyerRepository.findAll()).thenReturn(flyers);
        FaqRepository faqRepository = mock(FaqRepository.class);
        when(faqRepository.findAll()).thenReturn(faqs);
        ExpertAnswerRepository expertRepository = mock(ExpertAnswerRepository.class);
        when(expertRepository.findAll()).thenReturn(experts);
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        when(articleRepository.findAll()).thenReturn(articles);
        return new EditorialExpiryDiagnostics(lifecycle, clock(instant), resourceRepository, () -> news,
                flyerRepository, faqRepository, expertRepository, articleRepository, rss(lifecycle));
    }

    private EditorialExpiryDiagnostics diagnostics() {
        return diagnostics("2026-10-15T16:00:00Z", new EditorialExpiryConfiguration());
    }

    @ParameterizedTest
    @CsvSource({
        "2026-10-15T03:59:59Z, 0, NONE, 31",
        "2026-10-15T04:00:00Z, 1, APPROACHING_EXPIRY, 30",
        "2026-11-15T04:59:59Z, 1, APPROACHING_EXPIRY, 0",
        "2026-11-15T05:00:00Z, 1, EXPIRED, -1"
    })
    void shouldReportOnlyAttentionAtExactCommunityCalendarBoundaries(String instant, int count, String state, long days) {
        resources.add(item(new Resource(), "R", "2026-11-14"));
        var report = diagnostics(instant, new EditorialExpiryConfiguration()).collect(null);
        assertEquals(count, report.findings().size());
        if (count > 0) {
            assertEquals(state, report.findings().get(0).assessment().state().name());
            assertEquals(days, report.findings().get(0).daysUntilExpiry());
            assertTrue(report.findings().get(0).assessment().editorialAttentionRequired());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "2026-03-08T06:59:59Z, 2026-03-08, 0",
        "2026-03-08T07:00:00Z, 2026-03-08, 0",
        "2026-03-09T04:00:00Z, 2026-03-08, -1",
        "2026-11-01T05:30:00Z, 2026-11-01, 0",
        "2026-11-01T06:30:00Z, 2026-11-01, 0",
        "2026-11-02T05:00:00Z, 2026-11-01, -1"
    })
    void shouldCalculateCalendarDistanceAcrossDaylightSaving(String instant, String expiry, long days) {
        resources.add(item(new Resource(), "R", expiry));
        var finding = diagnostics(instant, new EditorialExpiryConfiguration()).collect(null).findings().get(0);
        assertEquals(days, finding.daysUntilExpiry());
        assertEquals(days < 0 ? EditorialLifecycle.State.EXPIRED : EditorialLifecycle.State.APPROACHING_EXPIRY,
                finding.assessment().state());
    }

    @Test
    void shouldUseOwningCommunityZonesAndExactScope() {
        var local = item(new Resource(), "local", "2026-10-15");
        var other = item(new Resource(), "other", "2026-10-15");
        other.communityId = "test-tokyo";
        resources.addAll(List.of(other, local));
        var configuration = new EditorialExpiryConfiguration();
        configuration.setCommunityZones(Map.of("test-tokyo", "Asia/Tokyo"));
        var service = diagnostics("2026-10-15T16:00:00Z", configuration);
        var localReport = service.collect("wilmington-de");
        assertEquals(1, localReport.inspectedRecords());
        assertEquals("local", localReport.findings().get(0).context().id());
        assertEquals(0, localReport.findings().get(0).daysUntilExpiry());
        var otherReport = service.collect("test-tokyo");
        assertEquals("other", otherReport.findings().get(0).context().id());
        assertEquals(-1, otherReport.findings().get(0).daysUntilExpiry());
        assertEquals(EditorialLifecycle.State.EXPIRED, otherReport.findings().get(0).assessment().state());
    }

    @Test
    void shouldHonorTheExistingConfiguredApproachingWindow() {
        resources.add(item(new Resource(), "R", "2026-10-30"));
        var configuration = new EditorialExpiryConfiguration();
        configuration.setApproachingDays(7);
        assertTrue(diagnostics("2026-10-15T16:00:00Z", configuration).collect(null).findings().isEmpty());
        assertEquals(1, diagnostics().collect(null).findings().size());
    }

    @Test
    void shouldNotInventFindingsOrExpiryFromMissingBlankAndUnrelatedDates() {
        resources.add(item(new Resource(), "missing", null));
        resources.add(item(new Resource(), "blank", " "));
        var flyer = item(new Flyer(), "old-event", null);
        flyer.eventDate = "2000-01-01";
        flyers.add(flyer);
        var answer = item(new ExpertAnswer(), "old-session", null);
        answer.sessionDate = "2000-01-01";
        experts.add(answer);
        var report = diagnostics().collect(null);
        assertEquals(4, report.inspectedRecords());
        assertTrue(report.findings().isEmpty());
        assertTrue(report.problems().isEmpty());
        assertNull(flyer.expirationDate);
        assertEquals(" ", resources.get(1).expirationDate);
        assertTrue(new EditorialLifecycle(clock("2026-10-15T16:00:00Z"), new EditorialExpiryConfiguration())
                .daysUntilExpiry(flyer).isEmpty());
    }

    @Test
    void shouldReadAllSevenContentClassesWithOriginalIdentityTypeAndProvenance() {
        resources.add(item(new Resource(), "resource", "2026-10-14"));
        news.add(item(new NewsItem(), "news", "2026-10-14"));
        flyers.add(item(new Flyer(), "flyer", "2026-10-14"));
        faqs.add(item(new FAQ(), "faq", "2026-10-14"));
        experts.add(item(new ExpertAnswer(), "expert", "2026-10-14"));
        articles.add(item(new Article(), "article", "2026-10-14"));
        var law = item(new NewsItem(), "law", "2026-10-14");
        law.contentType = ContentType.LAW;
        laws.add(law);
        var report = diagnostics().collect(null);
        assertEquals(7, report.inspectedRecords());
        assertEquals(7, report.findings().size());
        assertEquals(1, report.cachedRssRecords());
        assertEquals(List.of("Article/NEWS", "ExpertAnswer/EXPERT", "FAQ/EXPERT", "Flyer/FLYER", "NewsItem/LAW", "NewsItem/NEWS", "Resource/RESOURCE"),
                report.findings().stream().map(f -> f.context().contentClass() + "/" + f.context().contentType()).sorted().toList());
        assertTrue(report.findings().stream().allMatch(f -> "wilmington-de".equals(f.context().communityId())
                && "first-step".equals(f.context().contentSourceId())));
        assertEquals(List.of("article", "expert", "faq", "flyer", "law", "news", "resource"),
                report.findings().stream().map(f -> f.context().id()).sorted().toList());
    }

    @Test
    void shouldKeepInactiveWithdrawnAndUnapprovedContextIndependentOfExpiry() {
        var inactive = item(new NewsItem(), "inactive", "2026-11-14"); inactive.status = "inactive";
        var withdrawn = item(new NewsItem(), "withdrawn", "2026-10-14"); withdrawn.status = "withdrawn";
        news.addAll(List.of(inactive, withdrawn));
        var draft = item(new Article(), "draft", "2026-11-14");
        draft.editorialReview = new EditorialReview(); draft.editorialReview.status = "draft";
        articles.add(draft);
        var report = diagnostics().collect(null);
        assertEquals(3, report.findings().size());
        for (var finding : report.findings()) {
            if (finding.context().contentClass().equals("Article")) {
                assertEquals("draft", finding.context().articleReviewStatus());
                assertFalse(finding.context().articleApproved());
                assertTrue(finding.assessment().editorialExpiryEligible());
            } else {
                assertFalse(finding.context().statusAllowsPublication());
            }
        }
    }

    @Test
    void shouldIsolateMalformedExpiryWithoutAssigningAStateOrHidingValidRecords() {
        news.add(item(new NewsItem(), "bad", "2026-02-30"));
        news.add(item(new NewsItem(), "good", "2026-10-14"));
        var report = diagnostics().collect(null);
        assertEquals(List.of("good"), report.findings().stream().map(f -> f.context().id()).toList());
        assertEquals(1, report.problems().size());
        assertEquals("bad", report.problems().get(0).context().id());
        assertEquals("2026-02-30", report.problems().get(0).context().expirationDate());
        assertTrue(report.problems().get(0).reason().contains("no lifecycle state was assigned"));
        assertThrows(java.time.format.DateTimeParseException.class, () -> LifecycleFixture.fixed().assess(news.get(0)));
    }

    @Test
    void shouldProduceIdenticalFindingsAndOrderingForTheSameClockAndReorderedStoredRows() {
        news.add(item(new NewsItem(), "B", "2026-10-14"));
        news.add(item(new NewsItem(), "A", "2026-10-14"));
        news.add(item(new NewsItem(), "error-B", "bad"));
        news.add(item(new NewsItem(), "error-A", "bad"));
        var service = diagnostics();
        var first = service.collect(null);
        Collections.reverse(news);
        var second = service.collect(null);
        assertEquals(first, second);
        assertEquals(EditorialExpiryReport.render(first), EditorialExpiryReport.render(second));
        assertEquals(List.of("A", "B"), first.findings().stream().map(f -> f.context().id()).toList());
        assertEquals(List.of("error-A", "error-B"), first.problems().stream().map(f -> f.context().id()).toList());
    }

    @Test
    void shouldRetainStoredRecordsAndResidentExpiryBehaviorWithoutMutation() throws Exception {
        news.add(item(new NewsItem(), "approaching", "2026-11-14"));
        news.add(item(new NewsItem(), "expired", "2026-10-14"));
        news.add(item(new NewsItem(), "bad", "nonsense"));
        var mapper = new ObjectMapper();
        String before = mapper.writeValueAsString(news);
        var report = diagnostics().collect(null);
        assertEquals(before, mapper.writeValueAsString(news));
        assertEquals(2, report.findings().size());
        assertEquals(1, report.problems().size());
        var resident = new NewsService(() -> news.subList(0, 2), LifecycleFixture.fixed());
        assertEquals(List.of("approaching"), resident.getAll().stream().map(n -> n.id).toList());
        assertEquals(3, news.size());
        laws.add(item(new NewsItem(), "cached-expired", "2026-10-14"));
        var rss = rss(LifecycleFixture.fixed());
        assertTrue(rss.getRssItems().isEmpty());
        assertEquals("cached-expired", rss.getStoredRssItems().get(0).id);
        assertThrows(UnsupportedOperationException.class, () -> rss.getStoredRssItems().clear());
    }

    @Test
    void shouldRenderEmptyResultsAndMissingMetadataIntelligiblyWithoutTraces() {
        var empty = EditorialExpiryReport.render(diagnostics().collect("empty-community"));
        assertTrue(empty.contains("No approaching-expiry or expired records"));
        assertTrue(empty.contains("No invalid editorial-expiry metadata"));
        var item = item(new NewsItem(), null, "2026-10-14");
        item.title = "Line one\nLine two\u001b[31m"; item.contentSource = null;
        news.add(item);
        news.add(item(new NewsItem(), "bad", "not-a-date"));
        var rendered = EditorialExpiryReport.render(diagnostics().collect(null));
        assertTrue(rendered.contains("Source: (not recorded)"));
        assertTrue(rendered.contains("METADATA PROBLEMS (not a lifecycle state)"));
        assertTrue(rendered.contains("no lifecycle state was assigned"));
        assertFalse(rendered.contains("DateTimeParseException"));
        assertFalse(rendered.contains("\u001b"));
        assertFalse(rendered.contains("Line one\nLine two"));
    }
}
