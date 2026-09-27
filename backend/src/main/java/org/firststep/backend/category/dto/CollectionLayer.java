package org.firststep.backend.category.dto;

/**
 * Which resident job a {@link CategoryCollection} serves on the category
 * destination (Decision 051).
 *
 * <p>Three layers, three questions:
 *
 * <pre>
 *   ACTION       help me act        — programs, services, actionable opportunities
 *   INFORMATION  help me understand — the WHAT TO KNOW collections
 *   SUPPORTING   supporting exploration inside WHAT TO KNOW
 * </pre>
 *
 * <p><b>This is presentation structure, not a domain concept.</b> It says where a
 * collection sits in the destination's information architecture; it says nothing
 * about what the content <i>is</i>. That question is already answered by
 * {@code contentType}, and a layer never overrides it.
 *
 * <p><b>SUPPORTING is not a fourth primary collection.</b> FAQs and expert Q&amp;As
 * support the information layer rather than standing beside Policy Info, Law,
 * Flyers and First Rate News as peers — which is why they are distinguished here
 * rather than by being left out.
 *
 * <p>Spatial arrangement is deliberately NOT expressed. Whether a layer renders as
 * columns, stacked sections or something responsive is Pass 2 visual design; a DTO
 * that encoded position would make the backend own the layout.
 */
public enum CollectionLayer {

    /** "Help me act." Programs, services and actionable opportunities. */
    ACTION,

    /** "Help me understand." The primary WHAT TO KNOW collections. */
    INFORMATION,

    /** Supporting exploration within WHAT TO KNOW. Never a primary collection. */
    SUPPORTING
}
