package org.firststep.backend.resource.repository;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// JsonResourceRepository is the JSON-file-backed implementation of
// ResourceRepository. It loads app/data/resources.json (or falls back to a
// classpath copy) at startup and holds the parsed Resource list in memory —
// the exact loading mechanism v1's ResourceService used, moved here
// unchanged, plus new logic to populate the CivicContent fields the v1 JSON
// shape doesn't carry directly.
// =============================================================================

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;

import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.shared.model.ContentSource;
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

    private final ObjectMapper mapper = new ObjectMapper();
    private List<Resource> resources = Collections.emptyList();

    @Value("${app.data.dir:app/data}")
    private String dataDir;

    @Value("${app.default-community-id:wilmington-de}")
    private String defaultCommunityId;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
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

    private List<Resource> parseJsonNodeToList(JsonNode root) throws JsonProcessingException {
        if (root == null || root.isNull()) {
            return Collections.emptyList();
        }
        if (root.isArray()) {
            return convertResourceArray(root);
        }
        if (root.isObject()) {
            if (root.has("records") && root.get("records").isArray()) {
                return convertResourceArray(root.get("records"));
            }
            if (root.has("resources") && root.get("resources").isArray()) {
                return convertResourceArray(root.get("resources"));
            }
            Resource single = mapper.convertValue(root, Resource.class);
            applyContentSourceAndDefaults(single, root);
            return Collections.singletonList(single);
        }
        return Collections.emptyList();
    }

    private List<Resource> convertResourceArray(JsonNode arrayNode) {
        List<Resource> parsed = mapper.convertValue(arrayNode, new TypeReference<List<Resource>>() {});
        for (int i = 0; i < parsed.size(); i++) {
            applyContentSourceAndDefaults(parsed.get(i), arrayNode.get(i));
        }
        return parsed;
    }

    private void applyContentSourceAndDefaults(Resource resource, JsonNode node) {
        ContentSource contentSource = new ContentSource();
        contentSource.name = node.hasNonNull("source") ? node.get("source").asText() : null;
        contentSource.retrieved = node.hasNonNull("retrieved") ? node.get("retrieved").asText() : null;
        resource.contentSource = contentSource;

        resource.title = resource.organization;
        resource.createdDate = contentSource.retrieved;
        resource.updatedDate = contentSource.retrieved;

        if (resource.communityId == null) {
            resource.communityId = defaultCommunityId;
        }
    }

    @Override
    public List<Resource> findAll() {
        return resources;
    }

    @Override
    public Optional<Resource> findById(String id) {
        return resources.stream().filter(r -> id.equals(r.id)).findFirst();
    }

    private void validateResources(List<Resource> resources) {
        // unused today — see WHY section below
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// The external-file-then-classpath-fallback discovery, multi-shape JSON
// parsing (bare array / {records:[...]} / {resources:[...]} / single
// object), @Value-injected data directory, and System.out/err logging are
// ALL carried over unchanged in mechanism from v1's ResourceService.init() —
// this pass moves that logic into the repository layer, it doesn't rewrite
// it, per "don't refactor things that aren't broken."
//
// The one genuinely new piece is applyContentSourceAndDefaults: because
// Resource no longer has source/retrieved fields (they're superseded by the
// inherited contentSource: ContentSource), and because Jackson's
// convertValue() only maps JSON keys onto fields that exist on the target
// class, the flat source/retrieved/organization values need a manual
// post-deserialization step to become contentSource/title/createdDate/
// updatedDate. This runs by index across the original JsonNode array and the
// freshly-deserialized Resource list (Jackson's list conversion preserves
// order), rather than parsing each object twice.
//
// communityId defaults from app.default-community-id (new property) only
// when the JSON doesn't already have one — true for 100% of today's data,
// but written to not silently override a communityId if a future data file
// does carry one.
//
// validateResources(...) is carried over verbatim but left uncalled — it was
// dead code in v1 too (nothing invoked it there either). Not deleted, not
// wired in — deciding whether/how to surface validation results is out of
// this pass's scope; flagged here rather than silently dropped.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Implements ResourceRepository; ResourceService depends on the interface,
//   not this class directly.
// - Populates Resource.contentSource/title/createdDate/updatedDate/communityId
//   — fields defined on the shared CivicContent base class (see
//   CivicContent_annotated.java).
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Parsing each JSON object twice (once via Jackson's automatic Resource
//   mapping, once manually for source/retrieved/etc.): rejected in favor of
//   the index-based re-walk, which reuses Jackson's existing deserialization
//   pass instead of hand-parsing every field a second time.
// - Fixing NewsService's separate hardcoded-path inconsistency while touching
//   this sibling class: explicitly NOT done here — see the News slice's own
//   annotated reference and references/decisions.md Decision 007 for why
//   that's deliberately preserved as-is until the News slice migration.
// =============================================================================
