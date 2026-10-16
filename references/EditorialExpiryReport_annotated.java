/*
 * IM-02 annotated reference — complete mirror of backend/src/main/java/org/firststep/backend/shared/lifecycle/EditorialExpiryReport.java
 * WHAT: Read-only internal diagnostic/report integration; no persisted lifecycle state.
 * WHY: stewardship must inspect retained records without changing resident eligibility.
 * INTERACTION: diagnostics reuse IM-01 assessment and its Community calendar; status and
 * Article approval remain separate. The CLI is outside resident component scanning.
 * ALTERNATIVES: a second expiry algorithm, filtered resident inputs, live feed fetching,
 * or an Admin dashboard would violate this package's narrow contract.
 * See docs/v2-completion-plan/IM-02 Execution Report.md and the individual method comments.
 */
package org.firststep.backend.shared.lifecycle;

/** Thin human-readable formatter for internal diagnostics; never a public web endpoint. */
public final class EditorialExpiryReport {
    private EditorialExpiryReport() {}

    public static String render(EditorialExpiryDiagnostics.Report report) {
        StringBuilder out = new StringBuilder("EDITORIAL EXPIRY — HUMAN STEWARDSHIP REPORT\n");
        out.append("As of: ").append(report.asOf()).append("\nCommunity scope: ")
                .append(report.communityId() == null ? "ALL (owners retained)" : text(report.communityId())).append('\n');
        out.append("Stored records inspected: ").append(report.inspectedRecords())
                .append("\nRetained relevance-admitted RSS records included: ").append(report.cachedRssRecords()).append('\n');
        out.append("RSS scope: retained cache only; no live fetch or complete remote-law coverage is implied.\n");
        out.append("Days: signed Community-calendar distance to the inclusive expiry date (0=today).\n");
        out.append("Expiry eligibility alone does not establish resident visibility; classification and other projection gates still apply.\n");
        out.append("\nEXPIRY ATTENTION FINDINGS (negative days = past expiry)\n");
        if (report.findings().isEmpty()) out.append("No approaching-expiry or expired records in this scope.\n");
        for (var finding : report.findings()) {
            appendContext(out, finding.context());
            out.append("  Lifecycle: ").append(finding.assessment().state())
                    .append(" | Attention: ").append(finding.assessment().editorialAttentionRequired())
                    .append(" | Editorial-expiry eligible: ").append(finding.assessment().editorialExpiryEligible())
                    .append(" | Days to expiry: ").append(finding.daysUntilExpiry()).append('\n');
            out.append("  Reason: ").append(finding.reason()).append('\n');
        }
        out.append("\nMETADATA PROBLEMS (not a lifecycle state)\n");
        if (report.problems().isEmpty()) out.append("No invalid editorial-expiry metadata found.\n");
        for (var problem : report.problems()) {
            appendContext(out, problem.context());
            out.append("  Problem: ").append(problem.reason()).append('\n');
        }
        out.append("\nHuman follow-up only: verify, investigate, or use the authorized catalog-editing process. No records were changed; no tasks or notifications were created.\n");
        return out.toString();
    }

    private static void appendContext(StringBuilder out, EditorialExpiryDiagnostics.Context item) {
        out.append("- ").append(text(item.id())).append(" | ").append(text(item.title())).append('\n');
        out.append("  Class/type: ").append(text(item.contentClass())).append('/')
                .append(item.contentType() == null ? "(not recorded)" : item.contentType())
                .append(" | Community: ").append(text(item.communityId()))
                .append(" | Source: ").append(text(item.contentSourceId())).append('\n');
        out.append("  Editorial expiration date: ").append(text(item.expirationDate()))
                .append(" | Stored status: ").append(text(item.status()))
                .append(" | Status allows publication: ").append(item.statusAllowsPublication()).append('\n');
        if (item.articleApproved() != null) {
            out.append("  Article review: ").append(text(item.articleReviewStatus()))
                    .append(" | Approved: ").append(item.articleApproved()).append('\n');
        }
    }

    /** Keep source metadata on one terminal line, without interpreting its control characters. */
    private static String text(String value) {
        return value == null ? "(not recorded)" : value.replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}]", " ");
    }
}
