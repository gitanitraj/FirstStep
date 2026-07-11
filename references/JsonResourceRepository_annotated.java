package org.firststep.backend.resource.repository;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// JsonResourceRepository is the JSON-file-backed implementation of
// ResourceRepository. It loads app/data/resources.json AND
// app/data/resources.communities.json (each with a classpath fallback) at
// startup, concatenates both into one in-memory Resource list, and derives
// each record's communityId from its actual location city — the exact
// loading mechanism v1's ResourceService used for the first file, extended
// to a second file plus real per-record communityId logic in the Community
// multi-tenancy pass (decisions.md Decision 013).
// =============================================================================

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;

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

    private final ObjectMapper mapper = new ObjectMapper();
    private List<Resource> resources = Collections.emptyList();

    @Value("${app.data.dir:app/data}")
    private String dataDir;

    @Value("${app.default-community-id:wilmington-de}")
    private String defaultCommunityId;

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

        resource.communityId = communityIdFor(resource);
    }

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
// DUAL-FILE LOADING (Community multi-tenancy pass, decisions.md Decision
// 013): init() now loads BOTH resources.json (the 58 hand-curated records)
// and resources.communities.json (a new, structurally-mapped file covering
// 6 additional towns from the real DSCYF directory), concatenating both
// into one in-memory list. The existing external-then-classpath-fallback
// body was extracted into loadFile(String filename) and called twice
// rather than duplicated — a direct, minimal refactor required to support
// two files, not a speculative one. resources.communities.json missing is
// tolerated (log and continue, same pattern JsonFlyerRepository already
// uses for its own file) since it has no classpath fallback expectation —
// only resources.json needs one, matching production/test parity.
//
// communityId DERIVATION (the actual bugfix): previously
// "if (resource.communityId == null) { resource.communityId =
// defaultCommunityId; }" stamped app.default-community-id onto every
// record unconditionally, since no source JSON has ever set communityId
// itself. This silently mislabeled every non-Wilmington resource
// (including 5 of the original 58 curated records — 2 New Castle, 2
// Middletown, 1 Bear) as "wilmington-de". Fixed by deriving communityId
// from the resource's own locations[0].city via CommunitySlug.forCity(...)
// — see that class's annotated reference — falling back to
// defaultCommunityId only when no location/city exists at all. This is
// the change that makes /api/search's communityId filter meaningful for
// the first time; previously every record shared one community value, so
// filtering by it was a no-op no matter what data existed.
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
// - Depends on shared/util/CommunitySlug for the city -> communityId
//   derivation (see that class's annotated reference).
// - resources.communities.json is generated by a one-time script (not
//   shipped as app code — see references/decisions.md Decision 013 for
//   the full field-mapping rules); this class only ever reads the output.
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
// - Routing the raw-DSCYF-to-Resource transform through the pipeline/
//   package's Collector/Normalizer interfaces, or doing it at runtime
//   inside this repository: rejected — this is a static, infrequently-
//   changing snapshot (the DSCYF directory doesn't update in real time),
//   so a one-time offline transform producing a committed JSON file keeps
//   this class's hot-path logic simple (just "load these known files"),
//   consistent with how resources.json/Service_Directory_cleaned.json
//   themselves already arrived as static snapshots with no runtime
//   generation step.
// =============================================================================
