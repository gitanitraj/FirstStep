/*
 * WHAT: Complete IM-09 annotated mirror of backend/src/main/java/org/firststep/backend/news/model/NewsItem.java.
 * WHY: Preserve authored placement, canonical identity, truthful dates and private/public boundaries.
 * INTERACTION: Current inventory and Article approval remain independent of exact editorial
 * Expert/FAQ admissions; Category composition intersects Community, taxonomy and membership.
 * ALTERNATIVES: Source/session/verified inference and blanket Expert admission were rejected.
 * Tests cover approved admissions, denied sessions, preserved supporting views and safe links.
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
}
