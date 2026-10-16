package org.firststep.backend.home.dto;

/**
 * A hero AI-guidance chip, backend-owned so the homepage's suggested filters are
 * driven by the server rather than hardcoded in the frontend.
 *
 * `value` is what the chip contributes to a DecisionRequest: for an urgent chip
 * it toggles the `urgent` flag; otherwise `value` is added to preferredCategories.
 */
public record AiChip(String value, String label, boolean urgent) {
}
