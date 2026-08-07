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

@Service
public class TopicPageService {

    // Only three collaborators, and no NavigationService. This service needs the
    // ITEMS; NavigationService answers "how many?" and UpdatesService answers
    // "what changed?". Different questions, so no reuse to force.
    private final TaxonomyService taxonomyService;
    private final ResourceService resourceService;
    private final FlyerService flyerService;

    public TopicPageService(TaxonomyService taxonomyService, ResourceService resourceService,
            FlyerService flyerService) {
        this.taxonomyService = taxonomyService;
        this.resourceService = resourceService;
        this.flyerService = flyerService;
    }

    // BOTH lookups go through the taxonomy, and that is what makes topics
    // category-scoped rather than global. "Eviction Prevention" is declared by
    // Housing AND Legal; findTopicBySlug(categoryKey, slug) resolves it within
    // ONE category, so /housing/eviction-prevention and /legal/eviction-prevention
    // are two distinct pages and /housing/food-pantry is a 404 rather than an
    // empty housing page.
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
        // a load-date proxy and sorting by it would imply a recency the data
        // cannot back.
        items.sort(Comparator.comparing(ContentItem::title,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        TopicMetadata metadata = new TopicMetadata(
                definition.key(), definition.label(), definition.icon(),
                topic, slug, items.size(), countByType(items));
        return new TopicPage(metadata, List.copyOf(items));
    }

    // EDITORIAL CLASSIFICATION ONLY — categoryTags for the category, subcategory
    // for the topic. No text matching, no `tags`, no inference. Handed an
    // unclassified item this matches nothing, exactly like NavigationService and
    // UpdatesService. Classification is an ingestion concern.
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
                firstWebsite(r.websites));
    }

    private static ContentItem toContentItem(Flyer f) {
        ContentSource cs = f.contentSource;
        return new ContentItem(
                f.contentType, f.id, f.title, f.summary,
                f.organization,
                null, null, null,
                f.eventDate,
                cs != null ? cs.url : null);
    }

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
