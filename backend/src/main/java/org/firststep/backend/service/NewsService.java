package org.firststep.backend.service;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.firststep.backend.model.NewsItem;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NewsService implements DecisionAgentService.NewsServiceLike {

    private final ObjectMapper mapper = new ObjectMapper();

    private List<NewsItem> staticItems = Collections.emptyList();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            Path file = Path.of("app", "data", "news.json");
            JsonNode root = mapper.readTree(file.toFile());
            staticItems = mapper.convertValue(
                    root.get("records"),
                    new TypeReference<List<NewsItem>>() {});
            System.out.println("Loaded news items (" + staticItems.size() + " records)");
        } catch (Exception e) {
            System.err.println("Failed to load news.json: " + e.getMessage());
        }
    }

    @Override
    public List<NewsItem> getAllNews() {
        return getAll();
    }

    public List<NewsItem> getAll() {
        return Collections.unmodifiableList(staticItems);
    }

}
