package org.firststep.backend.ai.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.firststep.backend.ai.dto.DecisionRequest;
import org.firststep.backend.ai.dto.DecisionResponse;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.shared.model.Citation;
import org.firststep.backend.shared.model.ContentSource;
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

    /**
     * Main entry: retrieve relevant local items, then ask the AI to return STRICT JSON.
     */
    public DecisionResponse decide(DecisionRequest request) {
        String q = safeLower(request == null ? null : request.userQuery);
        boolean urgent = request != null && Boolean.TRUE.equals(request.urgent);
        List<String> preferredCategories = request == null ? List.of() : request.preferredCategories;

        if (!aiEnabled) {
            DecisionResponse resp = new DecisionResponse();
            resp.answerTitle = "AI is currently unavailable";
            resp.steps = List.of();
            resp.citations = List.of();
            resp.notes = "AI guidance is not configured on this server.";
            return resp;
        }

        List<Resource> resources = resourceService.getAllResources();
        List<NewsItem> news = newsService.getAllNews();

        // Lightweight retrieval (use existing in-memory lists as the cache)
        List<Resource> topResources = selectTopResources(resources, q, urgent, preferredCategories);
        List<NewsItem> topNews = selectTopNews(news, q, preferredCategories);

        String prompt = buildPrompt(q, urgent, preferredCategories, topResources, topNews);

        try {
            String raw = aiAssistant.generate(prompt, 0.2);
            DecisionResponse response = parseDecisionResponse(raw);
            resolveCitationSources(response.citations, topResources, topNews);
            return response;
        } catch (Exception e) {
            DecisionResponse fallback = new DecisionResponse();
            fallback.answerTitle = "Unable to generate guidance";
            fallback.steps = List.of();
            fallback.citations = List.of();
            fallback.notes = "AI call failed: " + e.getMessage();
            return fallback;
        }
    }

    private List<Resource> selectTopResources(List<Resource> all, String q, boolean urgent, List<String> preferredCategories) {
        if (all == null) return List.of();

        String categoryNeed = preferredCategories == null || preferredCategories.isEmpty() ? null : preferredCategories.get(0);

        List<ResourceScore> scored = new ArrayList<>();
        for (Resource r : all) {
            if (r == null) continue;

            int score = 0;
            score += scoreMatch(q, r.organization);
            score += scoreMatch(q, r.summary);
            score += scoreMatch(q, r.description);
            score += scoreMatch(q, r.category);
            score += scoreMatch(q, r.subcategory);
            score += scoreMatch(q, r.tags);

            if (urgent) {
                if (r.urgency != null) {
                    String u = safeLower(r.urgency);
                    if (u.equals("emergency") || u.equals("time-limited")) {
                        score += 8;
                    }
                }
            }

            if (categoryNeed != null && r.category != null) {
                String cat = safeLower(r.category);
                if (cat.contains(safeLower(categoryNeed))) {
                    score += 5;
                }
            }

            if (score > 0) {
                scored.add(new ResourceScore(r, score));
            }
        }

        scored.sort(Comparator.comparingInt(ResourceScore::score).reversed());

        int limit = 5;
        return scored.stream().limit(limit).map(ResourceScore::resource).toList();
    }

    private List<NewsItem> selectTopNews(List<NewsItem> all, String q, List<String> preferredCategories) {
        if (all == null) return List.of();

        String catNeed = preferredCategories == null || preferredCategories.isEmpty() ? null : preferredCategories.get(0);

        List<NewsScore> scored = new ArrayList<>();
        for (NewsItem n : all) {
            if (n == null) continue;

            int score = 0;
            score += scoreMatch(q, n.title);
            score += scoreMatch(q, n.summary);
            score += scoreMatch(q, n.whyItMatters);
            score += scoreMatch(q, n.tags);

            if (catNeed != null) {
                score += scoreMatch(catNeed, n.tags);
            }

            if (score > 0) {
                scored.add(new NewsScore(n, score));
            }
        }

        scored.sort(Comparator.comparingInt(NewsScore::score).reversed());

        int limit = 3;
        return scored.stream().limit(limit).map(NewsScore::news).toList();
    }

    private int scoreMatch(String q, String field) {
        if (q == null || q.isBlank() || field == null || field.isBlank()) return 0;
        String f = safeLower(field);
        return f.contains(q) ? 5 : 0;
    }

    private int scoreMatch(String q, List<String> fields) {
        if (q == null || q.isBlank() || fields == null) return 0;
        for (String f : fields) {
            int s = scoreMatch(q, f);
            if (s > 0) return s;
        }
        return 0;
    }

    private int scoreMatch(String q, String[] fields) {
        if (q == null || q.isBlank() || fields == null) return 0;
        for (String f : fields) {
            int s = scoreMatch(q, f);
            if (s > 0) return s;
        }
        return 0;
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    private String buildPrompt(
            String q,
            boolean urgent,
            List<String> preferredCategories,
            List<Resource> topResources,
            List<NewsItem> topNews
    ) {

        // Create a compact context block (trimmed fields only)
        StringBuilder ctx = new StringBuilder();
        ctx.append("USER_QUESTION:\n").append(q).append("\n");
        ctx.append("URGENT_FILTER: ").append(urgent).append("\n");
        ctx.append("PREFERRED_CATEGORIES: ").append(preferredCategories == null ? "[]" : preferredCategories).append("\n\n");

        ctx.append("LOCAL_RESOURCES (JSON ARRAY, TRIMMED FIELDS):\n");
        ctx.append(mapperToTrimmedResourcesJson(topResources)).append("\n\n");

        ctx.append("LOCAL_NEWS (JSON ARRAY, TRIMMED FIELDS):\n");
        ctx.append(mapperToTrimmedNewsJson(topNews)).append("\n\n");

        // Strict schema-first output
        return "You are a Decision Aid assistant for Wilmington residents. " +
                "You MUST ONLY use the provided LOCAL_RESOURCES and LOCAL_NEWS context. " +
                "If the context is missing some user-specific details, do NOT ask for external contact unless necessary. Instead, provide best-effort next actions using the closest matching resources from LOCAL_RESOURCES/LOCAL_NEWS. " +
                "Return STRICT JSON with exactly these keys: " +
                "answerTitle, steps, citations, notes. " +
                "Steps is an array of AT MOST 3 objects with keys: order, title, action, why; keep each value to one short sentence. " +
                "Citations is an array of AT MOST 2 objects with keys: sourceType, id, label. " +
                "Be concise so the JSON is complete. No markdown. No extra text. No trailing commas.\n\n" +
                ctx;
    }

    private String mapperToTrimmedResourcesJson(List<Resource> resources) {
        if (resources == null) return "[]";
        List<java.util.Map<String, Object>> trimmed = resources.stream().map(r -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", r.id);
            m.put("organization", r.organization);
            m.put("category", r.category);
            m.put("urgency", r.urgency);
            m.put("summary", r.summary);
            m.put("phone", (r.phones != null && !r.phones.isEmpty()) ? r.phones.get(0).number : null);
            return m;
        }).toList();


        try {
            return mapper.writeValueAsString(trimmed);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String mapperToTrimmedNewsJson(List<NewsItem> news) {
        if (news == null) return "[]";
        List<java.util.Map<String, String>> trimmed = news.stream().map(n -> {
            return java.util.Map.of(
                    "id", n.id,
                    "headline", n.title,
                    "summary", n.summary,
                    "whyItMatters", n.whyItMatters,
                    "urgency", n.urgency,
                    "published", n.published,
                    "sourceName", n.contentSource != null ? n.contentSource.name : ""
            );
        }).toList();


        try {
            return mapper.writeValueAsString(trimmed);
        } catch (Exception e) {
            return "[]";
        }
    }

    private DecisionResponse parseDecisionResponse(String raw) throws Exception {
        String trimmed = raw == null ? "" : raw.trim();

        // Some models may wrap JSON in text; attempt to extract the first {...}
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            trimmed = trimmed.substring(firstBrace, lastBrace + 1);
        }

        // Strip trailing commas before } or ] — common LLM JSON generation mistake
        trimmed = trimmed.replaceAll(",\\s*([}\\]])", "$1");

        // On a token-limited backend the model may be cut off mid-JSON, leaving
        // unclosed arrays/objects. If strict parsing fails, salvage the response by
        // truncating to the last complete top-level element and re-balancing brackets,
        // so fully-formed steps are still returned instead of falling back to an error.
        JsonNode root;
        try {
            root = mapper.readTree(trimmed);
        } catch (Exception e) {
            root = mapper.readTree(repairTruncatedJson(trimmed));
        }
        DecisionResponse resp = mapper.treeToValue(root, DecisionResponse.class);

        if (resp.steps == null) resp.steps = List.of();
        if (resp.citations == null) resp.citations = List.of();
        if (resp.answerTitle == null) resp.answerTitle = "Guidance";
        if (resp.notes == null) resp.notes = "";

        // Ensure step ordering
        resp.steps = resp.steps.stream().sorted(Comparator.comparingInt(s -> s.order)).toList();
        return resp;
    }

    /**
     * Best-effort repair of JSON that was cut off mid-generation. Cuts back to the
     * last completed element (a '}' or a quoted string), removes any trailing comma,
     * then appends the ']' and '}' needed to close still-open arrays/objects. Brackets
     * inside string values are ignored. Returns a balanced string; if nothing usable
     * remains, returns the input unchanged (the caller's readTree will then throw).
     */
    private String repairTruncatedJson(String s) {
        // Find the last structurally "safe" end point: a closing brace or a closed string.
        int lastObj = s.lastIndexOf('}');
        if (lastObj < 0) return s;
        String cut = s.substring(0, lastObj + 1);

        // Walk the salvaged prefix tracking unclosed { and [ (ignoring braces in strings).
        int curly = 0, square = 0;
        boolean inString = false, escaped = false;
        for (int i = 0; i < cut.length(); i++) {
            char c = cut.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') inString = true;
            else if (c == '{') curly++;
            else if (c == '}') curly--;
            else if (c == '[') square++;
            else if (c == ']') square--;
        }

        StringBuilder sb = new StringBuilder(cut);
        while (square-- > 0) sb.append(']');
        while (curly-- > 0) sb.append('}');
        return sb.toString();
    }

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

    private record ResourceScore(Resource resource, int score) {}

    private record NewsScore(NewsItem news, int score) {}

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
