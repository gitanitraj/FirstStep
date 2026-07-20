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

/**
 * Aggregates the homepage "Important Updates" feed: curated News + live RSS +
 * Flyers, normalized to {@link UpdateItem} and sorted newest-first.
 *
 * This is the single place cross-type "latest updates" merging happens — the
 * frontend polls GET /api/updates and just renders the result.
 */
@Service
public class UpdatesService {

    private static final int MAX_ITEMS = 8;

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

        // Curated news + live RSS. Both are NewsItems; dedupe by id in case a
        // curated item and an RSS item share one (curated wins by insertion order).
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

        for (Flyer f : flyerService.getAll()) {
            items.add(toUpdateItem(f));
        }

        // Newest first by the display date; items without a date sort last.
        items.sort(Comparator.comparing(UpdateItem::date,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return items.size() > MAX_ITEMS ? new ArrayList<>(items.subList(0, MAX_ITEMS)) : items;
    }

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
                n.urgency);
    }

    private UpdateItem toUpdateItem(Flyer f) {
        // Flyers have no `published`; prefer the event date, else the load date.
        String date = f.eventDate != null ? f.eventDate : f.updatedDate;
        return new UpdateItem(
                "flyer",
                f.id,
                f.title,
                f.summary,
                date,
                f.organization,
                null,
                null);
    }
}
