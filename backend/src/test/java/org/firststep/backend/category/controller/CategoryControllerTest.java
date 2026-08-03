package org.firststep.backend.category.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.firststep.backend.category.dto.CategoryMetadata;
import org.firststep.backend.category.dto.CategoryPage;
import org.firststep.backend.category.service.CategoryPageService;
import org.firststep.backend.category.service.CategoryService;
import org.firststep.backend.navigation.dto.TopicGroup;
import org.firststep.backend.navigation.dto.TopicNavigation;
import org.firststep.backend.organization.dto.OrgSummary;
import org.firststep.backend.shared.classification.ClassifierFixture;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.web.GlobalExceptionHandler;
import org.firststep.backend.updates.dto.UpdateItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@ContextConfiguration(classes = {CategoryController.class, GlobalExceptionHandler.class, CategoryControllerTest.TestConfig.class})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // The controller only routes, so the aggregate is mocked here: this test covers
    // path binding, the response envelope and the unknown-key path. Composition
    // correctness is CategoryPageServiceTest's job, against real services.
    @MockitoBean
    private CategoryPageService categoryPageService;

    @Configuration
    static class TestConfig {
        @Bean
        ResourceRepository resourceRepository() {
            return new ResourceRepository() {
                @Override
                public List<Resource> findAll() {
                    Resource r = new Resource();
                    r.id = "CI-001";
                    r.communityId = "wilmington-de";
                    r.category = "Housing Assistance";
                    r.contentSource = new ContentSource();
                    r.contentSource.id = "dscyf-directory";
                    // This stub stands in for JsonResourceRepository, which
                    // classifies at load (Slice F2). Without it the resource
                    // reaches CategoryService with no canonical categoryTags and
                    // counts toward nothing — the repository, not the query
                    // layer, is what normalizes the raw source category now.
                    ClassifierFixture.real().classify(r);
                    return List.of(r);
                }

                @Override
                public Optional<Resource> findById(String id) {
                    return Optional.empty();
                }
            };
        }

        @Bean
        NewsRepository newsRepository() {
            return List::of;
        }

        @Bean
        FlyerRepository flyerRepository() {
            return new FlyerRepository() {
                @Override
                public List<Flyer> findAll() {
                    return List.of();
                }

                @Override
                public Optional<Flyer> findById(String id) {
                    return Optional.empty();
                }
            };
        }

        @Bean
        ResourceService resourceService(ResourceRepository resourceRepository) {
            return new ResourceService(resourceRepository);
        }

        @Bean
        NewsService newsService(NewsRepository newsRepository) {
            return new NewsService(newsRepository);
        }

        @Bean
        FlyerService flyerService(FlyerRepository flyerRepository) {
            return new FlyerService(flyerRepository);
        }

        @Bean
        CategoryService categoryService(ResourceService resourceService, NewsService newsService, FlyerService flyerService) {
            return new CategoryService(new TaxonomyService("../app/data"), resourceService, newsService, flyerService);
        }
    }

    @Test
    void shouldReturn200WithApiResponseEnvelopeContainingTenCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(10));
    }

    @Test
    void shouldReflectSeedResourceInHousingCategoryCount() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.key=='housing')].resourceCount").value(1));
    }

    // ---- GET /api/category/{key} — the category page BFF (Slice F5a) --------

    private static CategoryPage housingPage() {
        return new CategoryPage(
                new CategoryMetadata("housing", "Housing", "🏠", 73,
                        Map.of(ContentType.RESOURCE, 44, ContentType.LAW, 20), "2026-06-15"),
                List.of(new UpdateItem("news", ContentType.LAW, "L1", "A signed bill", "Summary",
                        "2026-06-15", "Delaware Legislature", "https://example.gov/L1", null,
                        List.of("Housing"))),
                List.of(new TopicGroup("Need Help Right Away",
                        List.of(new TopicNavigation("Emergency Shelter", "emergency-shelter", 12,
                                Map.of(ContentType.RESOURCE, 12))))),
                List.of(),
                List.of(new OrgSummary("Housing Alliance", "housing-alliance", 12)));
    }

    @Test
    void shouldReturnAllThreePillarsInOneResponse() throws Exception {
        when(categoryPageService.getByKey("housing", null)).thenReturn(Optional.of(housingPage()));

        mockMvc.perform(get("/api/category/housing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metadata.label").value("Housing"))
                .andExpect(jsonPath("$.data.metadata.totalCount").value(73))
                .andExpect(jsonPath("$.data.metadata.countsByType.RESOURCE").value(44))
                // Stay Informed — and contentType distinguishes a law from news.
                .andExpect(jsonPath("$.data.updates[0].contentType").value("LAW"))
                // Discover
                .andExpect(jsonPath("$.data.groups[0].label").value("Need Help Right Away"))
                .andExpect(jsonPath("$.data.groups[0].topics[0].slug").value("emergency-shelter"))
                // Connect
                .andExpect(jsonPath("$.data.organizations[0].slug").value("housing-alliance"));
    }

    @Test
    void shouldReturn404WhenCategoryKeyIsNotInTheTaxonomy() throws Exception {
        when(categoryPageService.getByKey(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/category/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void shouldPassCommunityIdThroughToTheAggregate() throws Exception {
        when(categoryPageService.getByKey("housing", "wilmington-de"))
                .thenReturn(Optional.of(housingPage()));

        mockMvc.perform(get("/api/category/housing").param("communityId", "wilmington-de"))
                .andExpect(status().isOk());

        verify(categoryPageService).getByKey("housing", "wilmington-de");
    }
}
