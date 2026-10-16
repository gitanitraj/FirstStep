package org.firststep.backend.navigation.dto;

import java.util.List;

/**
 * A labeled cluster of topics, from {@code app/data/navigation.json}.
 *
 * <p>Groups are PRESENTATION, not domain — "Need Help Right Away" is an
 * editorial framing of several canonical topics, not a taxonomy level. That is
 * why they live in their own artifact with their own lifecycle (Decision 029),
 * and why only categories with enough topics to need them have any: a group
 * header above a single topic is noise, not hierarchy.
 */
public record TopicGroup(
        String label,
        List<TopicNavigation> topics
) {
}
