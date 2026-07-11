package org.firststep.backend.flyer.model;

import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.Location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Flyer extends CivicContent {
    public String organization;
    @JsonProperty("event_date")
    public String eventDate;
    public Location location;
    public String image;
}
