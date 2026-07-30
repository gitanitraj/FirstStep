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

// =============================================================================
// SLICE F1 UPDATE (Decision 032) — THIS METHOD IS THE NORMALIZE STAGE
// =============================================================================
// applyContentSourceAndDefaults() was always doing pipeline Normalize work
// (mapping a heterogeneous source file onto the shared knowledge model). Slice
// F1 made that its explicit job, because the CivicContent contract gave it a
// canonical target to map ONTO:
//
//     news.json key      ->  CivicContent contract field
//     ---------------        ---------------------------
//     headline           ->  title
//     source_name/_url   ->  contentSource
//     category_tags      ->  categoryTags   (via @JsonProperty, automatic)
//     resource_tags      ->  tags           (mapped here)
//     published          ->  publishDate    (mapped here)
//     expires            ->  expirationDate (mapped here)
//     active             ->  status         (mapped here)
//
// news.json itself is UNCHANGED. Source files keep their own historical
// vocabulary; translating it is the repository's job. See
// CivicContent_annotated.java Section 4 for why renaming the data file was
// rejected.
//
// THE LINE THAT CHANGED MEANING — and the bug it was causing:
//
//     BEFORE:  item.tags = node.get("category_tags")
//     AFTER:   item.tags = node.get("resource_tags")
//
// The old line loaded EDITORIAL classification into the DESCRIPTIVE field. It
// is the single line that made `tags` mean "which category is this" for a
// NewsItem and "what words describe this" for a Resource or Flyer — one field,
// two meanings, which every downstream consumer then had to disambiguate by
// checking the type first.
//
// It is also why the field is now absent from this method for category_tags:
// CivicContent declares @JsonProperty("category_tags") on categoryTags, so
// Jackson binds it with no code at all. The mapping that needed a line of code
// disappeared once the model named the concept correctly. That is usually the
// sign a model change was the right one.
//
// ON status: mapped from `active` rather than read directly, because the
// contract asks a lifecycle QUESTION ("when is it relevant?") and the file
// answers with a boolean. Note the expression treats a MISSING `active` key as
// "active" — absence of a deactivation flag means the item is live, which is
// the safer default for civic information (failing closed would silently hide
// content that no one marked either way).
//
// Deliberately NOT derived here: `status` does not consult expirationDate.
// Computing "expired" at load time would bake the load timestamp into the data
// and go stale in a long-running process. expirationDate is carried through as
// a fact; whoever renders decides what to do about it.
// =============================================================================
