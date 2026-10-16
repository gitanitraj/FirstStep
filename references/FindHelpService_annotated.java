/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../findhelp/service/FindHelpService.java
 * Slice G1b. See references/decisions.md Decision 050.
 * =============================================================================
 *
 * WHAT IT ASSEMBLES: Find Help — the page that answers "where should I start?"
 *
 * THE DISTINCTION THE WHOLE SLICE PROTECTS
 * ----------------------------------------
 *     Find Help          where should I start?
 *     Category page      what is available for this need?
 *     Organization page  who can I contact?
 *
 * Three destinations, three questions. Find Help HANDS OFF and never filters in
 * place — that is the design, not an implementation detail.
 *
 * An earlier scope proposed filtering the directory in place when a category was
 * chosen. Rejected, and the reason is the point of the whole information
 * architecture: filtering would have kept the resident inside the ORGANIZATION
 * table, when a category page carries resources, flyers, FAQs, expert answers,
 * First Step articles and updates. Filtering in place would have trapped them
 * inside whichever table Find Help happened to query first.
 *
 * WHY GATEWAYS COME FROM THE AUTHORED LIST, NOT FROM COUNTS
 * ---------------------------------------------------------
 * The tempting implementation is to show a door for each category that HAS
 * organizations. It is wrong twice over:
 *
 *   1. It would let a thin registry silently shrink the site's navigation. The
 *      data should populate the experience; it must not determine the
 *      information architecture.
 *   2. It would be measuring the wrong thing anyway. A category page carries six
 *      content forms, and organizations are one of them — a category with zero
 *      organizations can be rich in flyers, FAQs and updates.
 *
 * That second point was learned the hard way: an earlier measurement counted
 * organizations from resources.json alone, concluded "only 3 of 10 categories
 * have organizations", and nearly removed Employment, Health and Legal from the
 * page. See Decision 049's addendum.
 *
 * ONE AUTHORED LIST, SHARED WITH THE HOMEPAGE
 * -------------------------------------------
 * gateways() reuses PathwayService.getCommunityResources() — the same list the
 * homepage's Community Resources row renders. Both pages ask the same question
 * ("which needs are the ways in?"), so a second list would only be a copy that
 * drifts. Labels and icons resolve from the taxonomy, so renaming a category
 * renames it in both places.
 *
 * `available` IS COMPUTED, NOT AUTHORED
 * -------------------------------------
 * A category has a destination; the Seniors DISCOVERY pathway does not, until
 * Version 3's tag derivation exists. Deriving the flag from `kind` rather than
 * storing it means the door turns on by itself when /discover/:facet ships —
 * nobody has to remember to edit a file, and the flag cannot go stale.
 *
 * Note it is NOT the same question as `kind`, and the DTO says so: one describes
 * what a pathway IS, the other whether it currently leads anywhere.
 *
 * openToEveryone: THE LABEL IS THE DISCLOSURE
 * -------------------------------------------
 * "Popular ways to get help" and "Recently added" were both proposed and both
 * rejected on evidence: First Step collects NO usage data, so popularity would
 * be an invented signal of exactly the kind Decision 049 refuses; and resources
 * carry NO date field at all, so recency is not expressible.
 *
 * What survives is a rule that can be stated in its own heading — no age bound,
 * no gender restriction. Because the label names the criterion, the selection
 * discloses itself and implies nothing about quality or endorsement. That is a
 * reusable principle: A NEUTRAL SELECTION IS ONE WHOSE HEADING STATES ITS RULE.
 *
 * It is also the most useful set for this page's reader — someone who does not
 * know whether they qualify and does not want to be turned away.
 *
 * DIRECTORY: ALPHABETICAL HERE, AND ONLY HERE
 * -------------------------------------------
 * Sorting by name is right for the "I already know who I am looking for" path,
 * where predictability is the whole value. It would have been wrong as the
 * page's primary structure — that is the alphabetical database this page was
 * explicitly scoped against — which is why it sits last.
 *
 * Organizations with NO services still appear: eight are known only through a
 * flyer or an expert answer, and they are no less contactable for it.
 *
 * PHONE ATTRIBUTION — Decision 049 meeting the page that tests it
 * ---------------------------------------------------------------
 * Every phone in the corpus is service-scoped ("Main" = the main line FOR THAT
 * SERVICE). A directory whose job is "who do I call" is exactly where the
 * temptation lives to hoist one up beside the organization name. servicesOf()
 * keeps each number on its own ServiceLine, so the page answers the question
 * without inventing a fact.
 * ============================================================================= */

package org.firststep.backend.findhelp.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.firststep.backend.findhelp.dto.CategoryGateway;
import org.firststep.backend.findhelp.dto.DirectoryEntry;
import org.firststep.backend.findhelp.dto.FindHelpPage;
import org.firststep.backend.home.dto.ResourcePathway;
import org.firststep.backend.home.service.PathwayService;
import org.firststep.backend.organization.model.Organization;
import org.firststep.backend.organization.repository.OrganizationRepository;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.dto.ContentItem;
import org.springframework.stereotype.Service;

/**
 * Assembles Find Help — the page that answers "where should I start?".
 *
 * <p><b>It hands off; it does not filter.</b> Every gateway leads to a category
 * page and every directory entry leads to an organization page. Find Help never
 * expands in place, because the three destinations answer different questions:
 *
 * <pre>
 *   Find Help          where should I start?
 *   Category page      what is available for this need?
 *   Organization page  who can I contact?
 * </pre>
 *
 * <p><b>The gateways come from the authored pathway list, not from the data.</b>
 * A category with no organizations still gets a door: the corpus should populate
 * the experience, never determine the information architecture. Counting
 * organizations to decide which doors exist would let a thin registry silently
 * shrink the site's navigation — and would be wrong anyway, since a category page
 * carries six content forms and organizations are only one of them.
 */
@Service
public class FindHelpService {

    /** Services with no age or gender bound. The heading states this rule. */
    private static final int OPEN_TO_EVERYONE_LIMIT = 6;

    private final PathwayService pathwayService;
    private final OrganizationRepository organizations;
    private final ResourceService resourceService;

    public FindHelpService(PathwayService pathwayService, OrganizationRepository organizations,
            ResourceService resourceService) {
        this.pathwayService = pathwayService;
        this.organizations = organizations;
        this.resourceService = resourceService;
    }

    public FindHelpPage getPage() {
        return new FindHelpPage(gateways(), openToEveryone(), directory());
    }

    /**
     * The category doors, reusing the SAME authored list the homepage's Community
     * Resources row uses.
     *
     * <p>One authored list rather than two: a second copy would drift, and the
     * question both pages ask — "which needs are the ways in?" — is the same
     * question. {@code PathwayService} already resolves labels and icons from the
     * taxonomy, so a renamed category renames both.
     */
    private List<CategoryGateway> gateways() {
        List<CategoryGateway> out = new ArrayList<>();
        for (ResourcePathway p : pathwayService.getCommunityResources()) {
            boolean isCategory = ResourcePathway.CATEGORY.equals(p.kind());
            out.add(new CategoryGateway(
                    p.key(), p.label(), p.icon(),
                    // A discovery pathway has no destination until Version 3's tag
                    // derivation exists. It is still rendered — see CategoryGateway.
                    isCategory ? "/category/" + p.key() : null,
                    isCategory));
        }
        return List.copyOf(out);
    }

    /**
     * Services anyone can approach: no age bound, no gender restriction.
     *
     * <p>The most useful starting point for the resident this page exists for —
     * someone who does not know whether they qualify and does not want to be
     * turned away. The rule is stated in the page's heading, so the selection
     * discloses itself and implies nothing about quality.
     */
    private List<ContentItem> openToEveryone() {
        return resourceService.getAll().stream()
                .filter(r -> r.eligibilityAgeMin == null && r.eligibilityAgeMax == null)
                .filter(r -> {
                    String g = r.eligibilityGender;
                    return g == null || g.isBlank() || g.equalsIgnoreCase("any") || g.equalsIgnoreCase("all");
                })
                .sorted(Comparator.comparing(r -> r.title == null ? "" : r.title))
                .limit(OPEN_TO_EVERYONE_LIMIT)
                .map(FindHelpService::toContentItem)
                .toList();
    }

    /**
     * Every registered organization, by name.
     *
     * <p>Alphabetical is right HERE and would have been wrong as the page's
     * primary structure: this is the "I already know who I am looking for" path,
     * where predictable ordering is the point. It sits below the gateways for
     * exactly that reason.
     */
    private List<DirectoryEntry> directory() {
        List<Resource> all = resourceService.getAll();
        return organizations.findAll().stream()
                .sorted(Comparator.comparing(o -> o.name == null ? "" : o.name))
                .map(org -> new DirectoryEntry(org.name, org.slug, servicesOf(org, all)))
                .toList();
    }

    private static List<DirectoryEntry.ServiceLine> servicesOf(Organization org, List<Resource> all) {
        return all.stream()
                .filter(r -> org.id != null && org.id.equals(r.organizationId))
                .map(r -> new DirectoryEntry.ServiceLine(
                        r.title,
                        r.categoryTags != null && !r.categoryTags.isEmpty() ? r.categoryTags.get(0) : null,
                        r.locations != null && !r.locations.isEmpty() ? r.locations.get(0).city : null,
                        // THIS SERVICE'S number, carried as the service's own.
                        // Promoting it to the organization is what Decision 049
                        // refuses, and a directory is where the temptation lives.
                        r.phones != null && !r.phones.isEmpty() ? r.phones.get(0).number : null))
                .toList();
    }

    private static ContentItem toContentItem(Resource r) {
        return new ContentItem(
                r.contentType, r.id, r.title,
                r.summary != null ? r.summary : r.description,
                r.organization,
                r.locations != null && !r.locations.isEmpty() ? r.locations.get(0).city : null,
                r.cost, r.urgency, null,
                r.websites != null && !r.websites.isEmpty() ? r.websites.get(0).url : null,
                null);
    }
}
