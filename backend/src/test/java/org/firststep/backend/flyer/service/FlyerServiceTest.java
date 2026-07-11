package org.firststep.backend.flyer.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlyerServiceTest {

    private FlyerRepository repository;
    private FlyerService service;

    @BeforeEach
    void setUp() {
        repository = mock(FlyerRepository.class);
        service = new FlyerService(repository);
    }

    @Test
    void shouldReturnAllFlyersFromRepositoryWhenGetAllIsCalled() {
        Flyer flyer = new Flyer();
        flyer.id = "FL-001";
        when(repository.findAll()).thenReturn(List.of(flyer));

        List<Flyer> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals("FL-001", result.get(0).id);
    }

    @Test
    void shouldReturnFlyerByIdFromRepositoryWhenGetByIdIsCalled() {
        Flyer flyer = new Flyer();
        flyer.id = "FL-002";
        when(repository.findById("FL-002")).thenReturn(Optional.of(flyer));

        Optional<Flyer> result = service.getById("FL-002");

        assertTrue(result.isPresent());
        assertEquals("FL-002", result.get().id);
    }
}
