package org.firststep.backend.news.repository;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// JsonNewsRepository is the JSON-file-backed implementation of
// NewsRepository. It loads app/data/news.json (the "records" wrapper key
// only — no bare-array/other-wrapper-key support, unlike Resource's loader)
// at startup and holds the parsed NewsItem list in memory.
// =============================================================================

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

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// FIXED IN THIS PASS: v1's NewsService used a hardcoded relative path
// (Path.of("app", "data", "news.json")) instead of the app.data.dir property
// ResourceService already used — meaning it only worked when the process's
// working directory happened to be the repo root (true for `mvn
// spring-boot:run` locally, false inside the Docker container, whose
// WORKDIR is /app with data baked at /data via APP_DATA_DIR). This was
// confirmed empirically: running the real container showed "Failed to load
// news.json: app/data/news.json (No such file or directory)" in the logs
// while /api/resources and /api/news/rss both worked fine — see
// references/decisions.md Decision 007 for the full investigation. The
// user's report that "the RSS feed doesn't work in Docker" turned out to be
// this bug (the static /api/news endpoint), not the actual RSS feed, which
// was working the whole time.
//
// The fix: inject the same @Value("${app.data.dir:app/data}") property
// JsonResourceRepository uses, and build the path from it
// (Path.of(dataDir, "news.json")) instead of hardcoding "app"/"data".
// Decision 007 explicitly scoped this fix to "when NewsService's
// data-loading logic is being touched anyway" (this migration step) rather
// than as an out-of-band hotfix — deferred to the natural point where this
// code was already being rewritten, not left for a separate pass.
//
// Everything else about the loading mechanism is unchanged: still only
// supports the {records:[...]} wrapper shape (unlike Resource's loader,
// which also handles bare arrays / {resources:[...]} / single objects) —
// that inconsistency between the two loaders' shape-flexibility is
// pre-existing and NOT fixed here, since it doesn't block anything and
// wasn't the reported bug.
//
// applyContentSourceAndDefaults mirrors JsonResourceRepository's approach:
// a post-deserialization pass over the raw JsonNode array (index-aligned
// with the freshly-parsed NewsItem list) to populate contentSource/title/
// tags/createdDate/updatedDate/communityId from the v1 flat JSON keys
// Jackson would otherwise silently ignore.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Implements NewsRepository; NewsService depends on the interface.
// - Populates fields defined on the shared CivicContent base class.
// - Independent of RssFeedService/RssFeedSource — static news and live RSS
//   news are two separate data paths, both producing NewsItem, served by
//   two separate endpoints (/api/news vs /api/news/rss).
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Also adding the external-file/classpath-fallback and multi-shape
//   parsing ResourceRepository has: rejected — out of scope for fixing the
//   specific reported bug (the path), and would be an unrequested
//   behavioral change beyond what Decision 007 committed to.
// =============================================================================
