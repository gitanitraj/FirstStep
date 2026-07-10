package org.firststep.backend.shared.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CivicContent {
    public String id;
    public String communityId;
    public String title;
    public String summary;
    public Boolean verified;
    public List<String> tags;
    public ContentSource contentSource;
    public String createdDate;
    public String updatedDate;
}
