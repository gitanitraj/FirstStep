package org.firststep.backend.news.service;

import java.util.List;

import org.firststep.backend.news.model.NewsItem;

/**
 * The complete signed-legislation feed — every bill, whether or not it is
 * relevant to First Step's mission.
 *
 * <p><b>Why this is separate from {@link RssFeedSource}.</b> The two answer
 * different questions and must not be able to change each other:
 *
 * <ul>
 *   <li>{@code RssFeedSource} — "what civic content did we admit?" Relevance-gated.
 *       Feeds discovery: updates, categories, search, AI retrieval.</li>
 *   <li>{@code SignedLegislationSource} — "what has the Governor signed?" Ungated.
 *       Feeds the Delaware Laws rotator, which presents legislation rather than
 *       curating discovery.</li>
 * </ul>
 *
 * <p>Before Slice F2.1 one accessor served both, so introducing a relevance gate
 * would silently have emptied the rotator of every bill the classifier could not
 * categorize — a presentation feature broken by a discovery decision. Splitting
 * them keeps editorial taxonomy, source adaptation and legislation presentation
 * as three independent concerns.
 *
 * <p>Single-method by design, so tests can supply a lambda.
 */
public interface SignedLegislationSource {
    List<NewsItem> getSignedBills();
}
