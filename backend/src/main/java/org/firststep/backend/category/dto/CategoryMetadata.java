package org.firststep.backend.category.dto;

import java.util.Map;

import org.firststep.backend.shared.model.ContentType;

/**
 * A category's identity and size — the page header.
 *
 * <p>{@code countsByType} is what makes the two halves of the page legible
 * together: a category showing 44 resources and 20 laws is telling a resident
 * that browsing and staying informed will find them different things.
 *
 * <p><b>{@code lastUpdated} comes from the updates feed, never from
 * {@code Resource.updatedDate}.</b> That field is a load-date proxy, not edit
 * history — fine for sorting internally, but displaying it to residents as
 * "last updated" would imply a freshness guarantee the data does not have. The
 * dates here are editorial: when a bill was signed, when news was published, when
 * an expert spoke.
 *
 * <p>There is deliberately no {@code description}. taxonomy.json carries no
 * per-category prose, and inventing ten blurbs would be authoring editorial
 * content in a code slice. That work belongs to the future Admin project.
 */
public record CategoryMetadata(
        String key,
        String label,
        String icon,
        int totalCount,
        Map<ContentType, Integer> countsByType,
        String lastUpdated
) {
}
