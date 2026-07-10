package org.firststep.backend.news.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.firststep.backend.news.model.NewsItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises fetchFeeds() end-to-end (loadFeed -> convertEntry ->
 * classifyLegislation/extractRelatingTo) against local file:// RSS fixtures
 * instead of a real network call — avoids network flakiness while still
 * testing through the public API, since convertEntry/classifyLegislation/
 * extractRelatingTo are private and not directly callable from a test.
 */
class RssFeedServiceTest {

    private RssFeedService serviceFor(String feedUrl) {
        RssFeedService service = new RssFeedService();
        ReflectionTestUtils.setField(service, "rssFeedUrls", feedUrl);
        ReflectionTestUtils.setField(service, "defaultCommunityId", "wilmington-de");
        return service;
    }

    private String writeFeed(Path dir, String filename, String itemTitle, String itemDescription) throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<rss version=\"2.0\"><channel>" +
                "<title>Delaware General Assembly</title>" +
                "<link>https://legis.delaware.gov</link>" +
                "<description>Test feed</description>" +
                "<item>" +
                "<title>" + itemTitle + "</title>" +
                "<link>https://legis.delaware.gov/BillDetail/1</link>" +
                "<description>" + itemDescription + "</description>" +
                "<pubDate>Mon, 01 Jan 2024 00:00:00 GMT</pubDate>" +
                "</item>" +
                "</channel></rss>";
        Path file = dir.resolve(filename);
        Files.writeString(file, xml);
        return file.toUri().toString();
    }

    @Test
    void shouldClassifyHousingKeywordsIntoHousingTag(@TempDir Path tempDir) throws IOException {
        String feedUrl = writeFeed(tempDir, "housing.xml", "HB 1",
                "AN ACT RELATING TO EVICTION PROTECTIONS FOR TENANTS AND LANDLORDS.");

        RssFeedService service = serviceFor(feedUrl);
        service.fetchFeeds();

        NewsItem item = service.getRssItems().get(0);
        assertTrue(item.tags.contains("Housing"));
    }

    @Test
    void shouldExtractRelatingToClauseWhenPresent(@TempDir Path tempDir) throws IOException {
        String feedUrl = writeFeed(tempDir, "relating.xml", "HB 311",
                "AN ACT TO AMEND TITLE 5 OF THE DELAWARE CODE RELATING TO MONEY TRANSMISSION.");

        RssFeedService service = serviceFor(feedUrl);
        service.fetchFeeds();

        NewsItem item = service.getRssItems().get(0);
        assertEquals("Relating to money transmission.", item.title);
        assertEquals("Relating to money transmission.", item.whyItMatters);
    }

    @Test
    void shouldFallBackToGenericClassificationWhenNoKeywordsMatch(@TempDir Path tempDir) throws IOException {
        String feedUrl = writeFeed(tempDir, "generic.xml", "HB 2",
                "AN ACT CONCERNING THE STATE FLAG DESIGN.");

        RssFeedService service = serviceFor(feedUrl);
        service.fetchFeeds();

        NewsItem item = service.getRssItems().get(0);
        assertEquals(List.of("Delaware Legislation"), item.tags);
        assertEquals("Stay informed about new laws signed by the Governor of Delaware.", item.whyItMatters);
    }

    @Test
    void shouldTruncateRelatingToClauseAtCharacterCap(@TempDir Path tempDir) throws IOException {
        // Needs a period (or a "This <Word>" boundary) for extractRelatingTo to find an
        // end point at all — without one it returns null rather than truncating.
        String longClause = "RELATING TO " + "WORD ".repeat(30) + ".";
        String feedUrl = writeFeed(tempDir, "long.xml", "HB 3", longClause);

        RssFeedService service = serviceFor(feedUrl);
        service.fetchFeeds();

        NewsItem item = service.getRssItems().get(0);
        assertTrue(item.title.length() <= 121, "should truncate to the 120-char cap plus trailing period");
        assertTrue(item.title.endsWith("."));
    }

    @Test
    void shouldKeepLastGoodResultWhenFetchFails(@TempDir Path tempDir) throws IOException {
        String feedUrl = writeFeed(tempDir, "good.xml", "HB 4", "AN ACT RELATING TO FOOD ASSISTANCE.");

        RssFeedService service = serviceFor(feedUrl);
        service.fetchFeeds();
        assertEquals(1, service.getRssItems().size());

        // Point at a file that doesn't exist -> loadFeed fails, collected stays empty,
        // and fetchFeeds() must not overwrite the last good result with nothing.
        ReflectionTestUtils.setField(service, "rssFeedUrls", tempDir.resolve("missing.xml").toUri().toString());
        service.fetchFeeds();

        assertEquals(1, service.getRssItems().size());
        assertEquals("Relating to food assistance.", service.getRssItems().get(0).title);
    }
}
