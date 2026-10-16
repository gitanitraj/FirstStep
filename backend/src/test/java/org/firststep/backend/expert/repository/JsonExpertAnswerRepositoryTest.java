package org.firststep.backend.expert.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.firststep.backend.shared.classification.ClassifierFixture;
import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.shared.service.ContentSourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonExpertAnswerRepositoryTest {

    private JsonExpertAnswerRepository repositoryFor(String dataDir) {
        JsonExpertAnswerRepository repository = new JsonExpertAnswerRepository(ClassifierFixture.real(), new ContentSourceService("../app/data"));
        ReflectionTestUtils.setField(repository, "dataDir", dataDir);
        ReflectionTestUtils.setField(repository, "defaultCommunityId", "wilmington-de");
        return repository;
    }

    @Test
    void shouldLoadExpertAnswersFromRecordsWrapperKey(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{" +
                "\"id\":\"EA-001\",\"title\":\"Test Question\",\"question\":\"Test Question\"," +
                "\"answer\":\"Test Answer\",\"expert_name\":\"Jane Doe\"}]}";
        Files.writeString(tempDir.resolve("expert-answers.json"), json);

        JsonExpertAnswerRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        List<ExpertAnswer> answers = repository.findAll();
        assertEquals(1, answers.size());
        assertEquals("EA-001", answers.get(0).id);
        assertEquals("Jane Doe", answers.get(0).expertName);
    }

    @Test
    void shouldReturnEmptyListWhenFileMissing(@TempDir Path emptyDir) {
        JsonExpertAnswerRepository repository = repositoryFor(emptyDir.toString());
        repository.init();

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void shouldDefaultCommunityIdWhenNotPresentInJson(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{\"id\":\"EA-002\",\"question\":\"Q\",\"answer\":\"A\"}]}";
        Files.writeString(tempDir.resolve("expert-answers.json"), json);

        JsonExpertAnswerRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        assertEquals("wilmington-de", repository.findAll().get(0).communityId);
    }

    @Test
    void shouldNotOverrideCommunityIdWhenPresentInJson(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{\"id\":\"EA-003\",\"communityId\":\"newark-de\",\"question\":\"Q\",\"answer\":\"A\"}]}";
        Files.writeString(tempDir.resolve("expert-answers.json"), json);

        JsonExpertAnswerRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        assertEquals("newark-de", repository.findAll().get(0).communityId);
    }

    @Test
    void shouldFindExpertAnswerByIdAfterLoad(@TempDir Path tempDir) throws IOException {
        String json = "{\"records\":[{\"id\":\"EA-004\",\"question\":\"Q\",\"answer\":\"A\"}]}";
        Files.writeString(tempDir.resolve("expert-answers.json"), json);

        JsonExpertAnswerRepository repository = repositoryFor(tempDir.toString());
        repository.init();

        Optional<ExpertAnswer> found = repository.findById("EA-004");
        assertTrue(found.isPresent());
    }
}
