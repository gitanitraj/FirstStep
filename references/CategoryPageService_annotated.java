/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../category/service/CategoryPageService.java
 * Slice F5a (the category page as an aggregate). See decisions.md Decision 036.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS CLASS IS
 *   The category page's BFF. It composes three already-shaped results into the
 *   single payload GET /api/category/{key} returns. It classifies nothing,
 *   infers nothing, and holds no editorial rules — an AGGREGATE READ MODEL.
 *
 * THE PROBLEM IT SOLVES (measured, not assumed)
 *   F4 shipped the endpoint returning navigation alone, and putting totalCount
 *   beside the per-topic counts showed they do not meet:
 *
 *     housing  45 of 73     health   33 of 84
 *     legal     5 of 41     utilities 0 of 22
 *
 *   The cause is structural. Resources (229/229) and flyers (7/7) carry a
 *   subcategory because an editor gave them one. News (0/8), signed legislation
 *   (0/175) and expert content (0/12) do not — they are classified into a
 *   category, and the conservative classifier will not invent a topic.
 *
 * THE FIX THAT WAS REJECTED
 *   Making the classifier infer subcategories. That is deferred to VERSION 3
 *   (user's call, and the right one): it would trade a conservative engine for a
 *   guessing one, and the project has already measured what guessing costs —
 *   removing a single source mapping in F2.1 silently redistributed 37 housing
 *   resources into three other categories, plausibly and wrongly.
 *
 * THE FIX THAT WAS TAKEN
 *   Recognize that a category page serves TWO purposes: helping residents browse
 *   resources, and helping them understand what has changed. The 193 topicless
 *   items are not a gap in the first purpose — they ARE the second.
 *
 *     COVERAGE GROWS BY COMPOSITION, NOT BY INFERENCE.
 *
 *   Verified across all ten categories: browse ∪ updates == totalCount, exactly.
 *   Utilities is the sharpest case — 0 resources, 0 topics, a literally empty
 *   page before this slice; 22 signed bills reachable after it, none of them
 *   placed by a guess.
 * ============================================================================= */

package org.firststep.backend.category.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.category.dto.CategoryMetadata;
import org.firststep.backend.category.dto.CategoryPage;
import org.firststep.backend.navigation.dto.CategoryNavigation;
import org.firststep.backend.navigation.service.NavigationService;
import org.firststep.backend.organization.service.OrganizationService;
import org.firststep.backend.updates.dto.UpdateItem;
import org.firststep.backend.updates.service.UpdatesService;
import org.springframework.stereotype.Service;

@Service
public class CategoryPageService {

    // A dashboard, not an archive. Housing has 29 qualifying items; listing them
    // all would drown the browse half of the page. Complete access to legislation
    // is the Important Notices page (Slice H).
    private static final int MAX_UPDATES = 6;

    // Three collaborators, each keeping its own job. Note what is NOT here: no
    // NewsService, no RssFeedSource, no FlyerService, no ResourceService. This
    // class never touches raw content, which is what keeps it a read model rather
    // than a second place where "what belongs in Housing?" gets answered.
    private final NavigationService navigationService;
    private final UpdatesService updatesService;
    private final OrganizationService organizationService;

    public CategoryPageService(NavigationService navigationService, UpdatesService updatesService,
            OrganizationService organizationService) {
        this.navigationService = navigationService;
        this.updatesService = updatesService;
        this.organizationService = organizationService;
    }

    // Existence is the navigation read model's answer to give — it owns the
    // taxonomy lookup. Asking it FIRST, and mapping over the Optional, means the
    // other two collaborators are never called for a category that does not
    // exist. (The alternative — call all three, then check — would do three
    // full aggregations to produce a 404.)
    public Optional<CategoryPage> getByKey(String categoryKey, String communityId) {
        return navigationService.getByKey(categoryKey, communityId)
                .map(navigation -> build(categoryKey, communityId, navigation));
    }

    private CategoryPage build(String categoryKey, String communityId, CategoryNavigation navigation) {
        List<UpdateItem> updates = updatesService.getForCategory(categoryKey, communityId, MAX_UPDATES);

        // Metadata is PROJECTED from the navigation read model rather than
        // recomputed. Recomputing counts here would create a second source of
        // truth for "how big is this category?", and the two would drift the first
        // time either changed.
        CategoryMetadata metadata = new CategoryMetadata(
                navigation.key(), navigation.label(), navigation.icon(),
                navigation.totalCount(), navigation.countsByType(),
                mostRecentDate(updates));

        // groups/topics are passed through untouched, carrying F3's
        // mutually-exclusive invariant verbatim: one of the two is always empty.
        return new CategoryPage(
                metadata,
                updates,
                navigation.groups(),
                navigation.topics(),
                organizationService.getForCategory(categoryKey));
    }

    // The feed is already sorted newest-first with undated items last, so the head
    // IS the answer — unless the head is one of those undated items, in which case
    // its date is null and null is the honest value to report.
    private static String mostRecentDate(List<UpdateItem> updates) {
        return updates.isEmpty() ? null : updates.get(0).date();
    }
}

// =============================================================================
// WHY THIS SERVICE EXISTS, WHEN F4 REFUSED IT
// =============================================================================
// F4 explicitly declined a CategoryPageService and was right to. The rule has
// not changed; the facts did.
//
//   F4: one source (NavigationService), empty composition step.
//       A service would have forwarded a call. Refused.
//   F5a: three sources, real composition.
//       The second use an abstraction needs before it earns its name. Built.
//
// This is worth keeping as the worked example of the project's "no abstractions
// for single-use code" rule, because it shows the rule is a TEST rather than a
// preference — the same code was correctly rejected and correctly accepted eight
// hours apart, on evidence.
//
// The same test explains HomeService, which composes five aggregators plus
// static AI config and has always earned its keep.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
//   CategoryPageService
//     ├── NavigationService.getByKey()          → metadata + groups/topics
//     │     reads ONLY categoryTags + subcategory; unchanged by this slice
//     ├── UpdatesService.getForCategory()       → news + law + flyer + expert
//     │     the ONE cross-type merger; resources deliberately excluded
//     └── OrganizationService.getForCategory()  → organizations in this category
//
// NAVIGATIONSERVICE IS UNTOUCHED, AND THAT IS THE DESIGN CONSTRAINT. It still
// produces resident navigation and nothing else; composition happens one layer
// up, so the read model never learns about pages. The empirical check on that
// claim: NavigationServiceTest (14 tests) needed ZERO edits, and `git diff` on
// the whole navigation package is empty.
//
// This layering is only possible because CLASSIFICATION IS AN INGESTION CONCERN.
// By the time a request reaches this class, every item's editorial classification
// is settled, so all three collaborators can be pure aggregation and this one can
// be pure arrangement.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - MERGING NEWS/LAW/FLYER/EXPERT HERE instead of delegating to UpdatesService:
//   rejected. UpdatesService's own javadoc claims to be "the single place
//   cross-type 'latest updates' merging happens", and a second merger would
//   contradict a documented invariant to save ten lines. It already composes
//   News + RSS + Flyer; F5a added Expert and a category filter to the service
//   that owns the job. The genuinely fiddly reuse is the private toUpdateItem
//   mappers — date selection and source/url resolution — not the loop.
//
// - NESTING THE WHOLE CategoryNavigation as a `navigation` field: rejected. It
//   would repeat key, label, icon and countsByType in two places in one payload.
//   Projecting metadata + groups + topics as siblings keeps each fact once.
//
// - MODIFYING CategoryNavigation to suit the page: rejected outright. It is the
//   read model's contract, and reshaping it for a page is exactly the coupling
//   this slice exists to prevent.
//
// - INCLUDING RESOURCES IN THE UPDATES FEED: rejected. A resource is a standing
//   service, not an event. Excluding them is also what makes the two halves
//   complementary rather than overlapping — see the coverage identity above.
// =============================================================================
