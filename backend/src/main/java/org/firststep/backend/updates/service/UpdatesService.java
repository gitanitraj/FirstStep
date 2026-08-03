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

/**
 * Aggregates cross-type "what has changed" feeds: curated News + live RSS
 * legislation + Flyers + Expert content, normalized to {@link UpdateItem} and
 * sorted newest-first.
 *
 * This is the single place cross-type "latest updates" merging happens. Two
 * feeds, one merger:
 *
 * <ul>
 *   <li>{@link #getUpdates()} — the homepage's "Important Updates", everything
 *       recent regardless of category.</li>
 *   <li>{@link #getForCategory} — a category page's "Stay Informed", scoped to
 *       one editorial category (Slice F5a).</li>
 * </ul>
 *
 * <p><b>Resources are deliberately absent from both.</b> A resource is a standing
 * service, not an event — it belongs to browsing, not to "what changed". That is
 * also why this feed complements topic navigation so exactly: every content type
 * here carries a category and no subcategory, so these are precisely the items
 * topic tiles cannot reach.
 *
 * <p><b>RSS legislation arrives via {@link RssFeedSource}</b> — the relevance-
 * gated, classified feed. The ungated {@code SignedLegislationSource} that drives
 * the homepage rotator could not serve a category feed even if asked: an
 * unclassified bill has no category to be scoped to.
 */
@Service
public class UpdatesService {

    private static final int MAX_ITEMS = 8;

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

    /**
     * The homepage feed. Unchanged since Decision 020 — news + RSS + flyers, no
     * expert content, no category filter, capped at 8.
     */
    public List<UpdateItem> getUpdates() {
        List<UpdateItem> items = new ArrayList<>();
        newsAndLegislation().forEach(n -> items.add(toUpdateItem(n)));
        flyerService.getAll().forEach(f -> items.add(toUpdateItem(f)));
        return sortAndCap(items, MAX_ITEMS);
    }

    /**
     * One category's feed. Adds expert content to the mix, because a resident
     * asking "what has changed in Housing?" is served by a housing counselor's
     * answer in a way the homepage's urgency-oriented feed is not.
     *
     * <p>Scoping reads editorial classification only — the same
     * {@code matchesCategoryTags} rule CategoryService and NavigationService use.
     * An unknown category key yields an empty feed rather than an error; the
     * caller has already decided whether the key exists.
     */
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

    /**
     * Curated news + live RSS. Both are NewsItems; dedupe by id in case a curated
     * item and an RSS item share one (curated wins by insertion order).
     */
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

    private boolean matches(CategoryDefinition definition, CivicContent item, String communityId) {
        if (communityId != null && !communityId.isBlank() && !communityId.equals(item.communityId)) {
            return false;
        }
        return taxonomyService.matchesCategoryTags(definition, item.categoryTags);
    }

    /** Newest first by the display date; items without a date sort last. */
    private static List<UpdateItem> sortAndCap(List<UpdateItem> items, int limit) {
        items.sort(Comparator.comparing(UpdateItem::date,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return items.size() > limit ? new ArrayList<>(items.subList(0, limit)) : items;
    }

    private UpdateItem toUpdateItem(NewsItem n) {
        ContentSource cs = n.contentSource;
        return new UpdateItem(
                "news",
                // NEWS for curated items, LAW for signed legislation — the NewsItem
                // itself already knows which it is, so nothing is inferred here.
                n.contentType,
                n.id,
                n.title,
                n.summary,
                n.publishDate,
                cs != null ? cs.name : null,
                cs != null ? cs.url : null,
                n.urgency,
                n.categoryTags);
    }

    private UpdateItem toUpdateItem(Flyer f) {
        // Flyers have no `publishDate`; prefer the event date, else the load date.
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
                // Flyers now carry editorial classification like every other
                // CivicContent type (Decision 032), so this is no longer null —
                // the Weekly Updates page can group them server-side.
                f.categoryTags);
    }

    private UpdateItem toUpdateItem(ExpertAnswer e) {
        return new UpdateItem(
                "expert",
                e.contentType,
                e.id,
                e.title,
                e.summary,
                // When the expert actually spoke. Falls back to the load date, which
                // is a proxy — acceptable for sorting, which is all it is used for.
                e.sessionDate != null ? e.sessionDate : e.updatedDate,
                e.expertOrganization,
                null,
                null,
                e.categoryTags);
    }

    private UpdateItem toUpdateItem(FAQ f) {
        ContentSource cs = f.contentSource;
        return new UpdateItem(
                "expert",
                f.contentType,
                f.id,
                f.title,
                f.summary,
                // An FAQ has no editorial date of its own — it is distilled from an
                // ExpertAnswer rather than published on a day.
                f.updatedDate,
                cs != null ? cs.name : null,
                null,
                null,
                f.categoryTags);
    }
}
