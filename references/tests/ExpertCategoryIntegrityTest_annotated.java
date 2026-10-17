/*
 * WHAT: Complete IM-09 annotated mirror of backend/src/test/java/org/firststep/backend/category/service/ExpertCategoryIntegrityTest.java.
 * WHY: Preserve authored placement, canonical identity, truthful dates and private/public boundaries.
 * INTERACTION: Current inventory and Article approval remain independent of exact editorial
 * Expert/FAQ admissions; Category composition intersects Community, taxonomy and membership.
 * ALTERNATIVES: Source/session/verified inference and blanket Expert admission were rejected.
 * Tests cover approved admissions, denied sessions, preserved supporting views and safe links.
 */
package org.firststep.backend.category.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.repository.FaqRepository;
import org.firststep.backend.expert.repository.ExpertAnswerRepository;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.navigation.service.NavigationService;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.originals.service.ArticleService;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.lifecycle.LifecycleFixture;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.service.ContentSourceService;
import org.firststep.backend.updates.service.UpdatesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExpertCategoryIntegrityTest {
    private final TaxonomyService taxonomy = new TaxonomyService("../app/data");
    private final List<FAQ> storedFaqs = new ArrayList<>();
    private final List<ExpertAnswer> storedAnswers = new ArrayList<>();
    private CategoryContentService content;
    private NavigationService navigation;
    private UpdatesService updates;

    @BeforeEach
    void setUp() {
        FaqRepository faqRepository = mock(FaqRepository.class);
        when(faqRepository.findAll()).thenReturn(storedFaqs);
        ExpertAnswerRepository expertRepository = mock(ExpertAnswerRepository.class);
        when(expertRepository.findAll()).thenReturn(storedAnswers);
        FaqService faqs = new FaqService(faqRepository, LifecycleFixture.fixed());
        ExpertAnswerService experts = new ExpertAnswerService(expertRepository, LifecycleFixture.fixed());
        ResourceService resources = mock(ResourceService.class);
        NewsService news = mock(NewsService.class);
        FlyerService flyers = mock(FlyerService.class);
        ArticleService articles = mock(ArticleService.class);
        when(resources.getAll()).thenReturn(List.of());
        when(news.getAll()).thenReturn(List.of());
        when(flyers.getAll()).thenReturn(List.of());
        when(articles.getPublishable()).thenReturn(List.of());
        RssFeedSource rss = List::of;
        content = new CategoryContentService(taxonomy, resources, news, rss, flyers, experts, faqs, articles, new org.firststep.backend.category.service.FirstRateNewsMembership("../app/data"));
        navigation = new NavigationService("../app/data", taxonomy, resources, news, flyers, experts, faqs, rss, articles);
        updates = new UpdatesService(news, rss, flyers, experts, faqs, taxonomy, new ContentSourceService("../app/data"));
    }

    private <T extends CivicContent> T item(T item, String id, List<String> categories, String topic) {
        item.id = id; item.title = id; item.communityId = "test-community";
        item.categoryTags = categories; item.subcategory = topic;
        return item;
    }

    private List<String> categoryIds(String category, String community) {
        return content.collectionsFor(category, community).stream().flatMap(c -> c.items().stream()).map(ContentItem::id).sorted().toList();
    }

    @Test
    void shouldDeliverExplicitCategoryOnlyFaqAndExpertWithoutFabricatingTopic() {
        var faq = item(new FAQ(), "faq", List.of("Housing"), null);
        var answer = item(new ExpertAnswer(), "expert", List.of("Housing"), null);
        storedFaqs.add(faq); storedAnswers.add(answer);
        assertEquals(List.of("expert", "faq"), categoryIds("housing", "test-community"));
        assertNull(faq.subcategory); assertNull(answer.subcategory);
        assertTrue(content.forTopic(taxonomy.findByKey("housing").orElseThrow(), "Eviction Prevention", "test-community").isEmpty());
        assertEquals(2, navigation.getByKey("housing", "test-community").orElseThrow().totalCount());
    }

    @Test
    void shouldDeliverExplicitTopicsAndValidMultiCategoryIdentityOnlyUnderMatchingParents() {
        storedFaqs.add(item(new FAQ(), "faq", List.of("Housing", "Legal"), "Eviction Prevention"));
        storedAnswers.add(item(new ExpertAnswer(), "expert", List.of("Housing"), "Eviction Prevention"));
        assertEquals(List.of("expert", "faq"), categoryIds("housing", "test-community"));
        assertEquals(List.of("faq"), categoryIds("legal", "test-community"));
        var items = content.forTopic(taxonomy.findByKey("housing").orElseThrow(), "Eviction Prevention", "test-community");
        assertEquals(List.of("expert", "faq"), items.stream().map(ContentItem::id).sorted().toList());
        assertEquals(2, items.stream().map(ContentItem::id).distinct().count());
        assertTrue(content.forTopic(taxonomy.findByKey("food").orElseThrow(), "Eviction Prevention", "test-community").isEmpty());
    }

    @Test
    void shouldRejectMissingUnknownAndMalformedRelationshipsBeforeCategoryCountsOrUpdates() {
        storedFaqs.add(item(new FAQ(), "missing", null, "Eviction Prevention"));
        storedFaqs.add(item(new FAQ(), "unknown", List.of("Housing", "Imaginary"), null));
        storedFaqs.add(item(new FAQ(), "mismatch", List.of("Food"), "Eviction Prevention"));
        storedAnswers.add(item(new ExpertAnswer(), "invalid-topic", List.of("Housing"), "Made Up Topic"));
        storedAnswers.add(item(new ExpertAnswer(), "duplicate", List.of("Housing", "housing"), null));
        assertTrue(categoryIds("housing", "test-community").isEmpty());
        assertTrue(categoryIds("food", "test-community").isEmpty());
        assertEquals(0, navigation.getByKey("housing", "test-community").orElseThrow().totalCount());
        assertTrue(updates.getForCategory("housing", "test-community", 100).isEmpty());
        assertNull(storedFaqs.get(0).categoryTags);
    }

    @Test
    void shouldKeepClassificationIndependentOfExpiryStatusAndCommunityPartition() {
        for (String state : List.of("expired", "approaching", "inactive", "withdrawn")) {
            var faq = item(new FAQ(), state, List.of("Housing"), null);
            faq.expirationDate = state.equals("expired") ? "2026-10-14" : "2026-11-14";
            if (state.equals("inactive") || state.equals("withdrawn")) faq.status = state;
            storedFaqs.add(faq);
        }
        storedAnswers.add(item(new ExpertAnswer(), "other-community", List.of("Housing"), null));
        storedAnswers.get(0).communityId = "another-community";
        assertEquals(List.of("approaching"), categoryIds("housing", "test-community"));
        assertEquals(List.of("other-community"), categoryIds("housing", "another-community"));
        assertEquals(4, storedFaqs.size());
        assertTrue(storedFaqs.stream().allMatch(taxonomy::permitsExpertCategoryParticipation));
        assertEquals(List.of("approaching"), updates.getForCategory("housing", "test-community", 100).stream().map(i -> i.id()).toList());
    }

    @Test
    void shouldExplainExactValidationDefectsWhilePreservingCaseInsensitiveControlledMatching() {
        assertTrue(taxonomy.expertClassificationErrors(List.of("housing"), null).isEmpty());
        assertTrue(taxonomy.expertClassificationErrors(List.of("Housing", "Legal"), "Eviction Prevention").isEmpty());
        assertTrue(taxonomy.expertClassificationErrors(null, null).get(0).startsWith("missing_category:"));
        assertTrue(taxonomy.expertClassificationErrors(List.of("Food"), "Eviction Prevention").contains("parent_mismatch: Topic requires its explicitly assigned parent Category: Eviction Prevention"));
        assertTrue(taxonomy.expertClassificationErrors(Arrays.asList("Housing", null), null).stream().anyMatch(e -> e.startsWith("malformed_category:")));
        assertTrue(taxonomy.expertClassificationErrors(List.of("Housing"), "Not A Topic").get(0).startsWith("invalid_topic:"));
    }
}
