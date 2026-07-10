package org.firststep.backend.shared.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Contact {
    public List<Phone> phones;
    public List<Website> websites;
    public String email;
}
