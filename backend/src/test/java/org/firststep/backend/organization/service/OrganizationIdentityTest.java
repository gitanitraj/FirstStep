package org.firststep.backend.organization.service;

import java.nio.file.Path;
import java.util.List;

import org.firststep.backend.organization.model.Organization;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The organization registry, read from the REAL app/data files.
 *
 * <p>These assert the identity guarantees the registry exists to provide, and
 * they assert them against authored data rather than fixtures — the failure this
 * guards against is a data/model mismatch, which a hand-built object cannot
 * reproduce.
 */
class OrganizationIdentityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static List<Organization> registry() throws Exception {
        JsonNode root = MAPPER.readTree(Path.of("../app/data/organizations.json").toFile());
        return MAPPER.convertValue(root.get("organizations"), new TypeReference<List<Organization>>() {});
    }

    private static JsonNode resources() throws Exception {
        JsonNode root = MAPPER.readTree(Path.of("../app/data/resources.json").toFile());
        if (root.isArray()) {
            return root;
        }
        // The file is an object with metadata alongside the records, so take the
        // array-valued field rather than simply the first one.
        for (JsonNode child : root) {
            if (child.isArray()) {
                return child;
            }
        }
        throw new IllegalStateException("no records array in resources.json");
    }

    @Test
    void shouldGiveEveryOrganizationAStableIdAndAPublicSlug() throws Exception {
        for (Organization o : registry()) {
            assertNotNull(o.id, "an organization without an id has no identity");
            assertNotNull(o.slug, "an organization without a slug has no URL");
            assertNotNull(o.name, o.id + " has no name");
            assertTrue(o.id.startsWith("ORG-"), "unexpected id shape: " + o.id);
        }
    }

    @Test
    void shouldKeepIdentityDistinctFromTheUrl() throws Exception {
        // The whole point of the registry: the ID is not derived from the name,
        // so renaming an organization cannot change what resources point at.
        List<Organization> orgs = registry();

        long derivable = orgs.stream()
                .filter(o -> o.id.equals(o.slug) || o.id.equalsIgnoreCase(o.name))
                .count();

        assertEquals(0, derivable, "an id that is a function of the name is not a stable identity");
    }

    @Test
    void shouldNotIssueTheSameIdOrSlugTwice() throws Exception {
        List<Organization> orgs = registry();

        assertEquals(orgs.size(), orgs.stream().map(o -> o.id).distinct().count(),
                "duplicate id: resources would attach to whichever record loaded second");
        assertEquals(orgs.size(), orgs.stream().map(o -> o.slug).distinct().count(),
                "duplicate slug: one organization becomes unreachable");
    }

    @Test
    void shouldResolveEveryResourceToARegisteredOrganization() throws Exception {
        List<String> ids = registry().stream().map(o -> o.id).toList();

        for (JsonNode r : resources()) {
            JsonNode ref = r.get("organization_id");
            assertNotNull(ref, "resource " + r.get("id").asText() + " has no organization_id");
            assertTrue(ids.contains(ref.asText()),
                    "resource " + r.get("id").asText() + " references unknown " + ref.asText());
        }
    }

    @Test
    void shouldWithholdOrganizationWebsiteWhenTheDataDoesNotEstablishOne() throws Exception {
        // Decision 049: absence beats an arbitrary authoritative link. Most
        // organizations legitimately have none, and that must survive as null
        // rather than being backfilled from a program page.
        List<Organization> orgs = registry();
        long withSite = orgs.stream().filter(o -> o.website != null).count();

        assertTrue(withSite > 0, "some organizations do declare their own website");
        assertTrue(withSite < orgs.size(),
                "withholding is expected for organizations whose data does not establish a site");
    }

    @Test
    void shouldNotPromoteAnyPhoneToOrganizationLevel() throws Exception {
        // Phone labels are service-scoped ("Main" = the main line FOR THAT
        // SERVICE). There is deliberately no phone field on Organization; a
        // resident in crisis calling a guessed number is the failure this avoids.
        for (Organization o : registry()) {
            assertFalse(java.util.Arrays.stream(Organization.class.getFields())
                            .anyMatch(f -> f.getName().toLowerCase().contains("phone")),
                    "Organization must not carry a phone: " + o.id);
        }
    }
}
