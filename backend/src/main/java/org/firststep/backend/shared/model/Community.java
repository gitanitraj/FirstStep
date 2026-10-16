package org.firststep.backend.shared.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Community {
    public String id;
    public String name;
    public String city;
    public String state;
    public List<String> zipCodes;
    public Boolean active;
}
