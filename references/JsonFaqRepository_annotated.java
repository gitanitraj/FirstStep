package org.firststep.backend.expert.repository;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// JsonFaqRepository is the JSON-file-backed implementation of
// FaqRepository. Loads app/data/faq.json (external, then classpath
// fallback) at startup, holds the parsed list in memory.
// =============================================================================

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;

import org.firststep.backend.shared.classification.CivicContentClassifier;
import org.firststep.backend.expert.model.FAQ;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JsonFaqRepository implements FaqRepository {

    private final CivicContentClassifier classifier;

    public JsonFaqRepository(CivicContentClassifier classifier) {
        this.classifier = classifier;
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private List<FAQ> faqs = Collections.emptyList();

    @Value("${app.data.dir:app/data}")
    private String dataDir;

    @Value("${app.default-community-id:wilmington-de}")
    private String defaultCommunityId;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Path external = Path.of(dataDir, "faq.json");
        try {
            if (external.toFile().exists()) {
                JsonNode root = mapper.readTree(external.toFile());
                faqs = parseJsonNodeToList(root);
                System.out.println("Loaded FAQs from " + external + " (" + faqs.size() + " records)");
                return;
            }
        } catch (Exception e) {
            System.err.println("Failed to load " + external + ": " + e.getMessage());
        }

        try (InputStream is = getClass().getResourceAsStream("/faq.json")) {
            if (is != null) {
                JsonNode root = mapper.readTree(is);
                faqs = parseJsonNodeToList(root);
                System.out.println("Loaded FAQs from classpath faq.json (" + faqs.size() + " records)");
            } else {
                System.out.println("No faq.json found on classpath.");
            }
        } catch (Exception e) {
            System.err.println("Failed to load classpath faq.json: " + e.getMessage());
        }
    }

    private List<FAQ> parseJsonNodeToList(JsonNode root) throws JsonProcessingException {
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
            if (root.has("faqs") && root.get("faqs").isArray()) {
                return convertArray(root.get("faqs"));
            }
            FAQ single = mapper.convertValue(root, FAQ.class);
            applyDefaults(single);
            return Collections.singletonList(single);
        }
        return Collections.emptyList();
    }

    private List<FAQ> convertArray(JsonNode arrayNode) {
        List<FAQ> parsed = mapper.convertValue(arrayNode, new TypeReference<List<FAQ>>() {});
        parsed.forEach(this::applyDefaults);
        return parsed;
    }

    private void applyDefaults(FAQ faq) {
        if (faq.communityId == null) {
            faq.communityId = defaultCommunityId;
        }
        classifier.classify(faq);
    }

    @Override
    public List<FAQ> findAll() {
        return faqs;
    }

    @Override
    public Optional<FAQ> findById(String id) {
        return faqs.stream().filter(f -> id.equals(f.id)).findFirst();
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// SAME MIRROR-OF-Flyer REASONING AS JsonExpertAnswerRepository (see that
// class's annotated reference) — external-then-classpath-fallback
// discovery, multi-shape JSON support, communityId-only-if-null
// defaulting, no field-mapping adapter (app/data/faq.json is authored
// directly in FAQ's target shape, including 2 of 6 records carrying a
// real source_expert_answer_id linking to app/data/expert-answers.json).
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Implements FaqRepository; FaqService depends on the interface, not
//   this class directly.
// - Does NOT validate that a loaded FAQ.sourceExpertAnswerId actually
//   matches a real ExpertAnswer — this repository has no dependency on
//   JsonExpertAnswerRepository or ExpertAnswerService at all; the two
//   slices load and operate completely independently.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Cross-checking sourceExpertAnswerId against JsonExpertAnswerRepository
//   at load time: rejected — would introduce a load-order dependency
//   between two repositories for a "stub" pass where the hand-authored
//   data is already internally consistent; revisit if this data stops
//   being hand-curated.
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
// The FAQ record carries `contentSource.id` and NOT the producer's name.
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
