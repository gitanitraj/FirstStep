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

    /**
     * One line per service, with the redundant title suppressed.
     *
     * <p><b>Resources have no service name of their own.</b>
     * {@code JsonResourceRepository} derives {@code title} from
     * {@code organization}, so under an organization heading the title renders as
     * the organization's name a second time — "American Red Cross / American Red
     * Cross". Sending it would be sending nothing, twice.
     *
     * <p>So the title is carried only when it actually differs, and the line
     * falls back to category, city and phone — which do distinguish services,
     * including for the three organizations that list two.
     *
     * <p>The judgement lives HERE rather than in the client because "title is
     * derived from organization" is a fact about the data, not about
     * presentation. When resources gain a real service name this method starts
     * passing it through with no other change — see Decision 050's gap note.
     */
    private static List<DirectoryEntry.ServiceLine> servicesOf(Organization org, List<Resource> all) {
        return all.stream()
                .filter(r -> org.id != null && org.id.equals(r.organizationId))
                .map(r -> new DirectoryEntry.ServiceLine(
                        r.title != null && r.title.equalsIgnoreCase(org.name) ? null : r.title,
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
