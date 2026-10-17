/*
 * WHAT: Complete IM-09 annotated mirror of backend/src/test/java/org/firststep/backend/news/dto/NewsSummaryTest.java.
 * WHY: Preserve authored placement, canonical identity, truthful dates and private/public boundaries.
 * INTERACTION: Current inventory and Article approval remain independent of exact editorial
 * Expert/FAQ admissions; Category composition intersects Community, taxonomy and membership.
 * ALTERNATIVES: Source/session/verified inference and blanket Expert admission were rejected.
 * Tests cover approved admissions, denied sessions, preserved supporting views and safe links.
 */
package org.firststep.backend.news.dto;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.model.ContentType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NewsSummaryTest {
    @Test void t05t16PublicNewsKeepsLawIdentityAndPublicationButOmitsInternalFacts() throws Exception {
        NewsItem law = new NewsItem(); law.id = "LAW-1"; law.contentType = ContentType.LAW;
        law.publishDate = "2026-10-01"; law.whyItMatters = "Source explanation";
        law.verified = true; law.status = "active"; law.createdDate = "2026-10-02";
        law.expirationDate = "2026-12-01"; law.helpAccess = "ACTIONABLE_HELP_ACCESS";
        law.authoredCategoryTags = List.of("Housing");
        law.contentSource = new ContentSource(); law.contentSource.id = "original-producer";
        law.contentSource.url = "https://example.gov/original"; law.contentSource.retrieved = "2026-10-16";
        var tree = new ObjectMapper().valueToTree(NewsSummary.from(law));
        assertEquals("LAW", tree.path("contentType").asText()); assertEquals("LAW-1", tree.path("id").asText());
        assertEquals("2026-10-01", tree.path("publishDate").asText());
        assertEquals("Source explanation", tree.path("why_it_matters").asText());
        for (String field : List.of("verified", "status", "createdDate", "updatedDate", "expirationDate", "helpAccess", "authoredCategoryTags", "authoredSubcategory"))
            assertFalse(tree.has(field), field);
        assertFalse(tree.path("contentSource").has("retrieved"));
        assertEquals("https://example.gov/original", tree.path("contentSource").path("url").asText());
        law.subcategory = "Keyword inferred topic";
        assertNull(NewsSummary.from(law).subcategory());
        assertEquals(5, ContentType.values().length);
    }

    @Test void t18MissingOrUnsafeSourceUrlsStayAbsentRatherThanInvented() {
        NewsItem n = new NewsItem(); n.contentSource = new ContentSource();
        for (String url : List.of("javascript:alert(1)", "data:text/html,bad", "not a url", "//evil.example")) {
            n.contentSource.url = url; assertNull(NewsSummary.from(n).contentSource().url());
        }
        n.contentSource.url = null; assertNull(NewsSummary.from(n).contentSource().url());
        n.contentSource.url = "https://example.gov/real"; assertEquals(n.contentSource.url, NewsSummary.from(n).contentSource().url());
    }
}
