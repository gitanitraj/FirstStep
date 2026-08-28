/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../originals/service/ArticleService.java
 * Slice K. See references/decisions.md Decision 048.
 * =============================================================================
 *
 * WHAT IT IS: the publication boundary. The single place that decides whether a
 * First Step Original article may reach a reader.
 *
 * THE BOUNDARY LIVES IN THE QUERY
 * -------------------------------
 * This is the central architectural choice and it has a precedent in this
 * codebase. Community Notices does not ask every consumer to remember that
 * government flyers do not belong there; isInSector filters them out, so the rule
 * cannot be forgotten because there is nothing to remember.
 *
 * Same move here. getPublishable() does not return drafts, so no reading surface
 * CAN serve one — not by oversight, not under deadline, not in a consumer written
 * next year by someone who never read Decision 048.
 *
 * The alternative is a convention: "remember to check the status." Conventions
 * hold until the first busy afternoon. A rule enforced by selection holds
 * because violating it requires writing new code that deliberately goes around
 * this class.
 *
 * TWO METHODS, ONE RULE
 * ---------------------
 * getPublishable()      the list
 * getPublishableById()  a single article
 *
 * The second exists because a boundary with one entrance is not a boundary. A
 * direct link to a draft's id must not become the way around the list filter, so
 * the id lookup applies the same test. To a public reader an unapproved article
 * and a nonexistent one give the same answer; telling them apart is the future
 * Admin layer's job.
 *
 * THE DIRECTION OF THE TEST
 * -------------------------
 * Both delegate to Article.isPublishable(), which asks for a RECORDED DECISION TO
 * RELEASE rather than for a reason to withhold. Fail-closed, not fail-open: a
 * missing review, an unfinished one, an unreadable status and a flagged article
 * all resolve to "not public", which is the right answer to "we do not know
 * whether a human cleared this."
 *
 * WHAT THIS CLASS DELIBERATELY LACKS
 * ----------------------------------
 * No getAwaitingReview(). It would have no caller today. Version 3 owns the
 * administrative layer and is not pulled forward; the seam is the repository's
 * unfiltered findAll(), and a future Admin service reads against that without
 * touching this class. Slice K's obligation was to make that future possible,
 * not to build it.
 *
 * SLICE K ADDITION — getPublishableDetail()
 * -----------------------------------------
 * The public projection lives HERE, beside the publication rule, and that
 * placement is the point. The class that decides WHETHER a reader may see an
 * article also decides WHAT they see. Splitting them would let a future endpoint
 * build its own projection and quietly include a field this one excludes.
 *
 * Note the ordering inside it: getPublishableById() applies the boundary FIRST,
 * so an unapproved article is never even mapped. The DTO's shape is then a
 * second, independent guarantee. Two mechanisms, neither relying on the other.
 *
 * toDetail() is private and static: it needs no state, and keeping it private
 * means there is exactly one way to produce an ArticleDetail.
 * ============================================================================= */

package org.firststep.backend.originals.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.firststep.backend.originals.dto.ArticleDetail;
import org.firststep.backend.originals.model.Article;
import org.firststep.backend.originals.repository.ArticleRepository;
import org.springframework.stereotype.Service;

/**
 * The publication boundary for First Step Original articles.
 *
 * <p><b>A successfully generated article is not a publishable article.</b> This
 * class is where that becomes structural rather than a matter of discipline
 * (Decision 048).
 *
 * <p><b>The boundary lives in the QUERY.</b> Nothing downstream is asked to
 * remember that drafts are private; {@link #getPublishable()} does not return
 * them, so no reading surface can serve one. This is the same mechanism that
 * keeps government flyers out of Community Notices — selection, not convention —
 * and it is chosen for the same reason: a rule enforced by everyone who touches
 * the data holds until the first busy afternoon.
 *
 * <p><b>Excluded is not deleted.</b> Unapproved articles remain fully present in
 * the repository, which is the routing seam a future administrative editorial
 * queue selects from. That queue is Version 3 and is not built here. What Slice K
 * owes it is only that the state and the review record are rich enough to act on
 * — which article, which passages, which reasons, which recommended action.
 *
 * <p><b>What this class deliberately does NOT have</b> is an
 * {@code getAwaitingReview()} or similar. It would have no caller today, and this
 * codebase introduces an abstraction at its second use, not in anticipation of
 * one. The seam is the repository's unfiltered {@code findAll()}; a future Admin
 * service adds its own read against it without touching this one.
 */
@Service
public class ArticleService {

    private final ArticleRepository repository;

    public ArticleService(ArticleRepository repository) {
        this.repository = repository;
    }

    /**
     * Articles a public reading surface may serve: <b>approved only</b>, newest
     * first.
     *
     * <p>Every other state — draft, in-review, flagged, and a record with no
     * review at all — is withheld. Note the direction of the test: this asks for
     * a recorded decision to RELEASE, not for a reason to withhold. An article
     * whose review metadata is missing or unreadable is therefore private, which
     * is the correct answer to "we do not know whether this was reviewed".
     */
    public List<Article> getPublishable() {
        return repository.findAll().stream()
                .filter(Article::isPublishable)
                .sorted(Comparator.comparing((Article a) -> a.publishDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * One article for a public reading surface, or empty.
     *
     * <p>Applies the same rule as {@link #getPublishable()} — a direct link to an
     * unapproved article's id must not become the way around the boundary. An
     * unapproved article and a nonexistent one are the same answer HERE; telling
     * them apart is the future Admin layer's job, not the public reader's.
     */
    public Optional<Article> getPublishableById(String id) {
        return repository.findById(id).filter(Article::isPublishable);
    }

    /**
     * One article shaped for a public reading surface, or empty.
     *
     * <p>The mapping happens HERE rather than in the controller so that the class
     * owning the publication rule is also the class that decides what a reader
     * receives. Splitting them would let a future endpoint build its own
     * projection and quietly include a field this one excludes.
     *
     * <p>Note the ordering: {@link #getPublishableById} applies the boundary
     * first, so an unapproved article is never even mapped. The DTO's shape is
     * the second, independent guarantee — see {@link ArticleDetail}.
     */
    public Optional<ArticleDetail> getPublishableDetail(String id) {
        return getPublishableById(id).map(ArticleService::toDetail);
    }

    private static ArticleDetail toDetail(Article a) {
        return new ArticleDetail(
                a.id,
                a.title,
                a.summary,
                a.whyItMatters,
                a.body,
                a.byline,
                a.disclosure,
                a.publishDate,
                a.updatedDate,
                a.categoryTags,
                a.subcategory);
        // Nothing else is mapped, and nothing else CAN be: ArticleDetail has no
        // component for generatedBy, editorialReview or verified.
    }
}
