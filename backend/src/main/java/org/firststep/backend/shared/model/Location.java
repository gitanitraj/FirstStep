package org.firststep.backend.shared.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Location {
    public String label;
    public String address;
    public String city;
    public String state;
    public String zip;
    public Boolean confidential;
}
