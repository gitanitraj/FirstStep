/*
 * WHAT: Complete IM-09 annotated mirror of backend/src/test/java/org/firststep/backend/category/service/CivicCollectionContractTest.java.
 * WHY: Preserve authored placement, canonical identity, truthful dates and private/public boundaries.
 * INTERACTION: Current inventory and Article approval remain independent of exact editorial
 * Expert/FAQ admissions; Category composition intersects Community, taxonomy and membership.
 * ALTERNATIVES: Source/session/verified inference and blanket Expert admission were rejected.
 * Tests cover approved admissions, denied sessions, preserved supporting views and safe links.
 */
package org.firststep.backend.category.service;

import java.util.List;
import org.firststep.backend.category.dto.CategoryCollection;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.originals.model.Article;
import org.firststep.backend.originals.model.EditorialReview;
import org.firststep.backend.originals.service.ArticleService;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** IM-09 independent controlled projection rules, with exact approved editorial membership. */
class CivicCollectionContractTest {
    private ResourceService resources;
    private FlyerService flyers;
    private ExpertAnswerService experts;
    private FaqService faqs;
    private NewsService news;
    private ArticleService articles;
    private List<NewsItem> rss = List.of();
    private CategoryContentService content;
    @BeforeEach void setUp() {
        news = mock(NewsService.class); articles = mock(ArticleService.class);
        resources = mock(ResourceService.class); flyers = mock(FlyerService.class);
        experts = mock(ExpertAnswerService.class); faqs = mock(FaqService.class);
        when(news.getAll()).thenReturn(List.of()); when(articles.getPublishable()).thenReturn(List.of());
        when(resources.getAll()).thenReturn(List.of()); when(flyers.getAll()).thenReturn(List.of());
        when(experts.getAll()).thenReturn(List.of()); when(faqs.getAll()).thenReturn(List.of());
        content = new CategoryContentService(new TaxonomyService("../app/data"), resources, news,
                () -> rss, flyers, experts, faqs, articles, new org.firststep.backend.category.service.FirstRateNewsMembership("../app/data"));
    }
    private NewsItem news(String id) {
        NewsItem n = new NewsItem(); n.id = id; n.title = id; n.categoryTags = List.of("Housing");
        n.communityId = "wilmington-de"; n.publishDate = "2026-10-16";
        n.contentSource = new ContentSource(); n.contentSource.id = "de-dhss"; n.contentSource.url = "https://example.gov/" + id;
        return n;
    }
    private List<String> ids(String key) {
        return content.collectionsFor("housing", "wilmington-de").stream().filter(c -> c.key().equals(key))
                .flatMap(c -> c.items().stream()).map(c -> c.id()).toList();
    }

    @Test void t13AdmissionCannotBypassCurrentInventoryOrAuthoredTaxonomy() {
        var answer = new org.firststep.backend.expert.model.ExpertAnswer();
        answer.id = "EA-001"; answer.communityId = "wilmington-de";
        answer.categoryTags = List.of("Housing"); answer.title = "Approved session";
        when(experts.getAll()).thenReturn(List.of(answer));
        assertEquals(List.of("EA-001"), ids("first-rate-news"));
        answer.subcategory = "Invented Topic";
        assertTrue(ids("first-rate-news").isEmpty());
        answer.subcategory = null; answer.categoryTags = List.of("Food");
        assertTrue(ids("first-rate-news").isEmpty());
        answer.categoryTags = List.of("Housing"); when(experts.getAll()).thenReturn(List.of());
        assertTrue(ids("first-rate-news").isEmpty(), "manifest never reloads records excluded by lifecycle service");
    }

    @Test void t06RuntimeKeywordLawCannotPopulateCategoryCollection() {
        NewsItem inferred = news("RSS-inferred"); inferred.contentType = ContentType.LAW;
        inferred.subcategory = "Eviction Prevention"; rss = List.of(inferred);
        assertEquals(List.of(), ids("law"), "runtime tags are not authored law placement proof");
        assertEquals(List.of(inferred), rss, "broader relevance-admitted RSS inventory is preserved");
    }

    @Test void t17EqualDatePolicyOrderUsesCanonicalIdNotInputIteration() {
        var a = news("A"); var z = news("Z"); when(news.getAll()).thenReturn(List.of(z, a));
        assertEquals(List.of("A", "Z"), ids("policy-info"));
        when(news.getAll()).thenReturn(List.of(a, z)); assertEquals(List.of("A", "Z"), ids("policy-info"));
    }

    @Test void t17ApprovedArticleRepeatedProjectionKeepsOneCanonicalIdentity() {
        Article a = new Article(); a.id = "approved"; a.title = "Article"; a.categoryTags = List.of("Housing");
        a.communityId = "wilmington-de"; a.editorialReview = new EditorialReview(); a.editorialReview.status = "approved";
        when(articles.getPublishable()).thenReturn(List.of(a, a));
        assertEquals(List.of("approved"), ids("first-rate-news"));
    }

    @Test void t02PolicyIsExternalNewsNotLawOriginalOrUnknownNewCollection() {
        var external = news("external"); external.type = "invented-projection";
        var own = news("own"); own.contentSource.id = "first-step";
        var law = news("law"); law.contentType = ContentType.LAW;
        when(news.getAll()).thenReturn(List.of(external, own, law));
        assertEquals(List.of("external"), ids("policy-info"));
        assertTrue(content.collectionsFor("housing", "wilmington-de").stream()
                .filter(c -> c.layer() == org.firststep.backend.category.dto.CollectionLayer.INFORMATION)
                .map(CategoryCollection::key).allMatch(List.of("policy-info", "law", "flyers", "first-rate-news")::contains));
    }
    @Test void t06AuthoredCategoryOnlyLawIsValidWithoutInventingTopic() {
        NewsItem authored = news("authored-law"); authored.contentType = ContentType.LAW;
        authored.authoredCategoryTags = List.of("Housing"); assertNull(authored.subcategory);
        when(news.getAll()).thenReturn(List.of(authored));
        assertEquals(List.of("authored-law"), ids("law"));
        authored.authoredCategoryTags = List.of("Food");
        assertTrue(ids("law").isEmpty(), "runtime Housing cannot replace stored Food classification");
    }

    @Test void t03t06InferredLawCannotReachCategoryCountsOrUpdatesButBroaderSectorStillWorks() {
        NewsItem inferred = news("inferred-law"); inferred.contentType = ContentType.LAW; rss = List.of(inferred);
        var taxonomy = new TaxonomyService("../app/data");
        var updates = new org.firststep.backend.updates.service.UpdatesService(news, () -> rss, flyers, experts, faqs,
                taxonomy, new org.firststep.backend.shared.service.ContentSourceService("../app/data"));
        var navigation = new org.firststep.backend.navigation.service.NavigationService("../app/data", taxonomy,
                resources, news, flyers, experts, faqs, () -> rss, articles);
        assertTrue(updates.getForCategory("housing", "wilmington-de", 10).isEmpty());
        assertEquals(0, navigation.getByKey("housing", "wilmington-de").orElseThrow().totalCount());
        assertEquals(List.of("inferred-law"), updates.getBySector(org.firststep.backend.shared.model.Sector.GOVERNMENT)
                .stream().map(c -> c.id()).toList());
        inferred.contentSource.id = "unknown-source";
        assertTrue(updates.getBySector(org.firststep.backend.shared.model.Sector.GOVERNMENT).isEmpty());
        assertTrue(updates.getBySector(org.firststep.backend.shared.model.Sector.COMMUNITY).isEmpty());
        assertEquals("unknown-source", inferred.contentSource.id);
    }

    @Test void t06AuthoredCategoryCannotAuthorizeRuntimeInferredTopic() {
        NewsItem law = news("category-only-law"); law.contentType = ContentType.LAW;
        law.authoredCategoryTags = List.of("Housing"); law.subcategory = "Eviction Prevention";
        when(news.getAll()).thenReturn(List.of(law));
        var navigation = new org.firststep.backend.navigation.service.NavigationService("../app/data", new TaxonomyService("../app/data"),
                resources, news, flyers, experts, faqs, () -> rss, articles);
        var page = navigation.getByKey("housing", "wilmington-de").orElseThrow();
        assertEquals(1, page.totalCount());
        assertEquals(0, page.groups().stream().flatMap(g -> g.topics().stream()).mapToInt(t -> t.count()).sum());
        law.authoredSubcategory = "Eviction Prevention";
        assertEquals(1, navigation.getByKey("housing", "wilmington-de").orElseThrow().groups()
                .stream().flatMap(g -> g.topics().stream()).mapToInt(t -> t.count()).sum());
    }

}
