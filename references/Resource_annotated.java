package org.firststep.backend.resource.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// Resource represents a civic resource (a service, program, or organization
// residents can turn to) — the core content type First Step was originally
// built around. This is the resource slice's version, extending the shared
// CivicContent base instead of standing alone as a flat POJO.
// =============================================================================

import java.util.List;

import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.model.Location;
import org.firststep.backend.shared.model.Phone;
import org.firststep.backend.shared.model.Website;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Resource extends CivicContent {

    public Resource() {
        this.contentType = ContentType.RESOURCE;
    }

    /** Raw source category as published by the directory — NOT the canonical taxonomy label. */
    public String category;
    public String organization;
    @JsonProperty("parent_organization")
    public String parentOrganization;
    public String description;
    public String population;
    public String eligibility;
    @JsonProperty("eligibility_age_min")
    public Integer eligibilityAgeMin;
    @JsonProperty("eligibility_age_max")
    public Integer eligibilityAgeMax;
    @JsonProperty("eligibility_gender")
    public String eligibilityGender;
    public List<Location> locations;
    public List<Phone> phones;
    public List<Website> websites;
    public String county;
    @JsonProperty("access_mode")
    public List<String> accessMode;
    public String cost;
    public String urgency;
    public String notes;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Moved from org.firststep.backend.model.Resource (a standalone flat POJO)
// to org.firststep.backend.resource.model.Resource, now extending
// CivicContent. What moved onto the shared base vs. what stayed:
//
// - id, verified, communityId, title, summary, tags, contentSource,
//   createdDate, updatedDate: inherited from CivicContent, no longer
//   declared on this class.
// - title ← organization: app.js has always rendered resource.organization
//   as every resource card's heading (confirmed at app.js lines
//   ~683/733/844 before this migration) — that's the closest existing
//   analogue to a "title." organization itself was NOT renamed or removed;
//   it stays a Resource-specific field (still meaningful domain vocabulary
//   alongside parentOrganization). title is populated FROM organization by
//   JsonResourceRepository at load time — see
//   JsonResourceRepository_annotated.java for the actual mapping code.
// - summary and tags: v1's Resource.summary and Resource.tags mapped 1:1
//   onto CivicContent's fields of the same name — Jackson deserializes them
//   directly into the inherited fields with no code needed, since the JSON
//   key names already match.
// - source/retrieved (flat Strings in v1): removed. Superseded by the
//   inherited contentSource: ContentSource object, built from those same
//   flat JSON keys by JsonResourceRepository (Jackson would otherwise
//   silently ignore them as unknown properties, thanks to
//   @JsonIgnoreProperties(ignoreUnknown = true)).
// - category, subcategory, organization, parentOrganization, description,
//   population, eligibility*, county, accessMode, cost, urgency, notes:
//   unchanged, resource-specific — not part of the shared CivicContent
//   shape (they don't generalize to NewsItem/Flyer/etc.).
// - locations/phones/websites: retyped from Resource's old nested static
//   Location/Phone/Website classes to the now-shared top-level classes in
//   shared.model — same fields, same annotations, just promoted.
// =============================================================================

// =============================================================================
// SLICE F1 UPDATE (Decision 032) — THE CivicContent CONTRACT
// =============================================================================
// Two changes, both consequences of CivicContent becoming a formal contract:
//
// 1. `subcategory` MOVED UP to CivicContent. It is no longer a Resource field.
//    It was only ever declared here because resources were the only type that
//    had one — but "what topic is this about?" is a question EVERY content type
//    must answer, and flyers now answer it too. Leaving it on Resource would
//    mean a topic page had to ask "is this a Resource?" before it could read
//    the topic, which is the special-casing the contract removes.
//
// 2. The constructor sets contentType = RESOURCE. See
//    CivicContent_annotated.java Section 2 for why this is a constructor
//    assignment rather than an overridden abstract method.
//
// WHAT DELIBERATELY DID NOT CHANGE: `category` still holds the RAW source
// string ("Housing Assistance", "Recreational", "Before/After School Care") —
// DSCYF directory vocabulary, not a canonical taxonomy label. Resources are
// therefore the one content type still NOT carrying canonical categoryTags:
// CategoryService continues to translate their raw category through the
// taxonomy's matchCategories list.
//
// That is a deliberate seam, not an oversight. Normalizing a raw source
// category into a canonical one is CLASSIFICATION, and Slice F2 introduces
// shared/classification/ to do exactly that for every source at once (RSS has
// the same problem, worse). Doing it inline here in F1 would have meant writing
// a second classifier that F2 then deletes.
//
// When F2 lands, `category` becomes pure provenance — a record of what the
// upstream directory said — and `categoryTags` carries the canonical answer.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - JsonResourceRepository loads and constructs these from app/data/resources.json.
// - ResourceService/ResourceController expose them via GET /api/resources and
//   /api/resources/{id}, wrapped in ApiResponse<T>.
// - DecisionAgentService reads organization/summary/description/category/
//   subcategory/tags/urgency/phones directly (all still valid field
//   accesses after this migration — either inherited or unchanged) to score
//   and cite resources in AI-generated guidance.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Renaming organization to title directly instead of adding a separate
//   inherited title field populated from it: rejected — organization is
//   used throughout DecisionAgentService's scoring/prompt logic and the
//   eligibility/category fields conceptually pair with "which organization
//   provides this," so keeping the name avoids an unrelated rename ripple
//   through code this migration doesn't otherwise need to touch.
// =============================================================================
