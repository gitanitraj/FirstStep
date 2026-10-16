package org.firststep.backend.findhelp.dto;

/**
 * One door on Find Help's "What kind of help are you looking for?" row.
 *
 * <p><b>{@code destination} is a category page, not a filter.</b> Selecting a
 * gateway takes the resident OFF Find Help and into the category environment,
 * where every content form for that need is available — resources, flyers, FAQs,
 * expert answers, First Step articles and updates. Filtering in place would trap
 * them inside whichever table Find Help happened to query.
 *
 * <p><b>{@code available} is not the same question as {@code kind}.</b> A
 * category has a real destination today; the Seniors discovery pathway does not,
 * because it needs the Version 3 tag derivation. Rather than hide it — the data
 * would then be determining the information architecture — it renders visibly and
 * non-interactively, saying so.
 *
 * <p>The flag is computed rather than authored precisely so it stops being true
 * on its own: when {@code /discover/:facet} exists, discovery pathways become
 * available without anyone remembering to edit a file.
 */
public record CategoryGateway(
        String key,
        String label,
        String icon,
        /** Where this door leads, or null when it leads nowhere yet. */
        String destination,
        /** False for pathways whose destination is not built. Rendered, not hidden. */
        boolean available
) {
}
