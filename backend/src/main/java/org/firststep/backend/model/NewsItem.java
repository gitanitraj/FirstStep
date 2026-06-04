package org.firststep.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsItem {

    public String id;
    public String headline;
    public String summary;

    @JsonProperty("why_it_matters")
    public String whyItMatters;

    public String urgency;
    public String published;

    @JsonProperty("source_name")
    public String sourceName;

    @JsonProperty("source_url")
    public String sourceUrl;

    public Boolean active;
}