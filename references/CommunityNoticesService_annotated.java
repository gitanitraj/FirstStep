/*
 * WHAT: Complete annotated IM-10 mirror of backend/src/main/java/org/firststep/backend/notices/service/CommunityNoticesService.java.
 * WHY: Explicit occurrence eligibility must not expire Flyer media or alter canonical identity.
 * INTERACTION: Events alone intersects the injected-clock assessment with current inventory,
 * controlled notice kind, producer sector and optional exact Community scope; DTOs allowlist facts.
 * ALTERNATIVES: Prose-derived end times, global Flyer cutoffs and a new Event entity/type were rejected.
 * Tests separately prove nanosecond/end-of-day/DST boundaries, supporting visibility and privacy.
 */
package org.firststep.backend.notices.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.time.Instant;

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
    private final EventOccurrence occurrence;

    public CommunityNoticesService(FlyerService flyerService, NewsService newsService,
            TaxonomyService taxonomyService, ContentSourceService contentSources, EventOccurrence occurrence) {
        this.flyerService = flyerService;
        this.newsService = newsService;
        this.taxonomyService = taxonomyService;
        this.contentSources = contentSources;
        this.occurrence = occurrence;
    }

    public CommunityNoticesPage getPage(NoticeView view) { return getPage(view, null); }

    public CommunityNoticesPage getPage(NoticeView view, String communityId) {
        Map<NoticeView, List<ContentItem>> snapshot = new EnumMap<>(NoticeView.class);
        for (NoticeView v : NoticeView.discoveryViews()) snapshot.put(v, itemsFor(v, communityId));
        Map<NoticeView, Integer> counts = new EnumMap<>(NoticeView.class);
        for (NoticeView v : NoticeView.discoveryViews()) {
            counts.put(v, snapshot.get(v).size());
        }

        if (view == NoticeView.OVERVIEW) {
            List<CommunityNoticesPage.NoticePreview> previews = new ArrayList<>();
            for (NoticeView v : NoticeView.discoveryViews()) {
                List<ContentItem> all = snapshot.get(v);
                previews.add(new CommunityNoticesPage.NoticePreview(
                        v, all.size(), List.copyOf(all.subList(0, Math.min(PREVIEW_SIZE, all.size())))));
            }
            return new CommunityNoticesPage(view, counts, List.of(), List.copyOf(previews));
        }
        return new CommunityNoticesPage(view, counts, snapshot.get(view), List.of());
    }

    /**
     * One view's items, sorted the way that view is actually read.
     *
     * <p>Events additionally require explicit current/upcoming occurrence. Each sort choice
     * answers a resident question rather than a developer preference:
     *
     * <ul>
     *   <li><b>Events</b> — soonest first. "What is coming up?" A past event
     *       sorted to the top would be useless.</li>
     *   <li><b>Meetings</b> — retains the existing date order and independent membership.</li>
     *   <li><b>Announcements</b> — newest first. Nothing to attend; recency is
     *       the whole signal.</li>
     *   <li><b>Flyers</b> — soonest first, because a flyer is a poster FOR
     *       something and that something has a date.</li>
     * </ul>
     */
    private List<ContentItem> itemsFor(NoticeView view, String communityId) {
        Map<String, Instant> eventOrder = new LinkedHashMap<>();
        List<ContentItem> items = new ArrayList<>();

        for (Flyer f : flyerService.getAll()) {
            if (belongs(f, view, communityId)) {
                items.add(toContentItem(f, view));
                if (view == NoticeView.EVENTS) eventOrder.putIfAbsent(f.id, occurrence.orderTime(f));
            }
        }
        for (NewsItem n : newsService.getAll()) {
            if (belongs(n, view, communityId)) {
                items.add(toContentItem(n, view));
                if (view == NoticeView.EVENTS) eventOrder.putIfAbsent(n.id, occurrence.orderTime(n));
            }
        }

        Comparator<ContentItem> byDate = Comparator.comparing(
                ContentItem::date, Comparator.nullsLast(Comparator.naturalOrder()));
        items.sort(view == NoticeView.EVENTS
                ? Comparator.comparing((ContentItem i) -> eventOrder.get(i.id())).thenComparing(ContentItem::id)
                : view == NoticeView.ANNOUNCEMENTS ? byDate.reversed() : byDate);
        Map<String, ContentItem> unique = new LinkedHashMap<>();
        for (ContentItem item : items) if (item.id() != null) unique.putIfAbsent(item.id(), item);
        return List.copyOf(unique.values());
    }

    /**
     * Does this content belong in this view?
     *
     * <p>Sector remains mandatory: a government flyer is not a community notice no
     * matter what it is tagged. Then the view's own selector — a kind for three
     * of them, contentType for Flyers.
     *
     * <p>Unresolvable producers are excluded by {@code isInSector} returning false
     * for every sector, which is the Slice I failure boundary doing its job here
     * unchanged: such an item stays valid CivicContent everywhere else.
     */
    private boolean belongs(CivicContent content, NoticeView view, String communityId) {
        if (communityId != null && !communityId.equals(content.communityId)) return false;
        if (view == NoticeView.EVENTS && (!(content.contentType == org.firststep.backend.shared.model.ContentType.FLYER
                || content.contentType == org.firststep.backend.shared.model.ContentType.NEWS)
                || occurrence.assess(content).state() != EventOccurrence.State.CURRENT_OR_UPCOMING)) return false;
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

    private ContentItem toContentItem(Flyer f, NoticeView view) {
        return new ContentItem(
                f.contentType, f.id, f.title, f.summary,
                f.organization,
                null, null, null,
                view == NoticeView.EVENTS ? occurrence.displayDate(f) : f.eventDate,
                f.contentSource != null ? f.contentSource.url : null,
                // The gallery's whole reason to exist. Resolved by FlyerService,
                // which owns the encoding rule.
                flyerService.imageUrlFor(f));
    }

    private ContentItem toContentItem(NewsItem n, NoticeView view) {
        return new ContentItem(
                n.contentType, n.id, n.title, n.summary,
                n.contentSource != null ? n.contentSource.name : null,
                null, null, n.urgency,
                view == NoticeView.EVENTS ? occurrence.displayDate(n) : n.publishDate,
                n.contentSource != null ? n.contentSource.url : null,
                null);   // news carries no image
    }
}
