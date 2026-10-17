/*
 * Annotated reference: backend/src/test/java/org/firststep/backend/audit/Im12CatalogAuditTest.java
 * WHAT: Executes named regression cases for the identity, Community, provenance or resident-public contract.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Uses actual stored repositories or isolated synthetic fixtures, never mutating production catalogs or invoking remote providers.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
package org.firststep.backend.audit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.category.service.CategoryContentService;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.category.selection.AuthoredTopicEvidence;
import org.firststep.backend.expert.repository.ExpertAnswerRepository;
import org.firststep.backend.expert.repository.FaqRepository;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.originals.model.Article;
import org.firststep.backend.originals.repository.ArticleRepository;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.organization.repository.OrganizationRepository;
import org.firststep.backend.organization.service.OrganizationService;
import org.firststep.backend.shared.lifecycle.EditorialLifecycle;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.service.ContentSourceService;
import org.firststep.backend.notices.service.EventOccurrence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Read-only complete served-catalog/endpoint evidence; raw facts are never resident DTOs. */
@SpringBootTest(properties={"app.data.dir=../app/data","news.rss.initial-delay=86400000"})
@AutoConfigureMockMvc @Import(Im12CatalogAuditTest.TimeConfiguration.class)
class Im12CatalogAuditTest {
 @TestConfiguration static class TimeConfiguration {
  @Bean @Primary Clock fixedClock(){return Clock.fixed(Instant.parse("2026-10-17T16:00:00Z"),ZoneOffset.UTC);}
 }
 @Autowired ObjectMapper mapper; @Autowired MockMvc mvc;
 @Autowired ResourceRepository resources; @Autowired FlyerRepository flyers; @Autowired NewsRepository news;
 @Autowired ArticleRepository articles; @Autowired FaqRepository faqs; @Autowired ExpertAnswerRepository experts;
 @Autowired OrganizationRepository organizations; @Autowired OrganizationService providerPages;
 @Autowired EditorialLifecycle lifecycle; @Autowired CategoryContentService content; @Autowired TaxonomyService taxonomy;
 @Autowired AuthoredTopicEvidence proof; @Autowired ContentSourceService sources; @Autowired EventOccurrence occurrence;
 @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;
 private List<CivicContent> raw(){var rows=new ArrayList<CivicContent>();rows.addAll(resources.findAll());rows.addAll(flyers.findAll());rows.addAll(news.findAll());rows.addAll(articles.findAll());rows.addAll(faqs.findAll());rows.addAll(experts.findAll());return rows;}
 private Map<String,Object> map(){return new java.util.LinkedHashMap<>();}
 private void write(String name,Object value)throws Exception{
  String directory=System.getProperty("im12.evidence.dir");if(directory!=null)Files.writeString(Path.of(directory,name+"-"+System.getProperty("im12.phase","after")+".json"),mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)+"\n");
 }
 private void fields(JsonNode node,String path,TreeSet<String> paths){
  if(node.isObject())node.fields().forEachRemaining(e->{String key=path+"."+e.getKey();paths.add(key);fields(e.getValue(),key,paths);});
  else if(node.isArray())node.forEach(child->fields(child,path+"[]",paths));
 }
 @Test void im12T02T03T06T07T09T15T17CompleteRawCurrentAndProjectionIdentityAudit()throws Exception{
  String before=mapper.writeValueAsString(raw());var rows=new ArrayList<Map<String,Object>>();var communities=new TreeMap<String,Map<String,Integer>>();
  var globalIds=new TreeMap<String,Integer>();var links=new ArrayList<Map<String,Object>>();
  for(CivicContent item:raw()){
   globalIds.merge(item.id,1,Integer::sum);assertNotNull(item.communityId,item.id);
   communities.computeIfAbsent(item.communityId,k->new TreeMap<>()).merge(item.getClass().getSimpleName(),1,Integer::sum);
   var row=map();row.put("id",item.id);row.put("kind",item.getClass().getSimpleName());row.put("contentType",item.contentType);row.put("communityId",item.communityId);
   row.put("source",item.contentSource);row.put("producerSector",item.contentSource==null?null:sources.sectorOf(item.contentSource.id).orElse(null));row.put("categoryTags",item.categoryTags);row.put("topic",item.subcategory);
   row.put("editorial",lifecycle.assess(item));row.put("publicStatus",lifecycle.isPublicStatus(item));row.put("createdDate",item.createdDate);row.put("published",item.publishDate);
   row.put("articleApproved",item instanceof Article a?a.isPublishable():null);
   row.put("authoredCategoriesTopics",taxonomy.getCategories().stream().filter(c->proof.permits(item,c)).map(c->c.key()).toList());
   if(item instanceof Resource r){
    row.put("organizationId",r.organizationId);row.put("providerDisplay",r.organization);row.put("parentDisplayOnly",r.parentOrganization);row.put("locations",r.locations);
    var link=map();link.put("resourceId",r.id);link.put("communityId",r.communityId);link.put("organizationId",r.organizationId);link.put("display",r.organization);link.put("parentDisplayOnly",r.parentOrganization);
    var org=organizations.findById(r.organizationId).orElse(null);link.put("registry",org);link.put("sourceId",r.contentSource==null?null:r.contentSource.id);
    link.put("associationMeaning",org==null?"missing_explicit_provider_reference":"explicit_resource_provider_id");
    if(org!=null)link.put("scopedProviderResourceIds",providerPages.getBySlug(org.slug,r.communityId).orElseThrow().resources().stream().map(c->c.id()).toList());
    links.add(link);
   }
   if(item instanceof org.firststep.backend.flyer.model.Flyer f){row.put("eventDate",f.eventDate);row.put("startsAt",f.startsAt);row.put("endsAt",f.endsAt);row.put("occurrence",occurrence.assess(f));row.put("helpAccess",f.helpAccess);row.put("image",f.image);}
   if(item instanceof org.firststep.backend.news.model.NewsItem n){row.put("authoredCategoryTags",n.authoredCategoryTags);row.put("authoredSubcategory",n.authoredSubcategory);if(n.tags!=null&&n.tags.contains("event"))row.put("occurrence",occurrence.assess(n));}
   rows.add(row);
  }
  var projections=new TreeMap<String,Object>();
  for(String community:communities.keySet()){
   var categories=new TreeMap<String,Object>();
   for(var category:taxonomy.getCategories()){
    var collections=content.collectionsFor(category.key(),community);
    for(var collection:collections)assertEquals(collection.count(),collection.items().stream().map(c->c.id()).distinct().count());
    categories.put(category.key(),collections.stream().map(c->Map.of("key",c.key(),"count",c.count(),"ids",c.items().stream().map(i->i.id()).toList())).toList());
   }
   projections.put(community,categories);
  }
  var audit=map();audit.put("clock","2026-10-17T16:00:00Z");audit.put("rows",rows);audit.put("communities",communities);audit.put("duplicateIds",globalIds.entrySet().stream().filter(e->e.getValue()>1).toList());
  audit.put("organizations",organizations.findAll());audit.put("providerTraces",links);audit.put("projections",projections);audit.put("hostRepresentation","No typed HOST association persistence; Flyer organizer/source/location are not promoted into Resource PROVIDER links");
  write("catalog",audit);assertEquals(before,mapper.writeValueAsString(raw()));assertTrue(globalIds.values().stream().allMatch(c->c==1));assertEquals(36,content.helpFor("housing",null,"wilmington-de").size());assertEquals(8,flyers.findAll().size());
 }
 @Test void im12T11ReadOnlyEveryResidentEndpointAndEveryObservedCommunityResponseInventory()throws Exception{
  var endpoints=new ArrayList<Map<String,Object>>();
  for(var entry:mappings.getHandlerMethods().entrySet())if(entry.getValue().getBeanType().getName().startsWith("org.firststep")){
   var endpoint=map();endpoint.put("paths",entry.getKey().getPatternValues());endpoint.put("verbs",entry.getKey().getMethodsCondition().getMethods());endpoint.put("handler",entry.getValue().toString());endpoint.put("returnType",entry.getValue().getReturnType().getGenericParameterType().toString());endpoints.add(endpoint);
  }
  endpoints.sort(java.util.Comparator.comparing(e->e.get("paths").toString()));
  var responses=new TreeMap<String,Object>();
  var sampleOrg=organizations.findAll().get(0).slug;
  var paths=new ArrayList<>(List.of("/api/health","/api/resources","/api/resources/HA-001","/api/news","/api/news/rss","/api/flyers","/api/flyers/FL-008","/api/faqs","/api/faqs/FAQ-002","/api/expert-answers","/api/expert-answers/EA-005","/api/home","/api/find-help","/api/categories","/api/category/housing?communityId=wilmington-de","/api/category/housing/help?communityId=wilmington-de","/api/category/housing/eviction-prevention?communityId=wilmington-de","/api/organization/"+sampleOrg,"/api/updates","/api/updates/government","/api/updates/community","/api/updates/first-step","/api/community-notices","/api/community-notices/events","/api/community-notices/meetings","/api/community-notices/announcements","/api/community-notices/flyers","/api/search?q=housing&communityId=wilmington-de","/api/originals/OR-003","/api/originals/OR-001","/api/originals/OR-002","/api/originals/missing","/api/seasonal-images"));
  for(String community:raw().stream().map(i->i.communityId).distinct().sorted().toList()){
   var resource=resources.findAll().stream().filter(r->community.equals(r.communityId)).findFirst().orElse(null);
   paths.add("/api/category/housing/help?communityId="+community);paths.add("/api/news?communityId="+community);paths.add("/api/community-notices?communityId="+community);
   if(resource!=null)paths.add("/api/search?q="+java.net.URLEncoder.encode(resource.organization,java.nio.charset.StandardCharsets.UTF_8)+"&communityId="+community);
  }
  for(String path:paths){var response=mvc.perform(get(path)).andReturn().getResponse();JsonNode body;
   try{body=mapper.readTree(response.getContentAsString());}catch(Exception e){body=mapper.getNodeFactory().textNode(response.getContentAsString());}
   var row=map();var pathsFound=new TreeSet<String>();fields(body,"$",pathsFound);row.put("status",response.getStatus());row.put("body",body);row.put("fieldPaths",pathsFound);responses.put(path,row);
  }
  var response=mvc.perform(post("/api/decide").contentType("application/json").content("{\"userQuery\":\"\"}")).andReturn().getResponse();responses.put("POST /api/decide",Map.of("status",response.getStatus(),"body",mapper.readTree(response.getContentAsString())));
  write("endpoints",endpoints);write("http",responses);
 }
 private void publicFields(JsonNode node) {
  if(node.isObject()) node.fields().forEachRemaining(e -> {
   assertFalse(java.util.Set.of("verified","status","expirationDate","createdDate","editorialReview","helpAccess","retrieved","authoredCategoryTags","authoredSubcategory","diagnostics","tieStage","generatedBy").contains(e.getKey()),e.getKey());
   publicFields(e.getValue());
  }); else if(node.isArray()) node.forEach(this::publicFields);
 }
 private void ownedIds(JsonNode node,String owner,Map<String,CivicContent> catalog,TreeSet<String> seen) {
  if(node.isObject()) {
   String id=node.path("id").asText();
   if(catalog.containsKey(id)){assertEquals(owner,catalog.get(id).communityId,id);seen.add(id);}
   node.elements().forEachRemaining(n -> ownedIds(n,owner,catalog,seen));
  } else if(node.isArray()) node.forEach(n -> ownedIds(n,owner,catalog,seen));
 }
 @Test void im12T08T09T11T12T17EveryCommunityCategoryTopicProviderAndDirectScopeIsIsolated()throws Exception {
  var catalog=raw().stream().collect(java.util.stream.Collectors.toMap(c->c.id,c->c));
  var result=new TreeMap<String,Object>();
  for(String owner:raw().stream().map(c->c.communityId).distinct().sorted().toList()) {
   var paths=new ArrayList<>(List.of("/api/resources","/api/faqs","/api/expert-answers","/api/flyers","/api/news","/api/news/rss","/api/categories","/api/search?q=food","/api/community-notices","/api/community-notices/events","/api/community-notices/meetings","/api/community-notices/announcements","/api/community-notices/flyers"));
   for(var category:taxonomy.getCategories()) {
    paths.add("/api/category/"+category.key());paths.add("/api/category/"+category.key()+"/help");
    for(String topic:category.subcategories())paths.add("/api/category/"+category.key()+"/"+TaxonomyService.topicSlug(topic));
   }
   for(var org:organizations.findAll())paths.add("/api/organization/"+org.slug);
   for(String path:paths) {
    var response=mvc.perform(get(path).param("communityId",owner)).andReturn().getResponse();
    assertEquals(200,response.getStatus(),path+" "+owner);
    JsonNode body=mapper.readTree(response.getContentAsString());publicFields(body);
    var ids=new TreeSet<String>();ownedIds(body,owner,catalog,ids);
    var found=new TreeSet<String>();fields(body,"$",found);
    result.put(owner+" "+path,Map.of("status",response.getStatus(),"ids",ids,"fieldPaths",found));
   }
  }
  var housing=content.collectionsFor("housing","wilmington-de");
  assertEquals(List.of("OR-003","FAQ-002","EA-001","EA-005"),housing.stream().filter(c->c.key().equals("first-rate-news")).findFirst().orElseThrow().items().stream().map(c->c.id()).toList());
  for(String owner:raw().stream().map(c->c.communityId).distinct().toList())for(var category:taxonomy.getCategories())for(var collection:content.collectionsFor(category.key(),owner))if(collection.key().equals("first-rate-news"))assertTrue(collection.items().stream().noneMatch(c->List.of("EA-002","EA-003","EA-004","EA-006").contains(c.id())));
  write("scoped-endpoints",result);
 }
}
