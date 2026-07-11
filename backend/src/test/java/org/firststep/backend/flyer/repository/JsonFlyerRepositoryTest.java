package org.firststep.backend.flyer.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.firststep.backend.flyer.model.Flyer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFlyerRepositoryTest {

    private JsonFlyerRepository repositoryFor(String dataDir) {
        JsonFlyerRepository repository = new JsonFlyerRepository();
        ReflectionTestUtils.setField(repository, "dataDir", dataDir);
        ReflectionTestUtils.setField(repository, "defaultCommunityId", "wilmington-de");
        return repository;
    }

    @Test
    void shouldLoadFlyersFromRecordsWrapperKey(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{" +
                "\"id\":\"FL-001\",\"title\":\"Youth Program\",\"organization\":\"West End\"," +
                "\"event_date\":\"2026-08-01\",\"image\":\"Youth.jpg\"}]}";
        Files.writeString(tempDir.resolve("flyers.json"), json);

        JsonFlyerRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        List<Flyer> flyers = repository.findAll();
        assertEquals(1, flyers.size());
        assertEquals("FL-001", flyers.get(0).id);
        assertEquals("Youth.jpg", flyers.get(0).image);
    }

    @Test
    void shouldReturnEmptyListWhenFileMissing(@TempDir Path emptyDir) {
        JsonFlyerRepository repository = repositoryFor(emptyDir.toString());
        repository.init();

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void shouldDefaultCommunityIdWhenNotPresentInJson(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{\"id\":\"FL-002\",\"title\":\"Test\",\"organization\":\"Org\",\"image\":\"x.jpg\"}]}";
        Files.writeString(tempDir.resolve("flyers.json"), json);

        JsonFlyerRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        assertEquals("wilmington-de", repository.findAll().get(0).communityId);
    }

    @Test
    void shouldNotOverrideCommunityIdWhenPresentInJson(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{\"id\":\"FL-003\",\"communityId\":\"newark-de\",\"title\":\"Test\"," +
                "\"organization\":\"Org\",\"image\":\"x.jpg\"}]}";
        Files.writeString(tempDir.resolve("flyers.json"), json);

        JsonFlyerRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        assertEquals("newark-de", repository.findAll().get(0).communityId);
    }

    @Test
    void shouldFindFlyerByIdAfterLoad(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{\"id\":\"FL-004\",\"title\":\"Test\",\"organization\":\"Org\",\"image\":\"x.jpg\"}]}";
        Files.writeString(tempDir.resolve("flyers.json"), json);

        JsonFlyerRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        Optional<Flyer> found = repository.findById("FL-004");
        assertTrue(found.isPresent());
    }
}
