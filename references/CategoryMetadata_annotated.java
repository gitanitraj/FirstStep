/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../category/dto/CategoryMetadata.java
 * Slice F5a. See references/decisions.md Decision 036.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS RECORD IS
 *   A category's identity and size — the page header. Projected from the
 *   navigation read model rather than recomputed, so "how big is this category?"
 *   has one answer.
 * ============================================================================= */

package org.firststep.backend.category.dto;

import java.util.Map;

import org.firststep.backend.shared.model.ContentType;

public record CategoryMetadata(
        String key,
        String label,
        String icon,
        int totalCount,
        Map<ContentType, Integer> countsByType,
        String lastUpdated
) {
}

// =============================================================================
// WHY countsByType EARNS ITS PLACE
// =============================================================================
// It is what makes the two halves of the page legible together. A category
// reporting
//
//     { RESOURCE: 44, NEWS: 5, FLYER: 1, LAW: 20, EXPERT: 3 }   (housing, live)
//
// is telling a resident that browsing and staying informed will find them
// different things — 45 browsable items under topics, 29 dated items in the
// feed. A bare "73" would say nothing about which half holds what.
//
// It also makes the F5a coverage argument checkable from the payload alone:
// totalCount minus the sum of topic counts is exactly the topicless content, and
// non-RESOURCE types are exactly what the updates feed draws from.
// =============================================================================

// =============================================================================
// WHY lastUpdated COMES FROM THE UPDATES FEED ONLY
// =============================================================================
// It is derived from the head of the (already date-sorted) updates list — the
// most recent signed bill, published news item, flyer event or expert session.
//
// IT IS DELIBERATELY NOT DERIVED FROM Resource.updatedDate. That field is a
// LOAD-DATE PROXY, not edit history: it records when the pipeline ingested a
// record, not when anyone reviewed or changed it. Sorting by it internally is
// fine; showing it to a resident as "last updated" would imply a freshness
// guarantee the data cannot back, which is a standing project rule predating
// this slice.
//
// The honest consequence: a category holding only resources reports
// lastUpdated = null rather than a number that looks like currency but is not.
// Pinned by shouldNotReportALastUpdatedDateWhenNothingHasChanged.
// =============================================================================

// =============================================================================
// WHY THERE IS NO description FIELD
// =============================================================================
// A category page would read better opening with a sentence explaining what the
// category covers. taxonomy.json carries no such prose — only key, label, icon,
// keywords and subcategories — so adding it means AUTHORING ten editorial blurbs.
//
// taxonomy.json is the canonical EDITORIAL artifact. Its words are written by
// editors, not generated inside a code slice, and inventing them here would put
// machine-written copy into the file every other component treats as ground
// truth. Deferred by the user to the future Admin project, where category
// descriptions become an editing task with a UI behind it.
//
// The DTO does not need reshaping when that lands — a field is added, and this
// record's meaning is unchanged.
// =============================================================================
