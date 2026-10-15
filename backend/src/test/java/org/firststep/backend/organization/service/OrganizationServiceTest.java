package org.firststep.backend.organization.service;

import org.firststep.backend.shared.lifecycle.LifecycleFixture;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.organization.dto.OrgSummary;
import org.firststep.backend.organization.model.Organization;
import org.firststep.backend.organization.repository.OrganizationRepository;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.resource.service.ResourceService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationServiceTest {

    private static Resource resource(String id, String org) {
        Resource r = new Resource();
        r.id = id;
        r.organization = org;
        return r;
    }

    private static Resource resource(String id, String org, List<String> categoryTags) {
        Resource r = resource(id, org);
        r.categoryTags = categoryTags;
        return r;
    }

    private static OrganizationService service(List<Resource> resources) {
        ResourceRepository repo = new ResourceRepository() {
            @Override
            public List<Resource> findAll() {
                return resources;
            }

            @Override
            public Optional<Resource> findById(String id) {
                return resources.stream().filter(r -> r.id.equals(id)).findFirst();
            }
        };
        return new OrganizationService(new ResourceService(repo, LifecycleFixture.fixed()), new TaxonomyService("../app/data"),
                registryFor(resources));
    }

    /**
     * A registry containing exactly the organizations these fixtures name.
     *
     * <p>Needed because shortlists now take their slug from the REGISTRY rather
     * than from slugify(name) — an organization the registry does not know is
     * omitted, since linking to a page that cannot exist is worse than not
     * linking. Fixtures therefore have to be registered to appear at all, which
     * is the behaviour under test as much as a setup detail.
     */
    private static OrganizationRepository registryFor(List<Resource> resources) {
        List<Organization> orgs = resources.stream()
                .map(r -> r.organization)
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .map(name -> {
                    Organization o = new Organization();
                    o.name = name;
                    o.slug = name.toLowerCase(java.util.Locale.ROOT)
                            .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
                    o.id = "ORG-" + String.format("%03d", Math.abs(name.hashCode()) % 1000);
                    return o;
                })
                .toList();
        return new OrganizationRepository() {
            @Override
            public List<Organization> findAll() {
                return orgs;
            }

            @Override
            public Optional<Organization> findById(String id) {
                return orgs.stream().filter(o -> o.id.equals(id)).findFirst();
            }

            @Override
            public Optional<Organization> findBySlug(String slug) {
                return orgs.stream().filter(o -> o.slug.equals(slug)).findFirst();
            }
        };
    }

    @Test
    void shouldGroupByOrganizationAndCount() {
        OrganizationService service = service(List.of(
                resource("R1", "Red Cross"),
                resource("R2", "Red Cross"),
                resource("R3", "Ministry of Caring")));

        List<OrgSummary> orgs = service.getCuratedShortlist();

        assertEquals(2, orgs.size());
        assertEquals("Red Cross", orgs.get(0).name()); // 2 resources → first
        assertEquals(2, orgs.get(0).resourceCount());
        assertEquals(1, orgs.get(1).resourceCount());
    }

    @Test
    void shouldRankByCountThenNameForStableOrder() {
        OrganizationService service = service(List.of(
                resource("R1", "Zeta Org"),
                resource("R2", "Alpha Org")));

        List<OrgSummary> orgs = service.getCuratedShortlist();

        // Equal counts (1 each) → alphabetical tie-break.
        assertEquals("Alpha Org", orgs.get(0).name());
        assertEquals("Zeta Org", orgs.get(1).name());
    }

    @Test
    void shouldSkipBlankOrganizations() {
        OrganizationService service = service(List.of(
                resource("R1", "Red Cross"),
                resource("R2", null),
                resource("R3", "  ")));

        List<OrgSummary> orgs = service.getCuratedShortlist();

        assertEquals(1, orgs.size());
        assertEquals("Red Cross", orgs.get(0).name());
    }

    @Test
    void shouldCapAtEightOrganizations() {
        List<Resource> many = new java.util.ArrayList<>();
        for (int i = 0; i < 12; i++) {
            many.add(resource("R" + i, "Org " + i));
        }
        OrganizationService service = service(many);

        assertEquals(8, service.getCuratedShortlist().size());
    }

    @Test
    void shouldTakeTheSlugFromTheRegistryRatherThanDerivingIt() {
        OrganizationService service = service(List.of(
                resource("R1", "Be Ready CDC - Esther's Place")));

        OrgSummary org = service.getCuratedShortlist().get(0);

        // The value is the registry's, not slugify(name)'s. They agree here
        // because the registry was generated from these names — the point is
        // WHERE it came from, which is what survives a rename.
        assertEquals("be-ready-cdc-esther-s-place", org.slug());
        assertTrue(org.slug().matches("[a-z0-9-]+"));
    }

    // ---- getForCategory — a category page's "Connect" column (Slice F5a) ----

    @Test
    void shouldReturnOnlyOrganizationsWithResourcesInTheCategory() {
        OrganizationService service = service(List.of(
                resource("R1", "Housing Alliance", List.of("Housing")),
                resource("R2", "Food Bank", List.of("Food"))));

        List<OrgSummary> orgs = service.getForCategory("housing");

        assertEquals(1, orgs.size());
        assertEquals("Housing Alliance", orgs.get(0).name());
    }

    @Test
    void shouldRankByCountWithinTheCategoryNotOverall() {
        // A housing page should lead with organizations that do housing, even when
        // another organization is larger across the whole directory.
        OrganizationService service = service(List.of(
                resource("R1", "Big Generalist", List.of("Food")),
                resource("R2", "Big Generalist", List.of("Food")),
                resource("R3", "Big Generalist", List.of("Housing")),
                resource("R4", "Housing Specialist", List.of("Housing")),
                resource("R5", "Housing Specialist", List.of("Housing"))));

        List<OrgSummary> orgs = service.getForCategory("housing");

        assertEquals("Housing Specialist", orgs.get(0).name());
        assertEquals(2, orgs.get(0).resourceCount(), "counts are scoped to the category");
        assertEquals(1, orgs.get(1).resourceCount());
    }

    @Test
    void shouldNotPlaceOrganizationsFromUnclassifiedResources() {
        assertTrue(service(List.of(resource("R1", "Housing Alliance")))
                .getForCategory("housing").isEmpty());
    }

    @Test
    void shouldReturnEmptyForUnknownCategoryKey() {
        assertTrue(service(List.of(resource("R1", "Housing Alliance", List.of("Housing"))))
                .getForCategory("nonexistent").isEmpty());
    }
}
