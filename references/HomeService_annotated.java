/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../home/service/HomeService.java
 * Slice H (reshaped). Originally Step 5c, Decision 020. See Decision 042.
 * Keep this mirror in sync whenever the production file changes.
 * =============================================================================
 *
 * WHAT THIS CLASS IS
 *   The homepage's BFF composer. It assembles every section of the front door
 *   into ONE response so the SPA makes a single request on load.
 *
 * WHAT CHANGED IN SLICE H
 *   The homepage stopped being "five sections of whatever we had" and became a
 *   front door: a small number of progressively deeper choices. Two fields left,
 *   two arrived, and one method signature lost a parameter. All three changes
 *   are consequences of the same idea — see the sections at the bottom.
 * ============================================================================= */

package org.firststep.backend.home.service;

import java.util.ArrayList;
import java.util.List;

import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.home.dto.AiChip;
import org.firststep.backend.home.dto.AiConfig;
import org.firststep.backend.home.dto.HomePayload;
import org.firststep.backend.legislation.service.LegislationService;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.model.ContentSource;
import org.springframework.stereotype.Service;

/**
 * Assembles the single homepage payload by composing the existing aggregators
 * plus static hero config. No data logic is duplicated here — it only stitches
 * the pieces the homepage needs together.
 *
 * <p><b>Slice H reshaped what "the pieces" are.</b> The homepage is now a front
 * door: a small number of progressively deeper choices, with the complexity
 * behind it on destination pages. So this service composes SIX sections instead
 * of the old five, and dropped organizations entirely — see {@link HomePayload}.
 */
@Service
public class HomeService {

    /** ContentSource.id marking content First Step produced itself. */
    private static final String FIRST_STEP = "first-step";

    // Static hero config. Kept server-side so the homepage's AI form (chips,
    // prompts, placeholder) is backend-owned; the chip `value`s match what
    // the AI search sends to POST /api/decide.
    private static final AiConfig AI_CONFIG = new AiConfig(
            "E.g., I need rental help near Wilmington for seniors",
            List.of(
                    "I need help paying rent this month",
                    "Where can I find free food near me?",
                    "I just lost my job — where do I start?"),
            List.of(
                    new AiChip("urgent", "🚨 Urgent", true),
                    new AiChip("housing", "🏠 Housing", false),
                    new AiChip("essentials", "🛒 Essentials", false)));

    private final PathwayService pathwayService;
    private final FaqService faqService;
    private final LegislationService legislationService;
    private final FlyerService flyerService;

    public HomeService(PathwayService pathwayService,
            FaqService faqService, LegislationService legislationService,
            FlyerService flyerService) {
        this.pathwayService = pathwayService;
        this.faqService = faqService;
        this.legislationService = legislationService;
        this.flyerService = flyerService;
    }

    public HomePayload getHome() {
        return new HomePayload(
                AI_CONFIG,
                pathwayService.getCommunityResources(),
                originals(),
                legislationService.getRecentSignedBills(),
                flyerService.getCarouselCards());
    }

    /**
     * First Step Originals — CivicContent First Step produced itself, identified
     * by {@code contentSource.id}, never by a new ContentType or domain class
     * (Decision 041). "Originals" describes WHO MADE the content, and
     * ContentSource is the field that already answers that question.
     *
     * <p>Today that is curated FAQs and nothing else, which is why the filter
     * lives here as a private method rather than in an OriginalsService: it has
     * exactly one source, and this codebase introduces the service at the SECOND
     * one (the F4 → F5a rule). When briefings or data stories arrive, this method
     * is what becomes that service.
     *
     * <p>Note what this does NOT do: it does not treat every EXPERT item as an
     * original. Expert answers carry {@code contentSource.name = "Delaware
     * Volunteer Legal Services"} — same contentType, different producer. That
     * distinction is the entire argument for using ContentSource here.
     */
    private List<ContentItem> originals() {
        List<ContentItem> items = new ArrayList<>();
        for (FAQ faq : faqService.getAll()) {
            ContentSource source = faq.contentSource;
            if (source == null || !FIRST_STEP.equals(source.id)) {
                continue;
            }
            items.add(new ContentItem(
                    faq.contentType,
                    faq.id,
                    faq.title,
                    faq.summary,
                    source.name,
                    null, null, null,
                    faq.updatedDate,
                    // No url: this is First Step's own content, so there is no
                    // originating organization to send the resident to.
                    null,
                    null));   // FAQs carry no image
        }
        return List.copyOf(items);
    }
}

// =============================================================================
// SECTION 1 — WHAT LEFT, AND WHY
// =============================================================================
// `organizations` and `categories` are both gone from the payload.
//
//   categories     -> replaced by `communityResources`. The old field was all
//                     ten CategorySummary objects, each carrying counts and
//                     latest items the tiles rendered. The front door shows
//                     seven pathways with a label and an icon and nothing else,
//                     because the homepage's job is the way in, not the preview.
//                     The richer shape still exists on /api/categories.
//
//   organizations  -> the homepage no longer has an Organizations column at all.
//                     Organizations moved behind the Connect → Find Help
//                     pathway, where a directory can do them justice (Slice G).
//                     A shortlist of eight orgs ranked by resource count was
//                     always a placeholder metric standing in for editorial
//                     judgement, and the front door is the wrong place to spend
//                     that space.
//
// OrganizationService.getCuratedShortlist() is now unused. It was deliberately
// LEFT IN PLACE rather than deleted: Slice G is going to need organization
// aggregation, and deleting a working method days before rewriting it is churn.
// Flagged in Decision 042 so it does not become mystery dead code.
//
// =============================================================================
// SECTION 2 — FIRST STEP ORIGINALS IS A ContentSource QUESTION
// =============================================================================
// "Originals" means CivicContent First Step produced ITSELF. The temptation is
// to make that a ContentType (ORIGINAL) or a domain class (Original extends
// CivicContent). Both are wrong, and the data says so out loud:
//
//     faq.json            contentSource.name = "First Step"
//     expert-answers.json contentSource.name = "Delaware Volunteer Legal Services"
//     BOTH                contentType        = EXPERT
//
// Same KIND of content. Different PRODUCER. ContentType answers "what kind of
// thing is this?"; ContentSource answers "who made it?". Originals is the second
// question, and ContentSource already existed to answer it — the `id` field was
// simply never populated (it was null in every record in every file until this
// slice set it on the six FAQs).
//
// A new ContentType would also have cost far more than it looks: the enum,
// contentTypeLabel.ts (an exhaustive Record that FAILS THE BUILD if unhandled),
// five badge classes, UpdatesService's mappers, EditorialStabilityTest's pinned
// baseline, EN + ES i18n keys, and an annotated mirror per file touched.
//
// THE PAYOFF IS EXTENSIBILITY FOR FREE. When Community Briefings, YouTube, Data
// Stories or newsletters arrive, they carry contentSource.id = "first-step" and
// appear in this section with NO CODE CHANGE HERE. Enumerating types would have
// required editing this method for each one.
//
// WHY THE FILTER IS A PRIVATE METHOD AND NOT AN OriginalsService:
// it has exactly ONE source. This codebase introduces the service at the SECOND
// one — the same rule that made F4 refuse CategoryPageService and F5a accept it
// (Decision 035/036). When the second Originals source lands, this method is
// what becomes that service.
//
// =============================================================================
// SECTION 2b — WHY getHome() LOST ITS updates FEED (Decision 043)
// =============================================================================
// The homepage's Important Updates feed became TWO destination pages, split by
// WHO PRODUCED the content:
//
//     /updates            Latest Updates     GOVERNMENT — agencies, officials,
//                                            programs
//     /community-notices  Community Notices  NON-GOVERNMENT — churches,
//                                            nonprofits, community groups
//
// A church offering a free meal and a state agency changing SNAP eligibility are
// both "notices", and a merged feed on the front door flattens the difference a
// resident most needs: who is telling me this, and what does that imply?
//
// THAT SPLIT IS A ContentSource DISTINCTION — the third time ContentSource has
// answered a question that looked like it wanted a new ContentType (after First
// Step Originals above). It is becoming the default answer to "same shape,
// different in kind"; check it first.
//
// So `updates` left HomePayload and this service stopped injecting
// UpdatesService. `/api/updates` still serves the feed for those pages, and
// `ImportantUpdates.tsx` is retained unrendered as their starting point.
//
// SECTION 3 — WHY getHome() LOST ITS communityId
// =============================================================================
// The parameter existed to pass through to categoryService.getAll(communityId).
// With the category tiles gone, NOTHING the homepage returns is community-scoped,
// and the frontend never sent the parameter in the first place.
//
// A parameter that is accepted and silently ignored is worse than one that is
// absent: it advertises a filter that does not happen. So it was removed from
// both the service and the controller. Granular endpoints keep theirs — this is
// a statement about the homepage, not about communities.
//
// =============================================================================
// SECTION 4 — WHAT THIS SERVICE STILL DOES NOT DO
// =============================================================================
// It does not filter, rank, or classify. Every collaborator hands back something
// already aggregated, and this class only assembles. That is the BFF rule from
// Decisions 019/020 — backend aggregates, frontend displays — and it is why the
// method body is a constructor call with six arguments.
//
// Three homepage sections have NO field here at all, which is not an oversight:
//   · the three mission cards are static routing
//   · Community Information's Events/Meetings/Announcements are PATHWAYS into
//     the Community page, not content lists — which is exactly why this slice
//     needed no flyer grouping metadata
//   · the footer is static
//
// CALL PATH:
//   GET /api/home
//     -> HomeService.getHome()
//        -> PathwayService.getCommunityResources()    (seven authored pathways)
//        -> originals()                               (FAQs where contentSource.id = first-step)
//        -> LegislationService.getRecentSignedBills() (the retained RSS scroll)
//        -> FlyerService.getCarouselCards()           (flyer images; the client shows 3)
//     -> ApiResponse.success(HomePayload)
// =============================================================================

// =============================================================================
// SECTION 5 — SLICE J: THE FLYER ROW GOT A DESTINATION (Decision 046)
// =============================================================================
// getHome() is UNCHANGED in shape. The flyer carousel cards it already returned
// now feed a section labeled "Community Notices" that links to
// /community-notices instead of /community.
//
// That the backend needed no change is the point worth recording. The homepage
// row was already a preview of community-produced flyers; what Slice J supplied
// was a destination worth linking to. Renaming a section and re-pointing a link
// are presentation decisions, and they stayed entirely in the frontend — no new
// field, no new endpoint, no aggregate reshaped.
//
// Contrast with what WOULD have reached this class: if the homepage row had been
// asked to group flyers by notice kind, HomeService would have had to read the
// kind vocabulary and build four collections. It was never asked to, because the
// grouping belongs to the destination. See CommunityInformation_annotated.tsx
// Section 1 for the full history of that gap.
//
// The one mechanical touch: ContentItem gained `imageUrl`, so this class's
// ContentItem construction passes null for it — homepage cards here carry no
// poster. Same compile-time enumeration described in TopicPageService's mirror.
//
// WHAT THE RENAME DID CHANGE, one layer down: getCarouselCards() is now scoped
// to the community sector, because the row's new label made its absence a bug —
// see FlyerService_annotated.java's closing section. This class still just calls
// the method; the rule stayed with the method that already owned three quarters
// of it, rather than being re-implemented here as a post-filter.
// =============================================================================
