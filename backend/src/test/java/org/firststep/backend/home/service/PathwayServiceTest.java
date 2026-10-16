package org.firststep.backend.home.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.home.dto.ResourcePathway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PathwayService loads the authored homepage.json and resolves each category
 * pathway's label and icon from the taxonomy.
 *
 * <p>The tests that matter here are the ones about WHERE a label comes from.
 * Slice H's whole argument is that the homepage composes the existing model
 * rather than restating it, so a label appearing in two files would be the bug —
 * not a cosmetic one, a drift one.
 */
class PathwayServiceTest {

    /** The real taxonomy, so "resolved from taxonomy.json" means the real thing. */
    private static TaxonomyService taxonomy() {
        return new TaxonomyService("../app/data");
    }

    private static PathwayService serviceWith(Path dir, String json) throws Exception {
        Files.writeString(dir.resolve("homepage.json"), json);
        return new PathwayService(taxonomy(), dir.toString());
    }

    @Test
    void shouldLoadTheAuthoredPathwaysInFileOrder() {
        PathwayService service = new PathwayService(taxonomy(), "../app/data");

        List<String> keys = service.getCommunityResources().stream().map(ResourcePathway::key).toList();

        // Authored order IS display order — the file is the editorial artifact.
        assertEquals(
                List.of("housing", "employment", "health", "legal", "furniture-household", "seniors", "food"),
                keys);
    }

    @Test
    void shouldResolveCategoryLabelAndIconFromTheTaxonomy(@TempDir Path dir) throws Exception {
        // homepage.json authors ONLY the key. If label/icon come back populated,
        // they can only have come from taxonomy.json.
        PathwayService service = serviceWith(dir, """
                { "communityResources": [ { "key": "housing", "kind": "category" } ] }
                """);

        ResourcePathway pathway = service.getCommunityResources().get(0);

        assertEquals("Housing", pathway.label());
        assertEquals("🏠", pathway.icon());
        assertEquals(ResourcePathway.CATEGORY, pathway.kind());
    }

    @Test
    void shouldKeepAuthoredLabelAndIconForADiscoveryPathway(@TempDir Path dir) throws Exception {
        // Seniors has no taxonomy entry to resolve against, and must not get one.
        PathwayService service = serviceWith(dir, """
                { "communityResources": [
                    { "key": "seniors", "kind": "discovery", "label": "Seniors", "icon": "🧓" } ] }
                """);

        ResourcePathway pathway = service.getCommunityResources().get(0);

        assertEquals("Seniors", pathway.label());
        assertEquals("🧓", pathway.icon());
        assertEquals(ResourcePathway.DISCOVERY, pathway.kind());
    }

    @Test
    void shouldSkipAnUnknownCategoryRatherThanFailingToStart(@TempDir Path dir) throws Exception {
        // A hand-authored presentation file must not be able to take the app
        // down. validate_homepage.py is the gate; this is the safety net.
        PathwayService service = serviceWith(dir, """
                { "communityResources": [
                    { "key": "hosuing", "kind": "category" },
                    { "key": "food", "kind": "category" } ] }
                """);

        List<ResourcePathway> pathways = service.getCommunityResources();

        assertEquals(1, pathways.size());
        assertEquals("food", pathways.get(0).key());
    }

    @Test
    void shouldReturnAnEmptyListWhenTheFileIsMissing(@TempDir Path dir) {
        // Presentation degrades; it does not throw. Contrast TaxonomyService,
        // where a missing file IS fatal because it is the vocabulary.
        PathwayService service = new PathwayService(taxonomy(), dir.toString());

        assertTrue(service.getCommunityResources().isEmpty());
    }

    @Test
    void shouldNotInventALabelForADiscoveryPathwayThatOmitsOne(@TempDir Path dir) throws Exception {
        // The service reports what was authored. Guessing "Seniors" from the key
        // would hide the omission from the validator that exists to catch it.
        PathwayService service = serviceWith(dir, """
                { "communityResources": [ { "key": "seniors", "kind": "discovery" } ] }
                """);

        assertNull(service.getCommunityResources().get(0).label());
    }
}
