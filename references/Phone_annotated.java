package org.firststep.backend.shared.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// Phone is a labeled phone number associated with a piece of civic content.
// Promoted from a Resource-only nested class to the shared kernel so future
// slices (Expert, Flyer) can reuse it.
// =============================================================================

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Phone {
    public String number;
    public String label;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Lifted verbatim out of Resource's nested static Phone class — same two
// fields, same annotation. Pure promotion, no redesign.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Resource.phones is a List<Phone> (retyped from the old nested
//   Resource.Phone to this shared class in the resource slice migration).
// - Contact.phones also holds a List<Phone> — Contact is a new composite for
//   future slices; Resource itself keeps its flat phones list rather than
//   adopting Contact (see Contact_annotated.java).
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - None specific to this class beyond what's covered in Location_annotated.java
//   and Contact_annotated.java (the phones/websites-vs-Contact question).
// =============================================================================
