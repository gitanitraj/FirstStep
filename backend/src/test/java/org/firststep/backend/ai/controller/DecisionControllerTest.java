package org.firststep.backend.ai.controller;

import org.firststep.backend.ai.dto.DecisionResponse;
import org.firststep.backend.ai.service.DecisionAgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DecisionController.class)
@ContextConfiguration(classes = {DecisionController.class, DecisionControllerTest.TestConfig.class})
class DecisionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Configuration
    static class TestConfig {
        @Bean
        DecisionAgentService decisionAgentService() {
            DecisionAgentService service = mock(DecisionAgentService.class);
            DecisionResponse canned = new DecisionResponse();
            canned.answerTitle = "Test Guidance";
            canned.steps = List.of();
            canned.citations = List.of();
            canned.notes = "";
            when(service.decide(any())).thenReturn(canned);
            return service;
        }
    }

    @Test
    void shouldReturn200WithApiResponseEnvelopeWhenDecideCalled() throws Exception {
        mockMvc.perform(post("/api/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userQuery\":\"I need housing help\",\"urgent\":false,\"preferredCategories\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answerTitle").value("Test Guidance"));
    }
}
