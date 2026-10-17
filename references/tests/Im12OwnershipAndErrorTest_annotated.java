/*
 * Annotated reference: backend/src/test/java/org/firststep/backend/audit/Im12OwnershipAndErrorTest.java
 * WHAT: Executes named regression cases for the identity, Community, provenance or resident-public contract.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Uses actual stored repositories or isolated synthetic fixtures, never mutating production catalogs or invoking remote providers.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
package org.firststep.backend.audit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.firststep.backend.resource.repository.JsonResourceRepository;
import org.firststep.backend.shared.classification.ClassifierFixture;
import org.firststep.backend.ai.service.DecisionAgentService;
import org.firststep.backend.ai.dto.DecisionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

class Im12OwnershipAndErrorTest {
 @TempDir Path directory;
 @Test void im12T10ExplicitOwnerCannotBeReplacedByProvidersPhysicalOfficeCity()throws Exception{
  Files.writeString(directory.resolve("resources.json"),"[{\"id\":\"synthetic-owner\",\"communityId\":\"bear-de\",\"organization\":\"Example offering\",\"category_tags\":[\"Food\"],\"subcategory\":\"Food Pantry\",\"locations\":[{\"city\":\"Wilmington\",\"state\":\"DE\"}]}]");
  var repository=new JsonResourceRepository(ClassifierFixture.real());ReflectionTestUtils.setField(repository,"dataDir",directory.toString());ReflectionTestUtils.setField(repository,"defaultCommunityId","wilmington-de");repository.init();
  var record=repository.findById("synthetic-owner").orElseThrow();assertEquals("bear-de",record.communityId);assertEquals("Wilmington",record.locations.get(0).city);
 }
 @Test void im12T13AiFailureCannotExposeProviderExceptionDetails(){
  var service=new DecisionAgentService(true,List::of,List::of,(prompt,temp)->{throw new IllegalStateException("secret-provider-token /private/provider/config");});
  var result=service.decide(new DecisionRequest());assertTrue(result.citations.isEmpty());assertFalse(result.notes.contains("secret-provider-token"));assertFalse(result.notes.contains("/private/"));
 }
}
