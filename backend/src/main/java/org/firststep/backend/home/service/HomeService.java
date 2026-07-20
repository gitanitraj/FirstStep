package org.firststep.backend.home.service;

import java.util.List;

import org.firststep.backend.category.service.CategoryService;
import org.firststep.backend.home.dto.AiChip;
import org.firststep.backend.home.dto.AiConfig;
import org.firststep.backend.home.dto.HomePayload;
import org.firststep.backend.updates.service.UpdatesService;
import org.springframework.stereotype.Service;

/**
 * Assembles the single homepage payload by composing the existing aggregators
 * (UpdatesService, CategoryService) plus static hero config. No data logic is
 * duplicated here — it only stitches the pieces the homepage needs together.
 */
@Service
public class HomeService {

    // Static hero config. Kept server-side so the homepage's AI form (chips,
    // prompts, placeholder) is backend-owned; the chip `value`s match what
    // HeroGuidance sends to POST /api/decide.
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

    private final UpdatesService updatesService;
    private final CategoryService categoryService;

    public HomeService(UpdatesService updatesService, CategoryService categoryService) {
        this.updatesService = updatesService;
        this.categoryService = categoryService;
    }

    public HomePayload getHome(String communityId) {
        return new HomePayload(
                AI_CONFIG,
                updatesService.getUpdates(),
                categoryService.getAll(communityId));
    }
}
