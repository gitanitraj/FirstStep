/*
 * WHAT: Complete annotated IM-10 mirror of backend/src/main/java/org/firststep/backend/news/model/NewsItem.java.
 * WHY: Explicit occurrence eligibility must not expire Flyer media or alter canonical identity.
 * INTERACTION: Events alone intersects the injected-clock assessment with current inventory,
 * controlled notice kind, producer sector and optional exact Community scope; DTOs allowlist facts.
 * ALTERNATIVES: Prose-derived end times, global Flyer cutoffs and a new Event entity/type were rejected.
 * Tests separately prove nanosecond/end-of-day/DST boundaries, supporting visibility and privacy.
 */
package org.firststep.backend.news.model;

import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

/**
 * Curated news and policy updates, and — with {@code contentType} switched to
 * {@link ContentType#LAW} by RssFeedService — signed Delaware legislation.
 *
 * <p>Four fields moved onto the CivicContent contract in Slice F1 and are gone
 * from here: {@code published} → {@code publishDate}, {@code expires} →
 * {@code expirationDate}, {@code active} → {@code status}, and
 * {@code resourceTags} → the inherited descriptive {@code tags}. JsonNewsRepository
 * maps news.json's original key names onto them at load; the data file is
 * unchanged.
 *
 * <p>The {@code category_tags} in news.json now bind straight to
 * {@code CivicContent.categoryTags}, which ends the conflation Decision 031
 * exposed — that file used to load editorial category_tags INTO the descriptive
 * {@code tags} field, so one field meant two different things depending on
 * content type.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsItem extends CivicContent {

    public NewsItem() {
        this.contentType = ContentType.NEWS;
    }

    /** Original stored Category evidence, captured before keyword normalization; not a catalog field or DTO. */
    @JsonIgnore
    public List<String> authoredCategoryTags;
    @JsonIgnore
    public String authoredSubcategory;

    public String type;
    public String body;
    public String urgency;
    public String geography;
    public String author;

    @JsonProperty("why_it_matters")
    public String whyItMatters;
    /** Optional explicit occurrence instants (ISO offset timestamp); independent of editorial expiry. */
    public String startsAt;
    public String endsAt;
}
