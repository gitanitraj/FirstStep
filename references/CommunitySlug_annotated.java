package org.firststep.backend.shared.util;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// CommunitySlug turns a city/town name into the communityId format already
// established by the "wilmington-de" convention: lowercase, hyphenated,
// "-de" suffix. Used by JsonResourceRepository to derive each Resource's
// communityId from its actual location instead of a single blanket default.
// =============================================================================

import java.util.Locale;

public final class CommunitySlug {

    private CommunitySlug() {}

    public static String forCity(String city) {
        if (city == null || city.isBlank()) return null;
        String slug = city.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? null : slug + "-de";
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// A GENERAL SLUGIFIER, NOT A HARDCODED TOWN LIST: works for any city string
// Location.city ever holds, not just the 6 towns this pass happens to bring
// in data for. Adding a 7th town later (or any future community) needs zero
// code changes here — the function is purely mechanical.
//
// MATCHES THE EXISTING "wilmington-de" CONVENTION EXACTLY:
// forCity("Wilmington") == "wilmington-de", which is the literal string
// every repository's @Value("${app.default-community-id:wilmington-de}")
// has hardcoded as its fallback since the original migration. This means
// adopting CommunitySlug for Wilmington records is a no-op — no existing
// Wilmington-tagged data changes value, only the non-Wilmington records
// that were previously mislabeled get corrected.
//
// "-de" IS HARDCODED (NOT A STATE PARAMETER): every community this
// codebase has ever handled is in Delaware; genuinely out of scope to
// design multi-state support before a second state exists. If that day
// comes, this is a one-line, localized change.
//
// PUNCTUATION-STRIPPING REGEX HANDLES MULTI-WORD/PUNCTUATED NAMES: "New
// Castle" -> "new-castle-de", "St. Georges" -> "st-georges-de" — collapses
// any run of non-alphanumeric characters (spaces, periods, apostrophes)
// into a single hyphen, then trims leading/trailing hyphens.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - resource/repository/JsonResourceRepository.communityIdFor(Resource)
//   calls CommunitySlug.forCity(resource.locations.get(0).city) — see that
//   class's annotated reference for the full derivation/fallback logic.
// - No Spring wiring, no state — a plain static utility, same shape as
//   shared/util/TextScore.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - A hardcoded Map<String,String> of the 6 approved towns to their slugs:
//   rejected — would need updating every time a new town's data is added,
//   for no accuracy benefit over the mechanical slugify approach, which
//   already produces the exact right answer for towns it's never seen.
// =============================================================================
