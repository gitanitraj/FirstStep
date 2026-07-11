package org.firststep.backend.flyer.controller;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.flyer.service.FlyerService;
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

@WebMvcTest(FlyerController.class)
@ContextConfiguration(classes = {FlyerController.class, GlobalExceptionHandler.class, FlyerControllerTest.TestConfig.class})
class FlyerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Configuration
    static class TestConfig {
        @Bean
        FlyerRepository flyerRepository() {
            return new FlyerRepository() {
                @Override
                public List<Flyer> findAll() {
                    Flyer flyer = new Flyer();
                    flyer.id = "FL-001";
                    flyer.title = "Test Flyer";
                    flyer.image = "test.jpg";
                    return List.of(flyer);
                }

                @Override
                public Optional<Flyer> findById(String id) {
                    return Optional.empty();
                }
            };
        }

        @Bean
        FlyerService flyerService(FlyerRepository flyerRepository) {
            return new FlyerService(flyerRepository);
        }
    }

    @Test
    void shouldReturn200WithApiResponseEnvelopeWhenFlyersRequested() throws Exception {
        mockMvc.perform(get("/api/flyers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("FL-001"))
                .andExpect(jsonPath("$.data[0].image").value("test.jpg"));
    }

    @Test
    void shouldReturn404WithApiResponseEnvelopeWhenFlyerIdNotFound() throws Exception {
        mockMvc.perform(get("/api/flyers/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
