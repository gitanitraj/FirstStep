package org.firststep.backend.shared.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// Website is a labeled URL associated with a piece of civic content.
// Promoted from a Resource-only nested class to the shared kernel so future
// slices (Expert, Flyer) can reuse it.
// =============================================================================

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Website {
    public String url;
    public String label;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Lifted verbatim out of Resource's nested static Website class — same two
// fields, same annotation. Pure promotion, no redesign.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Resource.websites is a List<Website> (retyped from the old nested
//   Resource.Website to this shared class in the resource slice migration).
// - Contact.websites also holds a List<Website> — see Contact_annotated.java
//   for why Resource keeps its own flat list instead of adopting Contact.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - None specific to this class beyond what's covered in Location_annotated.java
//   and Contact_annotated.java.
// =============================================================================
