package org.firststep.backend.ai.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// DecisionAgentService is First Step's AI decision-aid: given a resident's
// free-text question, it retrieves the most relevant local Resources and
// NewsItems (a lightweight in-memory keyword-scoring retrieval, not a real
// search index), builds a prompt constraining the model to ONLY use that
// retrieved context, calls AiAssistant, and parses the model's STRICT JSON
// response into a DecisionResponse (answerTitle, steps, citations, notes).
// This is the most complex class in the codebase — the only place doing
// retrieval, prompt engineering, and defensive JSON parsing/repair of
// LLM output — and had no annotated reference doc until this migration.
// =============================================================================

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.firststep.backend.ai.dto.DecisionRequest;
import org.firststep.backend.ai.dto.DecisionResponse;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.shared.model.Citation;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.util.TextScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

@Service
public class DecisionAgentService {

    private static final Logger log = LoggerFactory.getLogger(DecisionAgentService.class);

    private final ObjectMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .build();

    private final boolean aiEnabled;
    private final ResourceServiceLike resourceService;
    private final NewsServiceLike newsService;
    private final AiAssistant aiAssistant;

    public DecisionAgentService(
            @org.springframework.beans.factory.annotation.Value("${ai.enabled:false}") boolean aiEnabled,
            ResourceServiceLike resourceService,
            NewsServiceLike newsService,
            AiAssistant aiAssistant) {
        this.aiEnabled = aiEnabled;
        this.resourceService = resourceService;
        this.newsService = newsService;
        this.aiAssistant = aiAssistant;
    }

    // See DecisionAgentService.java for the full method bodies (decide,
    // selectTopResources, selectTopNews, buildPrompt,
    // mapperToTrimmedResourcesJson/NewsJson, parseDecisionResponse,
    // repairTruncatedJson) — unchanged by the Search-slice pass except that
    // selectTopResources/selectTopNews now call TextScore.match(...)/
    // TextScore.lower(...) instead of this class's own private scoreMatch
    // (3 overloads) and safeLower, which were EXTRACTED (not duplicated) to
    // shared/util/TextScore.java when the new search/ slice needed the same
    // substring-scoring logic — see TextScore_annotated.java. This is a
    // behavior-preserving move: same substring-containment, flat-5-points-
    // per-field, first-match-wins-for-lists semantics, just relocated so a
    // second consumer (SearchService) doesn't have to reimplement it.
    // resolveCitationSources is documented in detail below. This reference
    // focuses on WHY each piece exists and HOW they interact, per governance;
    // it does not re-paste every line (see the production file for that).

    /**
     * Links each citation the model produced back to the real ContentSource of
     * the Resource/NewsItem it claims to cite, by matching Citation.id against
     * the same topResources/topNews lists that were fed into the prompt. Logs
     * at DEBUG which citation ids matched vs. didn't — over time this signal
     * shows whether the model consistently hallucinates ids, or whether
     * certain source types never get cited, which is useful input for how
     * Flyer/Expert/Search content gets cited once those slices exist.
     */
    private void resolveCitationSources(List<Citation> citations, List<Resource> topResources, List<NewsItem> topNews) {
        if (citations == null) return;

        for (Citation citation : citations) {
            ContentSource matched = null;

            for (Resource r : topResources) {
                if (r.id != null && r.id.equals(citation.id)) {
                    matched = r.contentSource;
                    break;
                }
            }
            if (matched == null) {
                for (NewsItem n : topNews) {
                    if (n.id != null && n.id.equals(citation.id)) {
                        matched = n.contentSource;
                        break;
                    }
                }
            }

            citation.contentSource = matched;
            if (matched != null) {
                log.debug("Citation {} matched a real source: {}", citation.id, matched.name);
            } else {
                log.debug("Citation {} did not match any retrieved resource/news item (possible hallucination)", citation.id);
            }
        }
    }

    /**
     * Tiny adapters so we don't have to depend on concrete services in signatures.
     */
    public interface ResourceServiceLike {
        List<Resource> getAllResources();
    }

    public interface NewsServiceLike {
        List<NewsItem> getAllNews();
    }

}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// TEXTSCORE EXTRACTION (Search-slice pass, decisions.md Decision 012): this
// class's private scoreMatch/safeLower were the only substring-scoring logic
// in the codebase until search/service/SearchService needed the identical
// behavior for its own cross-CivicContent matching. Rather than copy-paste
// a second implementation, the logic moved to shared/util/TextScore.java —
// this file's only change is calling that utility instead of its own
// removed private methods. This is the one place the Search-slice work
// touched pre-existing, already-tested code; DecisionAgentServiceTest was
// re-run and confirmed unchanged in behavior after the move.
//
// PACKAGE MOVE: org.firststep.backend.service -> org.firststep.backend.ai.service,
// alongside DecisionController and the DecisionRequest/Response/Step DTOs
// (now ai/dto/, no longer dto/ — that package is deleted entirely, it had
// nothing else in it). Internal retrieval/prompt/parsing logic is completely
// unchanged by the move — only imports (Resource/NewsItem now come from
// their own slices; Citation from shared.model; AiAssistant from this same
// ai.service package now, so no import needed).
//
// NEW BEHAVIOR — resolveCitationSources: this is what the original request
// named as a gap: "Citation... How a delivered answer points back to its
// ContentSource" was previously unimplemented — Citation had a contentSource
// field (added in Step 1) that nothing ever populated. This method closes
// that gap. It runs AFTER parseDecisionResponse (so it operates on the
// model's already-parsed citations) and BEFORE the response is returned to
// the controller, using the SAME topResources/topNews lists that were
// retrieved and fed into the prompt — not a fresh lookup against the full
// dataset — because those are the only items the model was allowed to cite
// from in the first place; matching against the full dataset would let a
// hallucinated-but-coincidentally-real id resolve to an item the model never
// actually saw.
//
// DEBUG-level logging (not INFO/WARN): citation mismatches are expected and
// routine (models do sometimes invent ids, or cite an item that scored just
// below the top-N cutoff) — not actionable on their own, so they shouldn't
// be operational noise at default log levels. The value is aggregate, over
// time: enabling DEBUG for this logger and tallying hit/miss rates would
// reveal whether hallucination is a persistent problem worth prompt-tuning,
// which is exactly the kind of signal worth having before Flyer/Expert/
// Search need their own citation story.
//
// WHY match by iterating topResources then topNews, not a Map: both lists
// are already tiny (5 and 3 items respectively, per the existing limit=5/
// limit=3 caps in selectTopResources/selectTopNews) — building a Map for
// O(1) lookup would be optimizing a loop that runs at most 8 times per
// citation, for at most 2 citations per response (the prompt caps citations
// at 2). A Map here would be complexity without a measurable benefit.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Depends on AiAssistant (interface, ai.service) for the actual LLM call —
//   decoupled from any concrete provider (see AiAssistant_annotated.java).
// - Depends on ResourceServiceLike/NewsServiceLike (nested marker interfaces
//   this class owns) — implemented by resource.service.ResourceService and
//   news.service.NewsService respectively. This is a cross-slice dependency
//   direction (resource/news slices implement a type the ai slice owns) —
//   pre-existing from v1, carried over unchanged; a future cleanup could
//   invert this (ai slice depends on resource/news's own repository
//   interfaces instead), but that's out of this migration's "move code
//   as-is" scope.
// - DecisionController is the only caller of decide().
// - Citation.contentSource (populated here) and ContentSource itself are
//   shared-kernel types (shared.model) — see their annotated docs for the
//   domain-model side of this relationship.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Resolving citations against the FULL Resource/NewsItem dataset instead
//   of just topResources/topNews: rejected — would let a hallucinated id
//   that happens to match a real item ANYWHERE in the dataset resolve
//   successfully, masking exactly the hallucination signal this feature
//   exists to surface. Scoping to what was actually retrieved is the whole
//   point.
// - Throwing/rejecting the response when a citation doesn't match: rejected
//   — a citation with no matching source is still useful to the resident
//   (the label/sourceType/id the model produced may still be meaningful even
//   without a resolved ContentSource), and failing the whole response over
//   one bad citation would be a worse user experience than the existing
//   graceful degradation this class already practices elsewhere (e.g. the
//   AI-disabled and AI-call-failed fallback paths).
// =============================================================================
