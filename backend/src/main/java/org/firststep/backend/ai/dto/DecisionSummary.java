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
