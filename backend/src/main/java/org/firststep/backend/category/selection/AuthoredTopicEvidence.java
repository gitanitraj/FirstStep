package org.firststep.backend.category.selection;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.category.model.CategoryDefinition;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.shared.classification.SourceMappingService;
import org.firststep.backend.shared.model.CivicContent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Authored classification facts only, joined to current services by identity; never a raw eligibility source.
 * Existing loaders can infer absent Topics. Runtime fields alone cannot certify editorial classification.
 * RSS currently has no authored Topic evidence channel and therefore cannot qualify merely from keywords.
 */
@Service
public class AuthoredTopicEvidence {
    private record Fact(Set<String> categoryKeys, String topic) {}
    private final Map<String, Fact> authored = new LinkedHashMap<>();
    private final List<String> loadLimitations;

    public AuthoredTopicEvidence(@Value("${app.data.dir:app/data}") String dataDir,
            TaxonomyService taxonomy, SourceMappingService mappings) {
        var limitations = new java.util.ArrayList<String>();
        Map<String, String> files = new LinkedHashMap<>();
        files.put("resources.json", "Resource"); files.put("resources.communities.json", "Resource");
        files.put("flyers.json", "Flyer"); files.put("news.json", "NewsItem");
        files.put("articles.json", "Article"); files.put("faq.json", "FAQ"); files.put("expert-answers.json", "ExpertAnswer");
        ObjectMapper mapper = new ObjectMapper();
        files.forEach((file, kind) -> {
            try {
                Path path = Path.of(dataDir, file);
                JsonNode root;
                if (Files.exists(path)) root = mapper.readTree(path.toFile());
                else try (InputStream stream = getClass().getResourceAsStream("/" + file)) {
                    root = stream == null ? null : mapper.readTree(stream);
                }
                if (root == null) { limitations.add("missing_authored_metadata_file:" + file); return; }
                JsonNode rows = root.isArray() ? root : null;
                if (rows == null) for (String wrapper : List.of("records", "resources", "flyers", "news", "articles", "faqs", "expertAnswers")) {
                    if (root.path(wrapper).isArray()) { rows = root.path(wrapper); break; }
                }
                if (rows == null) rows = mapper.createArrayNode().add(root);
                for (JsonNode row : rows) {
                    String id = row.path("id").asText("");
                    String topic = row.path("subcategory").isTextual() ? row.path("subcategory").asText() : null;
                    if (id.isBlank() || topic == null || topic.isBlank()) continue;
                    Set<String> keys = new TreeSet<>();
                    JsonNode tags = row.path("category_tags");
                    if (tags.isArray()) {
                        for (var category : taxonomy.getCategories()) {
                            for (JsonNode tag : tags) {
                                if (tag.isTextual() && category.matchCategoryTags().stream().anyMatch(label -> label.equalsIgnoreCase(tag.asText()))) keys.add(category.key());
                            }
                        }
                    } else if (kind.equals("Resource")) {
                        // Exact approved source mapping is explicit normalization, not keyword classification.
                        mappings.categoryKeyFor("dscyf-directory", row.path("category").asText(null)).ifPresent(keys::add);
                    }
                    Fact fact = new Fact(Set.copyOf(keys), topic);
                    String identity = kind + ":" + id;
                    Fact previous = authored.putIfAbsent(identity, fact);
                    if (previous != null && !previous.equals(fact)) authored.put(identity, new Fact(Set.of(), ""));
                }
            } catch (Exception error) {
                // Missing proof fails closed without creating a different public inventory or workflow.
                limitations.add("unreadable_authored_metadata_file:" + file);
            }
        });
        loadLimitations = List.copyOf(limitations);
    }

    public boolean permits(CivicContent item, CategoryDefinition category) {
        Fact fact = authored.get(item.getClass().getSimpleName() + ":" + item.id);
        return fact != null && fact.categoryKeys().contains(category.key())
                && item.subcategory != null && fact.topic().equalsIgnoreCase(item.subcategory);
    }

    public List<String> loadLimitations() { return loadLimitations; }
}
