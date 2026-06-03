package org.firststep.backend.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Resource {
  public String id;
  public String category;
  public String subcategory;
  public String organization;
  public String parent_organization;
  public String summary;
  public String description;
  public String population;
  public String eligibility;
  public Integer eligibility_age_min;
  public Integer eligibility_age_max;
  public String eligibility_gender;
  public List<Location> locations;
  public List<Phone> phones;
  public List<Website> websites;
  public String county;
  public List<String> access_mode;
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

