package org.firststep.backend.category.dto;

import java.util.List;

import org.firststep.backend.navigation.dto.TopicGroup;
import org.firststep.backend.navigation.dto.TopicNavigation;
import org.firststep.backend.organization.dto.OrgSummary;
import org.firststep.backend.updates.dto.UpdateItem;

/**
 * Resident Category aggregate. Legacy metadata/navigation/collection fields remain available;
 * destination is the explicit-Community IM-08 presentation allowlist, never selector evidence.
 * Groups and topics preserve the mutually exclusive navigation contract. Organizations on the
 * scoped resident path are a bounded preview of explicit current Resource-to-registry links.
 * No spatial layout, editorial diagnostics or verification date is serialized here.
 */
public record CategoryPage(
        CategoryMetadata metadata,
        List<UpdateItem> updates,
        List<TopicGroup> groups,
        List<TopicNavigation> topics,
        List<OrgSummary> organizations,
        List<CategoryCollection> collections,
        CategoryDestination destination
) {

    /** True when navigation.json groups this category's topics. */
    public boolean isGrouped() {
        return !groups.isEmpty();
    }
}
