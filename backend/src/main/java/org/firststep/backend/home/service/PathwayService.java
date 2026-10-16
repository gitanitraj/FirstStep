package org.firststep.backend.home.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.firststep.backend.category.model.CategoryDefinition;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.home.dto.ResourcePathway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads {@code app/data/homepage.json} — the authored list of discovery pathways
 * the homepage's Community Resources column offers.
 *
 * <p><b>Why an authored file rather than {@code CategoryService.getAll()}.</b>
 * The homepage shows a curated seven, not all ten categories, and one of the
 * seven (Seniors) is not a category at all. Neither fact is derivable from the
 * taxonomy, so it is editorial and belongs in an editorial artifact — the same
 * argument that split navigation.json from taxonomy.json in Decision 029. Every
 * category stays reachable through Discover; the homepage is a front door, not
 * an index.
 *
 * <p><b>Labels and icons are resolved, not authored.</b> A {@code category}
 * pathway carries only a key; its label and icon come from TaxonomyService. Two
 * files holding the same label is the drift bug Decision 032 removed when it
 * deleted {@code CategoryDefinition.ALL}, and re-introducing it for the homepage
 * would be the same mistake in a new place.
 *
 * <p><b>A missing file is not fatal</b> — unlike the taxonomy. This is
 * presentation, so the honest degradation is a homepage without that column,
 * matching how NavigationService treats a missing navigation.json. An unknown
 * category key is skipped rather than throwing, because a hand-authored
 * presentation file should not be able to take the application down;
 * {@code validate_homepage.py} is the gate that catches it before it ships.
 */
@Service
public class PathwayService {

    /** homepage.json's shape; version/source/note are metadata. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Homepage(List<Entry> communityResources) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Entry(String key, String kind, String label, String icon) {
    }

    private final List<ResourcePathway> communityResources;

    public PathwayService(TaxonomyService taxonomyService,
            @Value("${app.data.dir:app/data}") String dataDir) {
        this.communityResources = resolve(read(dataDir), taxonomyService);
        System.out.println("Loaded homepage pathways (" + communityResources.size() + ")");
    }

    /** The Community Resources column, in the file's authored order. */
    public List<ResourcePathway> getCommunityResources() {
        return communityResources;
    }

    private static List<ResourcePathway> resolve(Homepage homepage, TaxonomyService taxonomyService) {
        if (homepage == null || homepage.communityResources() == null) {
            System.out.println("No homepage.json found — the Community Resources column will be empty.");
            return List.of();
        }
        List<ResourcePathway> pathways = new ArrayList<>();
        for (Entry entry : homepage.communityResources()) {
            if (entry == null || entry.key() == null) {
                continue;
            }
            if (ResourcePathway.DISCOVERY.equals(entry.kind())) {
                // Authored label/icon: a discovery pathway has no taxonomy entry
                // to resolve against, which is the whole point of the kind.
                pathways.add(new ResourcePathway(
                        entry.key(), entry.label(), entry.icon(), ResourcePathway.DISCOVERY));
                continue;
            }
            CategoryDefinition definition = taxonomyService.findByKey(entry.key()).orElse(null);
            if (definition == null) {
                // Skipped, not thrown — see the class comment.
                System.out.println("homepage.json references unknown category '" + entry.key() + "' — skipped.");
                continue;
            }
            pathways.add(new ResourcePathway(
                    definition.key(), definition.label(), definition.icon(), ResourcePathway.CATEGORY));
        }
        return List.copyOf(pathways);
    }

    private static Homepage read(String dataDir) {
        ObjectMapper mapper = new ObjectMapper();
        Path external = Path.of(dataDir, "homepage.json");
        try {
            if (Files.exists(external)) {
                return mapper.readValue(external.toFile(), Homepage.class);
            }
            try (InputStream in = PathwayService.class.getResourceAsStream("/homepage.json")) {
                return in == null ? null : mapper.readValue(in, Homepage.class);
            }
        } catch (Exception e) {
            System.out.println("Failed to load homepage.json: " + e.getMessage());
            return null;
        }
    }
}
