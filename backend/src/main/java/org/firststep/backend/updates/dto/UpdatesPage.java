package org.firststep.backend.updates.dto;

import java.util.List;

import org.firststep.backend.shared.model.Sector;

/**
 * A whole sector's updates in one response — the BFF for both destination pages.
 *
 * <pre>
 *   /updates            sector = GOVERNMENT   Latest Updates
 *   /community-notices  sector = COMMUNITY    Community Notices
 * </pre>
 *
 * <p><b>Two pages, one endpoint and one record.</b> They have the same shape
 * because the only thing separating them is who published the content — so a
 * second DTO would have been the same fields under a different name. What differs
 * is the sector, and that is a parameter.
 *
 * <p><b>Empty groups cannot reach the client.</b> They are never built, so the
 * "do not render empty groups" rule (Decision 045) is guaranteed by the payload
 * rather than by a guard the frontend has to remember. A sector with no laws
 * simply has no LAW group.
 *
 * <p>{@code totalCount} counts the sector, not the groups, so a page can say
 * "14 updates" without the client summing anything — and it stays correct if
 * groups are ever capped.
 */
public record UpdatesPage(
        Sector sector,
        int totalCount,
        List<UpdateGroup> groups
) {
}
