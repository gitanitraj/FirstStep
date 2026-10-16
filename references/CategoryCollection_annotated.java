/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../category/dto/CategoryCollection.java
 * Category destination. See references/decisions.md Decision 051.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS RECORD IS
 *   One purposeful collection on a category destination: a stable key, an i18n
 *   label key, the resident job it serves, a count, and its items.
 *
 * THE IDEA THAT TOOK THREE DRAFTS TO GET RIGHT
 *   A COLLECTION IS AN EDITORIAL SELECTION, NOT A METADATA PROJECTION.
 *
 *   Earlier drafts of this design assumed the page's sections had to partition
 *   the category — every record landing in exactly one section, the totals
 *   reconciling. That assumption produced two bad moves in a row:
 *
 *     1. A view set that mixed four conceptual axes ("All", "Programs &
 *        services", "What's changed", "Answers") and only LOOKED coherent
 *        because each label was individually plausible.
 *     2. A proposal to widen "First Rate News" to `first-step OR
 *        type == expert-session` so three stranded records would have a home.
 *
 *   The second was rejected outright: a collection's MEANING is not a variable
 *   to tune until a total reconciles. The real error was the premise. The
 *   destination's job is to provide meaningful discovery pathways, not an
 *   exhaustive accounting — and Decision 036's `browse U updates == totalCount`
 *   identity was always a statement about the category AGGREGATE, never a
 *   requirement on a page's layout.
 *
 * WHY `of()` RETURNS Optional
 *   "Empty collections are not rendered" is a rule about what may EXIST in the
 *   payload, not about what a client chooses to draw. Enforcing it at
 *   construction means no caller can forget it, and the client needs no empty
 *   branch at all. Compare the alternative — build it, then filter later —
 *   which puts the rule in every consumer and eventually in none.
 *
 * WHY ALL ITEMS, NOT A CAPPED PREVIEW
 *   Progressive disclosure expands the collection IN PLACE, so a cap would mean
 *   a second request for content the first request already knew. Housing's
 *   largest collection is 44 items.
 *
 * WHY labelKey AND NOT label
 *   A display string in a payload is how English leaks into a Spanish page.
 *   The same rule ContentType's badge labels follow.
 * ============================================================================= */

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
        return Optional.of(
                new CategoryCollection(key, labelKey, layer, items.size(), List.copyOf(items)));
    }
}
