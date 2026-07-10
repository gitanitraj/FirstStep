package org.firststep.backend.news.model;

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
