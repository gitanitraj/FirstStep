package org.firststep.backend.shared.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.Sector;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The producer registry — loads {@code app/data/content-sources.json} and answers
 * two questions about a {@code contentSource.id}: what is this producer called,
 * and what sector are they?
 *
 * <p><b>Records reference a producer by id and never duplicate its attributes.</b>
 * {@code name} is resolved from here at load time, which is what collapses
 * "Delaware DHSS" and "Delaware Health and Social Services" into one agency. Two
 * files holding the same producer name is the drift bug Decision 032 removed for
 * category labels; this is the same rule applied to provenance.
 *
 * <p><b>THE FAILURE BOUNDARY — the important part of this class.</b>
 * An unresolvable id means the item cannot participate in SECTOR-SCOPED VIEWS.
 * It means nothing else.
 *
 * <pre>
 *   provenance resolution is a CAPABILITY, not a VALIDITY GATE
 * </pre>
 *
 * <p>An item whose id does not resolve is still perfectly good CivicContent — it
 * has a title, a summary, a category, a date. It is browsable, searchable,
 * classifiable, and appears on category and topic pages exactly as before. The
 * only thing it cannot do is answer "which sector produced this?", so it is
 * absent only from the views defined by that question.
 *
 * <p>The codebase already works this way: Decision 036 established that content
 * with a category but no {@code subcategory} is FULLY classified, not half
 * classified — it simply cannot appear on a topic page. A missing optional
 * dimension narrows WHERE content can appear; it never invalidates the content.
 *
 * <p><b>So this class never throws on an unknown id, and never guesses one.</b>
 * Throwing would make provenance a global validity requirement for all
 * CivicContent, which is precisely what the boundary forbids — exclude-and-log is
 * not a weaker version of failing fast, it is the behaviour the architecture
 * implies. {@code validate_content_sources.py} is the build-time gate that stops
 * a bad id shipping; this is defense in depth, and defense in depth must not be
 * the thing that breaks.
 *
 * <p>Unlike {@link org.firststep.backend.category.service.TaxonomyService}, a
 * MISSING FILE is also non-fatal here for the same reason — every page except the
 * two sector pages keeps working without it.
 */
@Service
public class ContentSourceService {

    /** content-sources.json's shape; version/note/sectors are metadata. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Registry(List<Producer> sources) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Producer(String id, String name, String sector, String feedUrl) {
    }

    private final Map<String, Producer> byId;
    /** Ids referenced by content but absent from the registry. Reported, never guessed. */
    private final Set<String> unknownIds = new LinkedHashSet<>();

    public ContentSourceService(@Value("${app.data.dir:app/data}") String dataDir) {
        this.byId = index(read(dataDir));
        System.out.println("Loaded content sources (" + byId.size() + " producers, "
                + feedUrls().size() + " with feeds)");
    }

    /** The producer's canonical display name, or empty if the id does not resolve. */
    public Optional<String> nameOf(String id) {
        Producer producer = resolve(id);
        return producer == null ? Optional.empty() : Optional.ofNullable(producer.name());
    }

    /**
     * NORMALIZE STAGE: fill in a record's producer name from its id.
     *
     * <p>Records carry the id and nothing else about the producer, so this is what
     * turns a reference into a display value. Repositories call it as they load —
     * the same place news.json's {@code headline} becomes {@code title} — so every
     * downstream consumer sees a resolved name without having to know the registry
     * exists.
     *
     * <p>Best-effort and non-blocking: an unresolvable id leaves the name null and
     * logs, exactly as the failure boundary requires. The item is still loaded.
     */
    public void resolveName(ContentSource contentSource) {
        if (contentSource == null || contentSource.name != null) {
            return;
        }
        nameOf(contentSource.id).ifPresent(name -> contentSource.name = name);
    }

    /**
     * The producer's sector, or empty when the id is null, absent or unknown.
     * Callers filtering by sector must treat empty as "not in any sector" — never
     * as a default.
     */
    public Optional<Sector> sectorOf(String id) {
        Producer producer = resolve(id);
        return producer == null ? Optional.empty() : Sector.fromKey(producer.sector());
    }

    /** True when this content belongs to the given sector. Unknown ids are false for every sector. */
    public boolean isInSector(ContentSource contentSource, Sector sector) {
        if (contentSource == null) {
            return false;
        }
        return sectorOf(contentSource.id).filter(sector::equals).isPresent();
    }

    /**
     * Every configured feed, as {@code producerId -> feedUrl}. RssFeedService reads
     * its feed list from here so a feed cannot exist without a declared producer,
     * and a runtime feed title can never become identity.
     */
    public Map<String, String> feedUrls() {
        Map<String, String> feeds = new LinkedHashMap<>();
        byId.values().stream()
                .filter(p -> p.feedUrl() != null && !p.feedUrl().isBlank())
                .forEach(p -> feeds.put(p.id(), p.feedUrl()));
        return feeds;
    }

    /** Ids that content referenced but the registry does not define. */
    public List<String> getUnknownIds() {
        return List.copyOf(unknownIds);
    }

    /**
     * One summary line once everything has loaded.
     *
     * <p>Individual ERROR lines are easy to lose in a boot log; this is the line
     * that makes a slow rot visible. It runs on ApplicationReadyEvent because
     * unknown ids are discovered as the repositories resolve names during their
     * own load — by the time the app is ready, every id referenced by content has
     * been seen.
     *
     * <p><b>Not exposed on {@code /api/health}</b>, which returns a bare "OK" and
     * lives in ResourceController: reshaping a liveness probe to carry editorial
     * diagnostics would put provenance in the one endpoint that must stay trivial,
     * and give a resource controller a reason to know about content sources.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void reportUnknownIds() {
        if (unknownIds.isEmpty()) {
            System.out.println("content-sources: " + byId.size() + " producers, all references resolved");
        } else {
            System.out.println("content-sources: " + byId.size() + " producers, "
                    + unknownIds.size() + " UNRESOLVED reference(s) " + unknownIds
                    + " — that content is excluded from sector views. Run "
                    + "validate_content_sources.py; this should never reach production.");
        }
    }

    public int getProducerCount() {
        return byId.size();
    }

    private Producer resolve(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        Producer producer = byId.get(id);
        if (producer == null && unknownIds.add(id)) {
            // Logged ONCE per distinct id — a broken feed would otherwise fill the
            // log with the same line and bury everything else.
            System.out.println("ERROR content-sources: unknown contentSource.id '" + id
                    + "' — excluded from sector views, still valid elsewhere.");
        }
        return producer;
    }

    private static Map<String, Producer> index(Registry registry) {
        if (registry == null || registry.sources() == null) {
            System.out.println("No content-sources.json found — no content can be placed in a sector.");
            return Map.of();
        }
        Map<String, Producer> map = new LinkedHashMap<>();
        List<String> duplicates = new ArrayList<>();
        for (Producer producer : registry.sources()) {
            if (producer == null || producer.id() == null) {
                continue;
            }
            if (map.putIfAbsent(producer.id(), producer) != null) {
                duplicates.add(producer.id());
            }
        }
        if (!duplicates.isEmpty()) {
            System.out.println("ERROR content-sources: duplicate producer ids " + duplicates
                    + " — first definition wins.");
        }
        return Map.copyOf(map);
    }

    private static Registry read(String dataDir) {
        ObjectMapper mapper = new ObjectMapper();
        Path external = Path.of(dataDir, "content-sources.json");
        try {
            if (Files.exists(external)) {
                return mapper.readValue(external.toFile(), Registry.class);
            }
            try (InputStream in = ContentSourceService.class.getResourceAsStream("/content-sources.json")) {
                return in == null ? null : mapper.readValue(in, Registry.class);
            }
        } catch (Exception e) {
            System.out.println("Failed to load content-sources.json: " + e.getMessage());
            return null;
        }
    }
}
