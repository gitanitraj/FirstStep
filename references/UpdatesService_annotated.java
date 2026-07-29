/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../updates/service/UpdatesService.java
 * Homepage-redesign Step 5b (Important Updates). See references/decisions.md
 * Decision 019. Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS CLASS IS
 *   The single server-side aggregator for the homepage "Important Updates" feed.
 *   It fans out to three existing services (curated News, live RSS, Flyers),
 *   normalizes every item into one flat display DTO (UpdateItem), and returns a
 *   date-sorted, capped list. GET /api/updates just calls getUpdates().
 *
 * WHY IT EXISTS (the governing principle — Decision 019)
 *   "Backend aggregates & normalizes; frontend only displays." The earlier plan
 *   had the browser calling /api/news/rss + /api/flyers and merging/sorting them
 *   in JavaScript. The user rejected that: cross-type merging, date selection,
 *   and source/url resolution belong on the server so the client renders a single
 *   uniform shape. This class is where that merging now lives.
 *
 * COMPANION FILES (trivial — no separate annotated mirror)
 *   - updates/dto/UpdateItem.java: a Java `record` with the display fields
 *     (type,id,title,summary,date,source,url,urgency). Plain camelCase — NO
 *     @JsonProperty, because these are brand-new display fields, not the
 *     snake_case domain models (Resource/NewsItem/Flyer) they're derived from.
 *   - updates/controller/UpdatesController.java: boilerplate mirroring
 *     NewsController — @GetMapping("/updates") → ApiResponse.success(getUpdates()).
 * ============================================================================= */

package org.firststep.backend.updates.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.updates.dto.UpdateItem;
import org.springframework.stereotype.Service;

@Service
public class UpdatesService {

    // Feed cap. Keeps the homepage section short; 8 is enough to fill the column
    // without turning it into an archive. Applied AFTER sorting so we keep the 8
    // newest across all sources.
    private static final int MAX_ITEMS = 8;

    // Constructor injection of the three EXISTING services — no new repositories,
    // no duplicated data access. Note RssFeedSource is the INTERFACE (not the
    // concrete RssFeedService), matching how NewsController injects it; this keeps
    // the dependency swappable and made the pure unit test trivial (a one-line
    // lambda RssFeedSource).
    private final NewsService newsService;
    private final RssFeedSource rssFeedSource;
    private final FlyerService flyerService;

    public UpdatesService(NewsService newsService, RssFeedSource rssFeedSource, FlyerService flyerService) {
        this.newsService = newsService;
        this.rssFeedSource = rssFeedSource;
        this.flyerService = flyerService;
    }

    public List<UpdateItem> getUpdates() {
        List<UpdateItem> items = new ArrayList<>();

        // --- News: curated + live RSS, deduped by id ---------------------------
        // Curated news and RSS-derived news are BOTH NewsItem. They can overlap
        // (the same item curated AND arriving via RSS). A LinkedHashMap keyed by
        // id dedupes while preserving insertion order; putIfAbsent means the FIRST
        // seen wins — and we insert curated first, so a curated version beats its
        // RSS duplicate. (Items with a null id are skipped from dedupe to avoid a
        // null map key collapsing them all into one.)
        Map<String, NewsItem> newsById = new LinkedHashMap<>();
        for (NewsItem n : newsService.getAll()) {
            if (n.id != null) {
                newsById.putIfAbsent(n.id, n);
            }
        }
        for (NewsItem n : rssFeedSource.getRssItems()) {
            if (n.id != null) {
                newsById.putIfAbsent(n.id, n);
            }
        }
        for (NewsItem n : newsById.values()) {
            items.add(toUpdateItem(n));
        }

        // --- Flyers ------------------------------------------------------------
        for (Flyer f : flyerService.getAll()) {
            items.add(toUpdateItem(f));
        }

        // --- Sort newest-first, undated last -----------------------------------
        // Dates are strings in yyyy-MM-dd, so natural String order == chronological
        // order. reverseOrder() gives descending (newest first); nullsLast keeps
        // items with no usable date at the bottom rather than the top.
        items.sort(Comparator.comparing(UpdateItem::date,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // Trim to the cap. subList is a view, so copy it into a fresh ArrayList to
        // avoid leaking a reference to the backing list.
        return items.size() > MAX_ITEMS ? new ArrayList<>(items.subList(0, MAX_ITEMS)) : items;
    }

    // NewsItem → UpdateItem. contentSource can be null (defensive), so guard it
    // before reading name/url. `published` is the news date field.
    private UpdateItem toUpdateItem(NewsItem n) {
        ContentSource cs = n.contentSource;
        return new UpdateItem(
                "news",
                n.id,
                n.title,
                n.summary,
                n.published,
                cs != null ? cs.name : null,
                cs != null ? cs.url : null,
                n.urgency,
                // Editorial classification carried through verbatim (Decision 031)
                // so the Weekly Updates page can group by category server-side.
                // This is category_tags — NOT resourceTags, which stay descriptive
                // metadata for search, filtering and AI retrieval.
                n.tags);
    }

    // Flyer → UpdateItem. Flyers have NO `published` field, so the display date is
    // the event date when present, else the load/updated date. Flyers carry no
    // urgency and no external url, so those are null — and no categoryTags either:
    // a Flyer has no editorial classification field, and its own `tags` are content
    // descriptors, so promoting them here would silently mix metadata into
    // navigation (exactly the conflation Decision 031 removed from news).
    private UpdateItem toUpdateItem(Flyer f) {
        String date = f.eventDate != null ? f.eventDate : f.updatedDate;
        return new UpdateItem(
                "flyer",
                f.id,
                f.title,
                f.summary,
                date,
                f.organization,
                null,
                null,
                null);
    }
}
