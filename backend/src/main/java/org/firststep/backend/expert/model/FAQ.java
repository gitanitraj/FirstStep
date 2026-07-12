package org.firststep.backend.expert.model;

import org.firststep.backend.shared.model.CivicContent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FAQ extends CivicContent {
    public String question;
    public String answer;
    @JsonProperty("source_expert_answer_id")
    public String sourceExpertAnswerId;
}
