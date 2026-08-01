package org.firststep.backend.home.controller;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.category.service.CategoryService;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.home.service.HomeService;
import org.firststep.backend.legislation.service.LegislationService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.organization.service.OrganizationService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.web.GlobalExceptionHandler;
import org.firststep.backend.updates.service.UpdatesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
@ContextConfiguration(classes = {HomeController.class, GlobalExceptionHandler.class, HomeControllerTest.TestConfig.class})
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Configuration
    static class TestConfig {
        @Bean
        HomeService homeService() {
            // Wire the REAL aggregators with fake repositories so the endpoint
            // exercises actual composition (aiConfig + updates + categories).
            NewsItem news = new NewsItem();
            news.id = "N1";
            news.title = "A new law";
            news.summary = "Summary";
            news.publishDate = "2026-05-01";

            Resource resource = new Resource();
            resource.id = "CI-001";
            resource.communityId = "wilmington-de";
            resource.category = "Housing Assistance";
            resource.organization = "American Red Cross";

            ResourceRepository resourceRepo = new ResourceRepository() {
                @Override
                public List<Resource> findAll() {
                    return List.of(resource);
                }

                @Override
                public Optional<Resource> findById(String id) {
                    return Optional.empty();
                }
            };
            Flyer flyer = new Flyer();
            flyer.id = "FL-1";
            flyer.title = "Free Community Health Fair";
            flyer.organization = "Westside Family Healthcare";
            flyer.image = "Health Fair.jpg";
            flyer.eventDate = "2026-08-05";
            FlyerRepository flyerRepo = new FlyerRepository() {
                @Override
                public List<Flyer> findAll() {
                    return List.of(flyer);
                }

                @Override
                public Optional<Flyer> findById(String id) {
                    return Optional.empty();
                }
            };

            NewsItem bill = new NewsItem();
            bill.id = "B1";
            bill.title = "Relating to Housing Supply and Housing Affordability.";
            bill.publishDate = "2026-07-13";

            NewsService newsService = new NewsService(() -> List.of(news));
            RssFeedSource rssSource = () -> List.of(bill);
            FlyerService flyerService = new FlyerService(flyerRepo);
            ResourceService resourceService = new ResourceService(resourceRepo);

            UpdatesService updatesService = new UpdatesService(newsService, rssSource, flyerService);
            CategoryService categoryService = new CategoryService(new TaxonomyService("../app/data"), resourceService, newsService, flyerService);
            OrganizationService organizationService = new OrganizationService(resourceService);
            LegislationService legislationService = new LegislationService(() -> List.of(bill));
            return new HomeService(updatesService, categoryService, organizationService, legislationService, flyerService);
        }
    }

    @Test
    void shouldReturnAggregatedHomePayload() throws Exception {
        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // Static AI config, backend-owned.
                .andExpect(jsonPath("$.data.aiConfig.placeholder").exists())
                .andExpect(jsonPath("$.data.aiConfig.chips[0].value").value("urgent"))
                .andExpect(jsonPath("$.data.aiConfig.chips[0].urgent").value(true))
                // Curated organization shortlist (the fake resource's org).
                .andExpect(jsonPath("$.data.organizations[0].name").exists())
                .andExpect(jsonPath("$.data.organizations[0].slug").exists())
                // Recent signed bills (from the fake RSS source).
                .andExpect(jsonPath("$.data.delawareLaws[0].title").value("Relating to Housing Supply and Housing Affordability."))
                // Community flyer carousel — imageUrl resolved + encoded server-side.
                .andExpect(jsonPath("$.data.communityFlyers[0].imageUrl").value("/images/seasonal/Health%20Fair.jpg"))
                // Composed feeds. The curated news is present (order-independent:
                // the RSS bill also merges into updates and may sort ahead by date).
                .andExpect(jsonPath("$.data.updates[?(@.id=='N1')]").exists())
                .andExpect(jsonPath("$.data.categories").isArray());
    }
}
