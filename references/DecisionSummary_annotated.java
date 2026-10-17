/*
 * Annotated reference: backend/src/main/java/org/firststep/backend/ai/dto/DecisionSummary.java
 * WHAT: Preserves the complete source below; see the explicit allowlists and optional exact-owner filters in the source.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Controllers adapt internal models only at the resident boundary; tests and evidence exercise real repositories plus labeled synthetic counterexamples.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
package org.firststep.backend.ai.dto;
import java.util.List;
import org.firststep.backend.shared.dto.ResidentContent;
/** Resident AI response: publication attribution is public; ingestion timestamps are not. */
public record DecisionSummary(String answerTitle, List<DecisionStep> steps, List<PublicCitation> citations, String notes) {
    public record PublicCitation(String sourceType, String id, String label, ResidentContent.Source contentSource) {}
    public static DecisionSummary from(DecisionResponse r) {
        return new DecisionSummary(r.answerTitle, r.steps == null ? List.of() : List.copyOf(r.steps),
                r.citations == null ? List.of() : r.citations.stream().map(c -> new PublicCitation(
                        c.sourceType, c.id, c.label, ResidentContent.source(c.contentSource))).toList(), r.notes);
    }
}
