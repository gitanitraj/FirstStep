package org.firststep.backend.updates.controller;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.web.GlobalExceptionHandler;
import org.firststep.backend.updates.service.UpdatesService;
import org.firststep.backend.shared.service.ContentSourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UpdatesController.class)
@ContextConfiguration(classes = {UpdatesController.class, GlobalExceptionHandler.class, UpdatesControllerTest.TestConfig.class})
class UpdatesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Configuration
    static class TestConfig {
        @Bean
        UpdatesService updatesService() {
            NewsItem n = new NewsItem();
            n.id = "N1";
            n.title = "A new law";
            n.summary = "Summary";
            n.publishDate = "2026-05-01";
            n.urgency = "high";
            ContentSource cs = new ContentSource();
            cs.name = "Delaware Legislature";
            cs.url = "https://example.gov/N1";
            n.contentSource = cs;

            NewsService newsService = new NewsService(() -> List.of(n));
            RssFeedSource rssSource = List::of;
            FlyerRepository flyerRepo = new FlyerRepository() {
                @Override
                public List<Flyer> findAll() {
                    return List.of();
                }

                @Override
                public Optional<Flyer> findById(String id) {
                    return Optional.empty();
                }
            };
            return new UpdatesService(newsService, rssSource, new FlyerService(flyerRepo),
                    mock(ExpertAnswerService.class), mock(FaqService.class),
                    new TaxonomyService("../app/data"), new ContentSourceService("../app/data"));
        }
    }

    @Test
    void shouldReturnNormalizedUpdatesFeed() throws Exception {
        mockMvc.perform(get("/api/updates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].contentType").value("NEWS"))
                // The legacy `type` must not be in the payload at all — the
                // criterion is deletion, not deprecation (Decision 045).
                .andExpect(jsonPath("$.data[0].type").doesNotExist())
                .andExpect(jsonPath("$.data[0].id").value("N1"))
                .andExpect(jsonPath("$.data[0].source").value("Delaware Legislature"))
                .andExpect(jsonPath("$.data[0].urgency").value("high"));
    }
}
