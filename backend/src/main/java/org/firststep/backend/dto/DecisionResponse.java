package org.firststep.backend.dto;

import java.util.List;

import org.firststep.backend.shared.model.Citation;

public class DecisionResponse {

    public String answerTitle;
    public List<DecisionStep> steps;

    /**
     * Citations that reference which local items were used.
     */
    public List<Citation> citations;

    /**
     * If the system couldn't find relevant matches, this explains why.
     */
    public String notes;
}

