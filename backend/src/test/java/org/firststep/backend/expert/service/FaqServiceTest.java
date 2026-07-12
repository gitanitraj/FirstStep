package org.firststep.backend.expert.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.repository.FaqRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FaqServiceTest {

    private FaqRepository repository;
    private FaqService service;

    @BeforeEach
    void setUp() {
        repository = mock(FaqRepository.class);
        service = new FaqService(repository);
    }

    @Test
    void shouldReturnAllFaqsFromRepositoryWhenGetAllIsCalled() {
        FAQ faq = new FAQ();
        faq.id = "FAQ-001";
        when(repository.findAll()).thenReturn(List.of(faq));

        List<FAQ> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals("FAQ-001", result.get(0).id);
    }

    @Test
    void shouldReturnFaqByIdFromRepositoryWhenGetByIdIsCalled() {
        FAQ faq = new FAQ();
        faq.id = "FAQ-002";
        when(repository.findById("FAQ-002")).thenReturn(Optional.of(faq));

        Optional<FAQ> result = service.getById("FAQ-002");

        assertTrue(result.isPresent());
        assertEquals("FAQ-002", result.get().id);
    }
}
