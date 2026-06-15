package org.firststep.backend.dto;

import java.util.List;

public class DecisionRequest {

    /**
     * Free-text question from the user.
     */
    public String userQuery;

    /**
     * Optional urgency filter for narrowing relevant resources.
     * Values expected: "urgent" or null/empty.
     */
    public Boolean urgent;

    /**
     * Optional categories to bias retrieval.
     * Example: ["housing", "essentials"]
     */
    public List<String> preferredCategories;
}

