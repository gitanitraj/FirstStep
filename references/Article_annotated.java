/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../originals/model/Article.java
 * Slice K. See references/decisions.md Decision 048 (review) and 047 (evidence).
 * =============================================================================
 *
 * WHAT IT IS
 * ----------
 * A First Step Original article — editorial content First Step wrote in its own
 * voice, as opposed to civic information it is relaying from someone else.
 *
 * THE MEASUREMENT THAT DECIDED THE ARCHITECTURE
 * ---------------------------------------------
 * The cheap option was to author articles in news.json as NewsItems carrying
 * source_id "first-step", reusing an entire existing pipeline for free. It was
 * checked before being chosen, and the check killed it:
 *
 *     NewsService.getAll() feeds SIX consumers — CategoryService, SearchService,
 *     NavigationService, UpdatesService, CommunityNoticesService.
 *
 * An unapproved draft would therefore have appeared in category pages and in
 * search results the moment it was authored. The approval filter would have had
 * to be repeated in six places, and remembered in every consumer added later.
 *
 * A BOUNDARY THAT LIVES IN SIX PLACES IS NOT A BOUNDARY. Its own dedicated
 * pipeline means the only route to an article is ArticleService, which owns the
 * rule. That is the entire justification for a seventh JSON loader — a real cost
 * (tech-debt item 1 already flags six), accepted because leaking unreviewed
 * editorial content into public search is a far larger one.
 *
 * This is worth generalizing: WHEN CONTENT NEEDS A GATE, THE GATE HAS TO SIT ON
 * A PATH NOTHING ELSE BYPASSES. Reuse that widens the number of doors is not
 * reuse, it is a leak with good intentions.
 *
 * WHY body EXISTS HERE AND NOWHERE ELSE
 * -------------------------------------
 * Before Slice K, First Step had never displayed the full text of ANYTHING.
 * NewsItem.body is authored, required by the validator, stored — and dropped at
 * the DTO boundary; it appears in no DTO and no client surface. Every existing
 * surface either links out to the producing organization or shows a summary.
 *
 * An article is the first content First Step HOSTS rather than points at. That
 * single fact is why it needs BOTH a reading surface (there is nowhere to send
 * the reader) and a review gate (there is no producer whose judgment First Step
 * is relaying — the judgment is First Step's own).
 *
 * WHY contentType IS NEWS AND NOT A NEW `ARTICLE` VALUE
 * -----------------------------------------------------
 * Deliberately deferred. ContentType drives PRESENTATION — "which card
 * treatment, which detail view." Until the reading surface exists there is no
 * distinct treatment for an ARTICLE value to select, so adding it now would be
 * an enum value that changes nothing. It is added when it changes what renders.
 *
 * isPublishable() TREATS A MISSING REVIEW AS NOT APPROVED
 * -------------------------------------------------------
 * A null editorialReview answers false. An article that reached the system with
 * no review metadata is precisely the case the boundary exists to catch, and
 * defaulting it to publishable would invert the decision in exactly the
 * situation where the system knows least.
 *
 * SLICE K ADDITION — byline AND disclosure
 * ----------------------------------------
 * Two PUBLIC fields joined generatedBy, and the three answer different questions:
 *
 *   byline       what the reader is told about authorship        PUBLIC
 *   disclosure   a controlled key -> standard translated wording PUBLIC
 *   generatedBy  who actually composed the prose                 PRIVATE
 *
 * byline is NOT derived from generatedBy. An article composed by AI and published
 * under an editorial byline is two true facts about different things, and
 * collapsing them would force a choice between them.
 *
 * disclosure is authored rather than computed, deliberately. Deriving it from
 * generatedBy would make a private field decide a public statement, and would
 * leave editorial unable to decide what a reader is told. It is also kept OUT of
 * the byline: credit and disclosure are separate claims that must stay
 * separately editable.
 * ============================================================================= */

package org.firststep.backend.originals.model;

import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A First Step Original article — editorial content First Step wrote in its own
 * voice, rather than civic information it is relaying from someone else.
 *
 * <p><b>Why articles have their own file, repository and service.</b> The obvious
 * cheaper option was to author them in news.json as NewsItems carrying
 * {@code source_id: "first-step"}, reusing an entire existing pipeline. That was
 * measured and rejected: {@code NewsService.getAll()} feeds SIX consumers —
 * CategoryService, SearchService, NavigationService, UpdatesService,
 * CommunityNoticesService — so an unapproved draft would surface in category
 * pages and search, and the approval filter would have to be repeated in six
 * places and remembered in every future one.
 *
 * <p>The boundary has to live in ONE place or it is not a boundary. A separate
 * pipeline means the only route to an article is through {@code ArticleService},
 * which owns the approval rule. The cost is a seventh JSON loader (tech-debt item
 * 1, deferred); leaking unreviewed editorial content into public search is the
 * larger cost by a wide margin.
 *
 * <p><b>Two different provenance questions, two fields.</b> {@link #generatedBy}
 * records who composed the prose; {@code editorialReview.reviewer} records who
 * evaluated it. Both may be AI and they must not be the same step. Neither is
 * news.json's {@code author} field, which despite its name means INGESTION method
 * ({@code manual}/{@code rss}/{@code api}) — how a record entered the system, a
 * third question again. An AI-drafted article a human pastes into a file is
 * manual by ingestion and AI by composition; both are true, about different
 * things.
 *
 * <p><b>{@link #body} is why this type exists at all.</b> Before Slice K, First
 * Step had never displayed the full text of anything — every surface either
 * linked out to the producing organization or showed a summary, and
 * {@code NewsItem.body} was authored, validated, then dropped at the DTO
 * boundary. An article is the first content First Step HOSTS rather than points
 * at, which is what makes it need both a reading surface and a review gate.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Article extends CivicContent {

    public Article() {
        // Articles present as news-style editorial content. A dedicated ARTICLE
        // ContentType is deliberately NOT introduced here: contentType drives
        // PRESENTATION, and until the reading surface exists there is no distinct
        // treatment for it to select. It is added when it changes what renders.
        this.contentType = ContentType.NEWS;
    }

    /** The full text. Blank lines separate paragraphs. */
    public String body;

    /** One sentence on why a resident should care — the NewsItem contract, kept. */
    public String whyItMatters;

    /**
     * GENERATION provenance: who composed this prose. Distinct from who reviewed
     * it and from how the record was ingested. See the class comment.
     *
     * <p><b>PRIVATE.</b> It has no component in {@code ArticleDetail} and never
     * reaches a reader.
     */
    public String generatedBy;

    /**
     * The PUBLIC credit — what a reader is told about authorship.
     *
     * <p>The fourth provenance question and the only public one. It is not
     * derived from {@link #generatedBy} and has no relationship to it: an article
     * composed by AI and published under an editorial byline is two true facts
     * about different things.
     */
    public String byline;

    /**
     * A controlled public disclosure key, or null. Currently {@code ai-assisted}.
     *
     * <p><b>Authored, never derived.</b> Nothing computes this from
     * {@link #generatedBy} — that field is private and stays private. This is a
     * deliberate editorial statement about what the reader is told, which is why
     * it is a field an author sets rather than a value the system infers.
     *
     * <p>Kept OUT of {@link #byline} on purpose: credit and disclosure are
     * different claims and must remain separately editable.
     *
     * <p>A key rather than prose, so the wording is standard across articles and
     * translatable. It stays a String rather than an enum on the same reasoning
     * as {@code ReviewFlag.issue}: one value today, nothing branches on it, and a
     * vocabulary fixed before it has grown tends to be fixed wrongly.
     */
    public String disclosure;

    /**
     * The editorial review record. <b>Never null in practice</b>, and treated as
     * "not approved" when it is — see {@link #isPublishable()}.
     */
    public EditorialReview editorialReview;

    /**
     * May a public reading surface serve this article?
     *
     * <p>A missing review record answers FALSE. An article that somehow reached
     * the system without review metadata is exactly the case this boundary exists
     * to catch, and defaulting it to publishable would invert the decision.
     */
    public boolean isPublishable() {
        return editorialReview != null && editorialReview.isApproved();
    }
}
