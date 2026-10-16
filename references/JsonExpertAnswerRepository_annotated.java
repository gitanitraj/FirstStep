package org.firststep.backend.expert.repository;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// JsonExpertAnswerRepository is the JSON-file-backed implementation of
// ExpertAnswerRepository. Loads app/data/expert-answers.json (external,
// then classpath fallback) at startup, holds the parsed list in memory.
// =============================================================================

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;

import org.firststep.backend.shared.classification.CivicContentClassifier;
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

    private final CivicContentClassifier classifier;

    public JsonExpertAnswerRepository(CivicContentClassifier classifier) {
        this.classifier = classifier;
    }

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
        classifier.classify(expertAnswer);
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

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// A LINE-FOR-LINE MIRROR OF JsonFlyerRepository: same external-then-
// classpath-fallback discovery, same multi-shape JSON support (bare array
// / {records:[...]} / {expertAnswers:[...]} / single object), same
// communityId-only-if-null defaulting, same System.out/err logging style.
// Confirmed file-for-file against JsonFlyerRepository.java before writing
// this, per direct instruction to build this pass "mirroring" Flyer's
// established pattern.
//
// NO FIELD-MAPPING ADAPTER (unlike JsonResourceRepository/
// JsonNewsRepository): app/data/expert-answers.json is hand-authored
// directly in ExpertAnswer's target shape (title, contentSource, tags all
// present as real JSON) — same choice Flyer made, for the same reason:
// brand-new data with no legacy shape to bridge. See
// references/decisions.md Decision 011 for the original version of this
// reasoning (Flyer) and Decision 015 for this slice's version.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Implements ExpertAnswerRepository; ExpertAnswerService depends on the
//   interface, not this class directly.
// - communityId defaulting uses the same app.default-community-id
//   property every other repository in the app shares.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - None beyond what's already documented for JsonFlyerRepository — this
//   is a direct, confirmed mirror of that class with no new design
//   decisions of its own.
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

// =============================================================================
// SLICE I — PRODUCER NAME IS RESOLVED HERE (Decision 045)
// =============================================================================
// The expert answer record carries `contentSource.id` and NOT the producer's name.
// This repository calls `contentSources.resolveName(...)` as it loads, which is
// the Normalize stage — the same place other source vocabulary becomes the
// CivicContent contract.
//
// Doing it at load rather than at each display site means every downstream
// consumer sees a resolved name without knowing the registry exists. It is also
// what collapses "Delaware DHSS" and "Delaware Health and Social Services" into
// one agency.
//
// BEST-EFFORT AND NON-BLOCKING: an unresolvable id leaves the name null and logs.
// The item is still loaded, still classified, still browsable — provenance
// resolution is a capability, not a validity gate.
// =============================================================================
