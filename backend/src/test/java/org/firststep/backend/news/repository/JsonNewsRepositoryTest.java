package org.firststep.backend.news.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.firststep.backend.shared.classification.ClassifierFixture;
import org.firststep.backend.news.model.NewsItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonNewsRepositoryTest {

    private JsonNewsRepository repositoryFor(String dataDir) {
        JsonNewsRepository repository = new JsonNewsRepository(ClassifierFixture.real());
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
    void shouldMapSourceNameAndSourceUrlIntoContentSource(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{" +
                "\"id\":\"NP-002\",\"headline\":\"Test Headline\",\"summary\":\"Test Summary\"," +
                "\"source_name\":\"Delaware News\",\"source_url\":\"http://example.com/news\"," +
                "\"published\":\"2024-02-01\"}]}";
        Files.writeString(tempDir.resolve("news.json"), json);

        JsonNewsRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        NewsItem item = repository.findAll().get(0);
        assertEquals("Delaware News", item.contentSource.name);
        assertEquals("http://example.com/news", item.contentSource.url);
        assertEquals("Test Headline", item.title);
        assertEquals("2024-02-01", item.createdDate);
        assertEquals("wilmington-de", item.communityId);
    }
}
