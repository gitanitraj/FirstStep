/*
 * WHAT: Complete IM-09 annotated mirror of backend/src/main/java/org/firststep/backend/category/dto/CategoryCollection.java.
 * WHY: Preserve authored placement, canonical identity, truthful dates and private/public boundaries.
 * INTERACTION: Current inventory and Article approval remain independent of exact editorial
 * Expert/FAQ admissions; Category composition intersects Community, taxonomy and membership.
 * ALTERNATIVES: Source/session/verified inference and blanket Expert admission were rejected.
 * Tests cover approved admissions, denied sessions, preserved supporting views and safe links.
 */
package org.firststep.backend.category.dto;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.shared.dto.ContentItem;

/**
 * One purposeful collection on the category destination (Decision 051).
 *
 * <h2>A collection is an editorial selection, not a metadata projection</h2>
 *
 * <p><b>{@code ContentType} is controlled metadata and does not entail a
 * collection.</b> Policy Info and First Rate News are both {@code NEWS}; Law is
 * {@code LAW}; the supporting Q&amp;A collections are both {@code EXPERT}. The set
 * of collections is chosen because each answers a resident question, not because
 * the enum has five values.
 *
 * <p>It follows that <b>the collections do not partition the category.</b> The
 * destination's job is to provide meaningful discovery pathways, not an exhaustive
 * accounting of every record. Decision 036's {@code browse ∪ updates == totalCount}
 * identity governs the category AGGREGATE — a statement about the model — and was
 * never a requirement on a page's layout.
 *
 * <h2>Fields</h2>
 *
 * <p>{@code key} identifies the collection and is stable; the client may use it for
 * expansion state, and it never changes with wording. {@code labelKey} is an i18n
 * key — <b>never a display string</b>, so a payload cannot leak English into a
 * Spanish page. {@code count} is {@code items.size()}, computed here because
 * counting is a backend concern and a client that counts is a client that filters.
 *
 * <p><b>All matching items are carried, not a capped preview.</b> Progressive
 * disclosure ("See more") expands the collection in place, so the alternative would
 * be a second request for content the first request already knew. Housing's largest
 * collection is 44 items; the payload-growth trigger is nowhere near.
 *
 * <p><b>An empty collection is never built.</b> A heading over nothing reads as
 * broken rather than informative, and absence of qualifying material is never a
 * reason to widen a collection's definition.
 */
public record CategoryCollection(
        String key,
        String labelKey,
        CollectionLayer layer,
        int count,
        List<ContentItem> items
) {

    /**
     * A collection of these items, or empty when there are none.
     *
     * <p>The factory returns {@code Optional} rather than letting callers build an
     * empty collection and filter later: "empty collections are not rendered" is a
     * rule about what may EXIST in the payload, and enforcing it at construction
     * means no caller can forget it.
     */
    public static Optional<CategoryCollection> of(String key, String labelKey,
            CollectionLayer layer, List<ContentItem> items) {
        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }
        // A projection counts canonical identities, not repeated appearances in its source list.
        var distinct = new java.util.LinkedHashMap<String, ContentItem>();
        items.stream().filter(item -> item.id() != null && !item.id().isBlank())
                .forEach(item -> distinct.putIfAbsent(item.id(), item));
        if (distinct.isEmpty()) return Optional.empty();
        var unique = List.copyOf(distinct.values());
        return Optional.of(new CategoryCollection(key, labelKey, layer, unique.size(), unique));
    }
}
