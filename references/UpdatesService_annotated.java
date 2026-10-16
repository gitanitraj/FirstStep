/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../updates/service/UpdatesService.java
 * Homepage-redesign Step 5b (Important Updates), extended in Slice F5a.
 * See references/decisions.md Decisions 019, 032, 036.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS CLASS IS
 *   THE single server-side aggregator for cross-type "what has changed" feeds.
 *   It fans out to the content services, normalizes every item into one flat
 *   display DTO (UpdateItem), and returns a date-sorted, capped list.
 *
 *   Two feeds, ONE merger:
 *     getUpdates()      → the homepage's "Important Updates"  (GET /api/updates)
 *     getForCategory()  → a category page's "Stay Informed"   (Slice F5a)
 *
 * WHY IT EXISTS (the governing principle — Decision 019)
 *   "Backend aggregates & normalizes; frontend only displays." The earlier plan
 *   had the browser calling /api/news/rss + /api/flyers and merging/sorting them
 *   in JavaScript. The user rejected that: cross-type merging, date selection,
 *   and source/url resolution belong on the server so the client renders a single
 *   uniform shape. This class is where that merging now lives.
 *
 * COMPANION FILES
 *   - updates/dto/UpdateItem.java: see UpdateItem_annotated.java (gained a
 *     contentType field in F5a).
 *   - updates/controller/UpdatesController.java: boilerplate mirroring
 *     NewsController — @GetMapping("/updates") → ApiResponse.success(getUpdates()).
 * ============================================================================= */

package org.firststep.backend.updates.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.firststep.backend.category.model.CategoryDefinition;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.updates.dto.UpdateItem;
import org.springframework.stereotype.Service;

@Service
public class UpdatesService {

    // Homepage feed cap. Keeps the section short; 8 is enough to fill the column
    // without turning it into an archive. Applied AFTER sorting so we keep the 8
    // newest across all sources. The CATEGORY feed's cap is the caller's choice
    // (CategoryPageService passes 6) — different pages, different densities.
    private static final int MAX_ITEMS = 8;

    // Constructor injection of EXISTING services — no new repositories, no
    // duplicated data access. Note RssFeedSource is the INTERFACE (not the
    // concrete RssFeedService), matching how NewsController injects it; this keeps
    // the dependency swappable and makes the pure unit test a one-line lambda.
    //
    // F5a added three: ExpertAnswerService + FaqService (expert content belongs in
    // a category feed) and TaxonomyService (category scoping IS the taxonomy's
    // matchCategoryTags rule — this service must not invent its own).
    private final NewsService newsService;
    private final RssFeedSource rssFeedSource;
    private final FlyerService flyerService;
    private final ExpertAnswerService expertAnswerService;
    private final FaqService faqService;
    private final TaxonomyService taxonomyService;

    public UpdatesService(NewsService newsService, RssFeedSource rssFeedSource, FlyerService flyerService,
            ExpertAnswerService expertAnswerService, FaqService faqService, TaxonomyService taxonomyService) {
        this.newsService = newsService;
        this.rssFeedSource = rssFeedSource;
        this.flyerService = flyerService;
        this.expertAnswerService = expertAnswerService;
        this.faqService = faqService;
        this.taxonomyService = taxonomyService;
    }

    // The homepage feed. BEHAVIOURALLY UNCHANGED since Decision 020 — news + RSS
    // + flyers, no expert content, no category filter, capped at 8. Verified live:
    // /api/updates still returns 8 items and no EXPERT content.
    public List<UpdateItem> getUpdates() {
        List<UpdateItem> items = new ArrayList<>();
        newsAndLegislation().forEach(n -> items.add(toUpdateItem(n)));
        flyerService.getAll().forEach(f -> items.add(toUpdateItem(f)));
        return sortAndCap(items, MAX_ITEMS);
    }

    // One category's feed (F5a). Adds expert content to the mix, because a
    // resident asking "what has changed in Housing?" is served by a housing
    // counselor's answer in a way the homepage's urgency-oriented feed is not.
    //
    // An unknown key yields an EMPTY FEED rather than an exception: the caller
    // (CategoryPageService) has already established whether the category exists,
    // via the navigation read model. Throwing here would make this service a
    // second authority on that question.
    public List<UpdateItem> getForCategory(String categoryKey, String communityId, int limit) {
        CategoryDefinition definition = taxonomyService.findByKey(categoryKey).orElse(null);
        if (definition == null) {
            return List.of();
        }

        List<UpdateItem> items = new ArrayList<>();
        for (NewsItem n : newsAndLegislation()) {
            if (matches(definition, n, communityId)) {
                items.add(toUpdateItem(n));
            }
        }
        for (Flyer f : flyerService.getAll()) {
            if (matches(definition, f, communityId)) {
                items.add(toUpdateItem(f));
            }
        }
        for (ExpertAnswer e : expertAnswerService.getAll()) {
            if (matches(definition, e, communityId)) {
                items.add(toUpdateItem(e));
            }
        }
        for (FAQ f : faqService.getAll()) {
            if (matches(definition, f, communityId)) {
                items.add(toUpdateItem(f));
            }
        }
        return sortAndCap(items, limit);
    }

    // Curated news + live RSS, deduped by id. Both are NewsItem and they can
    // overlap (the same item curated AND arriving via RSS). A LinkedHashMap keyed
    // by id dedupes while preserving insertion order; putIfAbsent means the FIRST
    // seen wins — and curated goes in first, so a curated version beats its RSS
    // duplicate. Items with a null id are skipped from dedupe to avoid a null map
    // key collapsing them all into one.
    //
    // Extracted in F5a so both feeds share it rather than one copying the other.
    private List<NewsItem> newsAndLegislation() {
        Map<String, NewsItem> byId = new LinkedHashMap<>();
        for (NewsItem n : newsService.getAll()) {
            if (n.id != null) {
                byId.putIfAbsent(n.id, n);
            }
        }
        for (NewsItem n : rssFeedSource.getRssItems()) {
            if (n.id != null) {
                byId.putIfAbsent(n.id, n);
            }
        }
        return new ArrayList<>(byId.values());
    }

    // Scoping reads EDITORIAL CLASSIFICATION only — the same matchesCategoryTags
    // rule CategoryService and NavigationService use. It never reads text, never
    // reads `tags`. Handed an unclassified item it matches nothing, exactly like
    // the navigation read model: no category, no placement.
    private boolean matches(CategoryDefinition definition, CivicContent item, String communityId) {
        if (communityId != null && !communityId.isBlank() && !communityId.equals(item.communityId)) {
            return false;
        }
        return taxonomyService.matchesCategoryTags(definition, item.categoryTags);
    }

    // Newest first, undated last. Dates are strings in yyyy-MM-dd, so natural
    // String order == chronological order; reverseOrder() gives descending and
    // nullsLast keeps undated items at the bottom rather than the top. subList is
    // a view, so copy into a fresh ArrayList rather than leaking the backing list.
    private static List<UpdateItem> sortAndCap(List<UpdateItem> items, int limit) {
        items.sort(Comparator.comparing(UpdateItem::date,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return items.size() > limit ? new ArrayList<>(items.subList(0, limit)) : items;
    }

    // NewsItem → UpdateItem. contentSource can be null (defensive), so guard it
    // before reading name/url. `publishDate` is the news date field.
    private UpdateItem toUpdateItem(NewsItem n) {
        ContentSource cs = n.contentSource;
        return new UpdateItem(
                "news",
                // NEWS for curated items, LAW for signed legislation. The NewsItem
                // already knows which it is (RssFeedService stamps LAW at
                // ingestion), so NOTHING IS INFERRED HERE — this reads a field.
                n.contentType,
                n.id,
                n.title,
                n.summary,
                n.publishDate,
                cs != null ? cs.name : null,
                cs != null ? cs.url : null,
                n.urgency,
                // Editorial classification carried through verbatim so a page can
                // group by category server-side. This is categoryTags — NOT `tags`,
                // which are descriptive metadata for search, filtering and AI.
                n.categoryTags);
    }

    // Flyer → UpdateItem. Flyers have NO `publishDate`, so the display date is the
    // event date when present, else the load/updated date. Flyers carry no urgency
    // and no external url, so those stay null. categoryTags IS populated: Decision
    // 032 gave flyers their own editorial classification, so there is a real field
    // to read — their descriptive `tags` are still ignored here.
    private UpdateItem toUpdateItem(Flyer f) {
        String date = f.eventDate != null ? f.eventDate : f.updatedDate;
        return new UpdateItem(
                "flyer",
                f.contentType,
                f.id,
                f.title,
                f.summary,
                date,
                f.organization,
                null,
                null,
                f.categoryTags);
    }

    // ExpertAnswer → UpdateItem (F5a). sessionDate is WHEN THE EXPERT SPOKE, which
    // is the editorially meaningful date; updatedDate is a load-date proxy and is
    // used only as a sort fallback, never displayed as "last updated".
    private UpdateItem toUpdateItem(ExpertAnswer e) {
        return new UpdateItem(
                "expert",
                e.contentType,
                e.id,
                e.title,
                e.summary,
                e.sessionDate != null ? e.sessionDate : e.updatedDate,
                e.expertOrganization,
                null,
                null,
                e.categoryTags);
    }

    // FAQ → UpdateItem (F5a). An FAQ has no editorial date of its own — it is
    // distilled from an ExpertAnswer rather than published on a day — so the load
    // date is all there is. Both ExpertAnswer and FAQ report ContentType.EXPERT,
    // so the UI badges them identically, which is correct: to a resident they are
    // the same kind of thing.
    private UpdateItem toUpdateItem(FAQ f) {
        ContentSource cs = f.contentSource;
        return new UpdateItem(
                "expert",
                f.contentType,
                f.id,
                f.title,
                f.summary,
                f.updatedDate,
                cs != null ? cs.name : null,
                null,
                null,
                f.categoryTags);
    }
}

// =============================================================================
// SLICE F5a UPDATE (Decision 036) — TWO FEEDS, ONE MERGER
// =============================================================================
// WHY THE CATEGORY FEED LIVES HERE rather than in CategoryPageService.
//
// This class's own javadoc has claimed since Decision 019 that it is "the single
// place cross-type 'latest updates' merging happens". Writing a second merger in
// the category BFF would have contradicted a documented invariant to save about
// ten lines of loop. It already composed News + RSS + Flyer; F5a added Expert and
// a category filter to the service that owns the job.
//
// The reuse that actually matters is not the loop — it is the private
// toUpdateItem mappers. Date selection (event date vs load date, session date vs
// load date), null-safe contentSource handling, and the tags/categoryTags
// distinction are the fiddly parts, and they now have one implementation each.
//
// THE HOMEPAGE FEED IS UNCHANGED. getUpdates() has the same sources, the same
// cap and no category filter. Expert content is deliberately absent from it: the
// homepage is an urgency feed ("what should I know right now?"), a category
// page's is a change feed ("what happened in Housing?"). Same shaping, different
// questions. Pinned by shouldNotIncludeExpertContentInTheHomepageFeed.
//
// RESOURCES ARE ABSENT FROM BOTH, and this is the load-bearing exclusion. A
// resource is a standing service, not an event. It is also what makes the feed
// complement topic navigation EXACTLY: every content type here carries a category
// and (almost always) no subcategory, so these are precisely the items topic
// tiles cannot reach. Measured across all ten categories,
// browse ∪ updates == totalCount.
//
// THE F2.1 FEED SPLIT PAYS OFF AGAIN. Legislation arrives via RssFeedSource — the
// relevance-gated, CLASSIFIED feed. SignedLegislationSource (the ungated rotator
// feed) could not serve a category feed even if asked, because an unclassified
// bill has no category to be scoped to. A split made for one reason turned out to
// be load-bearing for another.
// =============================================================================

// =============================================================================
// SLICE F1 UPDATE (Decision 032) — FLYERS NOW CARRY CLASSIFICATION TOO
// =============================================================================
// Three small changes, one of which reverses a Decision-031 conclusion:
//
//   n.published  -> n.publishDate     (contract field rename)
//   n.tags       -> n.categoryTags    (the field that actually holds editorial
//                                      classification now — `tags` became
//                                      descriptive metadata)
//   flyer null   -> f.categoryTags    (REVERSAL, below)
//
// THE REVERSAL. Decision 031 passed null for a Flyer's categoryTags and said
// why: "A Flyer has no editorial classification field, and its own tags are
// content descriptors, so promoting them would re-introduce exactly the
// conflation this decision removes."
//
// That reasoning was correct and the conclusion is now obsolete — because the
// premise stopped being true. Slice F1 gave flyers real category_tags in
// flyers.json, so there is now a genuine editorial field to read. Nothing is
// being promoted from descriptive tags; the descriptive tags are still ignored
// here, exactly as 031 intended.
//
// Worth naming the pattern: 031's rule was "don't fake classification from
// descriptive data". F1 satisfies that rule by ADDING the missing data rather
// than by relaxing the rule.
//
// STILL TRUE, AND STILL THE POINT: this service reads `categoryTags` and never
// `tags`. A flyer's descriptive tags ("Free", "Community", "Youth") play no part
// in grouping or in category scoping.
// =============================================================================

// =============================================================================
// SLICE I — getBySector(Sector) and getPage(Sector)
// =============================================================================
// The service gained a THIRD question. It already answered "what changed?" for
// the homepage (getUpdates) and "what changed in Housing?" for a category page
// (getForCategory); it now answers "what did GOVERNMENT publish?".
//
// getBySector walks the same four sources and asks one thing of each item:
//
//     contentSources.isInSector(item.contentSource, sector)
//
// It NEVER branches on contentType. Wilmington Housing Authority publishes both a
// news item and a flyer and both are government — so "flyers are community"
// would have been wrong. Sector is a property of the PRODUCER (Decision 045).
//
// Unresolvable ids are excluded, never guessed: isInSector is false for every
// sector, so such an item is on neither page while remaining valid CivicContent
// everywhere else. No cap — a destination is not a teaser.
//
// getPage adds the grouping, SERVER-SIDE, generated from the content present:
// a group is created the first time a type is met, so an empty group is never
// built and therefore never rendered (Decision 045's constraint, guaranteed by
// the payload rather than by a frontend guard). Group order follows the enum;
// item order within a group stays reverse-chronological.
//
// It lives here rather than in an UpdatesPageService because there is nothing to
// COMPOSE — one source, shaped. F4 refused a service for exactly this; F5a added
// one only once three sources had to be merged.
//
// UpdatesService is still "the single place cross-type merging happens"
// (Decision 036). Slice I added a filter and a projection, not a second merger.
// =============================================================================
