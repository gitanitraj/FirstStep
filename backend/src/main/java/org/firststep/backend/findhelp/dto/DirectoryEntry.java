package org.firststep.backend.findhelp.dto;

import java.util.List;

/**
 * One organization in the Find Help directory.
 *
 * <p><b>Contact is attributed to the SERVICE, never to the organization.</b>
 * That is Decision 049 surviving contact with a page whose whole job is "who do I
 * call": every phone number in the corpus is service-scoped, so a number under an
 * organization heading would be a fact First Step invented. Each
 * {@link ServiceLine} carries its own, labelled with the service it belongs to.
 *
 * <p>An organization with no services still appears — several are known only
 * through a flyer or an expert answer, and they are no less contactable for it.
 * Their entry is a name and a link, which is honest rather than empty.
 */
public record DirectoryEntry(
        String name,
        String slug,
        /** Empty when the organization is known through content other than a service. */
        List<ServiceLine> services
) {

    /**
     * One service, leading with what it IS rather than who runs it — the page
     * already says who runs it.
     */
    public record ServiceLine(
            /**
             * The service's own name, or NULL when it carries none.
             *
             * <p>Resources derive their title from the organization, so under an
             * organization heading it would repeat the name already above it.
             * Null means "this service has no name distinct from its provider" —
             * the meta line below identifies it instead.
             */
            String title,
            /** Quiet secondary orientation. The primary description is the title. */
            String category,
            String city,
            /** This service's number. Not the organization's. */
            String phone
    ) {
    }
}
