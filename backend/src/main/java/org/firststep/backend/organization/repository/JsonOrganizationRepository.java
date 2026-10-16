package org.firststep.backend.organization.repository;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.firststep.backend.organization.model.Organization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads {@code app/data/organizations.json}, following JsonFlyerRepository's
 * shape: external canonical file first, classpath fallback, once at ready.
 *
 * <p>Two indexes are built at load rather than scanned per request, because both
 * lookups are on the hot path — every resource card resolving its organization
 * hits {@code findById}.
 *
 * <p><b>A missing file is fatal by omission, not by exception.</b> If the
 * registry fails to load the maps stay empty, every lookup returns empty, and
 * organization pages 404 — loudly visible, and it does not take the rest of the
 * site down with it. That is the opposite of taxonomy.json (fatal: no categories
 * means a broken deployment) and matches articles.json (soft: the feature simply
 * is not there).
 */
@Repository
public class JsonOrganizationRepository implements OrganizationRepository {

    private final ObjectMapper mapper = new ObjectMapper();
    private List<Organization> organizations = Collections.emptyList();
    private Map<String, Organization> byId = Map.of();
    private Map<String, Organization> bySlug = Map.of();

    @Value("${app.data.dir:app/data}")
    private String dataDir;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Path external = Path.of(dataDir, "organizations.json");
        try {
            if (external.toFile().exists()) {
                index(parse(mapper.readTree(external.toFile())), external.toString());
                return;
            }
        } catch (Exception e) {
            System.err.println("Failed to load " + external + ": " + e.getMessage());
        }
        try (InputStream is = getClass().getResourceAsStream("/organizations.json")) {
            if (is != null) {
                index(parse(mapper.readTree(is)), "classpath:/organizations.json");
                return;
            }
        } catch (Exception e) {
            System.err.println("Failed to load classpath organizations.json: " + e.getMessage());
        }
        System.err.println("No organizations.json found — organization pages will 404.");
    }

    private List<Organization> parse(JsonNode root) {
        JsonNode records = root.has("organizations") ? root.get("organizations") : root;
        return mapper.convertValue(records, new TypeReference<List<Organization>>() {});
    }

    private void index(List<Organization> loaded, String origin) {
        organizations = List.copyOf(loaded);
        Map<String, Organization> ids = new LinkedHashMap<>();
        Map<String, Organization> slugs = new LinkedHashMap<>();
        for (Organization o : organizations) {
            if (o.id != null) ids.put(o.id, o);
            if (o.slug != null) slugs.put(o.slug, o);
        }
        byId = Map.copyOf(ids);
        bySlug = Map.copyOf(slugs);
        long withSite = organizations.stream().filter(o -> o.website != null).count();
        System.out.println("Loaded organizations from " + origin + " (" + organizations.size()
                + " records, " + withSite + " with an organization-level website)");
    }

    @Override
    public List<Organization> findAll() {
        return organizations;
    }

    @Override
    public Optional<Organization> findById(String id) {
        return Optional.ofNullable(id).map(byId::get);
    }

    @Override
    public Optional<Organization> findBySlug(String slug) {
        return Optional.ofNullable(slug).map(bySlug::get);
    }
}
