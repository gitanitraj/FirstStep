/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../home/service/HomeService.java
 * Homepage-redesign Step 5c. See references/decisions.md Decision 020 (and 019
 * for the aggregation principle). Keep this mirror in sync with the source.
 * =============================================================================
 *
 * WHAT THIS CLASS IS
 *   The assembler for GET /api/home — the single request that supplies the whole
 *   homepage main column. It returns one HomePayload of { aiConfig, updates,
 *   categories } so the browser makes ONE call on load instead of three.
 *
 * WHY IT EXISTS (Decision 019/020)
 *   "Backend aggregates; frontend displays." Rather than the SPA fetching
 *   /api/updates + /api/categories separately and owning hero config in JS, the
 *   server composes them here. Note this class contains NO data logic of its own
 *   — it delegates entirely to two existing aggregators and adds one static
 *   constant. That's deliberate: HomeService is a composition seam, not a new
 *   source of truth.
 *
 * COMPANION DTOs (trivial records — no separate annotated mirror)
 *   - home/dto/AiChip.java      : record(value, label, urgent)
 *   - home/dto/AiConfig.java    : record(placeholder, suggestedPrompts, chips)
 *   - home/dto/HomePayload.java : record(aiConfig, updates, categories)
 *   - home/controller/HomeController.java : GET /api/home?communityId= →
 *       ApiResponse.success(service.getHome(communityId)) (mirrors CategoryController)
 * ============================================================================= */

package org.firststep.backend.home.service;

import java.util.List;

import org.firststep.backend.category.service.CategoryService;
import org.firststep.backend.home.dto.AiChip;
import org.firststep.backend.home.dto.AiConfig;
import org.firststep.backend.home.dto.HomePayload;
import org.firststep.backend.updates.service.UpdatesService;
import org.springframework.stereotype.Service;

@Service
public class HomeService {

    // STATIC, BACKEND-OWNED hero config. Kept here (not in the React code) so the
    // homepage's AI form is server-driven — Decision 020's answer to "should AI be
    // in /api/home?": only its static setup, never pre-computed guidance (that
    // stays the interactive POST /api/decide). CRITICAL: each chip's `value` must
    // match what HeroGuidance sends to /api/decide — "urgent" flips the urgent
    // flag; "housing"/"essentials" go into preferredCategories.
    private static final AiConfig AI_CONFIG = new AiConfig(
            "E.g., I need rental help near Wilmington for seniors",
            List.of(
                    "I need help paying rent this month",
                    "Where can I find free food near me?",
                    "I just lost my job — where do I start?"),
            List.of(
                    new AiChip("urgent", "🚨 Urgent", true),
                    new AiChip("housing", "🏠 Housing", false),
                    new AiChip("essentials", "🛒 Essentials", false)));

    // The two EXISTING aggregators. UpdatesService (Decision 019) merges the
    // Important Updates feed; CategoryService produces the category summaries with
    // their per-category latestItems/latestPolicyUpdate. No repositories injected
    // directly — this class sits a layer above them.
    private final UpdatesService updatesService;
    private final CategoryService categoryService;

    public HomeService(UpdatesService updatesService, CategoryService categoryService) {
        this.updatesService = updatesService;
        this.categoryService = categoryService;
    }

    // Compose the three pieces. `communityId` is passed straight through to
    // CategoryService (nullable — same contract as GET /api/categories). The
    // updates feed is community-agnostic today, so it ignores the param.
    public HomePayload getHome(String communityId) {
        return new HomePayload(
                AI_CONFIG,
                updatesService.getUpdates(),
                categoryService.getAll(communityId));
    }
}
