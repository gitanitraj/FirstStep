package org.firststep.backend.shared.model;

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

    /**
     * Provenance of the cited item, resolved after retrieval (see
     * DecisionAgentService.resolveCitationSources). Null if the model cited
     * an id that didn't match any retrieved item.
     */
    public ContentSource contentSource;
}
