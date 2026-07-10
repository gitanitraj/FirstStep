package org.firststep.backend.news.repository;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.shared.model.ContentSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JsonNewsRepository implements NewsRepository {

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

    private void applyContentSourceAndDefaults(NewsItem item, JsonNode node) {
        ContentSource contentSource = new ContentSource();
        contentSource.name = node.hasNonNull("source_name") ? node.get("source_name").asText() : null;
        contentSource.url = node.hasNonNull("source_url") ? node.get("source_url").asText() : null;
        item.contentSource = contentSource;

        item.title = node.hasNonNull("headline") ? node.get("headline").asText() : null;
        item.tags = node.hasNonNull("category_tags")
                ? mapper.convertValue(node.get("category_tags"), new TypeReference<List<String>>() {})
                : null;
        item.createdDate = item.published;
        item.updatedDate = item.published;

        if (item.communityId == null) {
            item.communityId = defaultCommunityId;
        }
    }

    @Override
    public List<NewsItem> findAll() {
        return Collections.unmodifiableList(staticItems);
    }
}
