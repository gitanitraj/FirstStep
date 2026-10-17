/*
 * WHAT: Complete annotated mirror of backend/src/test/java/org/firststep/backend/organization/service/ScopedOrganizationPreviewTest.java for IM-08.
 * WHY: Integrate approved selector outcomes with scoped, accessible resident navigation.
 * INTERACTION: Existing eligibility and collection services own admission; the destination
 * projection allowlists cards/links, while React owns responsive presentation and route scope.
 * ALTERNATIVES: Reimplementing selectors or deriving providers from source names would violate
 * predecessor contracts. Tests cover pass-through, actual catalog HTTP, and browser journeys.
 */
package org.firststep.backend.organization.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.organization.model.Organization;
import org.firststep.backend.organization.repository.OrganizationRepository;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScopedOrganizationPreviewTest {
    @Test void t14ExplicitAssociationIdsAndCommunityExcludeSourceAndNameOnlyInference() {
        Resource local = resource("R", "one", "ORG-1");
        Resource duplicate = resource("R", "one", "ORG-1");
        Resource other = resource("OTHER", "two", "ORG-1");
        Resource noAssociation = resource("NO-ID", "one", null); noAssociation.organization = "Registered";
        Resource wrongCategory = resource("FOOD", "one", "ORG-1"); wrongCategory.categoryTags = List.of("Food");
        ResourceService resources = mock(ResourceService.class);
        when(resources.getAll()).thenReturn(List.of(local, duplicate, other, noAssociation, wrongCategory));
        Organization org = new Organization(); org.id = "ORG-1"; org.name = "Registered"; org.slug = "registered";
        OrganizationRepository registry = mock(OrganizationRepository.class);
        when(registry.findAll()).thenReturn(List.of(org)); when(registry.findBySlug("registered")).thenReturn(Optional.of(org));
        var service = new OrganizationService(resources, new TaxonomyService("../app/data"), registry);
        var result = service.getForCategory("housing", "one");
        assertEquals(1, result.size()); assertEquals(1, result.get(0).resourceCount());
        assertTrue(service.getForCategory("housing", "empty").isEmpty());
        var scoped = service.getBySlug("registered", "two").orElseThrow();
        assertEquals(List.of("OTHER"), scoped.resources().stream().map(c -> c.id()).toList());
    }
    private Resource resource(String id, String community, String org) {
        Resource r = new Resource(); r.id = id; r.title = id; r.communityId = community;
        r.organizationId = org; r.categoryTags = List.of("Housing"); return r;
    }
}
