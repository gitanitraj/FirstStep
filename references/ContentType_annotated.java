package org.firststep.backend.shared.model;

// =============================================================================
// WHAT THIS ENUM DOES
// =============================================================================
// ContentType answers the CivicContent contract's first question: "What kind of
// content is this?" Five values cover every source First Step ingests today.
//
// The distinction it encodes is small to write and easy to get wrong:
//
//     CONTENT TYPE decides HOW an item is PRESENTED.
//     CATEGORY     decides WHERE an item APPEARS.
//
// They are orthogonal. A bill about eviction protections is contentType = LAW
// and category = Housing. It renders with the Law treatment (bill number,
// signing date, "what this means for you") wherever it appears, and it appears
// under Housing alongside shelters and rental-assistance programs.
// =============================================================================

public enum ContentType {
    RESOURCE,
    NEWS,
    FLYER,
    LAW,
    EXPERT
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// SECTION 1 — WHY LAW IS A CONTENT TYPE AND NOT A CATEGORY
// -----------------------------------------------------------------------------
// This was the live question. RSS legislation was already classifying itself
// with strings like "Delaware Legislation" — which reads like a category and
// was being written into the same field as real categories. Two ways out:
//
//   (a) Add a "Legislation" category to taxonomy.json.
//       Rejected. It answers a different question than the other ten. Housing,
//       Food, Legal describe WHAT HELP a resident needs; "Legislation"
//       describes WHAT FORMAT the information arrived in. Mixing the two in one
//       list means a resident browsing for housing help sees a category that
//       isn't a kind of help, and a housing bill lands in "Legislation" instead
//       of under Housing where someone looking for housing help would find it.
//
//   (b) Keep format as contentType; classify laws into the real taxonomy.
//       Chosen. A law about housing IS housing information. It belongs on the
//       Housing page. The LAW type is what preserves the dedicated News/Policy/
//       Law experience — distinct card, distinct detail view — without a second
//       editorial vocabulary competing with the first.
//
// The general principle, worth remembering beyond this enum:
//
//     If a proposed category answers "what format is this?" rather than "what
//     is this about?", it is a content type, not a category.
//
// SECTION 2 — WHY AN ENUM RATHER THAN A STRING
// -----------------------------------------------------------------------------
// The rest of the model layer uses Strings freely (status, urgency, type). An
// enum is justified here because the value set is genuinely CLOSED and small,
// and because every consumer branches on it — a card renderer picking a
// treatment, a type-indicator badge, a filter. Typos in a String would fail
// silently at render time; an enum fails at compile time.
//
// Jackson serializes enum constants by name, so the JSON reads "contentType":
// "LAW" and the frontend can switch on it directly.
//
// Note the contrast with `status` ("active"/"inactive"), which stayed a String:
// it has two values today, no consumer branches on it yet, and the lifecycle
// vocabulary is still likely to grow (draft? archived? superseded?). Enumerating
// a set that is still moving buys nothing and costs a migration later. Enums
// are for sets that have stopped arguing about their members.
//
// SECTION 3 — WHY EXPERT COVERS TWO CLASSES
// -----------------------------------------------------------------------------
// Both ExpertAnswer and FAQ report EXPERT. They are different Java classes with
// different fields (an ExpertAnswer has a named expert and credentials; a FAQ is
// the distilled, reusable form of one). But from the reader's point of view they
// are the same KIND of thing — an authoritative answer to a question — and get
// the same card treatment. Content type describes presentation, so it follows
// presentation, not the class hierarchy.
//
// This is the same reasoning as LAW, running the other direction: LAW is one
// type sharing a class with NEWS, EXPERT is one type spanning two classes.
// contentType is deliberately NOT a proxy for `getClass()`.
//
// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - CivicContent declares the field; each subclass constructor sets its own
//   default (Resource -> RESOURCE, NewsItem -> NEWS, Flyer -> FLYER,
//   ExpertAnswer/FAQ -> EXPERT).
// - RssFeedService.convertEntry() overrides it to LAW after constructing the
//   NewsItem. This is the ONLY place a value is set outside a constructor, and
//   it is the reason contentType is a field rather than an abstract method —
//   see CivicContent_annotated.java Section 2.
// - Slice F6's shared ContentCard component switches on this value to pick a
//   card treatment and render the type indicator.
//
// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - A `LawItem extends NewsItem` subclass so contentType could be an abstract
//   method. Rejected: a class whose only distinguishing feature is its own type
//   tag models nothing. See CivicContent_annotated.java Section 2.
// - Deriving the type from the class at serialization time (a Jackson
//   @JsonGetter returning getClass().getSimpleName()). Rejected for the same
//   reason both directions above break: LAW and NEWS share a class, EXPERT
//   spans two, so the class name is simply the wrong answer.
// - Reusing the existing NewsItem.type field ("legislation", "deadline",
//   "general-news"). That field is an EDITORIAL sub-kind within news, not the
//   content type, and it exists only on NewsItem — no help to Resources or
//   Flyers. Left alone.
// =============================================================================
