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
 */
public record CategoryPage(
        CategoryMetadata metadata,
        List<UpdateItem> updates,
        List<TopicGroup> groups,
        List<TopicNavigation> topics,
        List<OrgSummary> organizations
) {

    /** True when navigation.json groups this category's topics. */
    public boolean isGrouped() {
        return !groups.isEmpty();
    }
}
