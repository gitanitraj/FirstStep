package org.firststep.backend.shared.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommunitySlugTest {

    @Test
    void shouldSlugifySingleWordCity() {
        assertEquals("wilmington-de", CommunitySlug.forCity("Wilmington"));
    }

    @Test
    void shouldSlugifyMultiWordCity() {
        assertEquals("new-castle-de", CommunitySlug.forCity("New Castle"));
    }

    @Test
    void shouldStripPunctuationWhenSlugifying() {
        assertEquals("st-georges-de", CommunitySlug.forCity("St. Georges"));
    }

    @Test
    void shouldTrimAndLowercase() {
        assertEquals("newark-de", CommunitySlug.forCity("  Newark  "));
        assertEquals("bear-de", CommunitySlug.forCity("BEAR"));
    }

    @Test
    void shouldReturnNullForNullOrBlankCity() {
        assertNull(CommunitySlug.forCity(null));
        assertNull(CommunitySlug.forCity("   "));
    }
}
