package org.firststep.backend.category.service;

import java.util.List;

import org.firststep.backend.category.dto.CategoryCollection;
import org.firststep.backend.category.dto.CollectionLayer;
import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.originals.model.Article;
import org.firststep.backend.originals.service.ArticleService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The destination's collections (Decision 051).
 *
 * <p>Wired with the REAL taxonomy over mocked content: what is under test is how
 * editorial classification and producer identity select content into collections,
 * and the taxonomy IS that vocabulary.
 */
class CategoryContentServiceTest {

    private ResourceService resourceService;
    private NewsService newsService;
    private RssFeedSource rssFeedSource;
    private FlyerService flyerService;
    private ExpertAnswerService expertAnswerService;
    private FaqService faqService;
    private ArticleService articleService;
    private CategoryContentService service;

    @BeforeEach
    void setUp() {
        resourceService = mock(ResourceService.class);
        newsService = mock(NewsService.class);
        flyerService = mock(FlyerService.class);
        expertAnswerService = mock(ExpertAnswerService.class);
        faqService = mock(FaqService.class);
        articleService = mock(ArticleService.class);
        rssFeedSource = List::of;
        when(resourceService.getAll()).thenReturn(List.of());
        when(newsService.getAll()).thenReturn(List.of());
        when(flyerService.getAll()).thenReturn(List.of());
        when(expertAnswerService.getAll()).thenReturn(List.of());
        when(faqService.getAll()).thenReturn(List.of());
        when(articleService.getPublishable()).thenReturn(List.of());
        rebuild();
    }

    private void rebuild() {
        service = new CategoryContentService(new TaxonomyService("../app/data"), resourceService,
                newsService, rssFeedSource, flyerService, expertAnswerService, faqService, articleService);
    }

    private List<CategoryCollection> housing() {
        return service.collectionsFor("housing", null);
    }

    private static CategoryCollection find(List<CategoryCollection> collections, String key) {
        return collections.stream().filter(c -> c.key().equals(key)).findFirst().orElse(null);
    }

    private static Resource resource(String id, String title) {
        Resource r = new Resource();
        r.id = id;
        r.title = title;
        r.summary = title + " summary";
        r.categoryTags = List.of("Housing");
        return r;
    }

    private static NewsItem news(String id, String title, String producerId, String published) {
        NewsItem n = new NewsItem();
        n.id = id;
        n.title = title;
        n.categoryTags = List.of("Housing");
        n.publishDate = published;
        n.contentSource = new ContentSource();
        n.contentSource.id = producerId;
        return n;
    }

    private static Article article(String id, String title) {
        Article a = new Article();
        a.id = id;
        a.title = title;
        a.categoryTags = List.of("Housing");
        a.contentSource = new ContentSource();
        a.contentSource.id = "first-step";
        return a;
    }

    // ---- Empty collections are never built ---------------------------------

    @Test
    void shouldOmitCollectionsEntirelyWhenTheyHaveNoContent() {
        // Not "present with count 0" — absent. A heading over nothing reads as
        // broken rather than informative.
        assertTrue(housing().isEmpty());
    }

    @Test
    void shouldOmitOnlyTheEmptyCollectionsWhenSomeHaveContent() {
        when(resourceService.getAll()).thenReturn(List.of(resource("R1", "A shelter")));

        List<CategoryCollection> collections = housing();

        assertEquals(List.of("get-help"), collections.stream().map(CategoryCollection::key).toList());
        assertFalse(collections.stream().anyMatch(c -> c.count() == 0));
    }

    @Test
    void shouldReturnNoCollectionsForAnUnknownCategoryKey() {
        when(resourceService.getAll()).thenReturn(List.of(resource("R1", "A shelter")));

        assertTrue(service.collectionsFor("not-a-category", null).isEmpty());
    }

    // ---- Selection is editorial classification only ------------------------

    @Test
    void shouldNotSelectContentClassifiedIntoADifferentCategory() {
        Resource wrongCategory = resource("R1", "A food pantry");
        wrongCategory.categoryTags = List.of("Food");
        when(resourceService.getAll()).thenReturn(List.of(wrongCategory));

        assertTrue(housing().isEmpty());
    }

    @Test
    void shouldNotSelectUnclassifiedContent() {
        Resource unclassified = resource("R1", "Emergency shelter for families");
        unclassified.categoryTags = null;
        unclassified.description = "Emergency shelter beds and housing support.";
        when(resourceService.getAll()).thenReturn(List.of(unclassified));

        assertTrue(housing().isEmpty());
    }

    // ---- A collection is not a ContentType projection -----------------------

    @Test
    void shouldSplitNewsBetweenPolicyInfoAndLawByContentType() {
        NewsItem policy = news("NP-1", "Voucher waitlist reopens", "de-housing-authority", "2026-06-02");
        NewsItem law = news("L-1", "A signed bill", "de-legislature", "2026-07-14");
        law.contentType = ContentType.LAW;
        when(newsService.getAll()).thenReturn(List.of(policy, law));

        List<CategoryCollection> collections = housing();

        assertEquals(List.of("NP-1"), ids(find(collections, "policy-info")));
        assertEquals(List.of("L-1"), ids(find(collections, "law")));
    }

    @Test
    void shouldExcludeFirstStepReportingFromPolicyInfo() {
        // Policy Info means EXTERNAL civic information. First Step's own byline
        // appearing there would misrepresent who is speaking.
        when(newsService.getAll()).thenReturn(List.of(
                news("NP-1", "City announces a change", "city-of-wilmington", "2026-06-01"),
                news("NP-2", "Our own reporting", "first-step", "2026-06-02")));

        assertEquals(List.of("NP-1"), ids(find(housing(), "policy-info")));
    }

    @Test
    void shouldPutApprovedArticlesInFirstRateNewsAndNotInPolicyInfo() {
        when(articleService.getPublishable()).thenReturn(List.of(article("OR-3", "How to read a notice")));

        List<CategoryCollection> collections = housing();

        assertEquals(List.of("OR-3"), ids(find(collections, "first-rate-news")));
        assertEquals(null, find(collections, "policy-info"));
    }

    @Test
    void shouldNotPutACuratedFaqInFirstRateNewsEvenThoughFirstStepProducedIt() {
        // The collection means First Step's JOURNALISM, not every record whose
        // producer id is first-step. Widening it to make a total reconcile is
        // exactly what Decision 051 rejected.
        FAQ faq = new FAQ();
        faq.id = "FAQ-2";
        faq.title = "What is the eviction notice period?";
        faq.categoryTags = List.of("Housing");
        faq.contentSource = new ContentSource();
        faq.contentSource.id = "first-step";
        when(faqService.getAll()).thenReturn(List.of(faq));

        List<CategoryCollection> collections = housing();

        assertEquals(null, find(collections, "first-rate-news"));
        assertEquals(List.of("FAQ-2"), ids(find(collections, "faqs")));
    }

    // ---- Layers -------------------------------------------------------------

    @Test
    void shouldMarkExpertContentAsSupportingRatherThanPrimary() {
        ExpertAnswer answer = new ExpertAnswer();
        answer.id = "EA-1";
        answer.title = "How much notice must a landlord give?";
        answer.categoryTags = List.of("Housing");
        when(expertAnswerService.getAll()).thenReturn(List.of(answer));
        when(resourceService.getAll()).thenReturn(List.of(resource("R1", "A shelter")));

        List<CategoryCollection> collections = housing();

        assertEquals(CollectionLayer.ACTION, find(collections, "get-help").layer());
        assertEquals(CollectionLayer.SUPPORTING, find(collections, "expert-qa").layer());
    }

    @Test
    void shouldOrderCollectionsByCompositionNotByContentVolume() {
        // One resource, three laws. Composition order still leads with Get Help —
        // the destination's sequence is a design decision, not a popularity sort.
        when(resourceService.getAll()).thenReturn(List.of(resource("R1", "A shelter")));
        NewsItem a = news("L-1", "Bill A", "de-legislature", "2026-07-01");
        NewsItem b = news("L-2", "Bill B", "de-legislature", "2026-07-02");
        NewsItem c = news("L-3", "Bill C", "de-legislature", "2026-07-03");
        a.contentType = ContentType.LAW;
        b.contentType = ContentType.LAW;
        c.contentType = ContentType.LAW;
        when(newsService.getAll()).thenReturn(List.of(a, b, c));

        assertEquals(List.of("get-help", "law"), housing().stream().map(CategoryCollection::key).toList());
    }

    // ---- Ordering within a collection --------------------------------------

    @Test
    void shouldOrderDatedCollectionsNewestFirst() {
        when(newsService.getAll()).thenReturn(List.of(
                news("NP-1", "Older", "city-of-wilmington", "2026-06-01"),
                news("NP-2", "Newer", "city-of-wilmington", "2026-08-18")));

        assertEquals(List.of("NP-2", "NP-1"), ids(find(housing(), "policy-info")));
    }

    @Test
    void shouldOrderResourcesAlphabeticallyBecauseTheyCarryNoEditorialDate() {
        when(resourceService.getAll()).thenReturn(List.of(
                resource("R1", "Zebra House"),
                resource("R2", "Anchor Shelter")));

        assertEquals(List.of("R2", "R1"), ids(find(housing(), "get-help")));
    }

    @Test
    void shouldPreserveAuthoredFileOrderForSupportingCollections() {
        // A curated question list is sequenced by whoever curated it, and that
        // ordering carries meaning no derived sort would improve on.
        FAQ first = faq("FAQ-9", "Asked most often");
        FAQ second = faq("FAQ-1", "Asked less often");
        when(faqService.getAll()).thenReturn(List.of(first, second));

        assertEquals(List.of("FAQ-9", "FAQ-1"), ids(find(housing(), "faqs")));
    }

    // ---- Community scoping --------------------------------------------------

    @Test
    void shouldFilterByCommunityWhenRequested() {
        Resource local = resource("R1", "Local");
        local.communityId = "wilmington-de";
        Resource elsewhere = resource("R2", "Elsewhere");
        elsewhere.communityId = "newark-de";
        when(resourceService.getAll()).thenReturn(List.of(local, elsewhere));

        assertEquals(List.of("R1"),
                ids(find(service.collectionsFor("housing", "wilmington-de"), "get-help")));
    }

    private static FAQ faq(String id, String title) {
        FAQ f = new FAQ();
        f.id = id;
        f.title = title;
        f.categoryTags = List.of("Housing");
        return f;
    }

    private static List<String> ids(CategoryCollection collection) {
        return collection == null ? List.of() : collection.items().stream().map(ContentItem::id).toList();
    }

    private static Flyer flyer(String id, String title, String eventDate) {
        Flyer f = new Flyer();
        f.id = id;
        f.title = title;
        f.categoryTags = List.of("Housing");
        f.eventDate = eventDate;
        return f;
    }

    @Test
    void shouldPlaceFlyersInTheirOwnCollectionRatherThanFoldingThemIntoPolicyInfo() {
        // A flyer stays a flyer — never reshaped into a text notice to fit a
        // neighbouring collection's format.
        when(flyerService.getAll()).thenReturn(List.of(flyer("FL-2", "Know Your Rights", "2026-09-18")));

        List<CategoryCollection> collections = housing();

        assertEquals(List.of("FL-2"), ids(find(collections, "flyers")));
        assertEquals(null, find(collections, "policy-info"));
    }
}
