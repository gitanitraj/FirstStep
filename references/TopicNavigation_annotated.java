package org.firststep.backend.navigation.dto;

import java.util.Map;

import org.firststep.backend.shared.model.ContentType;

/**
 * One topic (subcategory) as the navigation UI needs it: its canonical name, the
 * slug that appears in {@code /category/{key}/{topic}}, and how much content sits
 * under it.
 *
 * <p>{@code countsByType} drives the content-type indicators — a topic holding
 * three resources and a law should say so, because "4" alone tells a resident
 * nothing about what they will find.
 *
 * <p>A topic with {@code count == 0} is still returned. Suppressing empty topics
 * would hide the fact that a canonical topic exists and has nothing in it, which
 * is exactly the unreachability problem {@code validate_navigation.py} was
 * written to surface.
 */
public record TopicNavigation(
        String name,
        String slug,
        int count,
        Map<ContentType, Integer> countsByType
) {
}
