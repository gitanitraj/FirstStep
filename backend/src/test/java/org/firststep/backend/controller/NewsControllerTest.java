package org.firststep.backend.controller;

import org.firststep.backend.model.NewsItem;
import org.firststep.backend.service.NewsService;
import org.firststep.backend.service.RssFeedSource;
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
        item.headline = "Test Headline";
        item.summary = "Test Summary";
        item.body = "Test Summary";
        item.sourceName = "Test Source";
        item.sourceUrl = "http://example.com";
        item.published = "2024-01-01";
        item.active = true;
        item.type = "general-news";
        item.urgency = "standard";
        item.categoryTags = List.of("Community", "Updates");
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
        NewsService newsService() {
            return new NewsService();
        }
    }

    @Test
    void testGetRssNews() throws Exception {
        mockMvc.perform(get("/api/news/rss")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("rss-123"))
                .andExpect(jsonPath("$[0].headline").value("Test Headline"))
                .andExpect(jsonPath("$[0].summary").value("Test Summary"))
                .andExpect(jsonPath("$[0].source_name").value("Test Source"))
                .andExpect(jsonPath("$[0].source_url").value("http://example.com"))
                .andExpect(jsonPath("$[0].published").value("2024-01-01"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].type").value("general-news"))
                .andExpect(jsonPath("$[0].urgency").value("standard"))
                .andExpect(jsonPath("$[0].category_tags[0]").value("Community"))
                .andExpect(jsonPath("$[0].category_tags[1]").value("Updates"))
                .andExpect(jsonPath("$[0].why_it_matters").value("Testing"));
    }
}
