package org.firststep.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsItem {

    public String id;
    public String type;
    public String headline;
    public String summary;
    public String body;
    public String urgency;
    public String published;
    public String expires;
    public String geography;
    public Boolean verified;
    public Boolean active;
    public String author;

    @JsonProperty("why_it_matters")
    public String whyItMatters;

    @JsonProperty("source_name")
    public String sourceName;

    @JsonProperty("source_url")
    public String sourceUrl;

    @JsonProperty("category_tags")
    public List<String> categoryTags;

    @JsonProperty("resource_tags")
    public List<String> resourceTags;
}