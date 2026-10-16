package org.firststep.backend.organization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An organization that provides services First Step lists.
 *
 * <p><b>Before this existed, an organization had no record at all.</b> Its
 * identity was a display string repeated on N resources, and its URL was
 * {@code slugify(that string)} — so renaming "Kingswood Community Center" would
 * have broken every link to it, silently and with nothing to detect it.
 *
 * <p>That is the same failure ContentSource had before Slice I, and this is the
 * same remedy (Decision 045's precedent): <b>a registry of stable IDs, with
 * content referencing the ID rather than the name.</b>
 *
 * <p><b>{@link #id} is identity; {@link #slug} is a URL.</b> The distinction is
 * the point:
 *
 * <ul>
 *   <li>{@code id} never changes. Resources reference it, and nothing else.</li>
 *   <li>{@code slug} is derived from the name and is what a resident sees and
 *       shares. If the organization is renamed, the slug changes with it.</li>
 * </ul>
 *
 * <p><b>A renamed organization's old links will 404.</b> Historical slugs,
 * aliases and redirects are deliberately NOT implemented here, and this class
 * claims no rename preservation it does not deliver. What the ID buys today is
 * that the RECORD survives a rename intact — every resource stays attached, and
 * no data is orphaned. Preserving old URLs is a separate capability.
 *
 * <p><b>What this record deliberately does NOT carry</b> is contact information
 * that belongs to a service rather than to the organization. See
 * {@link #website}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Organization {

    /** Canonical, stable identity — {@code ORG-001}. Referenced by resources. */
    public String id;

    /** The resident-facing name. Changing it changes {@link #slug}, never {@link #id}. */
    public String name;

    /** URL spelling of the name. Public-facing, and not an identity. */
    public String slug;

    /**
     * The organization's own website, or null.
     *
     * <p><b>Null is a real answer and the common one</b> — 38 of 55 organizations
     * have none recorded, and that is deliberate rather than missing data.
     *
     * <p>A value appears here only when a resource's website is explicitly
     * labelled {@code "Organization website"} — the data declaring what it is.
     * The other 35 website entries are labelled "Program page", "ReStore
     * program", "Find a shelter" and so on: those are SERVICE facts and stay on
     * the resource.
     *
     * <p><b>No phone is promoted at all.</b> The phone labels are "Main" (56),
     * "Secondary", "Wilmington", "24-hour hotline" — and "Main" means the main
     * line FOR THAT SERVICE. Picking one to present as the organization's
     * authoritative number would be manufacturing a fact the data does not
     * contain. Absence beats an arbitrary phone number on a page a resident in
     * crisis might call.
     */
    public String website;
}
