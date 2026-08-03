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

/**
 * Aggregates curated shortlists of organizations — for the homepage Resource
 * Discovery column, and (Slice F5a) for one category page's "Connect" column.
 * First Step is a curated selection, so both are capped shortlists with NO
 * "see all" — the browser just renders what it's handed.
 *
 * Ranking is by resource count FOR NOW (a placeholder); the intended metric is
 * expected to be driven by policy updates / news. Kept server-side per the
 * backend-aggregates / frontend-displays principle.
 */
@Service
public class OrganizationService {

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

    /**
     * Organizations offering resources in one category, ranked by how many they
     * offer <b>within that category</b> — a housing page should lead with the
     * organizations that do housing, not with whoever is largest overall.
     *
     * <p>Scoping reads editorial classification only, the same rule every other
     * category-aware service uses. An unknown key yields an empty list.
     */
    public List<OrgSummary> getForCategory(String categoryKey) {
        CategoryDefinition definition = taxonomyService.findByKey(categoryKey).orElse(null);
        if (definition == null) {
            return List.of();
        }
        return rank(resourceService.getAll().stream()
                .filter(r -> taxonomyService.matchesCategoryTags(definition, r.categoryTags))
                .toList());
    }

    private List<OrgSummary> rank(List<Resource> resources) {
        // Count resources per organization, preserving first-seen order for stable
        // tie-breaking before the final sort.
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Resource r : resources) {
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
