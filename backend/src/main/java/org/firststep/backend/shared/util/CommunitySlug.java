package org.firststep.backend.shared.util;

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
