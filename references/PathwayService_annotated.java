/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../home/service/PathwayService.java
 * Slice H. See references/decisions.md Decision 042.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS CLASS IS
 *   The loader for app/data/homepage.json — the authored list of discovery
 *   pathways the homepage's Community Resources column offers.
 *
 * THE QUESTION IT ANSWERS
 *   "Which seven things does the front door show, and in what order?"
 *   That is an EDITORIAL question, not a derivable one, which is the entire
 *   reason this class exists instead of a call to CategoryService.getAll().
 * ============================================================================= */

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

@Service
public class PathwayService {

    // The file's shape. Metadata (version/source/note) is ignored, exactly as
    // NavigationService ignores navigation.json's.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Homepage(List<Entry> communityResources) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Entry(String key, String kind, String label, String icon) {
    }

    private final List<ResourcePathway> communityResources;

    // Loads in the CONSTRUCTOR, like TaxonomyService and unlike the content
    // repositories. The homepage payload is assembled on the first request, so
    // this has to be in memory by then; there is also nothing to wait for, since
    // the file is small and local.
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
                pathways.add(new ResourcePathway(
                        entry.key(), entry.label(), entry.icon(), ResourcePathway.DISCOVERY));
                continue;
            }
            CategoryDefinition definition = taxonomyService.findByKey(entry.key()).orElse(null);
            if (definition == null) {
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

// =============================================================================
// SECTION 1 — WHY AN AUTHORED FILE AND NOT CategoryService.getAll()
// =============================================================================
// The homepage shows SEVEN pathways. The taxonomy has TEN categories. And one of
// the seven — Seniors — is not a category at all.
//
// Neither of those facts is derivable:
//
//   · WHICH seven is an editorial judgement about what a resident most often
//     needs on arrival. Clothing, Utilities, Community Events and Community
//     Support are not absent because they are unimportant; they are absent
//     because a front door with ten doors is not a front door. They stay
//     reachable through Discover.
//
//   · Seniors is a DISCOVERY PATHWAY — a controlled query over existing
//     CivicContent metadata. Deriving it from the taxonomy would require putting
//     it IN the taxonomy, which is precisely what Decision 041 forbids.
//
// So the list is authored, and it lives in data rather than in the component so
// an editor can reorder or replace it without a code change. This is the same
// argument that split navigation.json from taxonomy.json in Decision 029:
// presentation is regenerable, vocabulary is not.
//
// =============================================================================
// SECTION 2 — LABELS ARE RESOLVED, NEVER AUTHORED
// =============================================================================
// A category entry in homepage.json carries ONLY a key:
//
//     { "key": "housing", "kind": "category" }
//
// and its label ("Housing") and icon ("🏠") are read from taxonomy.json at load
// time. The file could easily have carried them itself, and that would have been
// a bug in waiting: two files holding the same label is exactly the drift
// Decision 032 removed when it deleted CategoryDefinition.ALL and made Java read
// the taxonomy directly. Renaming a category would then require remembering to
// edit a second file, and nothing would fail if you forgot.
//
// validate_homepage.py enforces this in the other direction — a category entry
// that DOES author a label is an error, not a preference.
//
// A discovery entry is the exception, and for a principled reason: there is no
// taxonomy row to resolve against, and there must never be one. Seniors carries
// its own label and icon *because* it is not a category.
//
// =============================================================================
// SECTION 3 — WHY A BAD FILE DEGRADES INSTEAD OF THROWING
// =============================================================================
// Compare TaxonomyService, where a missing file is FATAL:
//
//     TaxonomyService   missing taxonomy.json  -> IllegalStateException, no boot
//     PathwayService    missing homepage.json  -> empty list, homepage renders
//
// The difference is what the file MEANS. Without the taxonomy the application
// cannot classify anything and every page is wrong; failing at startup is the
// cheapest possible diagnosis. Without homepage.json the application is entirely
// correct and one column is empty.
//
// The same logic applies to an unknown category key: it is skipped with a log
// line rather than thrown. A hand-authored presentation file should not be able
// to take down a civic information service because someone typed "hosuing".
// validate_homepage.py is the gate that catches the typo before it ships; this
// is the safety net for when the gate is bypassed.
//
// Note what is NOT done: the service does not invent a label for a discovery
// pathway that omits one. Guessing "Seniors" from the key would paper over the
// omission and hide it from the validator that exists to find it. The service
// reports what was authored — a test pins this.
//
// =============================================================================
// SECTION 4 — HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Depends on TaxonomyService (constructor-injected) purely to RESOLVE labels.
//   It never asks the taxonomy which pathways to show.
// - HomeService injects this and puts the result in HomePayload.communityResources.
// - The frontend's CommunityResources component reads `kind` to decide the
//   destination: /category/{key} or /discover/{key}.
//
// CALL PATH:
//   GET /api/home
//     -> HomeService.getHome()
//        -> PathwayService.getCommunityResources()   (authored order preserved)
//     -> HomePayload.communityResources
//
// =============================================================================
// SECTION 5 — ALTERNATIVES CONSIDERED
// =============================================================================
// - Adding a `homepage: true` flag to taxonomy.json's categories: rejected. It
//   puts a PRESENTATION decision inside the DOMAIN vocabulary, and it has no
//   answer at all for Seniors, which has no taxonomy row.
//
// - Extending navigation.json with a homepage block: rejected. That file has a
//   specific contract (how topics are GROUPED within a category) and a specific
//   regeneration story (a future AI generator rewrites it). A homepage column is
//   neither. Two small focused artifacts beat one overloaded one.
//
// - Hardcoding the seven in CommunityResources.tsx: rejected. It is editorial
//   content, and editorial content that requires a deploy to change is editorial
//   content that never changes.
//
// - Computing the seven by resource count: rejected. It would make the homepage
//   reorder itself as data loads, and it answers the wrong question — the most
//   populated category is not the most needed one.
// =============================================================================
