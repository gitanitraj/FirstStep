package org.firststep.backend.shared.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentSource {
    public String id;
    public String name;
    public String type;
    public String url;
    public String retrieved;
}
