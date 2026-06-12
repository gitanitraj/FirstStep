package org.firststep.backend.dto;

public class Citation {

    /**
     * Which dataset the citation came from.
     */
    public String sourceType; // "resource" | "news"

    /**
     * ID of the cited item.
     */
    public String id;

    /**
     * Short human-readable label to display.
     */
    public String label;
}

