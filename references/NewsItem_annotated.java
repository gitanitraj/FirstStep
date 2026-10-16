package org.firststep.backend.news.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// NewsItem represents a piece of civic news — a static editorial item or a
// live RSS-derived legislative update. This is the news slice's version,
// extending the shared CivicContent base instead of standing alone.
// =============================================================================

import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsItem extends CivicContent {

    public NewsItem() {
        this.contentType = ContentType.NEWS;
    }

    public String type;
    public String body;
    public String urgency;
    public String geography;
    public String author;

    @JsonProperty("why_it_matters")
    public String whyItMatters;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Moved from org.firststep.backend.model.NewsItem to
// org.firststep.backend.news.model.NewsItem, now extending CivicContent.
// What moved onto the shared base vs. what stayed:
//
// - id, verified, communityId, title, summary, tags, contentSource,
//   createdDate, updatedDate: inherited from CivicContent.
// - title ← headline: a direct rename — NewsItem had no separate concept
//   distinguishing "headline" from a generic "title" the way Resource had
//   organization vs. a synthesized title. Populated by JsonNewsRepository
//   (static items) and RssFeedService (RSS items) — see their annotated docs.
// - summary: v1's NewsItem.summary mapped 1:1 onto CivicContent.summary — no
//   code needed, JSON key name already matches.
// - tags ← categoryTags: these are classification/category tags, the
//   closest analogue to Resource.tags. resourceTags (cross-references to
//   specific Resource ids — a different, relational concept, not a category
//   classification) deliberately stays a NewsItem-specific field, NOT
//   folded into the shared tags — folding it in would silently lose the
//   distinction between "what category is this" and "which resources does
//   this relate to."
// - sourceName/sourceUrl (flat Strings in v1): removed. Superseded by the
//   inherited contentSource: ContentSource, built from those same values by
//   JsonNewsRepository/RssFeedService.
// - type, body, urgency, published, expires, geography, active, author,
//   whyItMatters, resourceTags: unchanged, news-specific — don't generalize
//   to Resource/Flyer/etc.
// =============================================================================

// =============================================================================
// SLICE F1 UPDATE (Decision 032) — FIVE FIELDS LEFT THIS CLASS
// =============================================================================
// NewsItem lost more to the contract than any other type, because it was the
// type that had accumulated the most parallel vocabulary:
//
//   published      -> CivicContent.publishDate
//   expires        -> CivicContent.expirationDate
//   active         -> CivicContent.status   ("active" / "inactive")
//   resourceTags   -> CivicContent.tags     (the DESCRIPTIVE half)
//   category_tags  -> CivicContent.categoryTags (the EDITORIAL half)
//
// news.json is UNCHANGED — it still says headline/published/expires/active/
// resource_tags. JsonNewsRepository maps those key names onto the contract at
// load (the Normalize stage). See CivicContent_annotated.java Section 4 for why
// the data file kept its own vocabulary.
//
// THE IMPORTANT ONE — the tags conflation is gone.
// -----------------------------------------------------------------------------
// The note in the section above ("tags <- categoryTags ... resourceTags
// deliberately stays a NewsItem-specific field") described the OLD arrangement,
// and that arrangement was the bug. It put news.json's EDITORIAL category_tags
// into the inherited `tags` field — the same field that holds DESCRIPTIVE
// metadata on Resource and Flyer. One field, two meanings, depending on which
// subclass you happened to be holding.
//
// The old reasoning was not unreasonable at the time: it read resourceTags as
// "cross-references to specific Resource ids", a relational concept genuinely
// distinct from classification. But the actual data was never that — it is
// free-text search vocabulary ("rental-assistance", "eviction", "low-income",
// "no-ID-required"), not resource ids. So the two fields were not
// "classification vs relations"; they were "editorial classification vs
// descriptive metadata", which is exactly the split the contract names.
//
// Corrected mapping, now that the concepts have the right names:
//
//     category_tags  = EDITORIAL   -> categoryTags -> drives navigation
//     resource_tags  = DESCRIPTIVE -> tags         -> drives search / AI
//
// This is also what closes Decision 031's open tech-debt item ("rename
// resource_tags -> tags in the CivicContent model") — it landed here as a
// consequence of the contract rather than as a standalone rename.
//
// CONTENT TYPE: the constructor sets NEWS, and RssFeedService overrides it to
// LAW on signed legislation. A Law is not a different class — same fields, same
// behavior, different presentation. See ContentType_annotated.java Section 1.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - JsonNewsRepository loads static items from app/data/news.json.
// - RssFeedService produces live items from the Delaware legislature RSS feed.
// - NewsService/NewsController expose them via GET /api/news and
//   /api/news/rss, wrapped in ApiResponse<T>.
// - DecisionAgentService reads title/summary/whyItMatters/tags/publishDate/
//   contentSource.name to score and cite news items in AI-generated guidance.
// - CategoryService reads categoryTags ONLY (never tags) to associate a news
//   item with a category.
// - UpdatesService carries categoryTags through to the Weekly Updates feed.
// - LegislationService sorts RSS items by publishDate for the Delaware Laws
//   rotator.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - (ORIGINAL, now SUPERSEDED) Folding resourceTags into the shared tags list
//   alongside categoryTags was rejected on the grounds that they answer
//   different questions. The instinct was right; the assignment was backwards.
//   The two fields DO answer different questions — but the shared `tags` field
//   is the DESCRIPTIVE one, so resource_tags is what belongs in it and
//   category_tags is what needed its own home. Slice F1 swapped them.
// - Keeping `published`/`expires`/`active` as NewsItem fields and ADDING the
//   contract's publishDate/expirationDate/status alongside them: rejected —
//   two fields meaning the same thing are guaranteed to drift, and every
//   consumer would have to guess which one is authoritative.
// =============================================================================
