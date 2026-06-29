package org.firststep.backend.controller;

import org.firststep.backend.service.ResourceService;
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

@WebMvcTest(ResourceController.class)
@ContextConfiguration(classes = {ResourceController.class, ResourceControllerTest.TestConfig.class})
@TestPropertySource(properties = "app.seasonal.images.dir=src/test/resources/seasonal-test")
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Configuration
    static class TestConfig {
        @Bean
        ResourceService resourceService() {
            return new ResourceService();
        }
    }

    @Test
    void shouldReturnSeasonalImagesFromConfiguredDirectory() throws Exception {
        mockMvc.perform(get("/api/seasonal-images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("images/seasonal/Sample.png"));
    }
}
