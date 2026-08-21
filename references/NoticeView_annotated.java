/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../notices/model/NoticeView.java
 * Slice J. See references/decisions.md Decision 046.
 * =============================================================================
 *
 * WHAT IT IS
 * ----------
 * The five states of the Community Notices page, as an enum: one landing state
 * (OVERVIEW) plus four resident-facing discovery views. The URL resolves to one
 * of these, and everything downstream — service, DTO, nav card — keys off it.
 *
 * WHY AN ENUM RATHER THAN A STRING
 * --------------------------------
 * The set of views is closed and known at compile time. An enum makes an unknown
 * view UNREPRESENTABLE past the controller boundary: once you hold a NoticeView,
 * it is one of five, and the switch in the service cannot be handed a typo. A
 * string would have pushed that check into every consumer.
 *
 * WHY THE `kind` FIELD IS NULLABLE — the design decision worth understanding
 * -------------------------------------------------------------------------
 * Three views select on a controlled kind carried in `tags`. FLYERS selects on
 * `contentType`, an axis that already existed. OVERVIEW selects nothing.
 *
 * That asymmetry is DELIBERATE and is the whole reason the views overlap rather
 * than partition. "Flyer" is not a kind of notice — it is a FORM a notice takes.
 * A health-fair flyer is genuinely an event AND genuinely a flyer, so it appears
 * in both views. Adding a fourth kind "flyer" would have encoded the same fact
 * on two axes and let the two copies disagree.
 *
 * `kind()` returns Optional rather than exposing the null, so a caller cannot
 * accidentally compare against it without deciding what "no kind" means.
 *
 * WHY fromKey RETURNS Optional AND NEVER DEFAULTS
 * -----------------------------------------------
 * A blank key IS the landing route, so that maps to OVERVIEW. An UNRECOGNIZED
 * key maps to nothing, so the controller can 404. The alternative — silently
 * returning OVERVIEW for anything unknown — would tell a resident following a
 * broken link that nothing went wrong. Same never-guess rule as
 * ContentSourceService.fromKey (Decision 045).
 *
 * ALTERNATIVE CONSIDERED: four separate enums or marker interfaces per view.
 * Rejected — the views differ only in what they select and how they sort, and
 * both of those are data, not behavior worth subclassing.
 * ============================================================================= */

package org.firststep.backend.notices.model;

import java.util.Locale;
import java.util.Optional;

/**
 * The five Community Notices views — one landing plus four discovery lenses.
 *
 * <p><b>These are LENSES, not buckets.</b> A flyer advertising a health fair
 * carries kind {@code event} and appears in BOTH {@link #EVENTS} and
 * {@link #FLYERS}, because they answer different resident questions — "what is
 * happening?" and "what posters are up?". Category and topic pages already
 * overlap the same way.
 *
 * <p><b>Note the asymmetry, which is deliberate.</b> Three views select on a
 * controlled kind carried in {@code tags}; FLYERS selects on {@code contentType},
 * an axis that already existed. That is why the vocabulary in taxonomy.json has
 * three entries rather than four: "flyer" is not a kind of notice, it is a form
 * the notice takes.
 *
 * <p>The URL is the source of truth for the active view, so this enum is what the
 * route resolves to. {@link #fromKey} returns empty rather than defaulting, so an
 * unknown view 404s instead of silently rendering the landing page — a resident
 * following a bad link should be told, not quietly redirected.
 */
public enum NoticeView {

    /** The landing route: four cards, counts and a preview of each. Never a redirect. */
    OVERVIEW(null),

    EVENTS("event"),
    MEETINGS("meeting"),
    ANNOUNCEMENTS("announcement"),

    /** Selects on contentType, not on a kind — hence the null. */
    FLYERS(null);

    private final String kind;

    NoticeView(String kind) {
        this.kind = kind;
    }

    /** The notice kind this view selects, or empty for OVERVIEW and FLYERS. */
    public Optional<String> kind() {
        return Optional.ofNullable(kind);
    }

    /** URL spelling — {@code events}, {@code meetings}, … */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<NoticeView> fromKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.of(OVERVIEW);
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (NoticeView view : values()) {
            if (view.key().equals(normalized)) {
                return Optional.of(view);
            }
        }
        return Optional.empty();
    }

    /** The four resident-facing views, in nav order. OVERVIEW is the page they sit on. */
    public static NoticeView[] discoveryViews() {
        return new NoticeView[] { EVENTS, MEETINGS, ANNOUNCEMENTS, FLYERS };
    }
}
