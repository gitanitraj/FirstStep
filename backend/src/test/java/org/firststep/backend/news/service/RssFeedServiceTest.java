package org.firststep.backend.news.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.firststep.backend.shared.classification.ClassifierFixture;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.shared.model.ContentType;
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
        RssFeedService service = new RssFeedService(ClassifierFixture.real());
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
        assertTrue(item.categoryTags.contains("Housing"));
    }

    @Test
    void shouldMarkSignedLegislationWithLawContentType(@TempDir Path tempDir) throws IOException {
        // Content type decides PRESENTATION (the dedicated Law experience);
        // category_tags decide WHERE it appears. A bill about housing classifies
        // into Housing like any other content while still rendering as a Law.
        String feedUrl = writeFeed(tempDir, "law.xml", "HB 1",
                "AN ACT RELATING TO EVICTION PROTECTIONS FOR TENANTS AND LANDLORDS.");

        RssFeedService service = serviceFor(feedUrl);
        service.fetchFeeds();

        NewsItem item = service.getRssItems().get(0);
        assertEquals(ContentType.LAW, item.contentType);
        assertTrue(item.categoryTags.contains("Housing"));
    }

    @Test
    void shouldExtractRelatingToClauseWhenPresent(@TempDir Path tempDir) throws IOException {
        String feedUrl = writeFeed(tempDir, "relating.xml", "HB 311",
                "AN ACT TO AMEND TITLE 5 OF THE DELAWARE CODE RELATING TO MONEY TRANSMISSION.");

        RssFeedService service = serviceFor(feedUrl);
        service.fetchFeeds();

        NewsItem item = service.getRssItems().get(0);
        assertEquals("Relating to Money Transmission.", item.title);
        assertEquals("Relating to Money Transmission.", item.whyItMatters);
    }

    @Test
    void shouldCapitalizeDelawareAndUseTitleCaseInExtractedClause(@TempDir Path tempDir) throws IOException {
        String feedUrl = writeFeed(tempDir, "delaware.xml", "HB 42",
                "AN ACT TO AMEND TITLE 5 RELATING TO DELAWARE BANKS AND TRUST COMPANIES.");

        RssFeedService service = serviceFor(feedUrl);
        service.fetchFeeds();

        NewsItem item = service.getRssItems().get(0);
        assertEquals("Relating to Delaware Banks and Trust Companies.", item.title);
    }

    @Test
    void shouldFallBackToGenericClassificationWhenNoKeywordsMatch(@TempDir Path tempDir) throws IOException {
        String feedUrl = writeFeed(tempDir, "generic.xml", "HB 2",
                "AN ACT CONCERNING THE STATE FLAG DESIGN.");

        RssFeedService service = serviceFor(feedUrl);
        service.fetchFeeds();

        NewsItem item = service.getRssItems().get(0);
        // "Delaware Legislation" is gone as a category tag. It described a
        // content TYPE, not a subject, and ContentType.LAW now expresses that
        // properly — so an uncategorizable bill is honestly uncategorized rather
        // than filed under a pseudo-category (Decision 033).
        assertTrue(item.categoryTags == null || item.categoryTags.isEmpty());
        assertEquals(ContentType.LAW, item.contentType);
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
    void shouldUseBillSelfDescriptionWhenNoRelatingToClause(@TempDir Path tempDir) throws IOException {
        // Real pattern (SB 336): appropriations Acts don't say "RELATING TO" —
        // they self-describe in a "This Act …" sentence that's already
        // normally-cased English, not shouted caps needing conversion.
        String feedUrl = writeFeed(tempDir, "act.xml", "SB 336",
                "AN ACT MAKING A ONE-TIME SUPPLEMENTAL APPROPRIATION FOR THE FISCAL YEAR ENDING JUNE 30, 2027, " +
                "TO THE OFFICE OF MANAGEMENT AND BUDGET. This Act appropriates $146,199,300 to provide " +
                "one-time funded items through the Office of Management and Budget.");

        RssFeedService service = serviceFor(feedUrl);
        service.fetchFeeds();

        NewsItem item = service.getRssItems().get(0);
        assertEquals("The bill appropriates $146,199,300 to provide one-time funded items through the Office of Management and Budget.", item.title);
        assertEquals(item.title, item.whyItMatters);
    }

    @Test
    void shouldUseFormalTitleForSenateJointResolution(@TempDir Path tempDir) throws IOException {
        // Real pattern (SJR 22): Resolutions never say "RELATING TO" — the formal
        // long title precedes a "This Resolution …" sentence, in ALL CAPS like the
        // RELATING TO case, so it does need title-casing.
        String feedUrl = writeFeed(tempDir, "sjr.xml", "SJR 22",
                "THE OFFICIAL GENERAL FUND REVENUE ESTIMATE FOR FISCAL YEAR 2027. This Resolution provides " +
                "the official revenue, refund, and unencumbered funds estimates for Fiscal Year 2027.");

        RssFeedService service = serviceFor(feedUrl);
        service.fetchFeeds();

        NewsItem item = service.getRssItems().get(0);
        assertEquals("Senate Joint Resolution: The Official General Fund Revenue Estimate for Fiscal Year 2027.", item.title);
    }

    @Test
    void shouldUseFormalTitleForHouseJointResolutionWithoutPeriodBeforePhrase(@TempDir Path tempDir) throws IOException {
        // Real pattern (HJR 9): some resolutions have no period between the
        // ALL-CAPS formal title and "This Joint Resolution" — the phrase match
        // itself is the only boundary available.
        String feedUrl = writeFeed(tempDir, "hjr.xml", "HJR 9",
                "EXTENDING THE REPORTING DATE OF THE DRIVING UNDER THE INFLUENCE PREVENTION TASK FORCE " +
                "This Joint Resolution extends the reporting date of the Driving Under the Influence " +
                "Prevention Task Force from January 1, 2026, to January 1, 2027.");

        RssFeedService service = serviceFor(feedUrl);
        service.fetchFeeds();

        NewsItem item = service.getRssItems().get(0);
        assertEquals("House Joint Resolution: Extending the Reporting Date of the Driving Under the Influence Prevention Task Force.", item.title);
    }

    @Test
    void shouldNotOverMatchWhenSelfDescriptionHasHouseOrSenateInfixAndAppearsTwice(@TempDir Path tempDir) throws IOException {
        // Real bug found in production data (a Medicaid-related SJR): the self-
        // description said "This Senate Joint Resolution directs …" (an infix word
        // between "This" and "Joint Resolution" the original regex didn't handle),
        // so the code skipped past it to a SECOND, unrelated "This Joint Resolution
        // also requires …" sentence later in the text — sweeping the entire first
        // sentence into the "formal title" instead of stopping at the heading's
        // own period.
        String feedUrl = writeFeed(tempDir, "sjr-infix.xml", "SJR 9",
                "DIRECTING THE DIVISION TO EXPLORE HEALTH INSURANCE INITIATIVES. This Senate Joint Resolution " +
                "directs the Division to explore amending the state plan to allow for adoption of certain " +
                "initiatives. This Joint Resolution also requires the Division to provide a report to the " +
                "General Assembly as to its findings.");

        RssFeedService service = serviceFor(feedUrl);
        service.fetchFeeds();

        NewsItem item = service.getRssItems().get(0);
        assertEquals("Senate Joint Resolution: Directing the Division to Explore Health Insurance Initiatives.", item.title);
    }

    @Test
    void shouldStopAtFormalTitlePeriodWhenNarrativeTextPrecedesSelfDescription(@TempDir Path tempDir) throws IOException {
        // Real bug found in production data (a grid-technology SJR): several
        // sentences of narrative background sat between the ALL-CAPS heading and
        // the (correctly matching, no infix) "This resolution directs …" sentence.
        // Using "everything before the matched phrase" alone swept all of that
        // background into the title instead of stopping at the heading's own period.
        String feedUrl = writeFeed(tempDir, "sjr-narrative.xml", "SJR 3",
                "DIRECTING ALL ELECTRIC UTILITIES TO PARTICIPATE IN AN ANALYSIS OF GRID TECHNOLOGIES. " +
                "Grid technologies offer efficient tools to increase capacity. Studies demonstrate real " +
                "benefits. This resolution directs the state energy office to conduct a cost-benefit analysis.");

        RssFeedService service = serviceFor(feedUrl);
        service.fetchFeeds();

        NewsItem item = service.getRssItems().get(0);
        assertEquals("Senate Joint Resolution: Directing All Electric Utilities to Participate in an Analysis of Grid Technologies.", item.title);
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
        assertEquals("Relating to Food Assistance.", service.getRssItems().get(0).title);
    }
}
