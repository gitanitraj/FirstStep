package org.firststep.backend.search.controller;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.search.service.SearchService;
import org.firststep.backend.shared.web.GlobalExceptionHandler;
import org.firststep.backend.shared.service.ContentSourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@ContextConfiguration(classes = {SearchController.class, GlobalExceptionHandler.class, SearchControllerTest.TestConfig.class})
@TestPropertySource(properties = "app.default-community-id=wilmington-de")
class SearchControllerTest {

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
                    r.organization = "Beautiful Gate Outreach Center";
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
            return new FlyerService(flyerRepository, new ContentSourceService("../app/data"));
        }

        @Bean
        SearchService searchService(ResourceService resourceService, NewsService newsService, FlyerService flyerService) {
            return new SearchService(resourceService, newsService, flyerService);
        }
    }

    @Test
    void shouldReturn200WithApiResponseEnvelopeWhenQueryMatches() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "beautiful"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].type").value("resource"))
                .andExpect(jsonPath("$.data[0].content.id").value("CI-001"));
    }

    @Test
    void shouldReturn200WithEmptyDataWhenNoMatch() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldReturn400WhenQueryParamMissing() throws Exception {
        mockMvc.perform(get("/api/search"))
                .andExpect(status().isBadRequest());
    }
}
