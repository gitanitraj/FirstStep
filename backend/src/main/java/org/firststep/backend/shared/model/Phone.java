package org.firststep.backend.shared.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Phone {
    public String number;
    public String label;
}
