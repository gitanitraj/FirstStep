package org.firststep.backend.news.service;

import org.firststep.backend.news.model.NewsItem;

import java.util.List;

/**
 * RSS-derived content that PASSED relevance assessment and is therefore
 * CivicContent — the discovery feed.
 *
 * <p>Bills the classification engine judged irrelevant to First Step never
 * appear here. For the complete signed-bill feed used by legislation
 * presentation, see {@link SignedLegislationSource}.
 */
public interface RssFeedSource {
    List<NewsItem> getRssItems();
}
