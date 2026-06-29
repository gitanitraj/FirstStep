package org.firststep.backend.service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;

import org.firststep.backend.model.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class ResourceService implements DecisionAgentService.ResourceServiceLike {

    private final ObjectMapper mapper = new ObjectMapper();
    private List<Resource> resources = Collections.emptyList();

    @Value("${app.data.dir:app/data}")
    private String dataDir;

    /**
     * Loads resources after the Spring application is ready.
     * Tries external canonical file app/data/resources.json first,
     * then falls back to classpath /resources.json.
     */
@EventListener(ApplicationReadyEvent.class)
public void init() {
    // Try external canonical file first
    Path external = Path.of(dataDir, "resources.json");
    try {
        if (external.toFile().exists()) {
            JsonNode root = mapper.readTree(external.toFile());
            resources = parseJsonNodeToList(root);
            System.out.println("Loaded resources from " + external + " (" + resources.size() + " records)");
            return;
        }
    } catch (Exception e) {
        System.err.println("Failed to load " + external + ": " + e.getMessage());
    }

    // Fallback: classpath resources.json
    try (InputStream is = getClass().getResourceAsStream("/resources.json")) {
        if (is != null) {
            JsonNode root = mapper.readTree(is);
            resources = parseJsonNodeToList(root);
            System.out.println("Loaded resources from classpath resources.json (" + resources.size() + " records)");
        } else {
            System.out.println("No resources.json found on classpath.");
        }
    } catch (Exception e) {
        System.err.println("Failed to load classpath resources.json: " + e.getMessage());
    }
}

/**
 * Helper to convert a JsonNode into List<Resource> supporting multiple top-level shapes.
 */
private List<Resource> parseJsonNodeToList(JsonNode root) throws JsonProcessingException {
    if (root == null || root.isNull()) {
        return Collections.emptyList();
    }

    // If it's already an array, deserialize directly
    if (root.isArray()) {
        return mapper.convertValue(root, new TypeReference<List<Resource>>() {});
    }

    // If it's an object with a common wrapper key
    if (root.isObject()) {
        if (root.has("records") && root.get("records").isArray()) {
            return mapper.convertValue(root.get("records"), new TypeReference<List<Resource>>() {});
        }
        if (root.has("resources") && root.get("resources").isArray()) {
            return mapper.convertValue(root.get("resources"), new TypeReference<List<Resource>>() {});
        }

        // If it's a single resource object, wrap it into a list
        return Collections.singletonList(mapper.convertValue(root, Resource.class));
    }

    // Fallback empty
    return Collections.emptyList();
}

    @Override
    public List<Resource> getAllResources() {
        return resources;
    }

    // existing endpoint uses getAllResources
    public List<Resource> getAll() {
        return getAllResources();
    }

    public Optional<Resource> getById(String id) {
        return resources.stream().filter(r -> id.equals(r.id)).findFirst();
    }

    private void validateResources(List<Resource> resources) {
    int missingIds = 0;
    int missingCategories = 0;

    for (Resource r : resources) {

        if (r.id == null || r.id.isBlank()) {
            missingIds++;
        }

        if (r.category == null || r.category.isBlank()) {
            missingCategories++;
        }
    }

    System.out.println(
        "Validation summary: "
        + missingIds + " missing ids, "
        + missingCategories + " missing categories"
    );
    }
}
