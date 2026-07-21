package org.firststep.backend.organization.dto;

/**
 * A curated-shortlist organization for the homepage Resource Discovery column.
 *
 * `slug` is a URL-safe form of the name for /organization/{slug} routing (the
 * Organization landing page is built later). `resourceCount` is the current
 * ranking metric — a placeholder; the eventual metric is expected to be driven
 * by policy updates / news, not raw counts.
 */
public record OrgSummary(String name, String slug, int resourceCount) {
}
