/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../category/dto/TopicMetadata.java (Slice F6)
 * See references/decisions.md Decision 040.
 * ============================================================================= */

package org.firststep.backend.category.dto;

import java.util.Map;

import org.firststep.backend.shared.model.ContentType;

/**
 * A topic's identity and size, plus enough of its parent category to render a
 * breadcrumb without a second request.
 *
 * <p>Carrying the category's label and icon here is the BFF principle applied to
 * navigation: the client knows the URL it asked for, but "housing" is a key, not
 * a display name, and it should not have to fetch the category page to learn
 * that this topic sits under "Housing".
 */
public record TopicMetadata(
        String categoryKey,
        String categoryLabel,
        String categoryIcon,
        /** Canonical topic name from the taxonomy, e.g. "Emergency Shelter". */
        String name,
        /** URL slug, e.g. "emergency-shelter". */
        String slug,
        int totalCount,
        Map<ContentType, Integer> countsByType
) {
}
