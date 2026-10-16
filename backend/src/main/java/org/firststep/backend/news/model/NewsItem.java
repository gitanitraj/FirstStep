package org.firststep.backend.news.model;

import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    public String type;
    public String body;
    public String urgency;
    public String geography;
    public String author;

    @JsonProperty("why_it_matters")
    public String whyItMatters;
}
