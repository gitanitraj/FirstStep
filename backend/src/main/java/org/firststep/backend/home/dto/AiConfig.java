package org.firststep.backend.home.dto;

import java.util.List;

/**
 * Static, backend-owned configuration for the hero's AI guidance widget:
 * the input placeholder, a few example prompts, and the filter chips. The
 * actual guidance is still produced on demand by POST /api/decide — this only
 * describes the form, so the frontend doesn't hardcode it.
 */
public record AiConfig(
        String placeholder,
        List<String> suggestedPrompts,
        List<AiChip> chips
) {
}
