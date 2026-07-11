package org.firststep.backend.shared.util;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// TextScore is a small, stateless substring-matching/scoring primitive:
// "does this field contain this query, case-insensitively, and if so, how
// many points is that worth." It has three match() overloads (single
// String field, List<String> fields, String[] fields) plus a lower()
// helper. Used by both ai/service/DecisionAgentService (AI-prompt
// retrieval) and search/service/SearchService (the /api/search endpoint).
// =============================================================================

import java.util.List;
import java.util.Locale;

public final class TextScore {

    private TextScore() {}

    public static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    public static int match(String query, String field) {
        String q = lower(query);
        if (q.isBlank() || field == null || field.isBlank()) return 0;
        String f = lower(field);
        return f.contains(q) ? 5 : 0;
    }

    public static int match(String query, List<String> fields) {
        String q = lower(query);
        if (q.isBlank() || fields == null) return 0;
        for (String f : fields) {
            int s = match(q, f);
            if (s > 0) return s;
        }
        return 0;
    }

    public static int match(String query, String[] fields) {
        String q = lower(query);
        if (q.isBlank() || fields == null) return 0;
        for (String f : fields) {
            int s = match(q, f);
            if (s > 0) return s;
        }
        return 0;
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// EXTRACTED, NOT NEW: this is a byte-for-byte-equivalent move of
// DecisionAgentService's private scoreMatch (3 overloads) + safeLower,
// which existed there first for AI-prompt retrieval. When search/service/
// SearchService needed the identical substring-scoring behavior for a
// second, independent purpose (a real user-facing search endpoint, not
// retrieval-for-an-LLM-prompt), duplicating ~25 lines of proven logic was
// weighed against extracting it — extraction won because this is a genuine
// second call site (not speculative reuse), and the two call sites having
// silently different scoring behavior over time (e.g. one gets a bugfix,
// the other doesn't) was a real risk worth avoiding. See
// references/decisions.md Decision 012 for the full tradeoff discussion,
// including why DecisionAgentService itself was touched despite the
// project's general "don't refactor working code" bias.
//
// FLAT +5 PER MATCH, SUBSTRING CONTAINMENT, NO FUZZY/TF-IDF: this was
// DecisionAgentService's existing scoring convention, carried over
// unchanged. Not reconsidered/redesigned during the extraction — the goal
// was relocating proven logic, not improving it. A smarter relevance
// scheme (weighted fields, fuzzy matching, TF-IDF) is a real future
// improvement but out of scope for this move.
//
// FIRST-MATCH-WINS FOR List<String>/String[] OVERLOADS: matches
// DecisionAgentService's original behavior exactly — a list/array field
// (e.g. tags) contributes at most one match's worth of score (5), not one
// per matching element. SearchService compensates for this at the
// call-site level by summing across DIFFERENT fields (organization +
// summary + tags, etc.) rather than asking TextScore to sum WITHIN a list
// field — preserving this method's tested semantics while still rewarding
// multi-field relevance one level up. See SearchService_annotated.java.
//
// match(String,String) LOWERCASES THE QUERY INTERNALLY (new, small
// behavior addition over the original private scoreMatch): the original
// only lowercased the field, because its one caller (DecisionAgentService)
// always pre-lowered the query once at the top of decide(). Making match()
// lowercase defensively is a no-op for that existing caller (lowercasing
// an already-lowercase string is idempotent) but means SearchService can
// pass a raw, unprocessed query string without needing to know that
// convention — a small correctness improvement that comes for free with
// the extraction, not a design change to the original algorithm.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - ai/service/DecisionAgentService.selectTopResources/selectTopNews call
//   TextScore.match(...) once per candidate field, summing the results.
// - search/service/SearchService does the same, for Resource/NewsItem/Flyer.
// - No Spring annotations, no state, no dependencies — a plain static
//   utility class (private constructor, `final`), safe to call from
//   anywhere without DI.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Leaving DecisionAgentService untouched and giving SearchService its own
//   independent copy: this was presented to the user as a real option
//   (lower risk — zero chance of destabilizing DecisionAgentService) but
//   explicitly NOT chosen; the user opted for the shared-util extraction
//   to avoid long-term drift between two copies of the same logic.
// - A richer relevance/ranking library or algorithm: rejected as out of
//   scope for a move whose entire point was relocating existing, proven
//   logic without changing its behavior.
// =============================================================================
