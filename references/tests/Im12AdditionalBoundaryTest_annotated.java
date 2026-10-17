/*
 * Annotated reference: backend/src/test/java/org/firststep/backend/audit/Im12AdditionalBoundaryTest.java
 * WHAT: Executes named regression cases for the identity, Community, provenance or resident-public contract.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Uses actual stored repositories or isolated synthetic fixtures, never mutating production catalogs or invoking remote providers.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
package org.firststep.backend.audit;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.ai.controller.DecisionController;
import org.firststep.backend.ai.service.DecisionAgentService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.shared.dto.ResidentContent;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.Location;
import org.firststep.backend.shared.model.Website;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Synthetic boundary cases. No remote AI invocation or production catalog mutation. */
class Im12AdditionalBoundaryTest {
 private Resource resource(String id,String owner){var r=new Resource();r.id=id;r.communityId=owner;r.summary="food";r.organization="Example";return r;}
 @Test void im12T09AiExplicitCommunityFiltersRetrievalBeforePrompt()throws Exception{
  var own=resource("BEAR-ONLY","bear-de");var other=resource("WILMINGTON-ONLY","wilmington-de");
  var prompt=new java.util.concurrent.atomic.AtomicReference<String>();
  var service=new DecisionAgentService(true,()->List.of(own,other),List::of,(p,t)->{prompt.set(p);return "{\"steps\":[],\"citations\":[]}";});
  var mvc=MockMvcBuilders.standaloneSetup(new DecisionController(service)).build();
  mvc.perform(post("/api/decide?communityId=bear-de").contentType("application/json").content("{\"userQuery\":\"food\"}"));
  assertTrue(prompt.get().contains("BEAR-ONLY"));assertFalse(prompt.get().contains("WILMINGTON-ONLY"));
 }
 @Test void im12T12AiCitationPreservesProducerWithoutRetrievalAudit()throws Exception{
  var r=resource("R1","bear-de");r.contentSource=new ContentSource();r.contentSource.id="SRC-X";r.contentSource.name="Producer";r.contentSource.url="https://example.org";r.contentSource.retrieved="PRIVATE-RETRIEVAL";
  var service=new DecisionAgentService(true,()->List.of(r),List::of,(p,t)->"{\"steps\":[],\"citations\":[{\"id\":\"R1\",\"sourceType\":\"resource\",\"label\":\"Offering\"}]}");
  var mvc=MockMvcBuilders.standaloneSetup(new DecisionController(service)).build();
  var json=mvc.perform(post("/api/decide").contentType("application/json").content("{\"userQuery\":\"food\"}")).andReturn().getResponse().getContentAsString();
  assertTrue(json.contains("SRC-X"));assertFalse(json.contains("retrieved"));assertFalse(json.contains("PRIVATE-RETRIEVAL"));
 }
 @Test void im12T12ConfidentialAddressAndUnsafeContactCannotBecomeResidentNavigation()throws Exception{
  var r=resource("R1","bear-de");var location=new Location();location.confidential=true;location.address="PRIVATE-SHELTER";location.zip="PRIVATE-ZIP";location.city="Wilmington";location.state="DE";r.locations=List.of(location);
  var site=new Website();site.url="javascript:alert(1)";site.label="Contact";r.websites=List.of(site);
  var json=new ObjectMapper().valueToTree(ResidentContent.from(r));
  assertTrue(json.path("locations").get(0).path("address").isNull());assertTrue(json.path("locations").get(0).path("zip").isNull());
  assertEquals("DE",json.path("locations").get(0).path("state").asText());assertTrue(json.path("websites").get(0).path("url").isNull());
 }
}
