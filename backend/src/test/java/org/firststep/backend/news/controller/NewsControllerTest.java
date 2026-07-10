package org.firststep.backend.news.controller;

import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.shared.model.ContentSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NewsController.class)
@ContextConfiguration(classes = {NewsController.class, NewsControllerTest.TestConfig.class})
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    static NewsItem testItem() {
        NewsItem item = new NewsItem();
        item.id = "rss-123";
        item.title = "Test Headline";
        item.summary = "Test Summary";
        item.body = "Test Summary";

        ContentSource contentSource = new ContentSource();
        contentSource.name = "Test Source";
        contentSource.url = "http://example.com";
        item.contentSource = contentSource;

        item.published = "2024-01-01";
        item.active = true;
        item.type = "general-news";
        item.urgency = "standard";
        item.tags = List.of("Community", "Updates");
        item.whyItMatters = "Testing";
        return item;
    }

    @Configuration
    static class TestConfig {
        @Bean
        RssFeedSource rssFeedSource() {
            return () -> List.of(testItem());
        }

        @Bean
        NewsRepository newsRepository() {
            return List::of;
        }

        @Bean
        NewsService newsService(NewsRepository newsRepository) {
            return new NewsService(newsRepository);
        }
    }

    @Test
    void testGetRssNews() throws Exception {
        mockMvc.perform(get("/api/news/rss")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("rss-123"))
                .andExpect(jsonPath("$.data[0].title").value("Test Headline"))
                .andExpect(jsonPath("$.data[0].summary").value("Test Summary"))
                .andExpect(jsonPath("$.data[0].contentSource.name").value("Test Source"))
                .andExpect(jsonPath("$.data[0].contentSource.url").value("http://example.com"))
                .andExpect(jsonPath("$.data[0].published").value("2024-01-01"))
                .andExpect(jsonPath("$.data[0].active").value(true))
                .andExpect(jsonPath("$.data[0].type").value("general-news"))
                .andExpect(jsonPath("$.data[0].urgency").value("standard"))
                .andExpect(jsonPath("$.data[0].tags[0]").value("Community"))
                .andExpect(jsonPath("$.data[0].tags[1]").value("Updates"))
                .andExpect(jsonPath("$.data[0].why_it_matters").value("Testing"));
    }
}
