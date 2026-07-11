package org.firststep.backend.shared.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// Community represents a geographic community First Step serves — today only
// Wilmington, DE, but the model exists so the platform can add more
// communities without redesigning the knowledge model. Every CivicContent
// object carries a communityId pointing at one of these.
// =============================================================================

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Community {
    public String id;
    public String name;
    public String city;
    public String state;
    public List<String> zipCodes;
    public Boolean active;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Field list matches the domain-model UML and the project's own
// docs/architecture/00-philosophy.md, which names Community "a first-class
// partition across the whole model" from day one, even with only one
// community's data existing today.
//
// This pass ships Community as a model class plus a tiny default-stamping
// mechanism (every Resource/NewsItem gets communityId = "wilmington-de" via
// an app.default-community-id property at load time) — it does not add a
// CommunityController or a CommunityService with real query behavior, since
// building query methods against a single hardcoded community would be
// speculative flexibility with nothing to exercise it yet.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - CivicContent.communityId is a plain String foreign key into Community.id
//   — there is no embedded Community object on CivicContent subtypes, keeping
//   the flat-POJO/JSON-file style used everywhere else in this codebase.
// - No repository or loader for Community itself exists yet in this pass;
//   the single default community is supplied via configuration, not loaded
//   from a data file.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Embedding a full Community object on every CivicContent instead of a
//   String id: rejected — needlessly duplicates the same Community data
//   across every Resource/NewsItem in memory, and doesn't match how this
//   codebase references other entities (always by flat id, e.g. Citation.id).
// =============================================================================
