package org.firststep.backend.shared.service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.Sector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The producer registry, and — more importantly — the FAILURE BOUNDARY.
 *
 * <p>Most of these tests are about what an unresolvable id does NOT do. That is
 * the load-bearing property: provenance resolution is a capability, not a
 * validity gate, so an unknown producer costs an item its sector and nothing
 * else (Decision 045).
 */
class ContentSourceServiceTest {

    private static ContentSourceService serviceWith(Path dir, String json) throws Exception {
        Files.writeString(dir.resolve("content-sources.json"), json);
        return new ContentSourceService(dir.toString());
    }

    private static ContentSource ref(String id) {
        ContentSource source = new ContentSource();
        source.id = id;
        return source;
    }

    /** The real registry, so "resolves from the registry" means the real thing. */
    private static ContentSourceService real() {
        return new ContentSourceService("../app/data");
    }

    @Test
    void shouldResolveAProducerNameFromItsId() {
        assertEquals("Delaware Health and Social Services", real().nameOf("de-dhss").orElseThrow());
    }

    @Test
    void shouldResolveAProducerSectorFromItsId() {
        assertEquals(Sector.GOVERNMENT, real().sectorOf("de-dhss").orElseThrow());
        assertEquals(Sector.COMMUNITY, real().sectorOf("ministry-of-caring").orElseThrow());
        assertEquals(Sector.FIRST_STEP, real().sectorOf("first-step").orElseThrow());
    }

    @Test
    void shouldPlaceOneProducerInOneSectorRegardlessOfWhatTheyPublish() {
        // Wilmington Housing Authority publishes BOTH a news item and a flyer.
        // Sector is a property of the PRODUCER, so both are government — a rule
        // of the form "flyers are community" would be wrong, which is why sector
        // could not be derived from contentType.
        ContentSourceService service = real();

        assertTrue(service.isInSector(ref("wilmington-housing-authority"), Sector.GOVERNMENT));
        assertFalse(service.isInSector(ref("wilmington-housing-authority"), Sector.COMMUNITY));
    }

    @Test
    void shouldReportNoSectorForAnUnknownIdRatherThanGuessingOne() {
        // THE CORE OF THE BOUNDARY. Never government, never community — absent
        // from both, so an editing mistake cannot misattribute a resident's
        // information to the wrong kind of organisation.
        ContentSourceService service = real();

        assertTrue(service.sectorOf("de-department-of-education").isEmpty());
        assertFalse(service.isInSector(ref("de-department-of-education"), Sector.GOVERNMENT));
        assertFalse(service.isInSector(ref("de-department-of-education"), Sector.COMMUNITY));
        assertFalse(service.isInSector(ref("de-department-of-education"), Sector.FIRST_STEP));
    }

    @Test
    void shouldReportNoSectorForContentWithNoSourceAtAll() {
        ContentSourceService service = real();

        assertFalse(service.isInSector(null, Sector.GOVERNMENT));
        assertFalse(service.isInSector(ref(null), Sector.GOVERNMENT));
        assertFalse(service.isInSector(ref("  "), Sector.GOVERNMENT));
    }

    @Test
    void shouldRecordUnknownIdsSoTheyCanBeSurfaced() {
        // An ERROR line is easy to lose; the count is what /api/health exposes.
        ContentSourceService service = real();
        service.sectorOf("de-department-of-education");
        service.sectorOf("de-department-of-education");
        service.sectorOf("another-missing-org");

        assertEquals(2, service.getUnknownIds().size(), "each distinct id reported once, not once per lookup");
        assertTrue(service.getUnknownIds().contains("de-department-of-education"));
    }

    @Test
    void shouldLeaveTheNameNullWhenTheProducerIsUnknown() {
        ContentSource source = ref("de-department-of-education");

        real().resolveName(source);

        assertNull(source.name, "a guessed attribution is worse than none");
    }

    @Test
    void shouldNotOverwriteANameThatIsAlreadySet() {
        // resolveName runs at load; it must be safe to call twice.
        ContentSource source = ref("de-dhss");
        source.name = "Something Authored";

        real().resolveName(source);

        assertEquals("Something Authored", source.name);
    }

    @Test
    void shouldPairEveryFeedWithThePublisherThatDeclaresIt(@TempDir Path dir) throws Exception {
        // A feed cannot exist without a producer, which is what stops a runtime
        // feed title becoming identity.
        ContentSourceService service = serviceWith(dir, """
                { "sources": [
                    { "id": "de-legislature", "name": "Delaware General Assembly",
                      "sector": "government", "feedUrl": "https://example.org/rss" },
                    { "id": "no-feed", "name": "No Feed", "sector": "community" } ] }
                """);

        assertEquals(1, service.feedUrls().size());
        assertEquals("https://example.org/rss", service.feedUrls().get("de-legislature"));
    }

    @Test
    void shouldDegradeToNoSectorsWhenTheRegistryIsMissing(@TempDir Path dir) {
        // NOT fatal, unlike a missing taxonomy. Every page except the two sector
        // pages keeps working without it — making this fatal would turn provenance
        // into the global validity requirement the boundary forbids.
        ContentSourceService service = new ContentSourceService(dir.toString());

        assertEquals(0, service.getProducerCount());
        assertFalse(service.isInSector(ref("de-dhss"), Sector.GOVERNMENT));
    }

    @Test
    void shouldIgnoreAnUnrecognisedSectorRatherThanCoercingIt(@TempDir Path dir) throws Exception {
        ContentSourceService service = serviceWith(dir, """
                { "sources": [ { "id": "odd", "name": "Odd", "sector": "quango" } ] }
                """);

        assertTrue(service.sectorOf("odd").isEmpty());
    }
}
