/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../category/service/CategoryContentService.java
 * Category destination. See references/decisions.md Decision 051.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS CLASS IS
 *   A category's content, resolved once and shaped into the destination's
 *   collections. It is the widest content reader in the codebase — seven
 *   sources — and deliberately so: it exists precisely to be the one place that
 *   knows how to turn any CivicContent into a ContentItem.
 *
 * WHY IT EXISTS (the F4/F5a rule, third application)
 *   TopicPageService already had private CivicContent -> ContentItem mappers.
 *   The destination needed the same mapping over more sources. TWO callers, one
 *   behaviour: that is the second use an abstraction needs before it earns its
 *   name. The same rule that REFUSED CategoryPageService in F4 and CREATED it
 *   in F5a creates this one now.
 *
 *   And it cuts the other way in the same file: there is no separate
 *   "composition service", because that would have one caller and would forward
 *   a call.
 *
 * THE COLLECTIONS ARE NOT A ContentType PROJECTION
 *   Policy Info and First Rate News are BOTH ContentType.NEWS. The two
 *   supporting collections are BOTH ContentType.EXPERT. If you read the
 *   collection list expecting one per enum value you will misread it entirely.
 *   The set is chosen because each answers a resident question.
 *
 * THE THREE-WAY SPLIT OF `NEWS`
 *   news + rss are merged, deduplicated, then partitioned by contentType and
 *   producer:
 *     contentType == LAW                  -> the Law collection
 *     contentType == NEWS, external       -> Policy Info
 *     approved Article (a separate source) -> First Rate News
 *   Policy Info means EXTERNAL civic information; First Step's own byline
 *   appearing there would misrepresent who is speaking. That is why the
 *   `isFirstStep` filter is definitional rather than defensive — it encodes
 *   what the collection MEANS, and it holds even though no first-step news
 *   record exists today.
 *
 * WHY A CURATED FAQ IS NOT FIRST RATE NEWS
 *   FAQ-002 carries `contentSource.id = "first-step"` and is still not First
 *   Rate News. The collection means First Step's JOURNALISM — researched,
 *   reported, written — not every record whose producer id happens to be ours.
 *   A test pins this, because it is exactly the rule a future reader will
 *   "simplify" into a one-line source check.
 *
 * ARTICLES AND THE REVIEW GATE (Decision 052)
 *   Reached through ArticleService.getPublishable() and never the repository,
 *   so the Decision 048 boundary travels WITH THE QUERY. That is the whole
 *   point of putting the rule there: a seventh consumer is safe by
 *   construction, and this service cannot publish a draft by forgetting a
 *   condition. Their admission to category navigation was its own decision —
 *   the review gate controls publication, not placement.
 *
 * ORDERING — WHERE AUTHORED ORDER SURVIVES
 *   Three different orders, each for a stated reason, none of them arbitrary:
 *     resources        alphabetical  — no editorial date exists; updatedDate is
 *                                      a load-date proxy and must not imply
 *                                      recency
 *     dated content    newest first  — the UpdatesService precedent
 *     supporting       FILE ORDER    — a curated question list is sequenced by
 *                                      whoever curated it, and that ordering
 *                                      carries meaning no derived sort improves
 *   The governing rule: do not introduce explicit presentation ordering unless
 *   the composition requires it; where an authored order has semantic meaning,
 *   preserve it; otherwise use a deterministic derived order. Resident-facing
 *   ordering is NOT data ordering — CategoryPage.tsx's TYPE_ORDER constant is
 *   the codebase's own proof, since the ContentType enum order was wrong for
 *   residents and a presentation order had to be authored beside it.
 * ============================================================================= */

package org.firststep.backend.category.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.firststep.backend.category.dto.CategoryCollection;
import org.firststep.backend.category.dto.CollectionLayer;
import org.firststep.backend.category.model.CategoryDefinition;
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
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.model.Location;
import org.firststep.backend.shared.model.Website;
import org.springframework.stereotype.Service;

/**
 * A category's content, resolved once and shaped for the destination page
 * (Decision 051).
 *
 * <h2>Why this service exists</h2>
 *
 * <p>Two callers needed the same thing and neither owned it. {@link TopicPageService}
 * had private {@code CivicContent -> ContentItem} mappers; the category destination
 * needs the same mapping over a wider set of sources. That is the second use an
 * abstraction needs before it earns its name — the F4/F5a rule applied again.
 *
 * <h2>What it does and does not decide</h2>
 *
 * <p>It reads <b>editorial classification only</b> — {@code categoryTags} for a
 * category, plus {@code subcategory} for a topic. No text matching, no tags, no
 * inference. Classification is an ingestion concern; by the time a request arrives
 * there is nothing left to decide.
 *
 * <p>It owns the <b>composition</b> — which collections exist and in what order —
 * because the F4/F5a rule cuts the other way here: a separate composition service
 * would forward a call. Composition is declared in Java and moves to a presentation
 * artifact only when a second destination demands genuinely different configuration
 * (Decision 051). {@code navigation.json} is not extended; navigation configuration
 * and destination composition are separate concerns.
 *
 * <h2>The collections are not a projection of ContentType</h2>
 *
 * <pre>
 *   get-help          ACTION       RESOURCE
 *   policy-info       INFORMATION  NEWS, external producers only
 *   law               INFORMATION  LAW
 *   flyers            INFORMATION  FLYER
 *   first-rate-news   INFORMATION  approved Articles
 *   faqs              SUPPORTING   FAQ
 *   expert-qa         SUPPORTING   ExpertAnswer
 * </pre>
 *
 * <p>Policy Info and First Rate News are both {@code NEWS}; the two supporting
 * collections are both {@code EXPERT}. The set is chosen because each answers a
 * resident question, <b>not</b> because the enum has five values — and it therefore
 * does not partition the category. That is deliberate: the destination provides
 * discovery pathways, not an exhaustive accounting.
 *
 * <h2>Articles</h2>
 *
 * <p>Reached through {@link ArticleService#getPublishable()} and never the
 * repository, so the Decision 048 review boundary travels with the query and this
 * service cannot publish a draft by forgetting a condition. Their admission to
 * category navigation is Decision 052: the review gate controls publication, not
 * placement.
 */
@Service
public class CategoryContentService {

    /** The producer id that marks content First Step published itself. */
    private static final String FIRST_STEP = "first-step";

    /**
     * A browse list wants a predictable order a resident can scan, and resources
     * have no editorial date to sort by — {@code updatedDate} is a load-date proxy
     * and must not imply recency.
     */
    private static final Comparator<ContentItem> ALPHABETICAL =
            Comparator.comparing(ContentItem::title, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    /** Dated collections lead with the most recent; undated items sink to the end. */
    private static final Comparator<ContentItem> NEWEST_FIRST =
            Comparator.comparing(ContentItem::date, Comparator.nullsLast(Comparator.reverseOrder()));

    private final TaxonomyService taxonomyService;
    private final ResourceService resourceService;
    private final NewsService newsService;
    private final RssFeedSource rssFeedSource;
    private final FlyerService flyerService;
    private final ExpertAnswerService expertAnswerService;
    private final FaqService faqService;
    private final ArticleService articleService;

    public CategoryContentService(TaxonomyService taxonomyService, ResourceService resourceService,
            NewsService newsService, RssFeedSource rssFeedSource, FlyerService flyerService,
            ExpertAnswerService expertAnswerService, FaqService faqService, ArticleService articleService) {
        this.taxonomyService = taxonomyService;
        this.resourceService = resourceService;
        this.newsService = newsService;
        this.rssFeedSource = rssFeedSource;
        this.flyerService = flyerService;
        this.expertAnswerService = expertAnswerService;
        this.faqService = faqService;
        this.articleService = articleService;
    }

    /**
     * The destination's collections, in composition order, with empty ones absent.
     *
     * <p>Returns an empty list for a key the taxonomy does not know — existence is
     * the navigation read model's answer to give, and this service does not
     * duplicate that judgement.
     */
    public List<CategoryCollection> collectionsFor(String categoryKey, String communityId) {
        CategoryDefinition definition = taxonomyService.findByKey(categoryKey).orElse(null);
        if (definition == null) {
            return List.of();
        }

        List<NewsItem> news = newsInCategory(definition, communityId);

        List<CategoryCollection> collections = new ArrayList<>();
        add(collections, CategoryCollection.of("get-help", "collection.getHelp",
                CollectionLayer.ACTION, resources(definition, communityId)));
        add(collections, CategoryCollection.of("policy-info", "collection.policyInfo",
                CollectionLayer.INFORMATION, policyInfo(news)));
        add(collections, CategoryCollection.of("law", "collection.law",
                CollectionLayer.INFORMATION, law(news)));
        add(collections, CategoryCollection.of("flyers", "collection.flyers",
                CollectionLayer.INFORMATION, flyers(definition, communityId)));
        add(collections, CategoryCollection.of("first-rate-news", "collection.firstRateNews",
                CollectionLayer.INFORMATION, firstRateNews(definition, communityId)));
        add(collections, CategoryCollection.of("faqs", "collection.faqs",
                CollectionLayer.SUPPORTING, faqs(definition, communityId)));
        add(collections, CategoryCollection.of("expert-qa", "collection.expertQa",
                CollectionLayer.SUPPORTING, expertAnswers(definition, communityId)));
        return List.copyOf(collections);
    }

    /**
     * Every item under one topic, alphabetically. Only resources and flyers carry a
     * {@code subcategory}, so only they can ever appear — news, legislation and
     * expert content are fully classified with a category and no topic, and reaching
     * them is the category page's job, not this one's.
     */
    public List<ContentItem> forTopic(CategoryDefinition definition, String topic, String communityId) {
        List<ContentItem> items = new ArrayList<>();
        for (Resource r : resourceService.getAll()) {
            if (inCategory(definition, r, communityId) && topic.equalsIgnoreCase(r.subcategory)) {
                items.add(toContentItem(r));
            }
        }
        for (Flyer f : flyerService.getAll()) {
            if (inCategory(definition, f, communityId) && topic.equalsIgnoreCase(f.subcategory)) {
                items.add(toContentItem(f));
            }
        }
        items.sort(ALPHABETICAL);
        return List.copyOf(items);
    }

    // ---- collection builders --------------------------------------------

    private List<ContentItem> resources(CategoryDefinition definition, String communityId) {
        return resourceService.getAll().stream()
                .filter(r -> inCategory(definition, r, communityId))
                .map(CategoryContentService::toContentItem)
                .sorted(ALPHABETICAL)
                .toList();
    }

    /**
     * Curated news and RSS items as one list, deduplicated by id with the curated
     * record winning — the same precedence {@code UpdatesService} applies, for the
     * same reason: a hand-written record carries editorial judgement a feed entry
     * does not.
     */
    private List<NewsItem> newsInCategory(CategoryDefinition definition, String communityId) {
        List<NewsItem> merged = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (NewsItem n : newsService.getAll()) {
            if (inCategory(definition, n, communityId) && seen.add(n.id)) {
                merged.add(n);
            }
        }
        for (NewsItem n : rssFeedSource.getRssItems()) {
            if (inCategory(definition, n, communityId) && seen.add(n.id)) {
                merged.add(n);
            }
        }
        return merged;
    }

    /**
     * External civic information — policy updates, official and community notices.
     *
     * <p>First Step's own reporting is deliberately excluded: it belongs to First
     * Rate News, and a collection defined as "external civic information" that
     * quietly carried First Step's byline would misrepresent who is speaking.
     */
    private static List<ContentItem> policyInfo(List<NewsItem> news) {
        return news.stream()
                .filter(n -> n.contentType == ContentType.NEWS)
                .filter(n -> !isFirstStep(n.contentSource))
                .map(CategoryContentService::toContentItem)
                .sorted(NEWEST_FIRST)
                .toList();
    }

    private static List<ContentItem> law(List<NewsItem> news) {
        return news.stream()
                .filter(n -> n.contentType == ContentType.LAW)
                .map(CategoryContentService::toContentItem)
                .sorted(NEWEST_FIRST)
                .toList();
    }

    private List<ContentItem> flyers(CategoryDefinition definition, String communityId) {
        return flyerService.getAll().stream()
                .filter(f -> inCategory(definition, f, communityId))
                .map(CategoryContentService::toContentItem)
                .sorted(NEWEST_FIRST)
                .toList();
    }

    /**
     * First Step's editorial collection.
     *
     * <p><b>Not every record whose producer is First Step.</b> The collection means
     * journalism and original editorial work First Step researched, reported or
     * wrote — approved {@link Article}s today. A curated FAQ also carries
     * {@code contentSource.id = "first-step"} and is NOT First Rate News; it is
     * supporting reference material with its own collection. Widening this rule to
     * make a total reconcile is precisely what Decision 051 rejected.
     */
    private List<ContentItem> firstRateNews(CategoryDefinition definition, String communityId) {
        return articleService.getPublishable().stream()
                .filter(a -> inCategory(definition, a, communityId))
                .map(CategoryContentService::toContentItem)
                .sorted(NEWEST_FIRST)
                .toList();
    }

    /**
     * Authored file order is preserved for both supporting collections. A curated
     * question list is sequenced by whoever curated it, and that ordering carries
     * meaning no derived sort would improve on.
     */
    private List<ContentItem> faqs(CategoryDefinition definition, String communityId) {
        return faqService.getAll().stream()
                .filter(f -> inCategory(definition, f, communityId))
                .map(CategoryContentService::toContentItem)
                .toList();
    }

    private List<ContentItem> expertAnswers(CategoryDefinition definition, String communityId) {
        return expertAnswerService.getAll().stream()
                .filter(e -> inCategory(definition, e, communityId))
                .map(CategoryContentService::toContentItem)
                .toList();
    }

    private static void add(List<CategoryCollection> collections, Optional<CategoryCollection> collection) {
        collection.ifPresent(collections::add);
    }

    // ---- classification --------------------------------------------------

    private boolean inCategory(CategoryDefinition definition, CivicContent item, String communityId) {
        if (communityId != null && !communityId.isBlank() && !communityId.equals(item.communityId)) {
            return false;
        }
        return taxonomyService.matchesCategoryTags(definition, item.categoryTags);
    }

    private static boolean isFirstStep(ContentSource source) {
        return source != null && FIRST_STEP.equals(source.id);
    }

    // ---- projections -----------------------------------------------------

    private static ContentItem toContentItem(Resource r) {
        return new ContentItem(
                r.contentType, r.id, r.title,
                // Resources carry both; the summary is the curated one-liner and
                // the description is the full text, so prefer the summary and
                // fall back rather than showing an empty card.
                r.summary != null ? r.summary : r.description,
                r.organization,
                firstCity(r.locations),
                r.cost,
                r.urgency,
                null,                       // no editorial date — see ALPHABETICAL
                firstWebsite(r.websites),
                null);                      // resources carry no image
    }

    private static ContentItem toContentItem(Flyer f) {
        return new ContentItem(
                f.contentType, f.id, f.title, f.summary,
                f.organization,
                null, null, null,
                f.eventDate,
                sourceUrl(f.contentSource),
                // Browse cards stay text — the flyer GALLERY is where the image is
                // the content (Slice J). Passing it here would change every browse
                // card's shape for one view's benefit.
                null);
    }

    private static ContentItem toContentItem(NewsItem n) {
        return new ContentItem(
                n.contentType, n.id, n.title, n.summary,
                producerName(n.contentSource),
                null, null,
                n.urgency,
                n.publishDate,
                sourceUrl(n.contentSource),
                null);
    }

    private static ContentItem toContentItem(Article a) {
        return new ContentItem(
                a.contentType, a.id, a.title, a.summary,
                // The byline, not the producer name: an article's visible credit is
                // who wrote it, and that is the one provenance question Article
                // answers publicly.
                a.byline,
                null, null, null,
                a.publishDate,
                // First Step HOSTS its articles rather than pointing at a provider,
                // so the reading surface is internal. The client owns URL shape, so
                // no href is manufactured here.
                null,
                null);
    }

    private static ContentItem toContentItem(FAQ f) {
        return new ContentItem(
                f.contentType, f.id, f.title, f.summary,
                producerName(f.contentSource),
                null, null, null,
                f.updatedDate,
                sourceUrl(f.contentSource),
                null);
    }

    private static ContentItem toContentItem(ExpertAnswer e) {
        return new ContentItem(
                e.contentType, e.id, e.title, e.summary,
                // The expert's organization, not the registry name: a resident
                // weighing an answer wants to know whose expertise it is.
                e.expertOrganization,
                null, null, null,
                e.sessionDate,
                sourceUrl(e.contentSource),
                null);
    }

    private static String producerName(ContentSource source) {
        return source == null ? null : source.name;
    }

    private static String sourceUrl(ContentSource source) {
        return source == null ? null : source.url;
    }

    /**
     * City only, never a street address. A browse card exists to help a resident
     * decide whether an item is near them; the full address belongs on a detail view
     * where the provider's own contact route is alongside it.
     */
    private static String firstCity(List<Location> locations) {
        if (locations == null) {
            return null;
        }
        return locations.stream()
                .filter(l -> l != null && l.city != null && !l.city.isBlank())
                .map(l -> l.city)
                .findFirst()
                .orElse(null);
    }

    private static String firstWebsite(List<Website> websites) {
        if (websites == null) {
            return null;
        }
        return websites.stream()
                .filter(w -> w != null && w.url != null && !w.url.isBlank())
                .map(w -> w.url)
                .findFirst()
                .orElse(null);
    }
}

// =============================================================================
// WHY COMPOSITION IS DECLARED HERE IN JAVA AND NOT IN A DATA FILE
// =============================================================================
// The order of the `add(...)` calls in collectionsFor IS the destination
// composition. It is code, not configuration, and that is deliberate.
//
// navigation.json was NOT extended: navigation configuration and destination
// presentation are separate concerns with separate lifecycles, and merging them
// would give one file two unrelated jobs. Nor was a new presentation data file
// created — "an abstraction earns its name at the second use", and a
// configuration format invented for its first and only consumer is a schema, a
// loader, a validator and a failure policy bought before anything needs them.
//
// The trigger to externalize is named rather than left to taste: a SECOND
// destination that needs a genuinely DIFFERENT composition. Community Events is
// that test. If it turns out to want the same collections in the same order,
// the right answer was code all along.
// =============================================================================

// =============================================================================
// WHY HOUSING AND COMMUNITY EVENTS LOOK DIFFERENT WITH NO PER-CATEGORY CODE
// =============================================================================
// Every category asks for the same seven collections in the same order. They
// render differently because CategoryCollection.of() refuses to build an empty
// one, so a category's shape falls out of its COUNTS rather than a template:
//
//   housing    get-help, policy-info, law, flyers, first-rate-news, faqs,
//              expert-qa
//   utilities  law only — no resources, no topics, nothing else classified
//
// This is worth stating because the design nearly acquired a per-topic
// arrangement config to achieve exactly what empty-suppression already gives
// for free.
// =============================================================================

// =============================================================================
// EIGHT DEPENDENCIES — WHEN A WIDE CONSTRUCTOR IS THE RIGHT ANSWER
// =============================================================================
// This is the widest constructor in the codebase and it should stay contained
// here. The alternative was CategoryPageService injecting all seven content
// services alongside its three existing collaborators, which would have made
// the page service know about every content type in the system just to hand
// them onward.
//
// The seam is honest: this class's single responsibility is "a category's
// content", and a class whose job is to see everything must be handed
// everything. What it must NOT grow is judgement — it classifies nothing,
// infers nothing, and applies no editorial rule. Classification is an ingestion
// concern; by the time a request arrives there is nothing left to decide.
// =============================================================================
