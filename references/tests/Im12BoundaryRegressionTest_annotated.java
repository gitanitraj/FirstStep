/*
 * Annotated reference: backend/src/test/java/org/firststep/backend/audit/Im12BoundaryRegressionTest.java
 * WHAT: Executes named regression cases for the identity, Community, provenance or resident-public contract.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Uses actual stored repositories or isolated synthetic fixtures, never mutating production catalogs or invoking remote providers.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
package org.firststep.backend.audit;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.data.dir=../app/data","news.rss.initial-delay=86400000"})
@AutoConfigureMockMvc @Import(Im12CatalogAuditTest.TimeConfiguration.class)
class Im12BoundaryRegressionTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;
 @ParameterizedTest @ValueSource(strings={"/api/resources","/api/resources/HA-001","/api/faqs","/api/faqs/FAQ-002","/api/expert-answers","/api/expert-answers/EA-001","/api/search?q=housing","/api/categories"})
 void im12T11T12ResidentEndpointsCannotBypassEditorialPrivacyThroughRawModels(String path)throws Exception{
  var response=mvc.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();noPrivate(mapper.readTree(response),"$");
 }
 private void noPrivate(JsonNode node,String path){
  if(node.isObject())node.fields().forEachRemaining(e->{
   assertFalse(List.of("verified","status","expirationDate","createdDate","editorialReview","helpAccess","retrieved","authoredCategoryTags","authoredSubcategory","diagnostics","tieStage","generatedBy").contains(e.getKey()),path+"."+e.getKey());noPrivate(e.getValue(),path+"."+e.getKey());
  });else if(node.isArray())node.forEach(child->noPrivate(child,path+"[]"));
 }
 @ParameterizedTest @ValueSource(strings={"/api/resources","/api/faqs","/api/expert-answers","/api/flyers"})
 void im12T09ExplicitUnknownCommunityMustNotReturnAggregateInventory(String path)throws Exception{
  mvc.perform(get(path).param("communityId","not-an-owning-community")).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
 }
 @ParameterizedTest @ValueSource(strings={"/api/resources/HA-001","/api/faqs/FAQ-002","/api/expert-answers/EA-001","/api/flyers/FL-008","/api/originals/OR-003"})
 void im12T09T13ByIdScopeCannotReturnAnotherCommunitysRecord(String path)throws Exception{
  mvc.perform(get(path).param("communityId","bear-de")).andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
 }
 @Test void im12T13UnexpectedExceptionMustNotExposeIngestionOrFilesystemDetails(){
  var response=new GlobalExceptionHandler().handleUnexpected(new IllegalStateException("secret-ingestion-token /private/source/path"));
  assertEquals(500,response.getStatusCode().value());assertEquals("INTERNAL_ERROR",response.getBody().errorCode);
  assertFalse(response.getBody().errorMessage.contains("secret-ingestion-token"));assertFalse(response.getBody().errorMessage.contains("/private/"));
 }
 @Test void im12T08T12ApprovedArticleRevisionAndGeographicStateRemainPublic()throws Exception{
  mvc.perform(get("/api/originals/OR-003")).andExpect(status().isOk()).andExpect(jsonPath("$.data.updatedDate").exists());
  mvc.perform(get("/api/resources/HA-001")).andExpect(status().isOk()).andExpect(jsonPath("$.data.locations[0].state").value("DE"));
  for(String id:List.of("OR-001","OR-002","missing"))mvc.perform(get("/api/originals/"+id)).andExpect(status().isNotFound()).andExpect(jsonPath("$.errorMessage").value("Article not found: "+id));
 }
}
