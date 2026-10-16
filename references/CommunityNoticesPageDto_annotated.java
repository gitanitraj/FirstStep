/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../notices/dto/CommunityNoticesPage.java
 * Slice J. See references/decisions.md Decision 046.
 * =============================================================================
 *
 * WHAT IT IS
 * ----------
 * The page-shaped BFF response for every one of the five Community Notices
 * routes. One record, five states.
 *
 * WHY ONE RECORD AND NOT FIVE
 * ---------------------------
 * Five DTOs would have been the same four fields under five names, and each
 * would have drifted as its view acquired a quirk. The page is ONE page in five
 * states, so the payload is one shape in five states.
 *
 * The cost of that choice, stated honestly: `items` is empty on OVERVIEW and
 * `previews` is empty everywhere else, so every response carries one field it
 * does not use. The alternative — a sealed hierarchy of OverviewPage/ViewPage —
 * would force the client to know which shape it asked for before it could read
 * the answer, which is worse for a display-only frontend.
 *
 * WHY `counts` IS ON EVERY RESPONSE
 * ---------------------------------
 * The four navigation cards render on EVERY route, counts included. A view
 * response that carried only its own items would leave the nav numberless until
 * a second request landed, and the cards would visibly fill in after the page
 * had already drawn. Sending four integers is cheaper than a second round trip
 * and removes a whole class of layout shift.
 *
 * BFF REASONING (Decision 030)
 * ----------------------------
 * The backend aggregates; the frontend displays. The client does no filtering,
 * no sorting and no counting — it maps the payload onto components. That is why
 * counts are computed here rather than derived client-side from `items`, which
 * would only ever have been able to count the ACTIVE view anyway.
 *
 * NESTED RECORD: NoticePreview carries `count` alongside `items` because the two
 * differ on purpose — three items shown, "See all (12)". Sending only the items
 * would have made the link lie.
 * ============================================================================= */

package org.firststep.backend.notices.dto;

import java.util.List;
import java.util.Map;

import org.firststep.backend.notices.model.NoticeView;
import org.firststep.backend.shared.dto.ContentItem;

/**
 * The whole Community Notices page, in one response, for any of its five routes.
 *
 * <pre>
 *   /community-notices                 view = OVERVIEW   counts + previews
 *   /community-notices/events          view = EVENTS     counts + items
 *   /community-notices/meetings        …
 *   /community-notices/announcements   …
 *   /community-notices/flyers          …
 * </pre>
 *
 * <p><b>One record for five routes, because it is one page in five states.</b>
 * Five DTOs would have been the same fields under five names, and would have made
 * the four views drift apart as each acquired its own shape.
 *
 * <p><b>{@code counts} rides on EVERY response, including each view's.</b> The
 * four navigation cards render on every route, so a view that returned only its
 * own items would leave the nav without numbers until a second request landed —
 * and the cards would visibly fill in after the page had already drawn.
 *
 * <p>{@code items} carries the active view and is empty on OVERVIEW;
 * {@code previews} is the reverse. Both are present as fields rather than split
 * into two records because the alternative is a client that has to know which
 * shape it asked for before it can read the answer.
 *
 * <p>The landing route deliberately carries real content. It answers "what kinds
 * of community information can I find here?" — a page that only routed onward
 * would be a menu wearing a destination's URL.
 */
public record CommunityNoticesPage(
        NoticeView view,
        /** Every discovery view's size, keyed by view. Always populated. */
        Map<NoticeView, Integer> counts,
        /** The active view's content. Empty on OVERVIEW. */
        List<ContentItem> items,
        /** A short sample of each view. OVERVIEW only. */
        List<NoticePreview> previews
) {

    /** A few items from one view, for the landing page. */
    public record NoticePreview(NoticeView view, int count, List<ContentItem> items) {
    }
}
