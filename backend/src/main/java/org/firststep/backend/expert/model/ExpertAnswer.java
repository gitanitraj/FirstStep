package org.firststep.backend.expert.model;

import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.Contact;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExpertAnswer extends CivicContent {
    public String question;
    public String answer;
    @JsonProperty("expert_name")
    public String expertName;
    @JsonProperty("expert_credentials")
    public String expertCredentials;
    @JsonProperty("expert_organization")
    public String expertOrganization;
    @JsonProperty("expert_contact")
    public Contact expertContact;
    @JsonProperty("session_date")
    public String sessionDate;
}
