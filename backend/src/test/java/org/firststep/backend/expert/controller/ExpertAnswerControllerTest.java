package org.firststep.backend.expert.controller;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.repository.ExpertAnswerRepository;
import org.firststep.backend.expert.service.ExpertAnswerService;
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

@WebMvcTest(ExpertAnswerController.class)
@ContextConfiguration(classes = {ExpertAnswerController.class, GlobalExceptionHandler.class, ExpertAnswerControllerTest.TestConfig.class})
class ExpertAnswerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Configuration
    static class TestConfig {
        @Bean
        ExpertAnswerRepository expertAnswerRepository() {
            return new ExpertAnswerRepository() {
                @Override
                public List<ExpertAnswer> findAll() {
                    ExpertAnswer answer = new ExpertAnswer();
                    answer.id = "EA-001";
                    answer.question = "Test Question";
                    return List.of(answer);
                }

                @Override
                public Optional<ExpertAnswer> findById(String id) {
                    return Optional.empty();
                }
            };
        }

        @Bean
        ExpertAnswerService expertAnswerService(ExpertAnswerRepository expertAnswerRepository) {
            return new ExpertAnswerService(expertAnswerRepository);
        }
    }

    @Test
    void shouldReturn200WithApiResponseEnvelopeWhenExpertAnswersRequested() throws Exception {
        mockMvc.perform(get("/api/expert-answers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("EA-001"))
                .andExpect(jsonPath("$.data[0].question").value("Test Question"));
    }

    @Test
    void shouldReturn404WithApiResponseEnvelopeWhenExpertAnswerIdNotFound() throws Exception {
        mockMvc.perform(get("/api/expert-answers/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
