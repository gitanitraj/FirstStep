package org.firststep.backend.updates.dto;

import java.util.List;

import org.firststep.backend.shared.model.ContentType;

/**
 * One content type's worth of updates, within a sector page.
 *
 * <p><b>This is a PRESENTATION grouping over controlled metadata, not a domain
 * concept.</b> Decision 045 amended Decision 041 to permit exactly this: grouping
 * existing CivicContent by {@code contentType} is a meaningful discovery model —
 * a resident scanning for "what laws changed" is doing something different from
 * scanning for "what did organisations announce" — and expressing it costs no new
 * domain type.
 *
 * <p>The constraints that keep it honest, all enforced elsewhere in this slice:
 * <ul>
 *   <li>{@code contentType} is EXISTING domain metadata; no group invents one.</li>
 *   <li>Groups are generated from the content present, never enumerated.</li>
 *   <li>An empty group is never built, so it can never be rendered.</li>
 *   <li>ONE generic component renders every group — no component per type.</li>
 *   <li>Items stay reverse-chronological WITHIN the group.</li>
 * </ul>
 *
 * <p>{@code count} is the group's own size rather than something the client
 * derives, so a future capped group cannot make the heading disagree with the
 * list beneath it.
 */
public record UpdateGroup(
        ContentType contentType,
        int count,
        List<UpdateItem> items
) {
}
