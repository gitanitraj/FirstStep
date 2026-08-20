package org.firststep.backend.news.repository;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.shared.classification.CivicContentClassifier;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.service.ContentSourceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JsonNewsRepository implements NewsRepository {

    private final CivicContentClassifier classifier;
    private final ContentSourceService contentSources;

    public JsonNewsRepository(CivicContentClassifier classifier, ContentSourceService contentSources) {
        this.classifier = classifier;
        this.contentSources = contentSources;
    }

    private final ObjectMapper mapper = new ObjectMapper();

    private List<NewsItem> staticItems = Collections.emptyList();

    @Value("${app.data.dir:app/data}")
    private String dataDir;

    @Value("${app.default-community-id:wilmington-de}")
    private String defaultCommunityId;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            Path file = Path.of(dataDir, "news.json");
            JsonNode root = mapper.readTree(file.toFile());
            JsonNode recordsNode = root.get("records");
            List<NewsItem> parsed = mapper.convertValue(
                    recordsNode,
                    new TypeReference<List<NewsItem>>() {});
            for (int i = 0; i < parsed.size(); i++) {
                applyContentSourceAndDefaults(parsed.get(i), recordsNode.get(i));
            }
            staticItems = parsed;
            System.out.println("Loaded news items (" + staticItems.size() + " records)");
        } catch (Exception e) {
            System.err.println("Failed to load news.json: " + e.getMessage());
        }
    }

    /**
     * Normalize stage: map news.json's own key vocabulary onto the CivicContent
     * contract. The data file keeps its historical names (headline, published,
     * expires, active, resource_tags); this is where they become the canonical
     * title / publishDate / expirationDate / status / tags every content type
     * shares.
     *
     * <p>Note what is NOT here any more: category_tags used to be loaded into
     * {@code item.tags}, which made one field mean "editorial classification"
     * for news and "descriptive metadata" for everything else. category_tags now
     * binds straight to {@code categoryTags} via CivicContent's @JsonProperty,
     * and {@code tags} carries resource_tags — descriptive, as the contract says.
     */
    private void applyContentSourceAndDefaults(NewsItem item, JsonNode node) {
        // The record references its producer by ID; the NAME is resolved from
        // content-sources.json rather than authored here, so "Delaware DHSS" and
        // "Delaware Health and Social Services" cannot describe two agencies.
        // `url` stays per-record — it is THIS item's link, not the producer's.
        ContentSource contentSource = new ContentSource();
        contentSource.id = node.hasNonNull("source_id") ? node.get("source_id").asText() : null;
        contentSource.url = node.hasNonNull("source_url") ? node.get("source_url").asText() : null;
        contentSources.resolveName(contentSource);
        item.contentSource = contentSource;

        item.title = node.hasNonNull("headline") ? node.get("headline").asText() : null;
        item.tags = node.hasNonNull("resource_tags")
                ? mapper.convertValue(node.get("resource_tags"), new TypeReference<List<String>>() {})
                : null;

        item.publishDate = node.hasNonNull("published") ? node.get("published").asText() : null;
        item.expirationDate = node.hasNonNull("expires") ? node.get("expires").asText() : null;
        item.status = node.hasNonNull("active") && !node.get("active").asBoolean() ? "inactive" : "active";

        item.createdDate = item.publishDate;
        item.updatedDate = item.publishDate;

        if (item.communityId == null) {
            item.communityId = defaultCommunityId;
        }

        // Curated news carries hand-authored category_tags, which are immutable
        // here. What this CAN fill is the absent subcategory — the per-field rule
        // in CivicContentClassifier, without which these items stay topic-less.
        classifier.classify(item);
    }

    @Override
    public List<NewsItem> findAll() {
        return Collections.unmodifiableList(staticItems);
    }
}
