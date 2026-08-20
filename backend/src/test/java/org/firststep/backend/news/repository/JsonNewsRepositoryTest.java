package org.firststep.backend.news.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.firststep.backend.shared.classification.ClassifierFixture;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.shared.service.ContentSourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonNewsRepositoryTest {

    private JsonNewsRepository repositoryFor(String dataDir) {
        JsonNewsRepository repository = new JsonNewsRepository(ClassifierFixture.real(), new ContentSourceService("../app/data"));
        ReflectionTestUtils.setField(repository, "dataDir", dataDir);
        ReflectionTestUtils.setField(repository, "defaultCommunityId", "wilmington-de");
        return repository;
    }

    @Test
    void shouldLoadNewsFromRecordsWrapperKey(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{" +
                "\"id\":\"NP-001\",\"headline\":\"Test Headline\",\"summary\":\"Test Summary\"," +
                "\"published\":\"2024-01-01\"}]}";
        Files.writeString(tempDir.resolve("news.json"), json);

        JsonNewsRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        List<NewsItem> items = repository.findAll();
        assertEquals(1, items.size());
        assertEquals("NP-001", items.get(0).id);
    }

    @Test
    void shouldReturnEmptyListWhenFileMissing(@TempDir Path emptyDir) {
        JsonNewsRepository repository = repositoryFor(emptyDir.toString());
        repository.init();

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void shouldResolveTheProducerNameFromTheRegistryRatherThanTheRecord(@TempDir Path tempDir) throws IOException {
        // The record names its producer by ID ONLY. The display name comes from
        // content-sources.json, which is what stops one agency appearing under two
        // spellings — "Delaware DHSS" and "Delaware Health and Social Services"
        // were the same body before the registry existed.
        String json = "{\"records\":[{" +
                "\"id\":\"NP-002\",\"headline\":\"Test Headline\",\"summary\":\"Test Summary\"," +
                "\"source_id\":\"de-dhss\",\"source_url\":\"http://example.com/news\"," +
                "\"published\":\"2024-02-01\"}]}";
        Files.writeString(tempDir.resolve("news.json"), json);

        JsonNewsRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        NewsItem item = repository.findAll().get(0);
        assertEquals("de-dhss", item.contentSource.id);
        assertEquals("Delaware Health and Social Services", item.contentSource.name);
        // The URL stays per-record: it is THIS item's link, not the producer's.
        assertEquals("http://example.com/news", item.contentSource.url);
        assertEquals("Test Headline", item.title);
        assertEquals("2024-02-01", item.createdDate);
        assertEquals("wilmington-de", item.communityId);
    }

    @Test
    void shouldLoadTheItemAnywayWhenItsProducerIsUnknown(@TempDir Path tempDir) throws IOException {
        // THE FAILURE BOUNDARY. An unresolvable producer costs the item its
        // attribution and its place in sector views — nothing else. It is still
        // loaded, still classified, still browsable. Provenance resolution is a
        // capability, not a validity gate (Decision 045).
        String json = "{\"records\":[{" +
                "\"id\":\"NP-099\",\"headline\":\"Orphan Headline\",\"summary\":\"S\"," +
                "\"source_id\":\"de-department-of-education\",\"published\":\"2024-02-01\"}]}";
        Files.writeString(tempDir.resolve("news.json"), json);

        JsonNewsRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        NewsItem item = repository.findAll().get(0);
        assertEquals("Orphan Headline", item.title);
        assertEquals("de-department-of-education", item.contentSource.id);
        assertNull(item.contentSource.name, "an unknown producer must not be given a guessed name");
    }
}
