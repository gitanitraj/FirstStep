/*
 * WHAT: Complete IM-09 annotated mirror of backend/src/main/java/org/firststep/backend/news/dto/NewsSummary.java.
 * WHY: Preserve authored placement, canonical identity, truthful dates and private/public boundaries.
 * INTERACTION: Current inventory and Article approval remain independent of exact editorial
 * Expert/FAQ admissions; Category composition intersects Community, taxonomy and membership.
 * ALTERNATIVES: Source/session/verified inference and blanket Expert admission were rejected.
 * Tests cover approved admissions, denied sessions, preserved supporting views and safe links.
 */
package org.firststep.backend.news.dto;

import java.net.URI;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.shared.model.ContentType;

/** Resident news/feed allowlist: preserve source/publication facts, omit internal audit/status fields. */
public record NewsSummary(ContentType contentType, String id, String communityId, String title,
        String summary, String body, Source contentSource, String publishDate, String type,
        String urgency, List<String> tags, @JsonProperty("why_it_matters") String whyItMatters,
        @JsonProperty("category_tags") List<String> categoryTags, String subcategory) {
    public record Source(String id, String name, String type, String url) {}
    public static NewsSummary from(NewsItem item) {
        var source = item.contentSource;
        // Broad feed membership stays intact; inferred classification is not public authored placement.
        var categories = item.contentType == ContentType.LAW ? item.authoredCategoryTags : item.categoryTags;
        String topic = item.contentType == ContentType.LAW ? item.authoredSubcategory : item.subcategory;
        return new NewsSummary(item.contentType, item.id, item.communityId, item.title, item.summary,
                item.body, source == null ? null : new Source(source.id, source.name, source.type, safeUrl(source.url)),
                item.publishDate, item.type, item.urgency, item.tags == null ? null : List.copyOf(item.tags),
                item.whyItMatters, categories == null ? null : List.copyOf(categories), topic);
    }
    private static String safeUrl(String value) {
        if (value == null) return null;
        try {
            var scheme = URI.create(value).getScheme();
            return scheme != null && Set.of("http", "https").contains(scheme.toLowerCase(java.util.Locale.ROOT)) ? value : null;
        } catch (IllegalArgumentException ignored) { return null; }
    }
}
