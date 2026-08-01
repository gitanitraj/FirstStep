package org.firststep.backend.shared.classification;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.firststep.backend.category.model.CategoryDefinition;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentSource;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <h2>The Editorial Stability Invariant, enforced.</h2>
 *
 * <blockquote>
 * Changes to the classification engine must not change editorial category counts
 * for manually classified CivicContent or for content placed by deterministic
 * source mappings. Those counts must remain stable regardless of keyword tuning,
 * classifier improvements, or threshold adjustments. Any change must result from
 * an intentional editorial decision or a taxonomy update.
 * </blockquote>
 *
 * <p><b>Why this is a test and not a paragraph.</b> An invariant that lives only
 * in a document is one nobody notices breaking. During Slice F2 this exact check
 * was run by hand against a running container — it caught a real bug, but only
 * because someone remembered to look. Here it fails the build.
 *
 * <p><b>What it protects.</b> Two classes of content, for two different reasons:
 * <ul>
 *   <li><b>Editorially classified</b> — flyers and curated news carry
 *       hand-authored {@code category_tags}. An editor decided; the engine has no
 *       mandate to move them.</li>
 *   <li><b>Source-mapped</b> — the 229 resources are placed by the deterministic
 *       mappings in {@code source-mappings.json}. Deterministic means the answer
 *       does not depend on how the keyword vocabulary is feeling today.</li>
 * </ul>
 *
 * <p><b>What it deliberately does NOT freeze:</b> automatically classified
 * content (RSS legislation). Those counts are <i>expected</i> to move as the
 * vocabulary improves — freezing them would make the invariant forbid the engine
 * from ever getting better, which is the opposite of its purpose.
 *
 * <p>Runs against the REAL data files. If one of these numbers changes, either an
 * editor changed the data (update the expectation, and say so in the decision
 * log) or the engine has drifted into content it does not own (fix the engine).
 */
class EditorialStabilityTest {

    /**
     * The pinned baseline, established in Slice F1 (Decision 032) and unchanged
     * through F2 and F2.1. 229 resources + 9 flyer placements (FL-002 and FL-005
     * are each editorially classified under two categories).
     */
    private static final Map<String, Integer> EXPECTED_COUNTS = new TreeMap<>(Map.of(
            "housing", 45,
            "food", 12,
            "clothing", 15,
            "health", 33,
            "employment", 6,
            "utilities", 0,
            "legal", 5,
            "community-events", 54,
            "furniture-household", 7,
            "community-support", 61));

    private static final int EXPECTED_TOTAL = 238;

    private final ObjectMapper mapper = new ObjectMapper();
    private final TaxonomyService taxonomy = new TaxonomyService("../app/data");
    private final CivicContentClassifier classifier = ClassifierFixture.real();

    @Test
    void shouldKeepEditorialAndSourceMappedCategoryCountsStable() throws IOException {
        Map<String, Integer> actual = countByCategory(loadProtectedContent());

        assertEquals(EXPECTED_COUNTS, actual,
                "Editorial Stability Invariant violated. Category counts for editorially "
                        + "classified and source-mapped content changed. If this was an intentional "
                        + "editorial or taxonomy decision, update EXPECTED_COUNTS and record it in "
                        + "references/decisions.md. If not, the classification engine has drifted "
                        + "into content it does not own.");
    }

    @Test
    void shouldKeepTheTotalStable() throws IOException {
        int total = countByCategory(loadProtectedContent()).values().stream().mapToInt(Integer::intValue).sum();

        assertEquals(EXPECTED_TOTAL, total);
    }

    @Test
    void shouldPlaceEveryResourceByDeterministicSourceMappingNotByKeywords() throws IOException {
        // The source adapter is expected to have 100% coverage of the directory.
        // A resource falling through to keyword inference is a missing mapping —
        // it would still classify, plausibly and unstably, which is exactly the
        // failure this invariant exists to make visible.
        List<Resource> resources = loadResources();
        List<String> unmapped = new ArrayList<>();
        for (Resource r : resources) {
            r.categoryTags = null;
            if (!classifier.classify(r).reason().startsWith("source mapping")) {
                unmapped.add(r.id + " (" + r.category + ")");
            }
        }

        assertEquals(List.of(), unmapped,
                "These resources are not covered by source-mappings.json and fell through to "
                        + "keyword inference, so their placement is no longer deterministic.");
    }

    // ---- Loading the protected content, classified exactly as at ingestion ----

    private List<CivicContent> loadProtectedContent() throws IOException {
        List<CivicContent> protectedContent = new ArrayList<>(loadResources());
        protectedContent.addAll(loadFlyers());
        protectedContent.addAll(loadNews());
        protectedContent.forEach(classifier::classify);
        return protectedContent;
    }

    private List<Resource> loadResources() throws IOException {
        List<Resource> all = new ArrayList<>();
        for (String file : List.of("resources.json", "resources.communities.json")) {
            for (Resource r : read(file, new TypeReference<List<Resource>>() {})) {
                // Stamped by JsonResourceRepository in production; source adaptation
                // is keyed by provider, so the id has to be present to translate.
                r.contentSource = new ContentSource();
                r.contentSource.id = "dscyf-directory";
                all.add(r);
            }
        }
        return all;
    }

    private List<Flyer> loadFlyers() throws IOException {
        return read("flyers.json", new TypeReference<List<Flyer>>() {});
    }

    private List<NewsItem> loadNews() throws IOException {
        return read("news.json", new TypeReference<List<NewsItem>>() {});
    }

    private <T> List<T> read(String filename, TypeReference<List<T>> type) throws IOException {
        JsonNode root = mapper.readTree(Path.of("../app/data", filename).toFile());
        JsonNode records = root.isArray() ? root : root.get("records");
        if (records == null) {
            records = root.get("resources");
        }
        return mapper.convertValue(records, type);
    }

    /**
     * Counts exactly as CategoryService does — by editorial classification, one
     * count per (category, item) pair, so dual-classified content counts in both.
     */
    private Map<String, Integer> countByCategory(List<CivicContent> items) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (CategoryDefinition definition : taxonomy.getCategories()) {
            int n = 0;
            for (CivicContent item : items) {
                // News is excluded: CategorySummary.resourceCount counts resources
                // and flyers only, and this test pins THAT number.
                if (item instanceof NewsItem) {
                    continue;
                }
                if (taxonomy.matchesCategoryTags(definition, item.categoryTags)) {
                    n++;
                }
            }
            counts.put(definition.key(), n);
        }
        return new TreeMap<>(counts);
    }
}
