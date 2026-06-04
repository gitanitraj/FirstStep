package org.firststep.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsItem {

    public String id;
    public String headline;
    public String summary;
    public String why_it_matters;
    public String urgency;
    public String published;
    public String source_name;
    public String source_url;
}