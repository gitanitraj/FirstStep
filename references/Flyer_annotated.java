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
import org.firststep.backend.shared.model.Location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Flyer extends CivicContent {
    public String organization;
    @JsonProperty("event_date")
    public String eventDate;
    public Location location;
    public String image;
}

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
