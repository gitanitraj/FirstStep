package org.firststep.backend.notices.controller;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.notices.service.CommunityNoticesService;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.service.ContentSourceService;
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

/**
 * The URL contract for Community Notices.
 *
 * <p>The point of these tests is that the URL is the source of truth: each of the
 * five routes must answer on its own, without the caller having passed through
 * the landing route first, and an unknown view must be told apart from an empty
 * one.
 */
@WebMvcTest(CommunityNoticesController.class)
@ContextConfiguration(classes = {CommunityNoticesController.class, GlobalExceptionHandler.class,
        CommunityNoticesControllerTest.TestConfig.class})
class CommunityNoticesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Configuration
    static class TestConfig {
        @Bean
        CommunityNoticesService communityNoticesService() {
            Flyer f = new Flyer();
            f.id = "FL-1";
            f.title = "Community health fair";
            f.summary = "Free screenings";
            f.contentType = ContentType.FLYER;
            f.organization = "Westside Family Healthcare";
            f.eventDate = "2026-09-12";
            f.tags = List.of("event");
            ContentSource cs = new ContentSource();
            cs.id = "westside-family-healthcare";
            cs.name = "Westside Family Healthcare";
            f.contentSource = cs;

            FlyerRepository flyerRepo = new FlyerRepository() {
                @Override
                public List<Flyer> findAll() {
                    return List.of(f);
                }

                @Override
                public Optional<Flyer> findById(String id) {
                    return Optional.of(f).filter(x -> x.id.equals(id));
                }
            };
            return new CommunityNoticesService(new FlyerService(flyerRepo, new ContentSourceService("../app/data")), new NewsService(List::of),
                    new TaxonomyService("../app/data"), new ContentSourceService("../app/data"));
        }
    }

    @Test
    void shouldReturnLandingStateWithCountsAndPreviewsWhenNoViewIsGiven() throws Exception {
        mockMvc.perform(get("/api/community-notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.view").value("OVERVIEW"))
                .andExpect(jsonPath("$.data.counts.EVENTS").value(1))
                // A destination, not a redirect: the landing route carries content.
                .andExpect(jsonPath("$.data.previews[0].items[0].id").value("FL-1"));
    }

    @Test
    void shouldAnswerAViewRouteDirectlyWithoutVisitingTheLandingRouteFirst() throws Exception {
        mockMvc.perform(get("/api/community-notices/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.view").value("EVENTS"))
                .andExpect(jsonPath("$.data.items[0].id").value("FL-1"))
                // Counts ride along so the nav cards never fill in late.
                .andExpect(jsonPath("$.data.counts.FLYERS").value(1));
    }

    @Test
    void shouldReturnEmptyItemsWithHttpOkWhenAViewExistsButHasNothingInIt() throws Exception {
        // An empty view is a valid answer. Contrast with the next test: these two
        // facts must not share a status code.
        mockMvc.perform(get("/api/community-notices/meetings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.view").value("MEETINGS"))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void shouldReturnNotFoundNamingTheUnknownViewRatherThanFallingBackToLanding() throws Exception {
        mockMvc.perform(get("/api/community-notices/newsletters"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                // The specific diagnostic, not merely a failure: a bad link must
                // say what was not recognized, and must not be answerable by the
                // landing page quietly rendering instead.
                .andExpect(jsonPath("$.errorMessage").value("Unknown notices view: newsletters"));
    }
}
