package org.firststep.backend.expert.repository;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JsonExpertAnswerRepository implements ExpertAnswerRepository {

    private final ObjectMapper mapper = new ObjectMapper();
    private List<ExpertAnswer> expertAnswers = Collections.emptyList();

    @Value("${app.data.dir:app/data}")
    private String dataDir;

    @Value("${app.default-community-id:wilmington-de}")
    private String defaultCommunityId;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Path external = Path.of(dataDir, "expert-answers.json");
        try {
            if (external.toFile().exists()) {
                JsonNode root = mapper.readTree(external.toFile());
                expertAnswers = parseJsonNodeToList(root);
                System.out.println("Loaded expert answers from " + external + " (" + expertAnswers.size() + " records)");
                return;
            }
        } catch (Exception e) {
            System.err.println("Failed to load " + external + ": " + e.getMessage());
        }

        try (InputStream is = getClass().getResourceAsStream("/expert-answers.json")) {
            if (is != null) {
                JsonNode root = mapper.readTree(is);
                expertAnswers = parseJsonNodeToList(root);
                System.out.println("Loaded expert answers from classpath expert-answers.json (" + expertAnswers.size() + " records)");
            } else {
                System.out.println("No expert-answers.json found on classpath.");
            }
        } catch (Exception e) {
            System.err.println("Failed to load classpath expert-answers.json: " + e.getMessage());
        }
    }

    private List<ExpertAnswer> parseJsonNodeToList(JsonNode root) throws JsonProcessingException {
        if (root == null || root.isNull()) {
            return Collections.emptyList();
        }
        if (root.isArray()) {
            return convertArray(root);
        }
        if (root.isObject()) {
            if (root.has("records") && root.get("records").isArray()) {
                return convertArray(root.get("records"));
            }
            if (root.has("expertAnswers") && root.get("expertAnswers").isArray()) {
                return convertArray(root.get("expertAnswers"));
            }
            ExpertAnswer single = mapper.convertValue(root, ExpertAnswer.class);
            applyDefaults(single);
            return Collections.singletonList(single);
        }
        return Collections.emptyList();
    }

    private List<ExpertAnswer> convertArray(JsonNode arrayNode) {
        List<ExpertAnswer> parsed = mapper.convertValue(arrayNode, new TypeReference<List<ExpertAnswer>>() {});
        parsed.forEach(this::applyDefaults);
        return parsed;
    }

    private void applyDefaults(ExpertAnswer expertAnswer) {
        if (expertAnswer.communityId == null) {
            expertAnswer.communityId = defaultCommunityId;
        }
    }

    @Override
    public List<ExpertAnswer> findAll() {
        return expertAnswers;
    }

    @Override
    public Optional<ExpertAnswer> findById(String id) {
        return expertAnswers.stream().filter(e -> id.equals(e.id)).findFirst();
    }
}
