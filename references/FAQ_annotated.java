package org.firststep.backend.expert.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// FAQ is a CivicContent subtype for a simple, curated question/answer entry
// — deliberately simpler than ExpertAnswer, with no individual expert
// attribution. Served via GET /api/faqs and /api/faqs/{id}.
// =============================================================================

import org.firststep.backend.shared.model.CivicContent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FAQ extends CivicContent {
    public String question;
    public String answer;
    @JsonProperty("source_expert_answer_id")
    public String sourceExpertAnswerId;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// DELIBERATELY SIMPLER THAN ExpertAnswer: no expertName/expertCredentials/
// expertOrganization/expertContact/sessionDate. A FAQ entry is a curated
// knowledge-base answer, not a specific person's session — the domain
// model doc has always listed FAQ and ExpertAnswer as two separate
// CivicContent subtypes (docs/architecture/01-domain-model.md), and this
// keeps them genuinely distinct in shape rather than one being a redundant
// subset of the other's fields.
//
// sourceExpertAnswerId (NULLABLE) DIRECTLY REFLECTS THE CONFIRMED "Expert
// answers are the basis for FAQ answers" RELATIONSHIP: a plain reference-
// by-id field, not a foreign-key/JPA relationship (there's no database —
// everything is JSON-file-backed) and not an embedded copy of the
// ExpertAnswer's fields (would drift out of sync). Deliberately NOT
// auto-resolved into a real ExpertAnswer object anywhere in this pass —
// no generation logic exists yet; some FAQ entries in app/data/faq.json
// simply carry a matching id (e.g. "EA-002") as a human-curated pointer,
// others carry null when a FAQ wasn't derived from a specific session.
//
// tags (INHERITED), NOT A category FIELD: same reasoning as ExpertAnswer —
// see ExpertAnswer_annotated.java's WHY section on avoiding a second
// free-text taxonomy after Decision 014.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - expert/repository/JsonFaqRepository loads instances from
//   app/data/faq.json.
// - expert/service/FaqService, expert/controller/FaqController mirror
//   ExpertAnswer's thin service/controller shape exactly.
// - sourceExpertAnswerId, when non-null, is expected to match an
//   ExpertAnswer.id — but nothing validates or resolves this link today;
//   a client wanting the full ExpertAnswer must call GET
//   /api/expert-answers/{sourceExpertAnswerId} itself.
// - NOT YET wired into Search or Category aggregation — see
//   ExpertAnswer_annotated.java's HOW section and Decision 015.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Merging FAQ into ExpertAnswer (one class, expert fields nullable):
//   rejected — the domain model doc already committed to two separate
//   subtypes, and a FAQ genuinely doesn't need 5 expert-attribution fields
//   sitting null on every record.
// - Validating/resolving sourceExpertAnswerId at load time (e.g. dropping
//   the reference if no matching ExpertAnswer exists): rejected as
//   unnecessary defensive logic for a "stub" pass with hand-authored,
//   already-consistent data — revisit if this data is ever
//   externally/automatically generated.
// =============================================================================
