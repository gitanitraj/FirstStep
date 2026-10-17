/*
 * Annotated reference: backend/src/main/java/org/firststep/backend/organization/service/OrganizationService.java
 * WHAT: Preserves the complete source below; see the explicit allowlists and optional exact-owner filters in the source.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Controllers adapt internal models only at the resident boundary; tests and evidence exercise real repositories plus labeled synthetic counterexamples.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
package org.firststep.backend.organization.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.firststep.backend.category.model.CategoryDefinition;
import org.firststep.backend.category.service.TaxonomyService;
import java.util.Optional;

import org.firststep.backend.organization.dto.OrgSummary;
import org.firststep.backend.organization.dto.OrganizationPage;
import org.firststep.backend.organization.model.Organization;
import org.firststep.backend.organization.repository.OrganizationRepository;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.dto.ResidentContent;
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
 *
 * <p><b>Slice G1a gave organizations an identity.</b> Before, this class derived
 * one by grouping the display NAME off each resource and slugifying it, so an
 * organization existed only as a string repeated N times and its URL was a
 * function of its spelling. Renaming it would have broken every link with
 * nothing to detect the break.
 *
 * <p>Shortlists still rank by counting resources — that is a ranking question,
 * not an identity one — but the slug now comes from the REGISTRY rather than
 * from {@code slugify(name)}, so what a shortlist links to is guaranteed to be a
 * page that exists.
 */
@Service
public class OrganizationService {

    private static final int MAX_ORGS = 8;

    private final ResourceService resourceService;
    private final TaxonomyService taxonomyService;
    private final OrganizationRepository repository;

    public OrganizationService(ResourceService resourceService, TaxonomyService taxonomyService,
            OrganizationRepository repository) {
        this.resourceService = resourceService;
        this.taxonomyService = taxonomyService;
        this.repository = repository;
    }

    /**
     * One organization and everything First Step lists for it, or empty.
     *
     * <p><b>Slug in, ID out.</b> The slug resolves to a registry record once, and
     * every resource is then matched on {@code organizationId}. Matching on the
     * display name instead would work today and reintroduce exactly the fragility
     * the registry removes.
     *
     * <p>An organization with no resources still returns a page rather than a
     * 404: it is a real organization that happens to list nothing right now, and
     * that is a different fact from "no such organization".
     */
    public Optional<OrganizationPage> getBySlug(String slug) {
        return repository.findBySlug(slug).map(org -> new OrganizationPage(
                org.id, org.name, org.slug, ResidentContent.safeUrl(org.website), resourcesFor(org)));
    }

    /** Explicit scoped destination path; historical directory overload remains unchanged. */
    public Optional<OrganizationPage> getBySlug(String slug, String communityId) {
        return repository.findBySlug(slug).map(org -> new OrganizationPage(org.id, org.name, org.slug, ResidentContent.safeUrl(org.website),
                resourceService.getAll().stream().filter(r -> communityId.equals(r.communityId))
                        .filter(r -> org.id.equals(r.organizationId)).map(OrganizationService::toContentItem).toList()));
    }

    /** IM-08 preview uses actual registry IDs, never a source, host or display-name inference. */
    public List<OrgSummary> getForCategory(String categoryKey, String communityId) {
        var category = taxonomyService.findByKey(categoryKey).orElse(null);
        if (category == null || communityId == null || communityId.isBlank()) return List.of();
        Map<String, java.util.Set<String>> ids = new java.util.TreeMap<>();
        resourceService.getAll().stream().filter(r -> communityId.equals(r.communityId))
                .filter(r -> taxonomyService.matchesCategoryTags(category, r.categoryTags))
                .filter(r -> r.organizationId != null && r.id != null)
                .forEach(r -> ids.computeIfAbsent(r.organizationId, ignored -> new java.util.TreeSet<>()).add(r.id));
        return repository.findAll().stream().filter(org -> ids.containsKey(org.id) && org.slug != null)
                .sorted(Comparator.<Organization>comparingInt(org -> ids.get(org.id).size()).reversed()
                        .thenComparing(org -> org.name).thenComparing(org -> org.id))
                .limit(4).map(org -> new OrgSummary(org.name, org.slug, ids.get(org.id).size())).toList();
    }

    private List<ContentItem> resourcesFor(Organization org) {
        return resourceService.getAll().stream()
                .filter(r -> org.id.equals(r.organizationId))
                .map(OrganizationService::toContentItem)
                .toList();
    }

    /**
     * Resources render as the SAME card shape the rest of the product uses.
     *
     * <p>An organization page asks a different question — "who provides this?" —
     * but the answer is still a list of resources, so it needs no card of its
     * own. Note the per-resource website is carried through: service-level
     * contact belongs on the service, which is precisely why the organization
     * record withholds it.
     */
    private static ContentItem toContentItem(Resource r) {
        return new ContentItem(
                r.contentType, r.id, r.title,
                r.summary != null ? r.summary : r.description,
                r.organization,
                r.locations != null && !r.locations.isEmpty() ? r.locations.get(0).city : null,
                r.cost, r.urgency, null,
                r.websites != null && !r.websites.isEmpty() ? r.websites.get(0).url : null,
                null);
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
                // The slug comes from the REGISTRY, never from slugify(name): a
                // shortlist must link to a page that exists, and only the
                // registry knows the current slug.
                .map(e -> new OrgSummary(e.getKey(), slugFor(e.getKey()), e.getValue()))
                .filter(o -> o.slug() != null)
                .toList();
    }

    /**
     * The registry's current slug for a display name, or null if unregistered.
     *
     * <p>Returning null rather than falling back to {@code slugify(name)} is
     * deliberate: a computed slug for an organization the registry does not know
     * would produce a link to a 404 — a shortlist entry that looks live and is
     * not. The caller filters those out instead.
     */
    private String slugFor(String name) {
        return repository.findAll().stream()
                .filter(o -> name.equals(o.name))
                .map(o -> o.slug)
                .findFirst().orElse(null);
    }
}
