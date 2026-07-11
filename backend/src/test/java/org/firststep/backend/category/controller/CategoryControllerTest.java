package org.firststep.backend.category.controller;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.category.service.CategoryService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@ContextConfiguration(classes = {CategoryController.class, GlobalExceptionHandler.class, CategoryControllerTest.TestConfig.class})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
            return new CategoryService(resourceService, newsService, flyerService);
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
}
