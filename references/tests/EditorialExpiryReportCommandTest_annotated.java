/*
 * IM-02 annotated reference — complete mirror of backend/src/test/java/org/firststep/maintenance/EditorialExpiryReportCommandTest.java
 * WHAT: Fixed-clock diagnostic/command acceptance evidence.
 * WHY: stewardship must inspect retained records without changing resident eligibility.
 * INTERACTION: diagnostics reuse IM-01 assessment and its Community calendar; status and
 * Article approval remain separate. The CLI is outside resident component scanning.
 * ALTERNATIVES: a second expiry algorithm, filtered resident inputs, live feed fetching,
 * or an Admin dashboard would violate this package's narrow contract.
 * See docs/v2-completion-plan/IM-02 Execution Report.md and the individual method comments.
 */
package org.firststep.maintenance;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class EditorialExpiryReportCommandTest {
    private record Result(int exit, String output) {}

    private Result run(String... args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int exit;
        try (PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            exit = EditorialExpiryReportCommand.execute(args, out);
        }
        return new Result(exit, bytes.toString(StandardCharsets.UTF_8));
    }

    @Test
    void shouldReportRealStoredExpiredPoliciesAndApproachingItemRepeatably() {
        String[] args = {"--at=2026-10-15T16:00:00Z", "--community=wilmington-de", "--app.data.dir=../app/data"};
        Result first = run(args);
        assertEquals(0, first.exit(), first.output());
        assertEquals(first, run(args));
        for (String id : List.of("NP-001", "NP-002", "NP-006")) {
            int start = first.output().indexOf("- " + id + " |");
            assertTrue(start >= 0, id);
            int end = first.output().indexOf("\n- ", start + 1);
            String item = first.output().substring(start, end < 0 ? first.output().length() : end);
            assertTrue(item.contains("Lifecycle: EXPIRED"), id);
            assertTrue(item.contains("Editorial-expiry eligible: false"), id);
        }
        assertTrue(first.output().contains("- NP-003 |"));
        assertTrue(first.output().contains("Lifecycle: APPROACHING_EXPIRY"));
        assertTrue(first.output().contains("Days to expiry: 16"));
        assertTrue(first.output().contains("As of: 2026-10-15T16:00:00Z"));
        assertTrue(first.output().contains("RSS scope: retained cache only"));
        assertFalse(first.output().contains("DateTimeParseException"));
    }

    @Test
    void shouldRenderAnIntelligibleEmptyCommunityReport() {
        Result result = run("--at=2026-10-15T16:00:00Z", "--community=no-such-community", "--app.data.dir=../app/data");
        assertEquals(0, result.exit(), result.output());
        assertTrue(result.output().contains("Stored records inspected: 0"));
        assertTrue(result.output().contains("No approaching-expiry or expired records in this scope."));
    }

    private void copyCatalog(Path target) throws Exception {
        try (var paths = Files.list(Path.of("../app/data"))) {
            for (Path source : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                Files.copy(source, target.resolve(source.getFileName()));
            }
        }
    }

    @Test
    void shouldIsolateAnInvalidStoredDateInTheCommandWithoutLosingOtherFindings(@TempDir Path catalog) throws Exception {
        copyCatalog(catalog);
        ObjectMapper mapper = new ObjectMapper();
        Path file = catalog.resolve("news.json");
        ObjectNode root = (ObjectNode) mapper.readTree(file.toFile());
        ((ArrayNode) root.get("records")).add(mapper.readTree("""
                {"id":"DIAGNOSTIC-INVALID", "headline":"Controlled malformed expiry fixture",
                 "expires":"2026-02-30", "source_id":"city-of-wilmington", "active":false,
                 "communityId":"wilmington-de", "category_tags":["Housing"]}
                """));
        Files.writeString(file, mapper.writeValueAsString(root));
        String before = Files.readString(file);
        Result result = run("--at=2026-10-15T16:00:00Z", "--community=wilmington-de", "--app.data.dir=" + catalog);
        assertEquals(1, result.exit(), result.output());
        assertTrue(result.output().contains("- NP-001 |"));
        assertTrue(result.output().contains("- DIAGNOSTIC-INVALID |"));
        assertTrue(result.output().contains("no lifecycle state was assigned"));
        assertTrue(result.output().contains("Stored status: inactive | Status allows publication: false"));
        assertFalse(result.output().contains("DateTimeParseException"));
        assertEquals(before, Files.readString(file));
    }

    @Test
    void shouldRejectPartialRepositoryLoadingRatherThanReportAFalseEmptySuccess(@TempDir Path catalog) throws Exception {
        copyCatalog(catalog);
        Files.writeString(catalog.resolve("news.json"), """
                {"records":[{"id":"BAD-SHAPE","category_tags":{"not":"a list"}}]}
                """);
        Result result = run("--at=2026-10-15T16:00:00Z", "--app.data.dir=" + catalog);
        assertEquals(2, result.exit(), result.output());
        assertTrue(result.output().contains("Stored catalog loading was incomplete"));
        assertFalse(result.output().contains("EDITORIAL EXPIRY — HUMAN STEWARDSHIP REPORT"));
    }

    @Test
    void shouldRejectInvalidClockOrOptionsWithoutStackTraces() {
        Result clock = run("--at=not-an-instant");
        assertEquals(2, clock.exit());
        assertTrue(clock.output().contains("Invalid --at"));
        assertFalse(clock.output().contains("Exception"));
        assertEquals(2, run("--community").exit());
        assertEquals(2, run("--at=2026-10-15T16:00:00Z", "--at=2026-10-16T16:00:00Z").exit());
        assertEquals(2, run("--unknown=value").exit());
    }

    @Test
    void shouldOfferUsageWithoutStartingTheApplication() {
        Result result = run("--help");
        assertEquals(0, result.exit());
        assertTrue(result.output().contains("--community=wilmington-de"));
        assertFalse(result.output().contains("Stored records inspected"));
    }
}
