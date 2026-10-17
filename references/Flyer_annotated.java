/*
 * WHAT: Complete annotated IM-10 mirror of backend/src/main/java/org/firststep/backend/flyer/model/Flyer.java.
 * WHY: Explicit occurrence eligibility must not expire Flyer media or alter canonical identity.
 * INTERACTION: Events alone intersects the injected-clock assessment with current inventory,
 * controlled notice kind, producer sector and optional exact Community scope; DTOs allowlist facts.
 * ALTERNATIVES: Prose-derived end times, global Flyer cutoffs and a new Event entity/type were rejected.
 * Tests separately prove nanosecond/end-of-day/DST boundaries, supporting visibility and privacy.
 */
package org.firststep.backend.flyer.model;

import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.model.Location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A community flyer. Flyers were the one CivicContent type with no editorial
 * classification at all — they reached a category only through a hardcoded
 * {@code includesFlyers} boolean on the Community Events category definition.
 * Slice F1 removed that special case: flyers.json now carries
 * {@code category_tags} and {@code subcategory} like every other content type,
 * and flyers classify through the same canonical taxonomy as everything else.
 *
 * <p>{@code eventDate} stays a Flyer-specific field rather than being folded into
 * the contract's {@code expirationDate} — when an event happens is not the same
 * question as when the content stops being relevant, and conflating them would
 * silently expire flyers the carousel still wants to show.
 */
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
    /** Optional explicit occurrence instants (ISO offset timestamp); independent of editorial expiry. */
    public String startsAt;
    public String endsAt;
}
