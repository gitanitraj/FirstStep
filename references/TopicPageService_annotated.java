/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../category/service/TopicPageService.java
 * Slice F6. See references/decisions.md Decision 040.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS CLASS IS
 *   The topic page's BFF — the FOURTH and final level of the navigation
 *   hierarchy (Decision 021): Category -> topic group -> topic -> CivicContent.
 *   This is where the content itself is finally listed.
 *
 * THE FACT THAT SHAPES IT
 *   Only RESOURCES (229/229) and FLYERS (7/7) carry a `subcategory`. News,
 *   signed legislation and expert answers carry none, so they can never appear
 *   on a topic page. That is not a limitation of this endpoint — it is the same
 *   measurement that made the CATEGORY page an aggregate (Decision 036), seen
 *   from the other side. Browse reaches what has a topic; the category page's
 *   updates feed reaches what does not. Together they cover everything.
 * ============================================================================= */

package org.firststep.backend.category.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.firststep.backend.category.dto.TopicMetadata;
import org.firststep.backend.category.dto.TopicPage;
import org.firststep.backend.category.model.CategoryDefinition;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.service.FlyerService;
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
 * The topic page's BFF (Slice F6) — the fourth level of the navigation
 * hierarchy, where CivicContent is finally listed.
 *
 * <p>Like every other read path here it reads <b>editorial classification
 * only</b>: an item appears under a topic when its {@code categoryTags} match the
 * category AND its {@code subcategory} equals the topic. No text matching, no
 * tags, no inference. Classification is an ingestion concern.
 *
 * <p><b>Why it composes ResourceService and FlyerService directly</b> rather than
 * going through NavigationService or UpdatesService: those answer "how many?" and
 * "what changed?". This one needs the items themselves, and only the two types
 * that carry a subcategory can ever appear.
 */
@Service
public class TopicPageService {

    private final TaxonomyService taxonomyService;
    private final ResourceService resourceService;
    private final FlyerService flyerService;

    public TopicPageService(TaxonomyService taxonomyService, ResourceService resourceService,
            FlyerService flyerService) {
        this.taxonomyService = taxonomyService;
        this.resourceService = resourceService;
        this.flyerService = flyerService;
    }

    /**
     * One topic, or empty when either the category key or the topic slug is
     * unknown. Both resolve through the taxonomy, so a topic that is not declared
     * under that category 404s even if another category declares it — which is
     * what makes "Eviction Prevention" two distinct pages under Housing and Legal
     * rather than one ambiguous page.
     */
    public Optional<TopicPage> getByKey(String categoryKey, String topicSlug, String communityId) {
        CategoryDefinition definition = taxonomyService.findByKey(categoryKey).orElse(null);
        if (definition == null) {
            return Optional.empty();
        }
        Optional<String> topic = taxonomyService.findTopicBySlug(categoryKey, topicSlug);
        if (topic.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(build(definition, topic.get(), topicSlug, communityId));
    }

    private TopicPage build(CategoryDefinition definition, String topic, String slug, String communityId) {
        List<ContentItem> items = new ArrayList<>();
        for (Resource r : resourceService.getAll()) {
            if (matches(definition, topic, r, communityId)) {
                items.add(toContentItem(r));
            }
        }
        for (Flyer f : flyerService.getAll()) {
            if (matches(definition, topic, f, communityId)) {
                items.add(toContentItem(f));
            }
        }

        // Alphabetical. A browse list wants a predictable order a resident can
        // scan, and resources have no editorial date to sort by — updatedDate is
        // a load-date proxy and must not imply recency.
        items.sort(Comparator.comparing(ContentItem::title,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        TopicMetadata metadata = new TopicMetadata(
                definition.key(), definition.label(), definition.icon(),
                topic, slug, items.size(), countByType(items));
        return new TopicPage(metadata, List.copyOf(items));
    }

    private boolean matches(CategoryDefinition definition, String topic, CivicContent item, String communityId) {
        if (communityId != null && !communityId.isBlank() && !communityId.equals(item.communityId)) {
            return false;
        }
        return taxonomyService.matchesCategoryTags(definition, item.categoryTags)
                && topic.equalsIgnoreCase(item.subcategory);
    }

    private static Map<ContentType, Integer> countByType(List<ContentItem> items) {
        Map<ContentType, Integer> counts = new EnumMap<>(ContentType.class);
        for (ContentItem item : items) {
            if (item.contentType() != null) {
                counts.merge(item.contentType(), 1, Integer::sum);
            }
        }
        return counts;
    }

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
                null,                       // no editorial date — see the sort comment
                firstWebsite(r.websites),
                null);                      // resources carry no image
    }

    private static ContentItem toContentItem(Flyer f) {
        ContentSource cs = f.contentSource;
        return new ContentItem(
                f.contentType, f.id, f.title, f.summary,
                f.organization,
                null, null, null,
                f.eventDate,
                cs != null ? cs.url : null,
                // Browse cards stay text — the flyer GALLERY is where the image
                // is the content (Slice J). Passing it here would change every
                // topic page's shape for one view's benefit.
                null);
    }

    /**
     * City only, never a street address. A browse card exists to help a resident
     * decide whether an item is near them; the full address belongs on a detail
     * view where the provider's own contact route is alongside it.
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
// CITY, NEVER A STREET ADDRESS
// =============================================================================
// firstCity() deliberately exposes only the city. A browse card exists to help a
// resident judge whether something is near them; the full address belongs on a
// detail view alongside the provider's own contact route.
//
// It also limits the blast radius of a data issue worth recording: the source
// data carries a `confidential` flag on locations (one record today — a domestic
// violence shelter), and `shared/model/Location` DOES NOT MAP THAT FIELD, so it
// is silently dropped at load and no consumer can honour it. F6 does not make
// this worse — /api/resources already returns those locations in full — but a
// privacy-relevant flag that exists in the data and not in the model is a real
// gap. Recorded in Decision 040; fixing it means adding the field AND deciding
// what every consumer does with it, which is more than a card slice.
// =============================================================================

// =============================================================================
// WHY A NEW ContentItem RATHER THAN REUSING UpdateItem
// =============================================================================
// UpdateItem already normalizes News/Law/Flyer/Expert for the updates feeds, and
// F5a argued hard for ONE cross-type merger. This is not that situation:
//
//   UpdateItem answers  "what changed?"  — dated, EXCLUDES resources by design
//   ContentItem answers "what is this?"  — everything, including resources
//
// Forcing resources into a DTO named "update" would repeat the exact naming
// confusion (`type` meaning two things) that Decision 036 is retiring. The two
// overlap in ~7 fields, and that overlap is acknowledged debt rather than an
// oversight.
//
// THE INTENDED END STATE: ContentItem becomes the single display DTO and
// UpdateItem disappears alongside `type` in Slice H — at which point the updates
// feeds return ContentItems sorted by date. Doing that now would mean touching
// /api/updates and the homepage, which is outside a topic-page slice.
// =============================================================================

// =============================================================================
// SLICE J TOUCH — A CONSTRUCTOR ARGUMENT, AND NOTHING ELSE
// =============================================================================
// ContentItem gained an `imageUrl` component for the Community Notices flyer
// gallery. This class builds ContentItems from Resources and from NewsItems,
// neither of which has an image, so both call sites pass null.
//
// Worth recording because the change is a good advertisement for records over
// builders: adding a component broke every construction site at COMPILE TIME,
// and the compiler enumerated each place that had to decide what the new field
// means here. A builder would have let these two sites silently keep their old
// shape and default the field, which is the same outcome by accident rather than
// by decision.
//
// The answer here was "topic pages have no images", and that is now stated in
// code rather than left implicit.
// =============================================================================
