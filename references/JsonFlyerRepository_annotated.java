package org.firststep.backend.flyer.repository;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// JsonFlyerRepository is the JSON-file-backed implementation of
// FlyerRepository. It loads app/data/flyers.json (external-file-then-
// classpath-fallback, same mechanism as JsonResourceRepository) at startup
// and holds the parsed Flyer list in memory.
// =============================================================================

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;

import org.firststep.backend.shared.classification.CivicContentClassifier;
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

    private final CivicContentClassifier classifier;

    public JsonFlyerRepository(CivicContentClassifier classifier) {
        this.classifier = classifier;
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private List<Flyer> flyers = Collections.emptyList();

    @Value("${app.data.dir:app/data}")
    private String dataDir;

    @Value("${app.default-community-id:wilmington-de}")
    private String defaultCommunityId;

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
        classifier.classify(flyer);
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

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// The file-discovery mechanism (external file at app.data.dir, then
// classpath fallback), multi-shape JSON support (bare array /
// {records:[...]} / {flyers:[...]} / single object), and System.out/err
// logging style are all copied verbatim in MECHANISM from
// JsonResourceRepository, per direct instruction to mirror ResourceService
// "exactly."
//
// WHAT'S DELIBERATELY MISSING compared to JsonResourceRepository/
// JsonNewsRepository: those two classes each have an
// applyContentSourceAndDefaults-style adapter that maps v1's flat legacy
// JSON fields (source/retrieved, sourceName/sourceUrl) onto the new
// CivicContent shape after Jackson's automatic deserialization. Flyer has
// no v1 legacy shape — it's a brand-new entity — so flyers.json is authored
// to already match Flyer's Java shape directly (title, contentSource, tags,
// createdDate, updatedDate all present as real JSON, not flat strings
// needing translation). The only post-processing kept is communityId
// defaulting, since none of today's data specifies one — everything else
// that Resource/News needed an adapter for simply isn't a problem here.
// This is a deliberate simplification, not an oversight: adding an unused
// adapter method would be exactly the "abstraction for a problem that
// doesn't exist" the project's conventions warn against.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Implements FlyerRepository; FlyerService depends on the interface, not
//   this class directly.
// - Populates Flyer.communityId (inherited from CivicContent) when absent;
//   every other CivicContent field (title, summary, tags, contentSource,
//   createdDate, updatedDate) comes straight from the JSON, no mapping.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Reusing ResourceRepository's exact applyContentSourceAndDefaults pattern
//   even though Flyer doesn't need it: rejected — would be dead-weight
//   complexity carried over for consistency's own sake, at odds with
//   "minimum code that solves the problem."
// - Routing loading through the pipeline/ package's Collector/Normalizer
//   interfaces (a natural fit conceptually — this IS a collect+normalize
//   step): explicitly deferred, per direct instruction ("no real pipeline/
//   package wiring"). The pipeline package stays scaffolding-only until a
//   case exists that actually needs its abstraction, per Step 7's decision.
// =============================================================================

// =============================================================================
// SLICE F2 UPDATE (Decision 033) — CLASSIFICATION AT INGESTION
// =============================================================================
// This repository now injects CivicContentClassifier and calls classify() as
// part of applying defaults. That single line is what "classification happens at
// ingestion" means concretely — by the time anything leaves this repository it
// carries canonical categoryTags, so no downstream service has to translate a
// source vocabulary at request time. CategoryService used to do exactly that for
// resources; it no longer does.
//
// All five Json*Repository classes and RssFeedService call the same method. That
// was the goal of F2: a shared classification ENGINE, not per-caller fixes.
//
// WHAT classify() WILL AND WILL NOT DO HERE — the policy in one line:
//
//     It fills editorial fields ONLY when they are absent, per field.
//
// So for flyers and curated news, which carry hand-authored category_tags from
// Decision 032, this is a no-op on the category field and can only ever fill an
// absent subcategory. For resources it maps the raw source category through the
// taxonomy's matchCategories (deterministic, tier 1). For expert content, which
// has never been editorially classified, it is the first time that content
// reaches the taxonomy at all — with no per-type code written for it.
//
// See CivicContentClassifier_annotated.java Section 1 for why the policy lives in
// the classifier rather than being re-stated at each of these six call sites.
//
// TESTING NOTE: the constructor change rippled into every test that builds this
// repository directly. They use shared/classification/ClassifierFixture.real(),
// which wires a real classifier to the real app/data/taxonomy.json — a mock
// would make these tests pass whether or not classification works at all.
