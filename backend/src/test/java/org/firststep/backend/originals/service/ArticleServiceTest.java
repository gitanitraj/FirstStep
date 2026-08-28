package org.firststep.backend.originals.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.originals.model.Article;
import org.firststep.backend.originals.model.EditorialReview;
import org.firststep.backend.originals.model.ReviewFlag;
import org.firststep.backend.originals.model.DispositionStatus;
import org.firststep.backend.originals.model.FlagDisposition;
import org.firststep.backend.originals.repository.ArticleRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The editorial review boundary (Decision 048).
 *
 * <p>Every test here asks one question in a different way: <b>can content that
 * was generated but not approved reach a public reading surface?</b> The answer
 * must be no through every route — the list, a direct id lookup, a missing review
 * record, and a review record that is unreadable.
 *
 * <p>Negative tests assert the SPECIFIC exclusion under test rather than merely
 * that the result is empty, per the standing testing rule: a green negative test
 * only tells you something was excluded, not that the rule you were testing did
 * the excluding. Each uses a fixture where an approved article is also present,
 * so "nothing came back" cannot pass for "the right thing came back".
 */
class ArticleServiceTest {

    private static Article article(String id, String status) {
        Article a = new Article();
        a.id = id;
        a.title = id + " title";
        a.summary = id + " summary";
        a.body = id + " body";
        a.publishDate = "2026-08-01";
        a.generatedBy = "ai:draft";
        if (status != null) {
            EditorialReview review = new EditorialReview();
            review.status = status;
            review.reviewedDate = "2026-08-02";
            review.reviewer = "human:editorial";
            a.editorialReview = review;
        }
        return a;
    }

    private static ArticleService serviceWith(Article... articles) {
        List<Article> all = List.of(articles);
        return new ArticleService(new ArticleRepository() {
            @Override
            public List<Article> findAll() {
                return all;
            }

            @Override
            public Optional<Article> findById(String id) {
                return all.stream().filter(a -> a.id.equals(id)).findFirst();
            }
        });
    }

    private static List<String> idsFrom(List<Article> articles) {
        return articles.stream().map(a -> a.id).toList();
    }

    // ---- the boundary -----------------------------------------------------

    @Test
    void shouldServeApprovedArticleToPublicReadingSurface() {
        ArticleService service = serviceWith(article("OK", "approved"));

        assertEquals(List.of("OK"), idsFrom(service.getPublishable()));
    }

    @Test
    void shouldNotServeDraftArticleBecauseGenerationIsNotPublicationApproval() {
        // The central claim of Decision 048, in one assertion.
        ArticleService service = serviceWith(article("DRAFT", "draft"), article("OK", "approved"));

        assertEquals(List.of("OK"), idsFrom(service.getPublishable()),
                "a generated-but-unreviewed article must never reach a reader");
    }

    @Test
    void shouldNotServeArticleStillInReview() {
        ArticleService service = serviceWith(article("PENDING", "in-review"), article("OK", "approved"));

        assertEquals(List.of("OK"), idsFrom(service.getPublishable()));
    }

    @Test
    void shouldNotServeFlaggedArticleThroughAnyPublicPath() {
        // There is deliberately no public route that serves flagged content with
        // a caveat — that would make the flag decorative.
        ArticleService service = serviceWith(article("FLAGGED", "flagged"), article("OK", "approved"));

        assertEquals(List.of("OK"), idsFrom(service.getPublishable()));
        assertTrue(service.getPublishableById("FLAGGED").isEmpty());
    }

    @Test
    void shouldNotServeArticleWithNoReviewRecordAtAll() {
        // Absent review is not approval. An article that reached the system
        // without review metadata is precisely what the boundary exists to catch.
        ArticleService service = serviceWith(article("NO_REVIEW", null), article("OK", "approved"));

        assertEquals(List.of("OK"), idsFrom(service.getPublishable()));
    }

    @Test
    void shouldNotServeArticleWhoseReviewStatusIsUnrecognized() {
        // Never guesses. An unreadable status does not become approved, and does
        // not become draft either — it becomes "not public".
        ArticleService service = serviceWith(article("TYPO", "aproved"), article("OK", "approved"));

        assertEquals(List.of("OK"), idsFrom(service.getPublishable()));
    }

    @Test
    void shouldNotServeUnapprovedArticleByDirectIdLookup() {
        // A direct link to a draft's id must not be the way around the list filter.
        ArticleService service = serviceWith(article("DRAFT", "draft"));

        assertTrue(service.getPublishableById("DRAFT").isEmpty());
    }

    @Test
    void shouldServeApprovedArticleByDirectIdLookup() {
        ArticleService service = serviceWith(article("OK", "approved"));

        assertEquals("OK", service.getPublishableById("OK").orElseThrow().id);
    }

    // ---- withheld is not deleted — the routing seam ------------------------

    @Test
    void shouldKeepUnapprovedArticlesInTheRepositoryForFutureEditorialHandling() {
        // Excluding an article from public queries says nothing about whether it
        // exists. A future administrative editorial queue selects exactly these.
        ArticleRepository repository = new ArticleRepository() {
            private final List<Article> all =
                    List.of(article("DRAFT", "draft"), article("FLAGGED", "flagged"), article("OK", "approved"));

            @Override
            public List<Article> findAll() {
                return all;
            }

            @Override
            public Optional<Article> findById(String id) {
                return all.stream().filter(a -> a.id.equals(id)).findFirst();
            }
        };

        assertEquals(3, repository.findAll().size(), "unapproved articles must remain present");
        assertEquals(List.of("OK"), idsFrom(new ArticleService(repository).getPublishable()));
    }

    // ---- provenance: generator is not reviewer -----------------------------

    @Test
    void shouldRecordGenerationProvenanceSeparatelyFromReviewProvenance() {
        // Draft generation and draft evaluation are separate steps even when both
        // are AI. Two fields is what preserves that at the data layer.
        Article a = article("OK", "approved");
        a.generatedBy = "ai:draft";
        a.editorialReview.reviewer = "human:editorial";

        assertFalse(a.generatedBy.equals(a.editorialReview.reviewer),
                "generator and reviewer are different questions and must be separately recorded");
    }

    // ---- flag disposition lifecycle ---------------------------------------

    private static ReviewFlag flag() {
        ReviewFlag f = new ReviewFlag();
        f.passage = "expected to have a positive impact";
        f.issue = "advocacy";
        f.reason = "Evaluative claim with no source.";
        f.recommendation = "remove";
        return f;
    }

    private static FlagDisposition disposition(String status) {
        FlagDisposition d = new FlagDisposition();
        d.status = status;
        d.date = "2026-08-23";
        d.actor = "human:editorial";
        d.reason = "Recorded reason.";
        return d;
    }

    @Test
    void shouldTreatFlagWithNoDispositionAsOpen() {
        assertTrue(flag().isOpen());
    }

    @Test
    void shouldTreatFullyRecordedDispositionAsClosingTheFlag() {
        ReviewFlag f = flag();
        f.disposition = disposition("overridden");

        assertFalse(f.isOpen());
        assertEquals(DispositionStatus.OVERRIDDEN, f.disposition.resolvedStatus().orElseThrow());
    }

    @Test
    void shouldKeepWithdrawnDistinctFromOverridden() {
        // The distinction the whole lifecycle exists for: one records a reviewer
        // false positive, the other a live objection a human deliberately
        // overruled. Collapsing them corrupts the review history as evidence.
        ReviewFlag withdrawn = flag();
        withdrawn.disposition = disposition("withdrawn");
        ReviewFlag overridden = flag();
        overridden.disposition = disposition("overridden");

        assertEquals(DispositionStatus.WITHDRAWN, withdrawn.disposition.resolvedStatus().orElseThrow());
        assertEquals(DispositionStatus.OVERRIDDEN, overridden.disposition.resolvedStatus().orElseThrow());
        assertFalse(withdrawn.disposition.resolvedStatus().equals(overridden.disposition.resolvedStatus()));
    }

    @Test
    void shouldPreserveTheOriginalFindingWhenAFlagIsDispositioned() {
        // A dispositioned flag is never rewritten and never deleted.
        ReviewFlag f = flag();
        f.disposition = disposition("withdrawn");

        assertEquals("expected to have a positive impact", f.passage);
        assertEquals("advocacy", f.issue);
        assertEquals("Evaluative claim with no source.", f.reason);
        assertEquals("remove", f.recommendation);
    }

    @Test
    void shouldLeaveFlagOpenWhenDispositionStatusIsUnrecognized() {
        // Never guesses, and errs toward the concern still standing.
        ReviewFlag f = flag();
        f.disposition = disposition("withdrawed");

        assertTrue(f.isOpen());
        assertTrue(f.disposition.resolvedStatus().isEmpty());
    }

    @Test
    void shouldLeaveFlagOpenWhenDispositionHasNoReason() {
        // An unexplained disposition is indistinguishable from a flag someone
        // stopped thinking about.
        ReviewFlag f = flag();
        f.disposition = disposition("resolved");
        f.disposition.reason = null;

        assertTrue(f.isOpen());
    }

    @Test
    void shouldLeaveFlagOpenWhenDispositionHasNoActor() {
        ReviewFlag f = flag();
        f.disposition = disposition("resolved");
        f.disposition.actor = "  ";

        assertTrue(f.isOpen(), "status, date, actor and reason must travel together");
    }

    @Test
    void shouldLeaveFlagOpenWhenDispositionHasNoDate() {
        ReviewFlag f = flag();
        f.disposition = disposition("resolved");
        f.disposition.date = null;

        assertTrue(f.isOpen());
    }

    @Test
    void shouldNotLetFlagDispositionAffectPublicationEligibility() {
        // Two lifecycles, deliberately uncoupled. Dispositioning every flag does
        // not approve an article; a human moves the article.
        Article stillFlagged = article("HELD", "flagged");
        ReviewFlag f = flag();
        f.disposition = disposition("overridden");
        stillFlagged.editorialReview.flags = List.of(f);

        assertFalse(stillFlagged.isPublishable(),
                "an overridden flag does not by itself publish the article");
        assertTrue(idsFrom(serviceWith(stillFlagged).getPublishable()).isEmpty());
    }

    @Test
    void shouldServeApprovedArticleThatStillCarriesADispositionedFlag() {
        // The other direction: a preserved flag does not un-publish an article a
        // human approved.
        Article approved = article("OK", "approved");
        ReviewFlag f = flag();
        f.disposition = disposition("overridden");
        approved.editorialReview.flags = List.of(f);

        assertEquals(List.of("OK"), idsFrom(serviceWith(approved).getPublishable()));
        assertEquals(1, approved.editorialReview.flags.size(), "the flag survives publication");
    }

    // ---- ordering ----------------------------------------------------------

    // ---- publishDate is descriptive, never a gate --------------------------

    @Test
    void shouldServeApprovedArticleThatHasNoPublishDate() {
        // The load-bearing case. Serving is gated by approval ALONE. If a null
        // publishDate withheld an article, this field would have become a second
        // publication state through an ordering detail.
        Article undated = article("UNDATED", "approved");
        undated.publishDate = null;

        assertEquals(List.of("UNDATED"), idsFrom(serviceWith(undated).getPublishable()));
        assertTrue(serviceWith(undated).getPublishableDetail("UNDATED").isPresent());
    }

    @Test
    void shouldServeApprovedArticleDatedInTheFuture() {
        // Scheduling is NOT implemented. A future date does not withhold; if it
        // did, scheduling would have arrived by accident and unreviewed.
        Article future = article("FUTURE", "approved");
        future.publishDate = "2099-01-01";

        assertEquals(List.of("FUTURE"), idsFrom(serviceWith(future).getPublishable()));
    }

    @Test
    void shouldSortUndatedApprovedArticlesLastRatherThanDroppingThem() {
        Article dated = article("DATED", "approved");
        dated.publishDate = "2026-08-01";
        Article undated = article("UNDATED", "approved");
        undated.publishDate = null;

        assertEquals(List.of("DATED", "UNDATED"), idsFrom(serviceWith(dated, undated).getPublishable()));
    }

    // ---- the public projection ---------------------------------------------

    @Test
    void shouldNotProjectAnUnapprovedArticleIntoThePublicDetailShape() {
        ArticleService service = serviceWith(article("DRAFT", "draft"));

        assertTrue(service.getPublishableDetail("DRAFT").isEmpty(),
                "the boundary applies before any mapping happens");
    }

    @Test
    void shouldCarryBylineAndDisclosureIntoThePublicDetailShape() {
        // Two public fields; generatedBy has no component to carry it into.
        Article a = article("OK", "approved");
        a.byline = "Admin";
        a.disclosure = "ai-assisted";
        a.generatedBy = "ai";

        var detail = serviceWith(a).getPublishableDetail("OK").orElseThrow();

        assertEquals("Admin", detail.byline());
        assertEquals("ai-assisted", detail.disclosure());
    }

    @Test
    void shouldOrderPublishableArticlesNewestFirst() {
        Article older = article("OLDER", "approved");
        older.publishDate = "2026-06-01";
        Article newer = article("NEWER", "approved");
        newer.publishDate = "2026-08-01";

        assertEquals(List.of("NEWER", "OLDER"), idsFrom(serviceWith(older, newer).getPublishable()));
    }
}
