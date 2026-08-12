package org.firststep.backend.home.dto;

/**
 * One entry in the homepage's Community Resources column.
 *
 * <p><b>A pathway is a UX composition, not a domain entity</b> (Decision 041).
 * It says "residents can start here"; it does not say what the thing IS. That
 * distinction is carried by {@code kind}:
 *
 * <ul>
 *   <li>{@code category} — a canonical taxonomy category. {@code label} and
 *       {@code icon} are resolved from taxonomy.json, never authored here, so
 *       the vocabulary cannot drift between two files.</li>
 *   <li>{@code discovery} — a controlled query over existing CivicContent
 *       metadata. Seniors is the only one today. It carries its own label and
 *       icon <b>because it is not a category and must not become one</b>: it
 *       answers "who is this relevant to?", which the taxonomy never asked.</li>
 * </ul>
 *
 * <p>No href field. The backend says what a pathway IS and lets the client route
 * it, matching how CategorySummary is consumed — URL shape is a client concern,
 * and baking one in would make every future route change a backend change.
 */
public record ResourcePathway(
        String key,
        String label,
        String icon,
        String kind
) {

    public static final String CATEGORY = "category";
    public static final String DISCOVERY = "discovery";
}
