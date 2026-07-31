package org.firststep.backend.expert.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.firststep.backend.shared.classification.ClassifierFixture;
import org.firststep.backend.expert.model.FAQ;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFaqRepositoryTest {

    private JsonFaqRepository repositoryFor(String dataDir) {
        JsonFaqRepository repository = new JsonFaqRepository(ClassifierFixture.real());
        ReflectionTestUtils.setField(repository, "dataDir", dataDir);
        ReflectionTestUtils.setField(repository, "defaultCommunityId", "wilmington-de");
        return repository;
    }

    @Test
    void shouldLoadFaqsFromRecordsWrapperKey(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{" +
                "\"id\":\"FAQ-001\",\"question\":\"Test Question\",\"answer\":\"Test Answer\"," +
                "\"source_expert_answer_id\":\"EA-001\"}]}";
        Files.writeString(tempDir.resolve("faq.json"), json);

        JsonFaqRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        List<FAQ> faqs = repository.findAll();
        assertEquals(1, faqs.size());
        assertEquals("FAQ-001", faqs.get(0).id);
        assertEquals("EA-001", faqs.get(0).sourceExpertAnswerId);
    }

    @Test
    void shouldReturnEmptyListWhenFileMissing(@TempDir Path emptyDir) {
        JsonFaqRepository repository = repositoryFor(emptyDir.toString());
        repository.init();

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void shouldDefaultCommunityIdWhenNotPresentInJson(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{\"id\":\"FAQ-002\",\"question\":\"Q\",\"answer\":\"A\"}]}";
        Files.writeString(tempDir.resolve("faq.json"), json);

        JsonFaqRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        assertEquals("wilmington-de", repository.findAll().get(0).communityId);
    }

    @Test
    void shouldNotOverrideCommunityIdWhenPresentInJson(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{\"id\":\"FAQ-003\",\"communityId\":\"newark-de\",\"question\":\"Q\",\"answer\":\"A\"}]}";
        Files.writeString(tempDir.resolve("faq.json"), json);

        JsonFaqRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        assertEquals("newark-de", repository.findAll().get(0).communityId);
    }

    @Test
    void shouldFindFaqByIdAfterLoad(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{\"id\":\"FAQ-004\",\"question\":\"Q\",\"answer\":\"A\"}]}";
        Files.writeString(tempDir.resolve("faq.json"), json);

        JsonFaqRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        Optional<FAQ> found = repository.findById("FAQ-004");
        assertTrue(found.isPresent());
    }
}
