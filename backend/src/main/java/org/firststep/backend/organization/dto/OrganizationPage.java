package org.firststep.backend.organization.dto;

import java.util.List;

import org.firststep.backend.shared.dto.ContentItem;

/**
 * One organization and the First Step resources it provides.
 *
 * <p>The shape follows the Front Door spec's Connect pathway:
 * <em>directory → organization → that organization's resources</em>. This is the
 * third step.
 *
 * <p><b>{@code website} is frequently null, by design.</b> Only a website the
 * source data explicitly labels "Organization website" is promoted here; a
 * program page belongs to the service that runs it. A resident who needs a phone
 * number finds it on the resource, where it is true, rather than at the top of
 * the page where it would be a guess.
 *
 * <p>Resources are plain {@link ContentItem}s — the same card shape topic pages,
 * search and notices already use. An organization page is a different QUESTION,
 * not a different kind of card, so it needs no new DTO of its own.
 */
public record OrganizationPage(
        /** Canonical identity. Present so a client can link without re-deriving it. */
        String id,
        String name,
        String slug,
        /** The organization's own site, or null when none is genuinely established. */
        String website,
        /** Everything First Step lists for this organization. */
        List<ContentItem> resources
) {
}
