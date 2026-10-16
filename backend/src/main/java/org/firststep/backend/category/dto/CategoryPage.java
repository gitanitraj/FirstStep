package org.firststep.backend.category.dto;

import java.util.List;

import org.firststep.backend.navigation.dto.TopicGroup;
import org.firststep.backend.navigation.dto.TopicNavigation;
import org.firststep.backend.organization.dto.OrgSummary;
import org.firststep.backend.updates.dto.UpdateItem;

/**
 * Everything a category page renders, in one response.
 *
 * <h2>Three pillars</h2>
 *
 * <table>
 *   <tr><th>Pillar</th><th>Question</th><th>Field</th></tr>
 *   <tr><td>Discover</td><td>What is available?</td><td>{@code groups} / {@code topics}</td></tr>
 *   <tr><td>Connect</td><td>Where do I go or contact next?</td><td>{@code organizations}</td></tr>
 *   <tr><td>Stay Informed</td><td>What has changed?</td><td>{@code updates}</td></tr>
 * </table>
 *
 * <p><b>Why the page is an aggregate and not just navigation.</b> Resources and
 * flyers carry a subcategory, so topic tiles reach them. News, legislation and
 * expert content carry a category and no subcategory — the classifier is
 * conservative by design and will not invent one. Those items are not a coverage
 * gap; they are the other half of the page, and they are reached through
 * {@code updates}. <b>Coverage grows by composition, not by inference.</b>
 *
 * <p><b>{@code groups} and {@code topics} are mutually exclusive</b>, carrying
 * {@link org.firststep.backend.navigation.dto.CategoryNavigation}'s invariant
 * verbatim: a category grouped in navigation.json returns groups and an empty
 * topic list; a category absent from it returns a flat topic list and no groups.
 * They are projected as siblings rather than nesting the whole
 * {@code CategoryNavigation}, which would repeat key, label, icon and the counts
 * in two places in one payload.
 *
 * <h2>{@code collections} — the destination composition (Decision 051)</h2>
 *
 * <p>The pillars above describe what the page KNOWS. They describe poorly what a
 * resident WANTS: nobody arrives at Housing thinking "I would like to Discover."
 * {@code collections} carries the destination's purposeful collections instead —
 * ordered, each already shaped, each naming the resident job it serves through its
 * {@link CollectionLayer}.
 *
 * <p><b>The pillars are preserved as information, retired as primary navigation.</b>
 * {@code updates}, {@code groups}, {@code topics} and {@code organizations} keep
 * their exact current semantics, so nothing that reads this payload today breaks. A
 * field is retired only when the design demonstrates it obsolete, never by leaving
 * a silent duplicate in place.
 *
 * <p><b>This is one additive field, not a UI data bag.</b> Each collection is a
 * named projection the destination architecture requires. The payload carries no
 * spatial information — whether collections render as columns, stacked sections or
 * something responsive is a visual-design question, and a DTO that answered it
 * would make the backend own the layout.
 */
public record CategoryPage(
        CategoryMetadata metadata,
        List<UpdateItem> updates,
        List<TopicGroup> groups,
        List<TopicNavigation> topics,
        List<OrgSummary> organizations,
        List<CategoryCollection> collections
) {

    /** True when navigation.json groups this category's topics. */
    public boolean isGrouped() {
        return !groups.isEmpty();
    }
}
