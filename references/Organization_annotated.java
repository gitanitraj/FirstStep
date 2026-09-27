/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../organization/model/Organization.java
 * Slice G1a. See references/decisions.md Decision 049.
 * =============================================================================
 *
 * WHAT IT IS: the first record an organization has ever had in this system.
 *
 * WHAT EXISTED BEFORE: nothing. An organization was a display STRING repeated on
 * N resources, and its URL was slugify(that string). Renaming "Kingswood
 * Community Center" would have broken every link to it — silently, with no test,
 * validator or log line able to notice.
 *
 * THE PRECEDENT THIS FOLLOWS
 * --------------------------
 * ContentSource had exactly this problem before Slice I, and Decision 045 fixed
 * it the same way: a registry of stable IDs, with content referencing the ID
 * rather than the name. Recognising the repeat is most of the value here — the
 * second instance of a problem is where the pattern becomes worth naming.
 *
 * id IS IDENTITY, slug IS A URL
 * -----------------------------
 * The distinction carries the whole design:
 *
 *   id     never changes. Resources reference it and nothing else.
 *   slug   derived from the name, public, shareable, and CHANGES ON RENAME.
 *
 * WHAT THIS HONESTLY DOES NOT DO
 * ------------------------------
 * A renamed organization's old links 404. There are no historical slugs, no
 * aliases, no redirects, and this class deliberately claims none.
 *
 * That limit is worth stating precisely, because "stable ID" invites the
 * assumption that URLs are stable too. What the ID buys TODAY is that the RECORD
 * survives a rename: every resource stays attached, nothing is orphaned, and the
 * organization does not silently split into two. Preserving old URLs is a
 * separate capability, and implying it while not shipping it would be worse than
 * the 404 — it would be a promise the data cannot keep.
 *
 * WHY THERE IS NO PHONE FIELD
 * ---------------------------
 * This is the most deliberate absence in the class, and it was measured rather
 * than assumed. The source data's phone labels are:
 *
 *     "Main" (56) · "Secondary" (2) · "Wilmington" · "Middletown"
 *     · "24-hour hotline"
 *
 * "Main" means the main line FOR THAT SERVICE. Each resource is a service, so
 * every phone in the dataset is service-scoped. For a single-service
 * organization that number LOOKS like the organization's — and promoting it
 * would be inference presented as a fact.
 *
 * A RESIDENT IN CRISIS CALLING A NUMBER FIRST STEP GUESSED AT is the failure
 * this avoids. Absence is the honest alternative: the service card below carries
 * the number that is actually true. A test asserts this class never grows a
 * phone field.
 *
 * WHY website IS DIFFERENT
 * ------------------------
 * 17 website entries are explicitly labelled "Organization website" — the data
 * declaring what the value IS, not this code inferring it. The other 38 are
 * "Program page", "ReStore program", "Find a shelter" and similar: service
 * facts, left on the service.
 *
 * So 17 organizations carry a website and 38 carry null, and NULL IS A REAL
 * ANSWER rather than missing data. The rule is not "promote when unambiguous" —
 * it is "promote only what the source declares to be organization-level."
 * ============================================================================= */

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
