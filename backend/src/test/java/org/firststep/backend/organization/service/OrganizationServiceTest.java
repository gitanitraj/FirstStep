package org.firststep.backend.organization.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.organization.dto.OrgSummary;
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
        return new OrganizationService(new ResourceService(repo), new TaxonomyService("../app/data"));
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
    void shouldSlugifyOrganizationName() {
        OrganizationService service = service(List.of(
                resource("R1", "Be Ready CDC - Esther's Place")));

        OrgSummary org = service.getCuratedShortlist().get(0);

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
