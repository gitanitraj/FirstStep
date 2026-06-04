package org.firststep.backend.service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;

import org.firststep.backend.model.Resource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class ResourceService {

    private final ObjectMapper mapper = new ObjectMapper();
    private List<Resource> resources = Collections.emptyList();

    /**
     * Loads resources after the Spring application is ready.
     * Tries external canonical file app/data/resources.json first,
     * then falls back to classpath /resources.json.
     */
@EventListener(ApplicationReadyEvent.class)
public void init() {
    // Try external canonical file first
    try {
        Path external = Path.of("app", "data", "resources.json");
        if (external.toFile().exists()) {
            JsonNode root = mapper.readTree(external.toFile());
            resources = parseJsonNodeToList(root);
            System.out.println("Loaded resources from app/data/resources.json (" + resources.size() + " records)");
            return;
        }
    } catch (Exception e) {
        System.err.println("Failed to load app/data/resources.json: " + e.getMessage());
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

    public List<Resource> getAll() {
        return resources;
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
