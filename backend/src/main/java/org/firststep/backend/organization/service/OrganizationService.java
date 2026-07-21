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

/**
 * Aggregates a curated shortlist of organizations for the homepage Resource
 * Discovery column. First Step is a curated selection, so this is a capped
 * shortlist with NO "see all" — the browser just renders what it's handed.
 *
 * Ranking is by resource count FOR NOW (a placeholder); the intended metric is
 * expected to be driven by policy updates / news. Kept server-side per the
 * backend-aggregates / frontend-displays principle.
 */
@Service
public class OrganizationService {

    private static final int MAX_ORGS = 8;

    private final ResourceService resourceService;

    public OrganizationService(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public List<OrgSummary> getCuratedShortlist() {
        // Count resources per organization, preserving first-seen order for stable
        // tie-breaking before the final sort.
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Resource r : resourceService.getAll()) {
            String org = r.organization;
            if (org == null || org.isBlank()) {
                continue;
            }
            counts.merge(org.trim(), 1, Integer::sum);
        }

        return counts.entrySet().stream()
                // Rank by count desc, then name asc for a deterministic order.
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(MAX_ORGS)
                .map(e -> new OrgSummary(e.getKey(), slugify(e.getKey()), e.getValue()))
                .toList();
    }

    // Lowercase, collapse any run of non-alphanumerics to a single hyphen, trim
    // leading/trailing hyphens. Enough for /organization/{slug} routing.
    static String slugify(String name) {
        String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return slug.replaceAll("(^-+)|(-+$)", "");
    }
}
