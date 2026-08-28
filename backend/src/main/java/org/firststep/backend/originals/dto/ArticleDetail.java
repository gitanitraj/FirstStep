package org.firststep.backend.originals.dto;

import java.util.List;

/**
 * A First Step Original article as a READER receives it.
 *
 * <p><b>This record is a boundary, and its shape is the enforcement.</b> Ten
 * components out of the nineteen fields an {@code Article} carries. The nine that
 * are absent cannot leak, because there is nowhere to put them — no mapping
 * mistake, no serialization setting and no future refactor of the mapper can add
 * a field a record does not declare.
 *
 * <p>That is the same mechanism by which {@code body} has never leaked through
 * {@code ContentItem} anywhere in this product: {@code ContentItem} has no
 * {@code body} component. Structural, not procedural.
 *
 * <p><b>Deliberately excluded — internal editorial state:</b>
 * <ul>
 *   <li>{@code generatedBy} — generation provenance</li>
 *   <li>{@code editorialReview} in its entirety — status, reviewedDate,
 *       reviewer, every flag, every disposition and its actor and reason</li>
 *   <li>{@code verified}</li>
 * </ul>
 *
 * <p><b>Omitted as unnecessary rather than secret:</b> {@code communityId},
 * {@code status}, {@code expirationDate}, {@code createdDate},
 * {@code contentType} (a detail page has one treatment), {@code tags}
 * (descriptive metadata, never navigation, of no use to a reader), and
 * {@code contentSource} — constant {@code first-step} for every Original, with
 * attribution carried by {@link #byline}.
 *
 * <p><b>Why this is not {@code ContentItem}.</b> {@code ContentItem} is the card
 * shape, shared by topic pages, search, notices and the homepage. Adding
 * {@code body} to it would put a full article on every card payload in the
 * product. Two shapes because there are two jobs.
 */
public record ArticleDetail(

        String id,
        String title,
        String summary,

        /** Why a resident should care. Resident-facing by design, so public. */
        String whyItMatters,

        /** The full text. The reason this endpoint exists. */
        String body,

        /**
         * The public credit — what the reader is told about authorship.
         *
         * <p>This is NOT {@code generatedBy}, which records who composed the
         * prose and stays private. Credit and composition are different
         * questions; see {@link #disclosure} for the third.
         */
        String byline,

        /**
         * A controlled disclosure key, or null.
         *
         * <p>Currently one value: {@code ai-assisted}. The client resolves it to
         * standard translated wording, so the statement reads identically across
         * articles and exists in both languages — free prose would have been
         * English-only and different every time.
         *
         * <p><b>Authored, never derived.</b> Nothing computes this from
         * {@code generatedBy}. It is a deliberate editorial statement about what
         * the reader is told, and it is kept out of {@link #byline} because
         * credit and disclosure are separate claims that must stay separately
         * editable.
         */
        String disclosure,

        /**
         * The date the article BEARS — descriptive, never a gate.
         *
         * <p>Serving is gated by review approval alone. A published article may
         * carry no date at all, and an approved one with a null value here is
         * still served; it simply displays none. Treating a missing or future
         * date as a reason to withhold would turn this field into a second
         * publication state by accident, which is exactly what the review
         * boundary exists to prevent.
         */
        String publishDate,

        /** Last editorial revision. A corrected civic article should show it. */
        String updatedDate,

        /** Breadcrumb back into the taxonomy — e.g. Housing, Legal. */
        List<String> categoryTags,

        /** Breadcrumb to the topic page — e.g. Eviction Prevention. */
        String subcategory
) {
}
