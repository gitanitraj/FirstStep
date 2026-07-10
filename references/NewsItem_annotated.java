package org.firststep.backend.news.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// NewsItem represents a piece of civic news — a static editorial item or a
// live RSS-derived legislative update. This is the news slice's version,
// extending the shared CivicContent base instead of standing alone.
// =============================================================================

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

import org.firststep.backend.shared.model.CivicContent;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsItem extends CivicContent {

    public String type;
    public String body;
    public String urgency;
    public String published;
    public String expires;
    public String geography;
    public Boolean active;
    public String author;

    @JsonProperty("why_it_matters")
    public String whyItMatters;

    @JsonProperty("resource_tags")
    public List<String> resourceTags;
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
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - JsonNewsRepository loads static items from app/data/news.json.
// - RssFeedService produces live items from the Delaware legislature RSS feed.
// - NewsService/NewsController expose them via GET /api/news and
//   /api/news/rss, wrapped in ApiResponse<T>.
// - DecisionAgentService reads title/summary/whyItMatters/tags/published/
//   contentSource.name (all still valid — either inherited or unchanged) to
//   score and cite news items in AI-generated guidance.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Folding resourceTags into the shared tags list alongside categoryTags:
//   rejected — they answer different questions ("what category" vs "which
//   resources"), and DecisionAgentService/future consumers would lose the
//   ability to distinguish them if merged.
// =============================================================================
