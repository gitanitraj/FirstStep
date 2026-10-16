package org.firststep.backend.shared.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Media {
    public String id;
    public String type;
    public String url;
    public String filename;
    public String mimeType;
}
