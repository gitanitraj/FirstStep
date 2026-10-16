package org.firststep.backend.shared.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// Contact is a composite of phones, websites, and an email address — a single
// "how to reach this" bundle for future slices (Expert, Flyer) where one
// contact block covers multiple channels at once.
// =============================================================================

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Contact {
    public List<Phone> phones;
    public List<Website> websites;
    public String email;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Resource already has working, separate phones/websites lists — nothing
// about that is broken, so this pass does not migrate Resource onto Contact.
// Contact ships now, per the domain-model UML, as a shared-kernel building
// block with zero real callers until Expert or Flyer actually needs a single
// combined contact bundle. Adding it now (rather than waiting) means the
// shared kernel matches the UML's committed shape from the start, without
// forcing an unrelated, unbroken class (Resource) to change to use it.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - No current class references Contact. It exists in shared/model ready for
//   the Expert or Flyer slice to add a `contact: Contact` field once those
//   are built out beyond this pass's package scaffolding.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Migrating Resource.phones/websites onto Contact now: rejected — "don't
//   refactor things that aren't broken." Resource's existing flat lists work
//   fine and nothing requires the composite shape yet.
// - Adding a `phone: String` / `website: String` singular pair instead of a
//   composite: rejected — Resource's existing data already supports multiple
//   phones and websites per item, and Contact is meant to generalize that,
//   not narrow it.
// =============================================================================
