package org.firststep.backend.navigation.dto;

import java.util.List;
import java.util.Map;

import org.firststep.backend.shared.model.ContentType;

/**
 * One category, shaped for the category page.
 *
 * <p><b>{@code groups} and {@code topics} are mutually exclusive.</b> A category
 * present in {@code navigation.json} returns its groups and an empty topic list;
 * a category absent from it returns a flat topic list and no groups. That is
 * Decision 029's rule — "a category absent from navigation.json renders a flat
 * topic list" — now enforced in code rather than only documented. Today housing
 * and community-support are grouped; the other eight are flat.
 *
 * <p>Counts cover ALL classified CivicContent, not just resources: a category
 * page that omitted the news and legislation classified into it would
 * under-represent what is actually on it.
 */
public record CategoryNavigation(
        String key,
        String label,
        String icon,
        int totalCount,
        Map<ContentType, Integer> countsByType,
        List<TopicGroup> groups,
        List<TopicNavigation> topics
) {

    /** True when navigation.json groups this category's topics. */
    public boolean isGrouped() {
        return !groups.isEmpty();
    }
}
