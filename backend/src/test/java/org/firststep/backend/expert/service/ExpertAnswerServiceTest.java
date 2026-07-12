package org.firststep.backend.expert.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.repository.ExpertAnswerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpertAnswerServiceTest {

    private ExpertAnswerRepository repository;
    private ExpertAnswerService service;

    @BeforeEach
    void setUp() {
        repository = mock(ExpertAnswerRepository.class);
        service = new ExpertAnswerService(repository);
    }

    @Test
    void shouldReturnAllExpertAnswersFromRepositoryWhenGetAllIsCalled() {
        ExpertAnswer answer = new ExpertAnswer();
        answer.id = "EA-001";
        when(repository.findAll()).thenReturn(List.of(answer));

        List<ExpertAnswer> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals("EA-001", result.get(0).id);
    }

    @Test
    void shouldReturnExpertAnswerByIdFromRepositoryWhenGetByIdIsCalled() {
        ExpertAnswer answer = new ExpertAnswer();
        answer.id = "EA-002";
        when(repository.findById("EA-002")).thenReturn(Optional.of(answer));

        Optional<ExpertAnswer> result = service.getById("EA-002");

        assertTrue(result.isPresent());
        assertEquals("EA-002", result.get().id);
    }
}
