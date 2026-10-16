package org.firststep.backend.originals.repository;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.firststep.backend.originals.model.Article;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads {@code app/data/articles.json}, following JsonFlyerRepository's shape:
 * external canonical file first, classpath fallback, load once when the
 * application is ready.
 *
 * <p><b>It loads every article regardless of review state.</b> Filtering here
 * would look like a safety improvement and would in fact destroy the routing
 * seam — a future editorial queue could no longer reach the drafts it exists to
 * manage, and "not public" would have quietly become "not stored".
 *
 * <p>The startup log reports the review breakdown rather than a bare count,
 * because "8 articles" hides the only number that matters operationally: how many
 * are actually reachable by a resident.
 */
@Repository
public class JsonArticleRepository implements ArticleRepository {

    private final ObjectMapper mapper = new ObjectMapper();
    private List<Article> articles = Collections.emptyList();

    @Value("${app.data.dir:app/data}")
    private String dataDir;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Path external = Path.of(dataDir, "articles.json");
        try {
            if (external.toFile().exists()) {
                articles = parse(mapper.readTree(external.toFile()));
                report(external.toString());
                return;
            }
        } catch (Exception e) {
            System.err.println("Failed to load " + external + ": " + e.getMessage());
        }

        try (InputStream is = getClass().getResourceAsStream("/articles.json")) {
            if (is != null) {
                articles = parse(mapper.readTree(is));
                report("classpath:/articles.json");
                return;
            }
        } catch (Exception e) {
            System.err.println("Failed to load classpath articles.json: " + e.getMessage());
        }

        // Not an error. Originals predates articles and still works without them:
        // the homepage panel simply carries FAQs alone.
        System.out.println("No articles.json found — First Step Originals will carry FAQs only.");
    }

    private List<Article> parse(JsonNode root) throws Exception {
        JsonNode records = root.has("records") ? root.get("records") : root;
        return mapper.convertValue(records, new TypeReference<List<Article>>() {});
    }

    private void report(String origin) {
        long publishable = articles.stream().filter(Article::isPublishable).count();
        System.out.println("Loaded articles from " + origin + " (" + articles.size()
                + " records, " + publishable + " approved for public serving)");
    }

    @Override
    public List<Article> findAll() {
        return articles;
    }

    @Override
    public Optional<Article> findById(String id) {
        return articles.stream().filter(a -> a.id != null && a.id.equals(id)).findFirst();
    }
}
