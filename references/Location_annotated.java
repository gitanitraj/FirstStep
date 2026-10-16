package org.firststep.backend.shared.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// Location is a physical address associated with a piece of civic content —
// today only Resource has locations, but it's promoted to the shared kernel
// so Flyer/Expert (whose venues/office locations are the same shape) can
// reuse it without duplicating the class.
// =============================================================================

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Location {
    public String label;
    public String address;
    public String city;
    public String state;
    public String zip;
    public Boolean confidential;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Lifted verbatim out of Resource's nested static Location class — same six
// fields, same @JsonIgnoreProperties(ignoreUnknown = true) annotation. No
// fields were added, removed, or renamed; this is a pure promotion from a
// nested type to a top-level shared type, not a redesign.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Resource.locations is a List<Location> (retyped from the old nested
//   Resource.Location to this shared class in the resource slice migration).
// - Not yet used by NewsItem or any other slice — no existing data needs it.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Folding Location into Contact (alongside Phone/Website): rejected —
//   a physical address and a phone/website are different kinds of contact
//   information with different fields (confidential flag doesn't apply to a
//   phone number), and Resource's existing locations/phones/websites are
//   already three separate lists, not one combined structure. Promoting them
//   to three separate shared classes (this one, Phone, Website) preserves
//   that existing shape rather than merging it.
// =============================================================================
