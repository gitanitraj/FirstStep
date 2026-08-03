/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../category/dto/CategoryPage.java
 * Slice F5a. See references/decisions.md Decision 036.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS RECORD IS
 *   The whole category page in one response — the BFF payload for
 *   GET /api/category/{key}. The client displays it; it fetches nothing else and
 *   filters nothing itself.
 *
 * THE THREE PILLARS (the user's framing, and the reason the DTO is shaped this
 * way rather than as a flat list of content)
 *
 *   Discover      What is available?              groups / topics
 *   Connect       Where do I go or contact next?  organizations
 *   Stay Informed What has changed?               updates
 *
 *   A category page answers three different questions, and a resident arrives
 *   with one of them. Collapsing them into a single ranked content list would
 *   serve none of the three well.
 * ============================================================================= */

package org.firststep.backend.category.dto;

import java.util.List;

import org.firststep.backend.navigation.dto.TopicGroup;
import org.firststep.backend.navigation.dto.TopicNavigation;
import org.firststep.backend.organization.dto.OrgSummary;
import org.firststep.backend.updates.dto.UpdateItem;

public record CategoryPage(
        CategoryMetadata metadata,
        List<UpdateItem> updates,
        List<TopicGroup> groups,
        List<TopicNavigation> topics,
        List<OrgSummary> organizations
) {

    // Convenience for the frontend's branch. Groups and topics are mutually
    // exclusive, so "is this grouped?" has exactly one correct reading.
    public boolean isGrouped() {
        return !groups.isEmpty();
    }
}

// =============================================================================
// WHY THE PAGE IS AN AGGREGATE AND NOT JUST NAVIGATION
// =============================================================================
// Measured, not assumed:
//
//   Resource            229    subcategory: 229   → browsable
//   Flyer                 7    subcategory:   7   → browsable
//   News                  8    subcategory:   0   → chronological
//   Signed legislation  175    subcategory:   0   → chronological
//   Expert / FAQ         12    subcategory:   0   → chronological
//
// 193 of 429 classified items carry a category and NO topic. Topic tiles reach
// only about half of a category, and the classifier will not invent the missing
// half — subcategory inference is deferred to Version 3 on purpose.
//
// Those items are not a coverage gap; they are the OTHER HALF of the page, and
// `updates` is how residents reach them. Coverage grows by composition, not by
// inference. Verified across all ten categories: browse ∪ updates == totalCount.
// =============================================================================

// =============================================================================
// WHY groups AND topics ARE SIBLINGS RATHER THAN A NESTED CategoryNavigation
// =============================================================================
// The obvious alternative was a single `navigation` field holding the whole
// CategoryNavigation record. Rejected because CategoryNavigation ALSO carries
// key, label, icon, totalCount and countsByType — every one of which is already
// in `metadata`. Nesting it would put the same five facts in two places in one
// payload, and a client would have to know which copy to trust.
//
// Projecting the two list fields out keeps each fact stated once, and carries
// F3's invariant across verbatim:
//
//   grouped category   → groups populated, topics EMPTY   (housing, community-support)
//   ungrouped category → topics populated, groups EMPTY   (the other eight)
//
// That is Decision 029's "a category absent from navigation.json renders a flat
// topic list", still structural rather than merely documented.
//
// CategoryNavigation ITSELF IS NOT MODIFIED by this slice. It is the navigation
// read model's contract, and reshaping it to suit a page would be exactly the
// coupling F5a exists to avoid — the read model must not learn about pages.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Built by category/service/CategoryPageService, which composes
//   NavigationService + UpdatesService + OrganizationService.
// - Reuses THREE existing DTOs rather than defining page-local copies:
//     navigation/dto/TopicGroup + TopicNavigation  (from the read model)
//     organization/dto/OrgSummary                  (same shape the homepage uses)
//     updates/dto/UpdateItem                       (same shape /api/updates uses)
//   Reuse matters here beyond saving code: a topic tile and an update card should
//   look and behave the same on the homepage and a category page, and sharing the
//   DTO is what makes that automatic instead of a coincidence to maintain.
// - Serialized by CategoryController through the standard ApiResponse<T> envelope.
// =============================================================================
