package org.firststep.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.model.Resource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        // Try to load canonical JSON from project root app/data/resources.json first
        try {
            Path external = Path.of("app", "data", "resources.json");
            if (external.toFile().exists()) {
                resources = mapper.readValue(external.toFile(), new TypeReference<List<Resource>>() {});
                System.out.println("Loaded resources from app/data/resources.json");
                return;
            }
        } catch (Exception e) {
            System.err.println("Failed to load app/data/resources.json: " + e.getMessage());
        }

        // Fallback: load packaged resources.json from classpath (src/main/resources)
        try (InputStream is = getClass().getResourceAsStream("/resources.json")) {
            if (is != null) {
                resources = mapper.readValue(is, new TypeReference<List<Resource>>() {});
                System.out.println("Loaded resources from classpath resources.json");
            } else {
                System.out.println("No resources.json found on classpath.");
            }
        } catch (Exception e) {
            System.err.println("Failed to load classpath resources.json: " + e.getMessage());
        }
    }

    public List<Resource> getAll() {
        return resources;
    }

    public Optional<Resource> getById(String id) {
        return resources.stream().filter(r -> id.equals(r.id)).findFirst();
    }
}
