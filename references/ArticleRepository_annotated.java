/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../originals/repository/ArticleRepository.java
 * Slice K. See references/decisions.md Decision 048.
 * =============================================================================
 *
 * WHAT IT IS: access to articles in EVERY review state.
 *
 * THE MOST IMPORTANT THING ABOUT THIS INTERFACE IS WHAT IT DOES NOT DO
 * --------------------------------------------------------------------
 * It does not filter. That looks like a missed safety opportunity and is the
 * opposite: filtering here would destroy the routing seam.
 *
 * The distinction the architecture has to preserve is between WITHHELD and GONE.
 * Excluding an article from public queries says nothing about whether it exists.
 * If the repository itself only returned approved articles, "not public" would
 * have quietly become "not stored", and a future administrative editorial queue
 * could no longer reach the drafts it exists to manage.
 *
 * So the responsibilities split cleanly:
 *
 *     ArticleRepository   every article, every state       (what exists)
 *     ArticleService      approved only                     (what a reader sees)
 *
 * THE SEAM IS AN ABSENCE, NOT A METHOD
 * ------------------------------------
 * There is deliberately no findAwaitingReview() or findByStatus() here. Version 3
 * owns the administrative layer and is not pulled forward; a method with no
 * caller would be speculative scaffolding, and this codebase introduces an
 * abstraction at its second use.
 *
 * What Slice K owes that future workflow is only that it CAN be built without
 * reshaping the article model: the states are modeled richly, the review record
 * carries passages and reasons and recommendations, and findAll() genuinely
 * returns everything. A future Admin service adds its own read against this
 * interface and touches nothing here.
 * ============================================================================= */

package org.firststep.backend.originals.repository;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.originals.model.Article;

/**
 * Access to First Step Original articles.
 *
 * <p><b>This interface returns articles in EVERY review state, and that is
 * deliberate.</b> Excluding an article from public queries says nothing about
 * whether it exists — unapproved content is withheld, never deleted, absent or
 * lost. Drafts, in-review and flagged articles are fully present here.
 *
 * <p>That is the <b>routing seam</b>: a future administrative editorial queue
 * needs to select precisely the articles the public cannot see, and it can do so
 * against this interface without reshaping the article model. No such caller
 * exists today and none is built here — the seam is the ABSENCE of filtering at
 * this layer, not a speculative method waiting for one.
 *
 * <p>The public rule lives one layer up, in {@code ArticleService}. Keeping it
 * there rather than here is what lets both audiences be served from one store.
 */
public interface ArticleRepository {

    /** Every article, in every review state. Filtering is the service's job. */
    List<Article> findAll();

    Optional<Article> findById(String id);
}
