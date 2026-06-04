package org.firststep.backend.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Resource {
    public String id;
    public String category;
    public String subcategory;
    public String organization;
    @JsonProperty("parent_organization")
    public String parentOrganization;
    public String summary;
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
    public List<String> tags;
    public String source;
    public String retrieved;
    public Boolean verified;
    public String notes;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        public String label;
        public String address;
        public String city;
        public String state;
        public String zip;
        public Boolean confidential;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Phone {
        public String number;
        public String label;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Website {
        public String url;
        public String label;
    }
}

