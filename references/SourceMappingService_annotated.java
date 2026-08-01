package org.firststep.backend.shared.classification;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Translates an upstream provider's category vocabulary into First Step's
 * canonical taxonomy. Loaded from {@code app/data/source-mappings.json}.
 *
 * <p><b>Why this is not in taxonomy.json.</b> "Housing Assistance",
 * "Before/After School Care" and "Early Childhood/Pre-K" are the DSCYF
 * directory's words, not First Step's. Keeping them in the canonical editorial
 * taxonomy meant the domain model carried one provider's vocabulary — and would
 * have carried every future provider's too, with no way to tell whose was whose.
 * This is a <b>deterministic source adapter</b>, which is an ingestion concern,
 * so it lives in the classification engine.
 *
 * <p><b>Why keyed by source.</b> The file names each provider and its mappings
 * separately, so adopting a second directory adds a block rather than merging
 * unfamiliar strings into an undifferentiated list. Provenance survives.
 *
 * <p><b>Why a missing file is NOT fatal</b>, unlike a missing taxonomy. The
 * taxonomy is the vocabulary — without it nothing can be classified and failing
 * fast is the cheap outcome. Source mappings are an optimization for content
 * that happens to arrive with an upstream category: only Resources have one, and
 * without mappings the classifier simply falls through to keyword inference.
 * Degraded, not broken.
 */
@Service
public class SourceMappingService {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SourceMappings(List<Source> sources) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Source(String id, String name, Map<String, String> mappings) {
    }

    /** sourceId -> (lowercased raw category -> canonical category key). */
    private final Map<String, Map<String, String>> bySource;

    public SourceMappingService(@Value("${app.data.dir:app/data}") String dataDir) {
        this.bySource = load(dataDir);
        int total = bySource.values().stream().mapToInt(Map::size).sum();
        System.out.println("Loaded source mappings (" + bySource.size() + " source(s), "
                + total + " category mappings)");
    }

    /**
     * The canonical category key an upstream category string maps to, or empty
     * when this source has no mapping for it.
     *
     * <p>Matching is case-insensitive and trimmed — a casing or whitespace slip
     * in provider data is a typo, not a different category. It is otherwise
     * EXACT: no fuzzy matching, because the whole value of this tier is being
     * deterministic. Anything not covered here falls through to keyword
     * inference, which is where uncertainty belongs.
     */
    public Optional<String> categoryKeyFor(String sourceId, String rawCategory) {
        if (sourceId == null || rawCategory == null || rawCategory.isBlank()) {
            return Optional.empty();
        }
        Map<String, String> mappings = bySource.get(sourceId);
        if (mappings == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mappings.get(normalize(rawCategory)));
    }

    /** True when the source is known, whether or not it maps this particular value. */
    public boolean knowsSource(String sourceId) {
        return bySource.containsKey(sourceId);
    }

    /** How many mappings a source declares. Used by the startup report and tests. */
    public int mappingCount(String sourceId) {
        return bySource.getOrDefault(sourceId, Map.of()).size();
    }

    private static String normalize(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, Map<String, String>> load(String dataDir) {
        ObjectMapper mapper = new ObjectMapper();
        SourceMappings parsed = read(mapper, dataDir);
        if (parsed == null || parsed.sources() == null) {
            System.out.println("No source-mappings.json found — classification will rely on keywords alone.");
            return Map.of();
        }
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (Source source : parsed.sources()) {
            Map<String, String> normalized = new LinkedHashMap<>();
            if (source.mappings() != null) {
                source.mappings().forEach((raw, key) -> normalized.put(normalize(raw), key));
            }
            result.put(source.id(), normalized);
        }
        return result;
    }

    private static SourceMappings read(ObjectMapper mapper, String dataDir) {
        Path external = Path.of(dataDir, "source-mappings.json");
        try {
            if (Files.exists(external)) {
                return mapper.readValue(external.toFile(), SourceMappings.class);
            }
            try (InputStream in = SourceMappingService.class.getResourceAsStream("/source-mappings.json")) {
                return in == null ? null : mapper.readValue(in, SourceMappings.class);
            }
        } catch (Exception e) {
            // Degraded, not fatal — but loud, because silently classifying 229
            // resources by keyword guess would look like a taxonomy problem.
            System.err.println("Failed to load source-mappings.json: " + e.getMessage());
            return null;
        }
    }
}

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// SourceMappingService translates an upstream provider's category vocabulary
// into First Step's canonical taxonomy. It is Tier 1 of CategoryClassifier —
// the deterministic half of classification.
// =============================================================================

// =============================================================================
// SECTION 1 — WHY THIS DATA LEFT taxonomy.json
// =============================================================================
// Until Slice F2.1 the same table lived in taxonomy.json as `matchCategories`:
//
//     "matchCategories": ["Housing Assistance", "Housing"]
//
// Those are DSCYF's words. "Before/After School Care" and "Early Childhood/
// Pre-K" are a state agency's service-directory labels, and they were sitting in
// the file that is supposed to define what FIRST STEP's categories are.
//
// The distinction the split makes concrete:
//
//     taxonomy.json          What First Step's categories ARE.        EDITORIAL
//     source-mappings.json   What a provider CALLS them.              INGESTION
//
// It is the same reasoning that produced the taxonomy/navigation split in
// Decision 029 — two artifacts because two lifecycles and two owners. An editor
// changes the taxonomy; adopting a new data provider changes the mappings; those
// events have nothing to do with each other and should not touch one file.
//
// The instruction that prompted it named the concept exactly: matchCategories is
// "a deterministic source adapter", and adapters belong in the engine that uses
// them, not in the domain model they adapt to.
//
// SECTION 2 — WHY THE FILE IS KEYED BY SOURCE
// -----------------------------------------------------------------------------
// The obvious shape would have been a flat map of raw string -> category key,
// which is exactly what the old matchCategories was. Keying by source instead:
//
//     { "sources": [ { "id": "dscyf-directory", "mappings": {...} } ] }
//
// buys two things that matter as soon as there is a second provider:
//
//   PROVENANCE. Merging a second directory's vocabulary into one flat list makes
//   it impossible to tell later whose word "Financial Support" was, or which
//   provider to ask when a mapping looks wrong.
//
//   CORRECTNESS. Two providers can legitimately use the SAME string for
//   different things. A flat map forces one to win silently. Keyed lookup means
//   a mapping only applies to the source that declared it — locked in by
//   CategoryClassifierTest.shouldIgnoreSourceMappingsFromAnUnknownSource.
//
// The cost is that content must carry its source identity. That is read from
// ContentSource.id, a field the model has always had for precisely this and
// never populated — see CivicContentClassifier_annotated.java.
//
// SECTION 3 — WHY A MISSING FILE IS NOT FATAL (unlike a missing taxonomy)
// -----------------------------------------------------------------------------
// TaxonomyService throws if its file is absent. This one logs and returns an
// empty map. The asymmetry is deliberate and worth stating, because "be
// consistent" would give the wrong answer here:
//
//   NO TAXONOMY   => no vocabulary at all. Nothing can be classified, every
//                    category renders empty, and the app looks fine while being
//                    entirely broken. Fail fast; it is cheaper.
//
//   NO MAPPINGS   => content that happens to carry an upstream category falls
//                    through to keyword inference. Only Resources have one, and
//                    they still classify — less precisely. Degraded, not broken.
//
// The rule generalizing both: fail fast when the alternative is a plausible
// wrong answer; degrade when the alternative is a less precise right one.
//
// Note the load failure is logged to stderr rather than swallowed, because
// silently classifying all 229 resources by keyword guess would present as a
// taxonomy problem and send someone looking in the wrong file.
//
// SECTION 4 — WHY MATCHING IS CASE-INSENSITIVE BUT NOT FUZZY
// -----------------------------------------------------------------------------
// Keys are normalized (trimmed, lowercased) on both load and lookup, so a casing
// or whitespace slip in provider data resolves. It is otherwise EXACT: no
// stemming, no partial matching, no similarity.
//
// That restraint is the entire value of Tier 1. A deterministic tier that is
// only mostly deterministic is just a second inference engine with better odds —
// and the Editorial Stability Invariant depends on this tier producing the same
// answer regardless of what the keyword vocabulary is doing. Anything uncertain
// belongs in Tier 2, where uncertainty is measured and reported.
//
// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - CategoryClassifier calls categoryKeyFor() before any keyword scoring.
// - JsonResourceRepository stamps ContentSource.id = "dscyf-directory" so the
//   lookup has a source to key on.
// - validate_schema.py, validate_navigation.py and enrich_resources.py all read
//   the same file from the Python side — the raw-category vocabulary they
//   validate against moved with it.
// - EditorialStabilityTest asserts all 229 resources are placed by THIS service
//   and not by keyword inference.
//
// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Leave the data in taxonomy.json and extract only the lookup logic. Zero data
//   migration and no Python changes — and it leaves a vendor's vocabulary in the
//   editorial domain model, which is the thing being fixed.
// - Flat file, no source key. Smaller migration; loses provenance and breaks the
//   moment two providers disagree about a word.
// - Pass sourceId as a parameter to classify() instead of reading ContentSource.
//   Explicit, but five of six ingestion points have no upstream vocabulary and
//   would pass null forever.
// - Infer the source from the filename being loaded. Works today because each
//   file has one provider; fails the moment a file is merged or a provider spans
//   two files, and encodes an accident of file layout as a business fact.
