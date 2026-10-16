/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../category/dto/CollectionLayer.java
 * Category destination. See references/decisions.md Decision 051.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS ENUM IS
 *   Which resident JOB a collection serves. Three values, three questions:
 *   help me act (ACTION), help me understand (INFORMATION), and supporting
 *   exploration inside the information layer (SUPPORTING).
 *
 * WHY AN ENUM AND NOT A STRING
 *   Same reason ContentType, Sector and NoticeView are enums: a controlled
 *   vocabulary the compiler can check. A client switching on free strings has
 *   no way to be told it missed a case.
 *
 * WHY IT IS NOT A DOMAIN CONCEPT
 *   It answers "where does this sit in the destination's information
 *   architecture", never "what is this content". The second question already
 *   has an owner in `contentType`, and a layer never overrides it. Presentation
 *   may compose the model; it may not add to it.
 *
 * THE THING IT DELIBERATELY DOES NOT ENCODE
 *   Position. No column index, no order, no width. Pass 1 settled architecture
 *   and left spatial arrangement to Pass 2 — a DTO that named a column would
 *   have made the backend own the layout, and every later visual change would
 *   have become an API change.
 * ============================================================================= */

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
