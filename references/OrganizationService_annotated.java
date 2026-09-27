/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../organization/service/OrganizationService.java
 * Slice F5a originally; rewritten onto the registry in Slice G1a (Decision 049).
 * =============================================================================
 *
 * WHAT IT DOES: ranks organizations for category shortlists, and assembles one
 * organization page — the third step of the Front Door's Connect pathway
 * (directory -> organization -> that organization's resources).
 *
 * WHAT CHANGED, AND WHY IT MATTERS MORE THAN IT LOOKS
 * ---------------------------------------------------
 * This class used to DERIVE organizations by grouping the display name off each
 * resource and slugifying it. Identity was therefore a function of spelling, and
 * the class that produced links was also the class that invented the identity
 * those links pointed at.
 *
 * Now the registry owns identity and this class only composes. slugify() is
 * DELETED from production code — the grep that proves it is worth re-running if
 * anyone is tempted to add it back for convenience.
 *
 * SLUG IN, ID OUT
 * ---------------
 * getBySlug resolves the slug ONCE against the registry, then matches resources
 * on organizationId. Matching on the display name instead would work today and
 * silently reintroduce the exact fragility the registry removes — which is why
 * the resolution happens at the boundary and never again.
 *
 * slugFor() RETURNS NULL RATHER THAN COMPUTING A FALLBACK
 * ------------------------------------------------------
 * When a shortlist meets an organization the registry does not know, the
 * tempting fallback is slugify(name). That would produce a link that LOOKS live
 * and 404s — the worst of both, because nothing surfaces it.
 *
 * Returning null and filtering the entry out means an unregistered organization
 * is simply absent from the shortlist. Absence is visible; a broken link is not.
 * Same never-guess rule as ContentSourceService and ReviewStatus, applied to
 * links instead of sectors.
 *
 * WHY AN EMPTY ORGANIZATION STILL RETURNS A PAGE
 * ----------------------------------------------
 * getBySlug returns a page for a registered organization with zero resources,
 * rather than empty. "This organization exists and lists nothing right now" and
 * "there is no such organization" are different facts, and collapsing them into
 * one 404 would tell a resident the organization does not exist when it does.
 *
 * WHY RESOURCES RENDER AS ContentItem
 * -----------------------------------
 * An organization page asks a different QUESTION — "who provides this?" — but
 * the answer is still a list of resources. Same card shape as topic pages,
 * search and notices; a new DTO would have been a new shape for the same thing.
 *
 * Note the per-resource website IS carried through. That is not an oversight:
 * service-level contact belongs on the service, which is precisely why the
 * organization record withholds it.
 * ============================================================================= */

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
                org.id, org.name, org.slug, org.website, resourcesFor(org)));
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
