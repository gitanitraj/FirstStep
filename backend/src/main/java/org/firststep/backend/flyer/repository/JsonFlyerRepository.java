package org.firststep.backend.flyer.repository;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;

import org.firststep.backend.flyer.model.Flyer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JsonFlyerRepository implements FlyerRepository {

    private final ObjectMapper mapper = new ObjectMapper();
    private List<Flyer> flyers = Collections.emptyList();

    @Value("${app.data.dir:app/data}")
    private String dataDir;

    @Value("${app.default-community-id:wilmington-de}")
    private String defaultCommunityId;

    /**
     * Loads flyers after the Spring application is ready.
     * Tries external canonical file app/data/flyers.json first,
     * then falls back to classpath /flyers.json.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Path external = Path.of(dataDir, "flyers.json");
        try {
            if (external.toFile().exists()) {
                JsonNode root = mapper.readTree(external.toFile());
                flyers = parseJsonNodeToList(root);
                System.out.println("Loaded flyers from " + external + " (" + flyers.size() + " records)");
                return;
            }
        } catch (Exception e) {
            System.err.println("Failed to load " + external + ": " + e.getMessage());
        }

        try (InputStream is = getClass().getResourceAsStream("/flyers.json")) {
            if (is != null) {
                JsonNode root = mapper.readTree(is);
                flyers = parseJsonNodeToList(root);
                System.out.println("Loaded flyers from classpath flyers.json (" + flyers.size() + " records)");
            } else {
                System.out.println("No flyers.json found on classpath.");
            }
        } catch (Exception e) {
            System.err.println("Failed to load classpath flyers.json: " + e.getMessage());
        }
    }

    /**
     * Helper to convert a JsonNode into List<Flyer> supporting multiple top-level shapes.
     * Unlike Resource/News, no v1 legacy shape exists for Flyer — flyers.json is authored
     * to already match this class's shape, so no post-deserialization field-mapping
     * adapter is needed; only communityId gets defaulted when absent.
     */
    private List<Flyer> parseJsonNodeToList(JsonNode root) throws JsonProcessingException {
        if (root == null || root.isNull()) {
            return Collections.emptyList();
        }

        if (root.isArray()) {
            return convertFlyerArray(root);
        }

        if (root.isObject()) {
            if (root.has("records") && root.get("records").isArray()) {
                return convertFlyerArray(root.get("records"));
            }
            if (root.has("flyers") && root.get("flyers").isArray()) {
                return convertFlyerArray(root.get("flyers"));
            }

            Flyer single = mapper.convertValue(root, Flyer.class);
            applyDefaults(single);
            return Collections.singletonList(single);
        }

        return Collections.emptyList();
    }

    private List<Flyer> convertFlyerArray(JsonNode arrayNode) {
        List<Flyer> parsed = mapper.convertValue(arrayNode, new TypeReference<List<Flyer>>() {});
        parsed.forEach(this::applyDefaults);
        return parsed;
    }

    private void applyDefaults(Flyer flyer) {
        if (flyer.communityId == null) {
            flyer.communityId = defaultCommunityId;
        }
    }

    @Override
    public List<Flyer> findAll() {
        return flyers;
    }

    @Override
    public Optional<Flyer> findById(String id) {
        return flyers.stream().filter(f -> id.equals(f.id)).findFirst();
    }
}
