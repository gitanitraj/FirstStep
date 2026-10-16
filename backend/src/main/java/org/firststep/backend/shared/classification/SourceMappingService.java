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
