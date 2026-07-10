package org.firststep.backend.resource.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceServiceTest {

    private ResourceRepository repository;
    private ResourceService service;

    @BeforeEach
    void setUp() {
        repository = mock(ResourceRepository.class);
        service = new ResourceService(repository);
    }

    @Test
    void shouldReturnAllResourcesFromRepositoryWhenGetAllIsCalled() {
        Resource resource = new Resource();
        resource.id = "r1";
        when(repository.findAll()).thenReturn(List.of(resource));

        List<Resource> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals("r1", result.get(0).id);
    }

    @Test
    void shouldReturnResourceByIdFromRepositoryWhenGetByIdIsCalled() {
        Resource resource = new Resource();
        resource.id = "r2";
        when(repository.findById("r2")).thenReturn(Optional.of(resource));

        Optional<Resource> result = service.getById("r2");

        assertTrue(result.isPresent());
        assertEquals("r2", result.get().id);
    }
}
