package org.firststep.backend.resource.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.firststep.backend.resource.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonResourceRepositoryTest {

    private JsonResourceRepository repositoryFor(String dataDir) {
        JsonResourceRepository repository = new JsonResourceRepository();
        ReflectionTestUtils.setField(repository, "dataDir", dataDir);
        ReflectionTestUtils.setField(repository, "defaultCommunityId", "wilmington-de");
        return repository;
    }

    @Test
    void shouldLoadResourcesFromExternalFileWhenPresent(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{" +
                "\"id\":\"CI-001\",\"organization\":\"Beautiful Gate\",\"category\":\"Clothing\"," +
                "\"source\":\"Test Directory\",\"retrieved\":\"2024-01-01\",\"verified\":true}]}";
        Files.writeString(tempDir.resolve("resources.json"), json);

        JsonResourceRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        List<Resource> resources = repository.findAll();
        assertEquals(1, resources.size());
        assertEquals("CI-001", resources.get(0).id);
    }

    @Test
    void shouldFallBackToClasspathWhenExternalFileMissing(@TempDir Path emptyDir) {
        // No /resources.json on the test classpath either -> empty, not an exception.
        JsonResourceRepository repository = repositoryFor(emptyDir.toString());
        repository.init();

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void shouldMapFlatSourceAndRetrievedIntoContentSource(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{" +
                "\"id\":\"CI-002\",\"organization\":\"Food Bank\",\"category\":\"Food\"," +
                "\"source\":\"FIRST Directory\",\"retrieved\":\"2024-03-15\",\"verified\":true}]}";
        Files.writeString(tempDir.resolve("resources.json"), json);

        JsonResourceRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        Resource resource = repository.findAll().get(0);
        assertEquals("FIRST Directory", resource.contentSource.name);
        assertEquals("2024-03-15", resource.contentSource.retrieved);
        assertEquals("Food Bank", resource.title);
        assertEquals("2024-03-15", resource.createdDate);
        assertEquals("2024-03-15", resource.updatedDate);
    }

    @Test
    void shouldDefaultCommunityIdWhenNotPresentInJson(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{\"id\":\"CI-003\",\"organization\":\"Org\",\"category\":\"Cat\"}]}";
        Files.writeString(tempDir.resolve("resources.json"), json);

        JsonResourceRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        assertEquals("wilmington-de", repository.findAll().get(0).communityId);
    }

    @Test
    void shouldFindResourceByIdAfterLoad(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{\"id\":\"CI-004\",\"organization\":\"Org\",\"category\":\"Cat\"}]}";
        Files.writeString(tempDir.resolve("resources.json"), json);

        JsonResourceRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        Optional<Resource> found = repository.findById("CI-004");
        assertTrue(found.isPresent());
    }
}
