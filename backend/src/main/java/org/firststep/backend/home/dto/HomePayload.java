package org.firststep.backend.home.dto;

import java.util.List;

import org.firststep.backend.category.dto.CategorySummary;
import org.firststep.backend.organization.dto.OrgSummary;
import org.firststep.backend.updates.dto.UpdateItem;

/**
 * Everything the homepage needs in a single response, so the SPA makes one
 * request on load rather than stitching several endpoints together (backend
 * aggregates, frontend displays — see references/decisions.md 019/020).
 *
 * `aiConfig` is static hero config; `updates` is the Important Updates feed
 * (also pollable standalone at /api/updates); `categories` + `organizations`
 * drive the Resource Discovery columns. AI guidance itself stays the interactive
 * POST /api/decide.
 */
public record HomePayload(
        AiConfig aiConfig,
        List<UpdateItem> updates,
        List<CategorySummary> categories,
        List<OrgSummary> organizations
) {
}
