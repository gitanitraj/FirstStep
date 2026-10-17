/*
 * Annotated reference: backend/src/test/java/org/firststep/backend/audit/Im12RelationshipTest.java
 * WHAT: Executes named regression cases for the identity, Community, provenance or resident-public contract.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Uses actual stored repositories or isolated synthetic fixtures, never mutating production catalogs or invoking remote providers.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
package org.firststep.backend.audit;

import java.util.List;
import java.util.Optional;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.organization.model.Organization;
import org.firststep.backend.organization.repository.OrganizationRepository;
import org.firststep.backend.organization.service.OrganizationService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.model.ContentSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Synthetic identities deliberately share display names to expose inference. */
class Im12RelationshipTest {
 private Organization org(String id,String slug){var o=new Organization();o.id=id;o.name="Same displayed name";o.slug=slug;return o;}
 private Resource resource(String id,String provider){var r=new Resource();r.id=id;r.title=id;r.communityId="bear-de";r.organization="Same displayed name";r.organizationId=provider;r.categoryTags=List.of("Food");return r;}
 @Test void im12T03T04T05ExplicitProviderSurvivesNameChangeWithoutSourceHostParentOrNameInference(){
  var first=org("ORG-001","first");var second=org("ORG-002","second");
  var explicit=resource("EXPLICIT","ORG-001");explicit.organization="Old provider name";
  var sameName=resource("NAME-ONLY",null);sameName.parentOrganization=first.name;
  sameName.contentSource=new ContentSource();sameName.contentSource.id=first.id;sameName.contentSource.name=first.name;
  var other=resource("OTHER-PROVIDER","ORG-002");
  var resources=mock(ResourceService.class);when(resources.getAll()).thenReturn(List.of(explicit,sameName,other));
  var registry=mock(OrganizationRepository.class);when(registry.findAll()).thenReturn(List.of(first,second));when(registry.findBySlug("first")).thenReturn(Optional.of(first));when(registry.findBySlug("second")).thenReturn(Optional.of(second));
  var service=new OrganizationService(resources,new TaxonomyService("../app/data"),registry);
  assertEquals(List.of("EXPLICIT"),service.getBySlug("first","bear-de").orElseThrow().resources().stream().map(c->c.id()).toList());
  assertEquals(List.of("OTHER-PROVIDER"),service.getBySlug("second","bear-de").orElseThrow().resources().stream().map(c->c.id()).toList());
  assertTrue(service.getBySlug("first","newark-de").orElseThrow().resources().isEmpty());
  assertNull(sameName.organizationId);assertEquals("Old provider name",explicit.organization);
  assertEquals(2,service.getForCategory("food","bear-de").size());
 }
 @Test void im12T12OrganizationWebsiteCannotBypassPublicNavigationGuard(){
  var org=org("ORG-001","first");org.website="javascript:alert(1)";
  var registry=mock(OrganizationRepository.class);when(registry.findBySlug("first")).thenReturn(Optional.of(org));
  var resources=mock(ResourceService.class);when(resources.getAll()).thenReturn(List.of());
  var service=new OrganizationService(resources,new TaxonomyService("../app/data"),registry);
  assertNull(service.getBySlug("first").orElseThrow().website());assertNull(service.getBySlug("first","bear-de").orElseThrow().website());
  org.website="https://example.org";assertEquals(org.website,service.getBySlug("first","bear-de").orElseThrow().website());
 }
}
