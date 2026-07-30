package org.firststep.backend.shared.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The CivicContent contract — the foundation of Version 2.
 *
 * <p>Every civic information item, whatever its source or shape (Resource, News,
 * Flyer, Law, Expert content, and whatever comes next), answers the SAME six
 * questions with the SAME fields. That uniformity is what lets search,
 * navigation, AI guidance, category pages and the future mobile app treat all
 * civic information consistently instead of special-casing each type.
 *
 * <pre>
 *   What kind of content is this?  contentType
 *   What is it about?              categoryTags + subcategory   (EDITORIAL)
 *   How can it be found?           tags                         (DESCRIPTIVE)
 *   Where did it come from?        contentSource
 *   Who is it for?                 communityId
 *   When is it relevant?           publishDate, expirationDate, status
 * </pre>
 *
 * <p><b>The classification split is the load-bearing rule.</b>
 * {@code categoryTags} and {@code subcategory} are EDITORIAL classification and
 * are the only things that drive navigation — which category page an item lands
 * on, which topic it appears under. {@code tags} are DESCRIPTIVE metadata for
 * search, filtering, AI enrichment and related-content discovery. <b>Tags must
 * never determine navigation.</b> Cross-category relationships ("this flyer
 * relates to that law") are an enrichment product computed in the pipeline and
 * persisted as metadata — never a second editorial taxonomy layered on tags.
 *
 * <p>Both classification fields draw their vocabulary from ONE place:
 * {@code app/data/taxonomy.json}, loaded by
 * {@link org.firststep.backend.category.service.TaxonomyService}. No service
 * carries its own hardcoded copy of the category vocabulary.
 *
 * <p><b>Why these are fields and not accessors.</b> The whole model uses public
 * fields (Jackson binds them directly, no boilerplate), so the contract follows
 * suit rather than introducing a getter convention only this class would use.
 * {@code contentType} in particular was considered as an abstract method
 * ({@code public abstract ContentType getContentType()}) so a subclass could not
 * forget it — rejected because RSS legislation is a NewsItem that must
 * report {@link ContentType#LAW}, so the value has to vary per instance, not per
 * class. Each subclass constructor sets its own default instead; see
 * references/decisions.md Decision 032.
 *
 * <p>Source JSON is normalized onto this contract at load time by each
 * repository — e.g. news.json's {@code published}/{@code expires}/{@code active}
 * become {@code publishDate}/{@code expirationDate}/{@code status}, and its
 * {@code resource_tags} become the descriptive {@code tags}. The raw files keep
 * their own vocabulary; mapping them is the repository's (Normalize stage) job.
 */
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
    /**
     * Canonical category labels from taxonomy.json (e.g. "Housing", "Legal").
     * A list because one item can legitimately belong to more than one category:
     * an eviction-rights session is both Housing and Legal.
     */
    @JsonProperty("category_tags")
    public List<String> categoryTags;

    /**
     * Canonical subcategory (topic) from taxonomy.json — the level below
     * category. Singular by design: an item has one primary topic even when it
     * carries several categoryTags. Where a topic exists under several
     * categories (e.g. "Eviction Prevention" under both Housing and Legal), one
     * value correctly places the item under both.
     */
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
