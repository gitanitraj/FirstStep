package org.firststep.backend.home.service;

import java.util.ArrayList;
import java.util.List;

import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.home.dto.AiChip;
import org.firststep.backend.home.dto.AiConfig;
import org.firststep.backend.home.dto.HomePayload;
import org.firststep.backend.legislation.service.LegislationService;
import org.firststep.backend.shared.dto.ContentItem;
import org.firststep.backend.shared.model.ContentSource;
import org.springframework.stereotype.Service;

/**
 * Assembles the single homepage payload by composing the existing aggregators
 * plus static hero config. No data logic is duplicated here — it only stitches
 * the pieces the homepage needs together.
 *
 * <p><b>Slice H reshaped what "the pieces" are.</b> The homepage is now a front
 * door: a small number of progressively deeper choices, with the complexity
 * behind it on destination pages. So this service composes SIX sections instead
 * of the old five, and dropped organizations entirely — see {@link HomePayload}.
 */
@Service
public class HomeService {

    /** ContentSource.id marking content First Step produced itself. */
    private static final String FIRST_STEP = "first-step";

    // Static hero config. Kept server-side so the homepage's AI form (chips,
    // prompts, placeholder) is backend-owned; the chip `value`s match what
    // the AI search sends to POST /api/decide.
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

    private final PathwayService pathwayService;
    private final FaqService faqService;
    private final LegislationService legislationService;
    private final FlyerService flyerService;

    public HomeService(PathwayService pathwayService,
            FaqService faqService, LegislationService legislationService,
            FlyerService flyerService) {
        this.pathwayService = pathwayService;
        this.faqService = faqService;
        this.legislationService = legislationService;
        this.flyerService = flyerService;
    }

    public HomePayload getHome() {
        return new HomePayload(
                AI_CONFIG,
                pathwayService.getCommunityResources(),
                originals(),
                legislationService.getRecentSignedBills(),
                flyerService.getCarouselCards());
    }

    /**
     * First Step Originals — CivicContent First Step produced itself, identified
     * by {@code contentSource.id}, never by a new ContentType or domain class
     * (Decision 041). "Originals" describes WHO MADE the content, and
     * ContentSource is the field that already answers that question.
     *
     * <p>Today that is curated FAQs and nothing else, which is why the filter
     * lives here as a private method rather than in an OriginalsService: it has
     * exactly one source, and this codebase introduces the service at the SECOND
     * one (the F4 → F5a rule). When briefings or data stories arrive, this method
     * is what becomes that service.
     *
     * <p>Note what this does NOT do: it does not treat every EXPERT item as an
     * original. Expert answers carry {@code contentSource.name = "Delaware
     * Volunteer Legal Services"} — same contentType, different producer. That
     * distinction is the entire argument for using ContentSource here.
     */
    private List<ContentItem> originals() {
        List<ContentItem> items = new ArrayList<>();
        for (FAQ faq : faqService.getAll()) {
            ContentSource source = faq.contentSource;
            if (source == null || !FIRST_STEP.equals(source.id)) {
                continue;
            }
            items.add(new ContentItem(
                    faq.contentType,
                    faq.id,
                    faq.title,
                    faq.summary,
                    source.name,
                    null, null, null,
                    faq.updatedDate,
                    // No url: this is First Step's own content, so there is no
                    // originating organization to send the resident to.
                    null));
        }
        return List.copyOf(items);
    }
}
