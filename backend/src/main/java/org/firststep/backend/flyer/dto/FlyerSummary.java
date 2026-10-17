package org.firststep.backend.flyer.dto;

import java.net.URI;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.shared.model.Location;

/** Resident Flyer facts; editorial status/audit/helpAccess and source retrieval remain private. */
public record FlyerSummary(ContentType contentType, String id, String communityId, String title,
        String summary, @JsonProperty("category_tags") List<String> categoryTags, String subcategory,
        List<String> tags, Source contentSource, String organization, @JsonProperty("event_date") String eventDate,
        String startsAt, String endsAt, Location location, String image, String imageUrl) {
    public record Source(String id, String name, String type, String url) {}
    public static FlyerSummary from(Flyer item, FlyerService service) {
        var source = item.contentSource;
        return new FlyerSummary(item.contentType, item.id, item.communityId, item.title, item.summary,
                item.categoryTags, item.subcategory, item.tags,
                source == null ? null : new Source(source.id, source.name, source.type, safeUrl(source.url)),
                item.organization, item.eventDate, item.startsAt, item.endsAt, item.location, item.image, service.imageUrlFor(item));
    }
    private static String safeUrl(String value) {
        if (value == null) return null;
        try {
            URI uri = URI.create(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null ? value : null;
        } catch (IllegalArgumentException e) { return null; }
    }
}
