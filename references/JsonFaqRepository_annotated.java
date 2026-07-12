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
