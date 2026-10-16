package org.firststep.backend.notices.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.notices.dto.CommunityNoticesPage;
import org.firststep.backend.notices.model.NoticeView;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.Sector;
import org.firststep.backend.shared.service.ContentSourceService;
import org.springframework.stereotype.Service;

/**
 * The Community Notices page, in any of its five states.
 *
 * <p><b>Community-produced information is not the same thing as community
 * resources.</b> A resource is a service a resident can use; a notice is
 * something an organisation is telling the neighbourhood. This page carries the
 * second, and everything it returns is scoped to {@link Sector#COMMUNITY} —
 * government content has its own destination in Latest Updates.
 *
 * <p><b>Four views, three kinds, one contentType.</b> Events, Meetings and
 * Announcements select on a controlled kind carried in {@code tags}; Flyers
 * selects on {@code contentType}. That asymmetry is deliberate — "flyer" is not a
 * kind of notice, it is a form a notice takes — and it is why the views OVERLAP
 * rather than partition. A health-fair flyer is in both Events and Flyers,
 * because a resident asking "what is happening?" and one asking "what posters are
 * up?" are asking different questions about the same item.
 *
 * <p>No new domain concept was introduced to make any of this work: the kind
 * vocabulary lives in taxonomy.json and rides in the existing {@code tags} field,
 * the same mechanism as the Seniors discovery tag (Decision 041's amendment).
 */
@Service
public class CommunityNoticesService {

    /** Items shown per view on the landing page. Enough to show the flavour, not the feed. */
    private static final int PREVIEW_SIZE = 3;

    private final FlyerService flyerService;
    private final NewsService newsService;
    private final TaxonomyService taxonomyService;
    private final ContentSourceService contentSources;

    public CommunityNoticesService(FlyerService flyerService, NewsService newsService,
            TaxonomyService taxonomyService, ContentSourceService contentSources) {
        this.flyerService = flyerService;
        this.newsService = newsService;
        this.taxonomyService = taxonomyService;
        this.contentSources = contentSources;
    }

    public CommunityNoticesPage getPage(NoticeView view) {
        Map<NoticeView, Integer> counts = new EnumMap<>(NoticeView.class);
        for (NoticeView v : NoticeView.discoveryViews()) {
            counts.put(v, itemsFor(v).size());
        }

        if (view == NoticeView.OVERVIEW) {
            List<CommunityNoticesPage.NoticePreview> previews = new ArrayList<>();
            for (NoticeView v : NoticeView.discoveryViews()) {
                List<ContentItem> all = itemsFor(v);
                previews.add(new CommunityNoticesPage.NoticePreview(
                        v, all.size(), List.copyOf(all.subList(0, Math.min(PREVIEW_SIZE, all.size())))));
            }
            return new CommunityNoticesPage(view, counts, List.of(), List.copyOf(previews));
        }
        return new CommunityNoticesPage(view, counts, itemsFor(view), List.of());
    }

    /**
     * One view's items, sorted the way that view is actually read.
     *
     * <p>The sort is the only place the views genuinely differ, and each choice
     * answers a resident question rather than a developer preference:
     *
     * <ul>
     *   <li><b>Events</b> — soonest first. "What is coming up?" A past event
     *       sorted to the top would be useless.</li>
     *   <li><b>Meetings</b> — soonest first, for the same reason. A meeting you
     *       can still attend beats one you missed.</li>
     *   <li><b>Announcements</b> — newest first. Nothing to attend; recency is
     *       the whole signal.</li>
     *   <li><b>Flyers</b> — soonest first, because a flyer is a poster FOR
     *       something and that something has a date.</li>
     * </ul>
     */
    private List<ContentItem> itemsFor(NoticeView view) {
        List<ContentItem> items = new ArrayList<>();

        for (Flyer f : flyerService.getAll()) {
            if (belongs(f, view)) {
                items.add(toContentItem(f));
            }
        }
        for (NewsItem n : newsService.getAll()) {
            if (belongs(n, view)) {
                items.add(toContentItem(n));
            }
        }

        Comparator<ContentItem> byDate = Comparator.comparing(
                ContentItem::date, Comparator.nullsLast(Comparator.naturalOrder()));
        items.sort(view == NoticeView.ANNOUNCEMENTS ? byDate.reversed() : byDate);
        return List.copyOf(items);
    }

    /**
     * Does this content belong in this view?
     *
     * <p>Sector first, always: a government flyer is not a community notice no
     * matter what it is tagged. Then the view's own selector — a kind for three
     * of them, contentType for Flyers.
     *
     * <p>Unresolvable producers are excluded by {@code isInSector} returning false
     * for every sector, which is the Slice I failure boundary doing its job here
     * unchanged: such an item stays valid CivicContent everywhere else.
     */
    private boolean belongs(CivicContent content, NoticeView view) {
        if (!contentSources.isInSector(content.contentSource, Sector.COMMUNITY)) {
            return false;
        }
        if (view == NoticeView.FLYERS) {
            return content.contentType == org.firststep.backend.shared.model.ContentType.FLYER;
        }
        return view.kind()
                .filter(kind -> taxonomyService.noticeKindOf(content.tags)
                        .filter(kind::equals).isPresent())
                .isPresent();
    }

    private ContentItem toContentItem(Flyer f) {
        return new ContentItem(
                f.contentType, f.id, f.title, f.summary,
                f.organization,
                null, null, null,
                f.eventDate,
                f.contentSource != null ? f.contentSource.url : null,
                // The gallery's whole reason to exist. Resolved by FlyerService,
                // which owns the encoding rule.
                flyerService.imageUrlFor(f));
    }

    private ContentItem toContentItem(NewsItem n) {
        return new ContentItem(
                n.contentType, n.id, n.title, n.summary,
                n.contentSource != null ? n.contentSource.name : null,
                null, null, n.urgency,
                n.publishDate,
                n.contentSource != null ? n.contentSource.url : null,
                null);   // news carries no image
    }
}
