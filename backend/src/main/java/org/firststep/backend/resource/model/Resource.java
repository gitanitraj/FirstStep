package org.firststep.backend.resource.model;

import java.util.List;

import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.Location;
import org.firststep.backend.shared.model.Phone;
import org.firststep.backend.shared.model.Website;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Resource extends CivicContent {
    public String category;
    public String subcategory;
    public String organization;
    @JsonProperty("parent_organization")
    public String parentOrganization;
    public String description;
    public String population;
    public String eligibility;
    @JsonProperty("eligibility_age_min")
    public Integer eligibilityAgeMin;
    @JsonProperty("eligibility_age_max")
    public Integer eligibilityAgeMax;
    @JsonProperty("eligibility_gender")
    public String eligibilityGender;
    public List<Location> locations;
    public List<Phone> phones;
    public List<Website> websites;
    public String county;
    @JsonProperty("access_mode")
    public List<String> accessMode;
    public String cost;
    public String urgency;
    public String notes;
}
