/**
 * Category vertical slice — GET /api/categories?communityId=... aggregates
 * Resource/Flyer/NewsItem into the fixed 10-category taxonomy defined in
 * model/CategoryDefinition, returning per-category counts, latest items
 * (via search.dto.SearchResult, reused rather than duplicated), and the
 * most recent linked policy update from News. See
 * references/decisions.md's Decision 014 and the homepage redesign
 * roadmap (Step 1 of 8) in docs/architecture/03-application-architecture.md.
 */
package org.firststep.backend.category;
