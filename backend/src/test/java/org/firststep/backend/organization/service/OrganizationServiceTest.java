package org.firststep.backend.organization.service;

import java.util.List;
import java.util.Optional;

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
        return new OrganizationService(new ResourceService(repo));
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
}
