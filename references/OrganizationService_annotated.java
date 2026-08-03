/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../organization/service/OrganizationService.java
 * Slice D (Resource Discovery), extended in Slice F5a.
 * See references/decisions.md Decisions 023 and 036.
 * Keep this mirror in sync with the source.
 * =============================================================================
 *
 * WHAT THIS CLASS IS
 *   Aggregates curated Organizations shortlists — for the homepage Resource
 *   Discovery column, and (F5a) for one category page's "Connect" column.
 *   Composes ONLY the existing ResourceService and TaxonomyService (no new
 *   repository) — a BFF-style aggregator, same shape as UpdatesService.
 *
 * WHY A CURATED SHORTLIST (not "all orgs")
 *   There are ~178 distinct organizations in the loaded data, most with 1–2
 *   resources — an unusable list. First Step is a CURATED selection, so this
 *   returns a capped, ranked shortlist and there is deliberately NO "see all".
 *   Ranking is by resource count FOR NOW (a placeholder); the intended metric is
 *   policy/news-driven and will replace the count later.
 *
 * COMPANION DTO
 *   organization/dto/OrgSummary.java: record(name, slug, resourceCount). `slug`
 *   is for /organization/{slug} routing (the Organization landing page is Slice G).
 * ============================================================================= */

package org.firststep.backend.organization.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.firststep.backend.category.model.CategoryDefinition;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.organization.dto.OrgSummary;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService {

    // Shortlist cap — keeps the discovery column above the fold. Shared by both
    // shortlists deliberately: a category page's Connect column and the homepage's
    // Organizations column are the same UI affordance at the same density, so a
    // second constant would be a knob with no question behind it.
    private static final int MAX_ORGS = 8;

    private final ResourceService resourceService;
    private final TaxonomyService taxonomyService;

    public OrganizationService(ResourceService resourceService, TaxonomyService taxonomyService) {
        this.resourceService = resourceService;
        this.taxonomyService = taxonomyService;
    }

    public List<OrgSummary> getCuratedShortlist() {
        return rank(resourceService.getAll());
    }

    // F5a. Organizations offering resources in ONE category, ranked by how many
    // they offer WITHIN that category. Scoping reads editorial classification
    // only, the same matchesCategoryTags rule every other category-aware service
    // uses. An unknown key yields an empty list rather than an exception — the
    // caller has already established whether the category exists.
    public List<OrgSummary> getForCategory(String categoryKey) {
        CategoryDefinition definition = taxonomyService.findByKey(categoryKey).orElse(null);
        if (definition == null) {
            return List.of();
        }
        return rank(resourceService.getAll().stream()
                .filter(r -> taxonomyService.matchesCategoryTags(definition, r.categoryTags))
                .toList());
    }

    // Extracted in F5a so both shortlists share one ranking implementation. The
    // ONLY difference between the two public methods is which resources they hand
    // in, which is the correct seam: "how do we rank organizations?" is one
    // question with one answer, asked about two populations.
    private List<OrgSummary> rank(List<Resource> resources) {
        // Count resources per organization. A LinkedHashMap preserves first-seen
        // order, which only matters as a pre-sort baseline; the real ordering is
        // the sort below. Blank/null org names are skipped (some records lack one).
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Resource r : resources) {
            String org = r.organization;
            if (org == null || org.isBlank()) {
                continue;
            }
            counts.merge(org.trim(), 1, Integer::sum); // +1 per resource
        }

        return counts.entrySet().stream()
                // Rank by count DESC, then name ASC. The name tie-break makes the
                // shortlist DETERMINISTIC — without it, orgs with equal counts could
                // reorder between requests (flaky UI + flaky tests).
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(MAX_ORGS)
                .map(e -> new OrgSummary(e.getKey(), slugify(e.getKey()), e.getValue()))
                .toList();
    }

    // Name → URL-safe slug: lowercase, collapse each run of non-alphanumerics to a
    // single hyphen, then trim leading/trailing hyphens. Enough to route
    // /organization/{slug}; the Organization page (G) resolves back by matching.
    // (Package-private so the test can exercise it directly.)
    static String slugify(String name) {
        String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return slug.replaceAll("(^-+)|(-+$)", "");
    }
}

// =============================================================================
// SLICE F5a — WHY COUNTS ARE SCOPED, NOT JUST THE MEMBERSHIP
// =============================================================================
// getForCategory could have filtered which organizations appear while still
// ranking them by their TOTAL resource count. That would be wrong, and the test
// shouldRankByCountWithinTheCategoryNotOverall pins the difference:
//
//   Big Generalist      2 food + 1 housing
//   Housing Specialist  2 housing
//
// Ranked overall, the generalist leads a HOUSING page with three resources, only
// one of which is about housing. Ranked within the category, the specialist leads
// with two. A resident on the Housing page is asking "who does housing?", and the
// count shown beside each name has to answer that same question or it is
// misleading rather than merely imprecise.
//
// THE PLACEHOLDER STATUS IS UNCHANGED. Resource count is still a stand-in for the
// intended policy/news-driven metric (Decision 023). F5a scoped the existing
// metric correctly; it did not promote it.
//
// UNCLASSIFIED RESOURCES CONTRIBUTE NOTHING — no categoryTags, no placement, the
// same rule NavigationService and UpdatesService follow. Pinned by
// shouldNotPlaceOrganizationsFromUnclassifiedResources.
// =============================================================================
