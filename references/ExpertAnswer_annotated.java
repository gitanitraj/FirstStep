package org.firststep.backend.expert.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// ExpertAnswer is a CivicContent subtype representing one question answered
// by a named expert in a curated session — the "monthly curated Q&A"
// backlog item. Served via GET /api/expert-answers and /api/expert-answers/{id}.
// =============================================================================

import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.Contact;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExpertAnswer extends CivicContent {
    public String question;
    public String answer;
    @JsonProperty("expert_name")
    public String expertName;
    @JsonProperty("expert_credentials")
    public String expertCredentials;
    @JsonProperty("expert_organization")
    public String expertOrganization;
    @JsonProperty("expert_contact")
    public Contact expertContact;
    @JsonProperty("session_date")
    public String sessionDate;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// PUBLIC FIELDS, NOT PRIVATE+GETTERS: matches 100% of the existing domain
// model convention (Resource, NewsItem, Flyer) — no reason to deviate here.
//
// expertContact REUSES shared/model/Contact (phones/websites/email) RATHER
// THAN FLAT expertPhone/expertWebsite/expertEmail FIELDS: the shared-kernel
// docs (references/Contact_annotated.java) explicitly anticipated Expert as
// Contact's first real adopter since the original migration — this is that
// adoption. Using the composite instead of flat fields also means adding a
// second phone/website later needs no schema change.
//
// tags (INHERITED) CARRIES TOPIC LABELS ("housing", "benefits") INSTEAD OF
// A NEW category FIELD: Decision 014 just spent real effort discovering
// that Resource.category was uncontrolled free text needing a whole mapping
// layer to become useful. Introducing a second, separate free-text category
// field on ExpertAnswer would repeat that exact problem. tags is already
// the established free-form topic-labeling field on CivicContent — reusing
// it avoids inventing a second taxonomy that would eventually need its own
// Decision-014-style reconciliation.
//
// snake_case JSON KEYS VIA @JsonProperty FOR MULTI-WORD FIELDS: matches the
// existing convention used by both Resource (parent_organization,
// eligibility_age_min, etc.) and Flyer (event_date) — this codebase's
// hand-authored JSON data files consistently use snake_case, bridged to
// camelCase Java fields via explicit @JsonProperty annotations.
//
// NO FIELD-MAPPING ADAPTER IN THE REPOSITORY (see
// JsonExpertAnswerRepository_annotated.java): app/data/expert-answers.json
// is authored directly in this class's target shape, including a full
// contentSource object per record — same choice Flyer made, for the same
// reason (brand-new data, no legacy shape to bridge).
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - expert/repository/JsonExpertAnswerRepository loads and deserializes
//   instances from app/data/expert-answers.json.
// - expert/service/ExpertAnswerService, expert/controller/
//   ExpertAnswerController delegate straight through, mirroring Flyer's
//   thin-service/thin-controller shape.
// - expert/model/FAQ.sourceExpertAnswerId can reference an ExpertAnswer.id,
//   though nothing resolves that reference automatically yet (see
//   FAQ_annotated.java).
// - NOT YET wired into search/service/SearchService or
//   category/service/CategoryService — deliberately deferred, per direct
//   instruction, to its own follow-up once real content volume exists. See
//   references/decisions.md Decision 015.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Flat expertPhone/expertWebsite/expertEmail fields instead of Contact:
//   rejected — Contact already exists specifically for this purpose; using
//   it now is completing a design decision made during the original
//   migration, not introducing a new one.
// - A dedicated expertTopic/category field: rejected in favor of reusing
//   inherited tags — see WHY section.
// =============================================================================
