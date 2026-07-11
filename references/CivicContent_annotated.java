package org.firststep.backend.shared.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// CivicContent is the abstract shared base for every knowledge-object type in
// First Step: Resource and NewsItem extend it today; Flyer, FAQ, and
// ExpertAnswer will extend it once those slices are built. It holds the
// fields every knowledge object has regardless of type: identity,
// which Community it belongs to, a display title/summary, verification
// status, category tags, where it came from (ContentSource), and when it
// was created/updated.
// =============================================================================

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CivicContent {
    public String id;
    public String communityId;
    public String title;
    public String summary;
    public Boolean verified;
    public List<String> tags;
    public ContentSource contentSource;
    public String createdDate;
    public String updatedDate;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// docs/architecture/01-domain-model.md originally left this open: the shared
// characteristics across Resource/NewsItem/Flyer/FAQ/ExpertAnswer were
// described as a *conceptual* "KnowledgeObject", explicitly not committing to
// inheritance, composition, or interfaces. The project's own domain-model UML
// (docs/architecture/uml/domain-model-uml.md) resolved this by drawing
// CivicContent as an <<abstract>> class with Resource/NewsItem/Flyer/FAQ/
// ExpertAnswer as subtypes — this class is that decision made concrete.
//
// Field selection follows the UML literally: id, title, summary, verified,
// tags, contentSource, createdDate, updatedDate. communityId is not shown as
// a boxed field in the UML (Community is drawn as a relationship arrow into
// CivicContent instead), but the Java class still needs a concrete way to
// carry that relationship under JSON-file storage — a plain String foreign
// key, not an embedded Community object, matching how Resource/NewsItem
// already reference other things by flat id (e.g. Citation.id) rather than
// by nested object graphs.
//
// tags is a single shared List<String>, not left per-subtype. This does cost
// something: NewsItem previously had two distinct tag lists (categoryTags
// and resourceTags) with different meanings. Only categoryTags maps onto
// this shared field — resourceTags stays a NewsItem-specific field because
// it's a different concept (cross-references to specific Resource ids, not
// a category classification). See NewsItem_annotated.java for that mapping.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Resource and NewsItem extend CivicContent (see resource/model/Resource.java,
//   news/model/NewsItem.java) and add their own type-specific fields.
// - contentSource is populated by each slice's repository at load time from
//   whatever flat provenance fields existed in v1 (Resource.source/retrieved,
//   NewsItem.sourceName/sourceUrl) — see JsonResourceRepository/JsonNewsRepository.
// - communityId is defaulted by each repository from the
//   app.default-community-id property when the source JSON has none (true for
//   all current data, since no Community concept existed before this pass).
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Composition (a `CivicContentAttributes` field embedded in Resource/NewsItem
//   instead of inheritance): would avoid Java single-inheritance constraints,
//   but every accessor becomes two hops (resource.attributes.title instead of
//   resource.title) for no present benefit — no subtype needs to swap out
//   its base at runtime. Rejected in favor of the simpler direct-inheritance
//   shape the UML already committed to.
// - Interfaces with default methods: would allow multiple inheritance later,
//   but Java interfaces can't hold instance fields, so every implementing
//   class would still need to declare all nine fields itself — no
//   deduplication benefit for a project with flat POJOs and no shared
//   behavior beyond field storage.
// =============================================================================
