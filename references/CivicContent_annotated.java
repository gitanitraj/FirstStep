package org.firststep.backend.shared.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// CivicContent is the abstract shared base for EVERY knowledge-object type in
// First Step: Resource, NewsItem, Flyer, FAQ and ExpertAnswer all extend it,
// and RSS legislation is a NewsItem presenting as a Law.
//
// As of Slice F1 (Decision 032) this class is no longer "the fields these types
// happen to share" — it is a CONTRACT. Version 2 rests on a single rule:
//
//     Every CivicContent item, whatever its type or source, answers the SAME
//     six questions with the SAME fields.
//
//         What kind of content is this?  contentType
//         What is it about?              categoryTags + subcategory  (EDITORIAL)
//         How can it be found?           tags                        (DESCRIPTIVE)
//         Where did it come from?        contentSource
//         Who is it for?                 communityId
//         When is it relevant?           publishDate, expirationDate, status
//
// That uniformity is the whole point. Search, navigation, AI guidance, category
// pages and the future mobile app can each be written ONCE against this
// contract instead of carrying a branch per content type.
// =============================================================================

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CivicContent {

    // ---- Identity -------------------------------------------------------
    public String id;
    public String title;
    public String summary;
    public Boolean verified;

    /** What kind of content this is. Set by each subclass; LAW is set on RSS items. */
    public ContentType contentType;

    // ---- What is it about? (EDITORIAL classification — drives navigation) -
    @JsonProperty("category_tags")
    public List<String> categoryTags;

    public String subcategory;

    // ---- How can it be found? (DESCRIPTIVE metadata — never navigation) --
    public List<String> tags;

    // ---- Where did it come from? ----------------------------------------
    public ContentSource contentSource;

    // ---- Who is it for? -------------------------------------------------
    public String communityId;

    // ---- When is it relevant? -------------------------------------------
    public String publishDate;
    public String expirationDate;
    /** Lifecycle state, e.g. "active" / "inactive". */
    public String status;

    // ---- Audit ----------------------------------------------------------
    public String createdDate;
    public String updatedDate;
}

// =============================================================================
// SECTION 1 — THE EDITORIAL / DESCRIPTIVE SPLIT (the load-bearing rule)
// =============================================================================
// Two fields look similar and are emphatically not interchangeable:
//
//   categoryTags + subcategory   EDITORIAL classification.
//                                The ONLY thing that drives navigation. Which
//                                category page an item lands on, which topic it
//                                appears under. Authored deliberately, drawn
//                                from taxonomy.json.
//
//   tags                         DESCRIPTIVE metadata.
//                                Search, filtering, AI enrichment, related-
//                                content discovery. Free-form. NEVER decides
//                                placement in the hierarchy.
//
// WHY THIS MATTERS — the bug it prevents. Before F1, JsonNewsRepository loaded
// news.json's `category_tags` INTO the inherited `tags` field:
//
//     item.tags = node.get("category_tags")           // <- the conflation
//
// So `tags` meant "editorial classification" for a NewsItem and "descriptive
// metadata" for a Resource or Flyer. The same field, two meanings, depending on
// what you happened to be holding. Every consumer then had to know which type
// it had before it could interpret the field — precisely the special-casing the
// contract exists to abolish. Decision 031 spotted the symptom (news items
// unreachable from their categories); F1 removed the cause by giving the two
// concepts two fields.
//
// The same rule is why cross-category relationships ("this flyer relates to
// that law") are NOT modeled by matching tags at request time. Relationships
// are an ENRICHMENT product: computed in the pipeline from canonical
// categories, subcategories, tags and semantic similarity, then persisted as
// metadata so they stay deterministic and cheap to serve. Tags contribute
// evidence to that computation; they never become a second navigation
// taxonomy.
//
// =============================================================================
// SECTION 2 — WHY contentType IS A FIELD, NOT AN ABSTRACT METHOD
// =============================================================================
// The obvious OO move is polymorphism:
//
//     public abstract ContentType getContentType();       // REJECTED
//
// It is genuinely attractive: a subclass CANNOT forget to answer, the compiler
// enforces it, and Jackson would serialize the getter just fine. It was
// rejected for one concrete reason:
//
//     Signed Delaware legislation is a NewsItem that must report LAW.
//
// RssFeedService builds NewsItem instances and sets contentType = LAW on them.
// With an abstract method, NewsItem.getContentType() would have to return NEWS
// for every instance of that class — so representing a Law would require a
// LawItem subclass whose only difference is its return value. That is a class
// invented to satisfy a mechanism, not to model anything real: a Law and a News
// item share every field and every behavior.
//
// So the value varies PER INSTANCE, not per class, and a field is the honest
// representation. Each subclass constructor sets its own default:
//
//     public Resource()  { this.contentType = ContentType.RESOURCE; }
//     public NewsItem()  { this.contentType = ContentType.NEWS; }
//     public Flyer()     { this.contentType = ContentType.FLYER; }
//
// ...and RssFeedService overrides it to LAW after construction. The cost is
// that a future subclass could forget to set it (null rather than a compile
// error); CivicContentTest covers all five types to catch that.
//
// LESSON: "use polymorphism" is not automatically right. Polymorphism binds
// behavior to a CLASS. When the varying thing is a property of an INSTANCE, a
// field models it and a subclass hierarchy distorts it.
//
// =============================================================================
// SECTION 3 — WHY PUBLIC FIELDS AND NOT ACCESSORS
// =============================================================================
// Unchanged from the original decision, restated because the contract makes it
// more visible: the whole model layer uses public fields. Jackson binds them
// directly, there is no validation or derivation to protect, and these classes
// are data carriers rather than behavior-bearing objects. Introducing a
// getter/setter convention for this one class would make it inconsistent with
// its own subclasses. Encapsulation earns its keep when there is an invariant
// to defend; here there is none.
//
// =============================================================================
// SECTION 4 — THE NORMALIZE SEAM (why the JSON files were not renamed)
// =============================================================================
// news.json still says `published`, `expires`, `active`, `resource_tags`.
// flyers.json says `event_date`. resources.json says `category`. The contract
// says publishDate / expirationDate / status / tags. Those are NOT the same
// names, on purpose.
//
// Source files keep their own historical vocabulary; mapping them onto the
// canonical contract is the REPOSITORY's job — the Normalize stage of the
// information pipeline (pipeline/normalize/Normalizer). JsonNewsRepository does
// exactly this in applyContentSourceAndDefaults().
//
// Renaming the data files as well was considered and rejected for this slice:
// it doubles the blast radius (validators, fixtures, every hand-authored
// record) for no architectural gain, since the model is what every consumer
// sees. The contract is about the OBJECT, not about the bytes on disk.
//
// One field deliberately did NOT get folded in: Flyer.eventDate. "When the
// event happens" and "when the content stops being relevant" are different
// questions, and mapping eventDate onto expirationDate would silently expire
// flyers the Community Information carousel still wants to show.
//
// =============================================================================
// SECTION 5 — WHERE THE VOCABULARY COMES FROM
// =============================================================================
// Both editorial fields draw on ONE vocabulary: app/data/taxonomy.json, loaded
// at startup by category/service/TaxonomyService. No service holds a hardcoded
// copy any more — the old CategoryDefinition.ALL constant was deleted in the
// same slice. See TaxonomyService_annotated.java for why that file is loaded in
// a constructor rather than on ApplicationReadyEvent.
//
// The architectural principle this enforces, stated once:
//
//     EVERY CivicContent source — Resources, News, RSS/Laws, Flyers and Expert
//     content — classifies into the SAME canonical taxonomy. Upstream label
//     drift is normalized AT THE SOURCE, never absorbed by widening the
//     taxonomy's match lists downstream.
//
// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Resource, NewsItem, Flyer, FAQ, ExpertAnswer extend this class and set
//   contentType in their constructors.
// - JsonNewsRepository / JsonFlyerRepository / JsonResourceRepository normalize
//   their source files onto these fields at load, and default communityId from
//   the app.default-community-id property when the source JSON has none.
// - RssFeedService sets contentType = LAW and populates categoryTags from its
//   keyword classifier (still emitting non-canonical values as of F1 —
//   normalizing that classifier is Slice F2).
// - CategoryService reads categoryTags ONLY, via TaxonomyService.
// - UpdatesService carries categoryTags through to the Weekly Updates feed.
// - SearchService and the AI assistant read `tags` (descriptive) — the other
//   half of the split, doing the job tags are actually for.
//
// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Composition (a `CivicContentAttributes` field embedded in each subtype
//   instead of inheritance): would avoid Java's single-inheritance constraint,
//   but every accessor becomes two hops (resource.attributes.title) for no
//   present benefit — no subtype needs to swap out its base at runtime.
// - Interfaces with default methods, one per question (Classifiable,
//   Searchable, Datable): more flexible in principle, but Java interfaces hold
//   no instance fields, so every implementing class would still declare all the
//   fields itself. And since all five types answer all six questions, the
//   interfaces would always be implemented together — ceremony without
//   variation.
// - A `Classification` value object holding categoryTags + subcategory
//   together. Cleaner grouping, and it would make "an item is classified" a
//   single nullable thing. Rejected as premature: it buys nothing until
//   something needs to pass classification around independently of the item,
//   and it complicates the flat JSON binding every repository relies on.
// - Leaving type-specific fields alone and ADDING contract fields alongside
//   them (published AND publishDate both present). Smallest diff, but it leaves
//   two fields meaning the same thing and guarantees they drift.
// =============================================================================
