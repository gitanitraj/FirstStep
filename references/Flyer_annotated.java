package org.firststep.backend.flyer.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// Flyer represents a community flyer — an event or announcement image with
// structured metadata (organization, event date, location) attached, so it
// can be browsed and searched alongside Resources and NewsItems instead of
// only existing as an opaque image file.
// =============================================================================

import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.model.Location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Flyer extends CivicContent {

    public Flyer() {
        this.contentType = ContentType.FLYER;
    }

    public String organization;
    @JsonProperty("event_date")
    public String eventDate;
    public Location location;
    public String image;
}

// =============================================================================
// SLICE F1 UPDATE (Decision 032) — FLYERS FINALLY CLASSIFY
// =============================================================================
// Flyers were the ONLY CivicContent type with no editorial classification at
// all. They had no `category` field and no category_tags; the only way a flyer
// reached a category page was a hardcoded boolean on the category definition:
//
//     CategoryDefinition("community-events", ..., includesFlyers = true)
//     List<Flyer> matched = definition.includesFlyers() ? flyers : List.of();
//
// Every flyer, into Community Events, regardless of what it was about. The
// eviction-rights session, the health fair and the furniture giveaway all filed
// under "Community Events" — so a tenant browsing Housing for eviction help
// found nothing, while the flyer that would have helped sat under events.
//
// F1 added `category_tags` and `subcategory` to the 7 records in flyers.json.
// The Flyer class needed NO new fields for this: both are inherited from the
// CivicContent contract, and Jackson binds them automatically (category_tags
// via the @JsonProperty on the base class). The only Java change here is the
// constructor. That is the contract paying for itself — giving a content type a
// capability it never had, with zero new fields on the type.
//
// includesFlyers was deleted from CategoryDefinition in the same slice.
//
// WHAT THE CLASSIFICATION LOOKS LIKE, and one thing it exposed:
//
//   FL-001 Summer Youth Enrichment    ["Community Events"]              Youth Programs
//   FL-002 Eviction Prevention Session["Housing", "Legal"]              Eviction Prevention
//   FL-003 Volunteer Recruitment      ["Community Support"]             Volunteer Opportunities
//   FL-004 Back-to-School Supply Drive["Community Support"]             Education & Training
//   FL-005 Disability Services Fair   ["Legal", "Community Support"]    Disability Advocacy
//   FL-006 Community Health Fair      ["Health"]                        Medical Care
//   FL-007 Free Furniture Giveaway    ["Furniture & Household"]         Starter Kits
//
// FL-002 is the case that proves the model: "Eviction Prevention" is declared a
// subcategory under BOTH housing and legal, so ONE subcategory value correctly
// places the flyer under both categories.
//
// FL-005 is the case that strains it: it is genuinely Legal (Disability
// Advocacy) and Community Support (Information & Referral), but `subcategory`
// is singular, so only one topic can be named. "Disability Advocacy" was chosen
// as primary. A multi-valued subcategory was considered and rejected — see
// ALTERNATIVES below.
//
// WHY tags WERE LEFT ALONE: the descriptive tags on these records ("Free",
// "Community", "Youth", "Medical Assistance") are exactly what tags are for —
// search and AI retrieval. They were NOT promoted to classification, because
// promoting them is the conflation the contract exists to prevent. Note that
// several of them ("Rental Assistance", "Eviction Prevention") happen to look
// like topic names; under the old validate_navigation.py rule those free-form
// strings were literally being counted as navigation placement. That rule was
// removed in the same slice.
//
// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Extends CivicContent (id, communityId, title, summary, verified, tags,
// contentSource, createdDate, updatedDate all inherited) — Flyer is the first
// real slice built against the shared kernel from scratch, not migrated from
// a v1 flat POJO the way Resource/NewsItem were. That matters: there's no
// legacy JSON shape to bridge, so flyers.json is authored to already match
// this class directly — no post-deserialization field-mapping adapter is
// needed the way JsonResourceRepository/JsonNewsRepository needed one (see
// JsonFlyerRepository_annotated.java).
//
// Two deliberate deviations from the original class sketch this was built
// from (public class Flyer extends CivicContent { private String
// organization; private LocalDate eventDate; private Location location;
// private String image; }):
// - Fields are PUBLIC, not private-with-getters — matching every other
//   domain class in this codebase (Resource, NewsItem, CivicContent,
//   ContentSource, Citation, Location, Phone, Website, Contact, Media all
//   use public fields, no getters/setters). Consistency with 100% of
//   existing precedent won over the sketch's private-field convention.
// - eventDate is String, not java.time.LocalDate — every other date-shaped
//   field in this codebase (NewsItem.published/.expires,
//   ContentSource.retrieved, CivicContent.createdDate/.updatedDate) is a
//   plain String. LocalDate would be the first java.time usage in the whole
//   domain model, and the repositories' hand-constructed `new
//   ObjectMapper()` instances (not Spring's autoconfigured one) don't have
//   the JavaTimeModule registered — deserializing a LocalDate would throw
//   without adding that dependency/registration for one field.
//
// location is a single Location (not List<Location> like Resource) — a
// flyer describes one event at one place, unlike Resource which can list
// multiple office locations for one organization.
//
// image is the bare filename (e.g. "Youth.jpg"), not a full path — matches
// the user's literal instruction ("the filename stored in the Flyer") and
// keeps the field reusable regardless of which static directory ends up
// serving flyer images long-term; the frontend is responsible for prefixing
// the base path.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - JsonFlyerRepository loads these from app/data/flyers.json.
// - FlyerService/FlyerController expose them via GET /api/flyers and
//   /api/flyers/{id}, wrapped in ApiResponse<T> — mirroring
//   ResourceService/ResourceController exactly, per direct instruction.
// - Not wired into DecisionAgentService's retrieval (no
//   FlyerServiceLike-style interface) — out of scope for this pass; AI
//   citation of flyers is a future consideration, not built here.
// - Not wired into the pipeline/ package's Collector/Enricher interfaces —
//   also out of scope; loading is direct, mirroring ResourceService, not
//   routed through the (still-scaffolding-only) pipeline abstraction.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - OCR/AI-assisted metadata extraction from the flyer images themselves:
//   explicitly deferred, per direct instruction. The 7 real images at
//   backend/src/main/resources/static/images/seasonal/ have only standard
//   EXIF metadata (dimensions, timestamps) — no descriptive text embedded —
//   so flyers.json's metadata (organization, event date, location, summary)
//   is manually authored for this pass, not extracted. This is called out
//   explicitly in references/decisions.md so it isn't mistaken for real
//   extracted data later.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED (Slice F1)
// =============================================================================
// - Make `subcategory` a List<String> so FL-005 could name both Disability
//   Advocacy and Information & Referral. Rejected for now: it changes the
//   contract for all five content types (and the 229 resources that each have
//   exactly one) to serve one flyer, and "an item has one primary topic" is a
//   defensible editorial rule. Revisit if multi-topic content becomes common
//   rather than exceptional — the cross-category relationship GRAPH (the
//   enrichment product) is the intended answer to "this also relates to that".
// - Leave flyers unclassified until the relationship graph lands and let the
//   graph surface them. Rejected: the graph is for RELATED content, a secondary
//   surface. A flyer about eviction prevention is not merely "related to"
//   Housing — it IS housing content, and belongs in the primary listing.
// - Derive flyer classification from the existing descriptive tags
//   automatically (several already match topic names). Rejected outright: that
//   is tags driving navigation, the one thing the contract forbids. The tags
//   that look right today do so by luck, and "Free" would classify nothing.
// =============================================================================
