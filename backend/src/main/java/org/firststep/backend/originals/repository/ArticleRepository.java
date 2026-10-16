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
