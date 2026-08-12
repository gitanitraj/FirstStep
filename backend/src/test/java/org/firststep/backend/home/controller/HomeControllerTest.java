package org.firststep.backend.home.controller;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.repository.FaqRepository;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.home.service.HomeService;
import org.firststep.backend.home.service.PathwayService;
import org.firststep.backend.legislation.service.LegislationService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.web.GlobalExceptionHandler;
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
            // exercises actual composition rather than a mocked payload.
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

            // Two FAQs with DIFFERENT producers, so the Originals filter has
            // something to reject as well as something to accept — an EXPERT item
            // First Step did not make must not become an "Original".
            FAQ ours = new FAQ();
            ours.id = "FAQ-001";
            ours.title = "How do I apply for SNAP benefits?";
            ours.summary = "Apply online through Delaware ASSIST.";
            ours.updatedDate = "2026-07-11";
            ours.contentSource = contentSource("first-step", "First Step");

            FAQ theirs = new FAQ();
            theirs.id = "FAQ-999";
            theirs.title = "Someone else's answer";
            theirs.contentSource = contentSource(null, "Delaware Volunteer Legal Services");

            FaqRepository faqRepo = new FaqRepository() {
                @Override
                public List<FAQ> findAll() {
                    return List.of(ours, theirs);
                }

                @Override
                public Optional<FAQ> findById(String id) {
                    return Optional.empty();
                }
            };

            FlyerService flyerService = new FlyerService(flyerRepo);
            FaqService faqService = new FaqService(faqRepo);

            TaxonomyService taxonomyService = new TaxonomyService("../app/data");
            PathwayService pathwayService = new PathwayService(taxonomyService, "../app/data");
            LegislationService legislationService = new LegislationService(() -> List.of(bill));
            return new HomeService(pathwayService, faqService, legislationService, flyerService);
        }

        private static ContentSource contentSource(String id, String name) {
            ContentSource source = new ContentSource();
            source.id = id;
            source.name = name;
            return source;
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
                // The seven authored Community Resources pathways. Housing leads,
                // and its label/icon are RESOLVED from taxonomy.json rather than
                // authored in homepage.json — that is the anti-drift rule.
                .andExpect(jsonPath("$.data.communityResources[0].key").value("housing"))
                .andExpect(jsonPath("$.data.communityResources[0].label").value("Housing"))
                .andExpect(jsonPath("$.data.communityResources[0].kind").value("category"))
                // Seniors is a DISCOVERY pathway, never a category.
                .andExpect(jsonPath("$.data.communityResources[?(@.key=='seniors')].kind").value("discovery"))
                // First Step Originals: ours in, theirs out — both are EXPERT, so
                // only contentSource can tell them apart.
                .andExpect(jsonPath("$.data.originals.length()").value(1))
                .andExpect(jsonPath("$.data.originals[0].id").value("FAQ-001"))
                .andExpect(jsonPath("$.data.originals[0].organization").value("First Step"))
                // Recent signed bills (from the fake RSS source).
                .andExpect(jsonPath("$.data.delawareLaws[0].title").value("Relating to Housing Supply and Housing Affordability."))
                // Community flyer carousel — imageUrl resolved + encoded server-side.
                .andExpect(jsonPath("$.data.communityFlyers[0].imageUrl").value("/images/seasonal/Health%20Fair.jpg"))
                // The homepage carries neither an organizations column nor an
                // updates feed. Organizations moved behind Connect → Find Help;
                // the feed became two destination pages split by producer
                // (Latest Updates = government, Community Notices = community).
                .andExpect(jsonPath("$.data.organizations").doesNotExist())
                .andExpect(jsonPath("$.data.updates").doesNotExist());
    }
}
