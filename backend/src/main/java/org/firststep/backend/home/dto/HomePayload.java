package org.firststep.backend.home.dto;

import java.util.List;

import org.firststep.backend.flyer.dto.FlyerCard;
import org.firststep.backend.legislation.dto.LawItem;
import org.firststep.backend.shared.dto.ContentItem;

/**
 * Everything the homepage needs in a single response, so the SPA makes one
 * request on load rather than stitching several endpoints together (backend
 * aggregates, frontend displays — see references/decisions.md 019/020).
 *
 * <p>Each field is one homepage section, and the record reads top-to-bottom in
 * the order the page renders:
 *
 * <pre>
 *   aiConfig            AI Search — "What do you need help with today?"
 *   delawareLaws        New Laws in Delaware (the retained RSS scroll)
 *   communityResources  the main column — seven authored discovery pathways
 *   originals           the sidebar — First Step-produced CivicContent
 *   communityFlyers     Community Information's flyer preview
 * </pre>
 *
 * <p><b>{@code updates} was removed.</b> The homepage's Important Updates feed
 * became TWO destination pages, split by who produced the content — Latest
 * Updates (government: agencies, officials, programs) and Community Notices
 * (non-government: churches, nonprofits, community groups). A merged feed on the
 * front door could not honour that distinction, and the front door's job is the
 * way in. {@code /api/updates} still serves the feed for those pages.
 *
 * <p><b>{@code organizations} was removed in Slice H.</b> The homepage no longer
 * has an Organizations column: organizations moved behind the Connect → Find Help
 * pathway, where a directory can do them justice (Decision 041, Slice G owns it).
 * {@code OrganizationService.getCuratedShortlist()} is consequently unused —
 * left in place for Slice G rather than deleted.
 *
 * <p>Sections with no data here are deliberate, not omissions: the Mission Cards
 * and the footer are static. Community Information carries flyers alone — it
 * groups nothing, so no flyer grouping metadata is needed on the homepage.
 */
public record HomePayload(
        AiConfig aiConfig,
        List<ResourcePathway> communityResources,
        List<ContentItem> originals,
        List<LawItem> delawareLaws,
        List<FlyerCard> communityFlyers
) {
}
