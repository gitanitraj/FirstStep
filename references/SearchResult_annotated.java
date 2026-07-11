package org.firststep.backend.search.dto;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// SearchResult wraps a single matched item from a /api/search query: which
// type of CivicContent it is, how well it scored, and the item itself.
// SearchService returns a List<SearchResult> — one unified, ranked list
// mixing Resource/NewsItem/Flyer matches together, sorted by score.
// =============================================================================

import org.firststep.backend.shared.model.CivicContent;

public record SearchResult(String type, int score, CivicContent content) {
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// UNIFIED LIST, NOT GROUPED-BY-TYPE: the alternative design
// ({resources: [...], news: [...], flyers: [...]}) was presented to the
// user and explicitly rejected in favor of this one — the whole point of
// searching across all CivicContent types is a real cross-type ranking (a
// highly-relevant Flyer should be able to outrank a weakly-relevant
// Resource); three separate lists would just push that interleaving work
// onto every future client instead of doing it once, here.
//
// `type` IS A PLAIN STRING DISCRIMINATOR ("resource"/"news"/"flyer"), NOT
// A JACKSON @JsonTypeInfo POLYMORPHISM SETUP: simplest thing that works —
// a client just checks result.type to know how to interpret result.content.
// No inheritance-aware deserialization is needed because SearchResult is
// only ever produced (serialized), never consumed/deserialized by this
// backend.
//
// `content` IS TYPED AS THE ABSTRACT CivicContent, NOT Object: gives
// compile-time safety in SearchService (can only put real CivicContent
// subtypes in here) while still serializing every subtype-specific field
// (Resource.organization, NewsItem.whyItMatters, Flyer.image, etc.) —
// confirmed Jackson's default ObjectMapper serializes a field's RUNTIME
// type, not its declared static type (no MapperFeature.USE_STATIC_TYPING
// is set anywhere in this codebase), so nothing extra was needed to make
// this work.
//
// A RECORD, NOT A CLASS: matches the project's existing DecisionAgentService
// pattern for small internal data carriers (ResourceScore/NewsScore are
// private records there) — no mutable state, no behavior, just three
// fields and generated accessors/equals/hashCode.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Constructed only by search/service/SearchService.
// - Returned wrapped in ApiResponse<List<SearchResult>> by
//   search/controller/SearchController — same envelope every other
//   endpoint in the app uses.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Grouped-by-type response shape: see WHY section above — explicitly
//   considered and rejected by the user in favor of a unified ranked list.
// - @JsonTypeInfo-annotated CivicContent for "proper" polymorphic
//   serialization: unnecessary complexity for a type that's never
//   deserialized on this side; the plain `type` string field already gives
//   clients everything they need to interpret `content` correctly.
// =============================================================================
