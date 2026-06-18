package org.firststep.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.firststep.backend.dto.Citation;
import org.firststep.backend.dto.DecisionRequest;
import org.firststep.backend.dto.DecisionResponse;
import org.firststep.backend.dto.DecisionStep;
import org.firststep.backend.model.NewsItem;
import org.firststep.backend.model.Resource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

@Service
public class DecisionAgentService {

    private final ObjectMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .build();

    private final boolean aiEnabled;
    private final ResourceServiceLike resourceService;
    private final NewsServiceLike newsService;
    private final OllamaService ollamaService;

    public DecisionAgentService(
            @org.springframework.beans.factory.annotation.Value("${ai.enabled:false}") boolean aiEnabled,
            ResourceServiceLike resourceService,
            NewsServiceLike newsService,
            OllamaService ollamaService) {
        this.aiEnabled = aiEnabled;
        this.resourceService = resourceService;
        this.newsService = newsService;
        this.ollamaService = ollamaService;
    }

    /**
     * Main entry: retrieve relevant local items, then ask Ollama to return STRICT JSON.
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
            String raw = ollamaService.generate(prompt, 0.2);
            return parseDecisionResponse(raw);
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
            score += scoreMatch(q, n.headline);
            score += scoreMatch(q, n.summary);
            score += scoreMatch(q, n.whyItMatters);
            score += scoreMatch(q, n.categoryTags);

            if (catNeed != null) {
                score += scoreMatch(catNeed, n.categoryTags);
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
                "Steps is an array of objects with keys: order, title, action, why. " +
                "Citations is an array of objects with keys: sourceType, id, label. " +
                "No markdown. No extra text. No trailing commas.\n\n" +
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
                    "headline", n.headline,
                    "summary", n.summary,
                    "whyItMatters", n.whyItMatters,
                    "urgency", n.urgency,
                    "published", n.published,
                    "sourceName", n.sourceName
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

        JsonNode root = mapper.readTree(trimmed);
        DecisionResponse resp = mapper.treeToValue(root, DecisionResponse.class);

        if (resp.steps == null) resp.steps = List.of();
        if (resp.citations == null) resp.citations = List.of();
        if (resp.answerTitle == null) resp.answerTitle = "Guidance";
        if (resp.notes == null) resp.notes = "";

        // Ensure step ordering
        resp.steps = resp.steps.stream().sorted(Comparator.comparingInt(s -> s.order)).toList();
        return resp;
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

