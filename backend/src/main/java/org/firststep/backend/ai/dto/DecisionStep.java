package org.firststep.backend.ai.dto;

public class DecisionStep {
    public int order;
    public String title;
    public String action;

    /**
     * Optional explanation to help residents understand why.
     */
    public String why;
}
