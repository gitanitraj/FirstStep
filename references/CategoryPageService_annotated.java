/*
 * WHAT: Complete annotated mirror of backend/src/main/java/org/firststep/backend/category/service/CategoryPageService.java for IM-08.
 * WHY: Integrate approved selector outcomes with scoped, accessible resident navigation.
 * INTERACTION: Existing eligibility and collection services own admission; the destination
 * projection allowlists cards/links, while React owns responsive presentation and route scope.
 * ALTERNATIVES: Reimplementing selectors or deriving providers from source names would violate
 * predecessor contracts. Tests cover pass-through, actual catalog HTTP, and browser journeys.
 */
package org.firststep.backend.category.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.firststep.backend.category.dto.CategoryMetadata;
import org.firststep.backend.category.dto.CategoryPage;
import org.firststep.backend.navigation.dto.CategoryNavigation;
import org.firststep.backend.navigation.dto.TopicGroup;
import org.firststep.backend.navigation.dto.TopicNavigation;
import org.firststep.backend.navigation.service.NavigationService;
import org.firststep.backend.organization.service.OrganizationService;
import org.firststep.backend.shared.model.ContentType;
import org.firststep.backend.updates.dto.UpdateItem;
import org.firststep.backend.updates.service.UpdatesService;
import org.springframework.stereotype.Service;

/**
 * The category page's BFF (Slice F5a) — an <b>aggregate read model</b>.
 *
 * <h2>Why this service exists, when F4 refused it</h2>
 *
 * F4 declined a CategoryPageService and was right to: the endpoint had one source
 * ({@link NavigationService}) and an empty composition step, so the service would
 * have forwarded a call and nothing else. F5a has three sources and a real
 * composition, which is the second use an abstraction needs before it earns its
 * name. The rule is unchanged; the facts changed.
 *
 * <h2>What each collaborator keeps</h2>
 *
 * <pre>
 * CategoryPageService
 *   ├── NavigationService.getByKey()          → metadata + groups/topics   (Discover)
 *   ├── UpdatesService.getForCategory()       → news + law + flyer + expert (Stay Informed)
 *   ├── OrganizationService.getForCategory()  → organizations              (Connect)
 *   ├── CategoryContentService.collectionsFor() → existing controlled collections
 *   └── CategoryDestinationService.build()      → public featured cards and context links (IM-08)
 * </pre>
 *
 * <p>The fourth collaborator arrived with the category destination. It supplies
 * purposeful collections shaped around resident jobs — help me act, help me
 * understand — while the three pillars above keep their exact semantics. Retiring
 * a pillar field is a deliberate act for a later slice, never a silent duplicate
 * left in place.
 *
 * <p><b>{@link NavigationService} is untouched by this slice.</b> It remains a
 * pure read model producing resident navigation and nothing else. Composition
 * happens here, one layer up, so the read model never learns about pages. The
 * empirical check on that claim is that NavigationServiceTest needed no edits.
 *
 * <p>This service classifies nothing, infers nothing, and applies no editorial
 * rules — it reads three already-shaped results and arranges them. Classification
 * is an ingestion concern; by the time a request arrives there is nothing left to
 * decide.
 */
@Service
public class CategoryPageService {

    /**
     * A dashboard, not an archive. Housing has 29 qualifying items; listing them
     * all would drown the browse half of the page. Complete access to legislation
     * is the Important Notices page (Slice H).
     */
    private static final int MAX_UPDATES = 6;

    private final CategoryDestinationService destinationService;
    private final NavigationService navigationService;
    private final UpdatesService updatesService;
    private final OrganizationService organizationService;
    private final CategoryContentService categoryContentService;

    public CategoryPageService(NavigationService navigationService, UpdatesService updatesService,
            OrganizationService organizationService, CategoryContentService categoryContentService,
            CategoryDestinationService destinationService) {
        this.destinationService = destinationService;
        this.navigationService = navigationService;
        this.updatesService = updatesService;
        this.organizationService = organizationService;
        this.categoryContentService = categoryContentService;
    }

    /**
     * The whole page, or empty when the key is not in the taxonomy. Existence is
     * the navigation read model's answer to give — it owns the taxonomy lookup, so
     * asking it first means the other two collaborators are never called for a
     * category that does not exist.
     */
    public Optional<CategoryPage> getByKey(String categoryKey, String communityId) {
        return navigationService.getByKey(categoryKey, communityId)
                .map(navigation -> build(categoryKey, communityId, navigation));
    }

    private CategoryPage build(String categoryKey, String communityId, CategoryNavigation navigation) {
        List<UpdateItem> updates = updatesService.getForCategory(categoryKey, communityId, MAX_UPDATES);

        CategoryMetadata metadata = new CategoryMetadata(
                navigation.key(), navigation.label(), navigation.icon(),
                navigation.totalCount(), navigation.countsByType(),
                mostRecentDate(updates));

        var collections = categoryContentService.collectionsFor(categoryKey, communityId);
        return new CategoryPage(
                metadata,
                updates,
                navigation.groups().stream().map(group -> new TopicGroup(
                        group.label(), helpTopics(categoryKey, group.topics(), communityId))).toList(),
                helpTopics(categoryKey, navigation.topics(), communityId),
                communityId == null ? organizationService.getForCategory(categoryKey)
                        : organizationService.getForCategory(categoryKey, communityId),
                collections,
                communityId == null ? null : destinationService.build(communityId, categoryKey, collections));
    }

    /** Navigation toward help pages reports help-only counts; generic navigation keeps its own semantics. */
    private List<TopicNavigation> helpTopics(String key,
            List<TopicNavigation> topics, String communityId) {
        return topics.stream().map(topic -> {
            var items = categoryContentService.helpFor(key, topic.name(), communityId);
            Map<ContentType, Integer> counts =
                    new EnumMap<>(ContentType.class);
            items.forEach(item -> counts.merge(item.contentType(), 1, Integer::sum));
            return new TopicNavigation(topic.name(), topic.slug(),
                    items.size(), counts);
        }).toList();
    }

    /**
     * The feed is already sorted newest-first with undated items last, so the head
     * is the answer — unless it is one of those undated items, in which case there
     * is no date to report and null is the honest value.
     */
    private static String mostRecentDate(List<UpdateItem> updates) {
        return updates.isEmpty() ? null : updates.get(0).date();
    }
}
