/**
 * Search vertical slice — GET /api/search?q=...&communityId=... searching
 * across Resource, NewsItem, and Flyer in one community-aware, ranked list.
 *
 * SearchService composes the existing ResourceService/NewsService/
 * FlyerService (not their repositories directly) and scores matches using
 * shared.util.TextScore — the same substring-scoring primitive
 * ai/service/DecisionAgentService uses for its own AI-prompt retrieval,
 * extracted so both slices share one implementation. See
 * references/decisions.md's Decision 012.
 */
package org.firststep.backend.search;
