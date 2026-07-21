/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../organization/service/OrganizationService.java
 * Slice D (Resource Discovery). See references/decisions.md Decision 023.
 * Keep this mirror in sync with the source.
 * =============================================================================
 *
 * WHAT THIS CLASS IS
 *   Aggregates the curated Organizations shortlist for the homepage Resource
 *   Discovery column. Composes ONLY the existing ResourceService (no new
 *   repository) — a BFF-style aggregator, same shape as UpdatesService.
 *
 * WHY A CURATED SHORTLIST (not "all orgs")
 *   There are ~178 distinct organizations in the loaded data, most with 1–2
 *   resources — an unusable homepage list. First Step is a CURATED selection, so
 *   this returns a capped, ranked shortlist and there is deliberately NO
 *   "see all". Ranking is by resource count FOR NOW (a placeholder); the intended
 *   metric is policy/news-driven and will replace the count later.
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

import org.firststep.backend.organization.dto.OrgSummary;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService {

    // Shortlist cap — keeps the discovery column above the fold.
    private static final int MAX_ORGS = 8;

    private final ResourceService resourceService;

    public OrganizationService(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public List<OrgSummary> getCuratedShortlist() {
        // Count resources per organization. A LinkedHashMap preserves first-seen
        // order, which only matters as a pre-sort baseline; the real ordering is
        // the sort below. Blank/null org names are skipped (some records lack one).
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Resource r : resourceService.getAll()) {
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
