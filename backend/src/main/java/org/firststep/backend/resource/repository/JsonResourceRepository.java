package org.firststep.backend.resource.repository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;

import org.firststep.backend.shared.classification.CivicContentClassifier;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.util.CommunitySlug;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@Repository
public class JsonResourceRepository implements ResourceRepository {

    private final CivicContentClassifier classifier;

    public JsonResourceRepository(CivicContentClassifier classifier) {
        this.classifier = classifier;
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private List<Resource> resources = Collections.emptyList();

    @Value("${app.data.dir:app/data}")
    private String dataDir;

    @Value("${app.default-community-id:wilmington-de}")
    private String defaultCommunityId;

    /**
     * Loads resources after the Spring application is ready.
     * Loads the curated app/data/resources.json plus the structurally-mapped
     * app/data/resources.communities.json (see decisions.md Decision 013),
     * concatenating both into one in-memory list.
     */
@EventListener(ApplicationReadyEvent.class)
public void init() {
    List<Resource> curated = loadFile("resources.json");
    List<Resource> communities = loadFile("resources.communities.json");

    List<Resource> combined = new ArrayList<>(curated);
    combined.addAll(communities);
    resources = combined;

    System.out.println("Loaded " + resources.size() + " total resources ("
            + curated.size() + " from resources.json, "
            + communities.size() + " from resources.communities.json)");
}

/**
 * Loads and parses a single resource JSON file: external file at
 * dataDir/filename first, then classpath /filename as a fallback. Returns
 * an empty list (not an exception) if neither is found.
 */
private List<Resource> loadFile(String filename) {
    Path external = Path.of(dataDir, filename);
    try {
        if (external.toFile().exists()) {
            JsonNode root = mapper.readTree(external.toFile());
            List<Resource> loaded = parseJsonNodeToList(root);
            System.out.println("Loaded resources from " + external + " (" + loaded.size() + " records)");
            return loaded;
        }
    } catch (Exception e) {
        System.err.println("Failed to load " + external + ": " + e.getMessage());
    }

    try (InputStream is = getClass().getResourceAsStream("/" + filename)) {
        if (is != null) {
            JsonNode root = mapper.readTree(is);
            List<Resource> loaded = parseJsonNodeToList(root);
            System.out.println("Loaded resources from classpath " + filename + " (" + loaded.size() + " records)");
            return loaded;
        } else {
            System.out.println("No " + filename + " found on classpath.");
        }
    } catch (Exception e) {
        System.err.println("Failed to load classpath " + filename + ": " + e.getMessage());
    }
    return Collections.emptyList();
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
        return convertResourceArray(root);
    }

    // If it's an object with a common wrapper key
    if (root.isObject()) {
        if (root.has("records") && root.get("records").isArray()) {
            return convertResourceArray(root.get("records"));
        }
        if (root.has("resources") && root.get("resources").isArray()) {
            return convertResourceArray(root.get("resources"));
        }

        // If it's a single resource object, wrap it into a list
        Resource single = mapper.convertValue(root, Resource.class);
        applyContentSourceAndDefaults(single, root);
        return Collections.singletonList(single);
    }

    // Fallback empty
    return Collections.emptyList();
}

/**
 * Deserializes an array node to List<Resource>, then re-walks the same array
 * (by index — Jackson list conversion preserves order) to populate each
 * Resource's contentSource/title/createdDate/updatedDate/communityId from
 * fields the v1 JSON shape carries flat (source, retrieved) or that don't
 * exist in the source data at all (communityId) — see
 * applyContentSourceAndDefaults.
 */
private List<Resource> convertResourceArray(JsonNode arrayNode) {
    List<Resource> parsed = mapper.convertValue(arrayNode, new TypeReference<List<Resource>>() {});
    for (int i = 0; i < parsed.size(); i++) {
        applyContentSourceAndDefaults(parsed.get(i), arrayNode.get(i));
    }
    return parsed;
}

/**
 * Populates the CivicContent fields that don't map 1:1 onto the v1 JSON
 * shape: contentSource is built from the flat source/retrieved keys (Jackson
 * ignores them as unknown properties since Resource has no matching fields);
 * title is derived from organization (the field app.js has always rendered
 * as a resource's display title); createdDate/updatedDate default to the
 * retrieved date (the closest existing timestamp-like value); communityId
 * defaults to app.default-community-id since none of today's data has one.
 */
private void applyContentSourceAndDefaults(Resource resource, JsonNode node) {
    ContentSource contentSource = new ContentSource();
    contentSource.name = node.hasNonNull("source") ? node.get("source").asText() : null;
    contentSource.retrieved = node.hasNonNull("retrieved") ? node.get("retrieved").asText() : null;
    resource.contentSource = contentSource;

    resource.title = resource.organization;
    resource.createdDate = contentSource.retrieved;
    resource.updatedDate = contentSource.retrieved;

    resource.communityId = communityIdFor(resource);

    // Normalize the raw source category into canonical editorial
    // classification. Fills categoryTags only when absent; a resource's
    // subcategory is already editorially assigned and is never touched.
    classifier.classify(resource);
}

/**
 * Derives communityId from the resource's primary location city (e.g.
 * "Newark" -> "newark-de"), falling back to app.default-community-id only
 * when no location/city is available.
 */
private String communityIdFor(Resource resource) {
    if (resource.locations != null && !resource.locations.isEmpty()) {
        String slug = CommunitySlug.forCity(resource.locations.get(0).city);
        if (slug != null) {
            return slug;
        }
    }
    return defaultCommunityId;
}

    @Override
    public List<Resource> findAll() {
        return resources;
    }

    @Override
    public Optional<Resource> findById(String id) {
        return resources.stream().filter(r -> id.equals(r.id)).findFirst();
    }

    // Unused today (no caller wires this in) — carried over as-is from v1's
    // ResourceService rather than removed or newly hooked up; deciding
    // whether/how to surface validation results is out of this pass's scope.
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
