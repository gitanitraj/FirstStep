package org.firststep.backend.expert.controller;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.repository.FaqRepository;
import org.firststep.backend.expert.service.FaqService;
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

@WebMvcTest(FaqController.class)
@ContextConfiguration(classes = {FaqController.class, GlobalExceptionHandler.class, FaqControllerTest.TestConfig.class})
class FaqControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Configuration
    static class TestConfig {
        @Bean
        FaqRepository faqRepository() {
            return new FaqRepository() {
                @Override
                public List<FAQ> findAll() {
                    FAQ faq = new FAQ();
                    faq.id = "FAQ-001";
                    faq.question = "Test Question";
                    return List.of(faq);
                }

                @Override
                public Optional<FAQ> findById(String id) {
                    return Optional.empty();
                }
            };
        }

        @Bean
        FaqService faqService(FaqRepository faqRepository) {
            return new FaqService(faqRepository);
        }
    }

    @Test
    void shouldReturn200WithApiResponseEnvelopeWhenFaqsRequested() throws Exception {
        mockMvc.perform(get("/api/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("FAQ-001"))
                .andExpect(jsonPath("$.data[0].question").value("Test Question"));
    }

    @Test
    void shouldReturn404WithApiResponseEnvelopeWhenFaqIdNotFound() throws Exception {
        mockMvc.perform(get("/api/faqs/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
