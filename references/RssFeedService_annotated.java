package org.firststep.backend.news.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// RssFeedService fetches one or more RSS feeds on a schedule, converts each
// entry into a NewsItem, classifies it by civic topic (housing, healthcare,
// food, etc.), and holds the result in memory for the NewsController to serve
// at GET /api/news/rss.
//
// It implements RssFeedSource so it can be swapped with a fake in tests
// without needing Mockito on concrete classes.
// =============================================================================

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.shared.model.ContentSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RssFeedService implements RssFeedSource {

    private static final Logger log = LoggerFactory.getLogger(RssFeedService.class);

    // WHY: Comma-separated URL list from application.properties so URLs can be
    // changed or added without recompiling.
    // Configured as: news.rss.urls=https://legis.delaware.gov/rss/RssFeeds/GovernorSignedLegislation
    @Value("${news.rss.urls:}")
    private String rssFeedUrls;

    @Value("${app.default-community-id:wilmington-de}")
    private String defaultCommunityId;

    // WHY: volatile ensures the list is visible across threads when @Scheduled
    // writes it and the request thread reads it concurrently.
    private volatile List<NewsItem> rssItems = List.of();

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");

    // =============================================================================
    // SCHEDULED FETCH
    // =============================================================================
    // WHY initialDelayString vs initialDelay: The property-driven form allows the
    // delay to be tuned in tests and deployment without recompiling. Default 500ms
    // is fast enough that items appear before the user first loads the page.
    // WHY fixedDelay (not fixedRate): fixedDelay waits after the previous run
    // completes, avoiding overlap if a fetch takes longer than the interval.
    @Scheduled(
            fixedDelayString  = "${news.rss.refresh-interval:3600000}",
            initialDelayString = "${news.rss.initial-delay:500}"
    )
    public void fetchFeeds() {
        if (rssFeedUrls == null || rssFeedUrls.isBlank()) {
            log.warn("RSS: No feed URLs configured");
            return;
        }

        List<NewsItem> collected = new ArrayList<>();

        for (String rawUrl : rssFeedUrls.split(",")) {
            String url = rawUrl.trim();
            if (url.isEmpty()) continue;

            try {
                SyndFeed feed = loadFeed(url);
                if (feed == null) continue;

                for (SyndEntry entry : feed.getEntries()) {
                    NewsItem item = convertEntry(feed, entry);
                    collected.add(item);
                }

                log.info("RSS: Loaded {} entries from {}", feed.getEntries().size(), url);

            } catch (Exception ex) {
                log.error("RSS: Failed to load {} → {}", url, ex.getMessage());
            }
        }

        // WHY only replace when non-empty: a transient network failure during a
        // refresh should not wipe out the last good result.
        if (!collected.isEmpty()) {
            rssItems = List.copyOf(collected);
        }
    }

    public List<NewsItem> getRssItems() {
        return rssItems;
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    // WHY setAllowDoctypes(true): Delaware's feed includes a DOCTYPE declaration
    // which JAXP blocks by default as a security measure. We allow it here
    // because this is a trusted government feed.
    private SyndFeed loadFeed(String url) {
        try {
            URL u = new URL(url);
            URLConnection conn = u.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (RSS Reader)");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            SyndFeedInput input = new SyndFeedInput();
            input.setAllowDoctypes(true);
            return input.build(new XmlReader(conn));

        } catch (Exception e) {
            log.error("RSS: Error reading {} → {}", url, e.getMessage());
            return null;
        }
    }

    // =============================================================================
    // ENTRY → NewsItem CONVERSION
    // =============================================================================
    // HOW IT WORKS:
    // 1. Map raw RSS fields (title, description, link, date) onto NewsItem fields
    //    — title (inherited from CivicContent) and contentSource (built from the
    //    feed's title/entry's link) instead of v1's flat headline/sourceName/
    //    sourceUrl fields.
    // 2. Run keyword classification to assign civic category tags (now the
    //    inherited `tags` field, not a NewsItem-only categoryTags) and a generic
    //    "why it matters" sentence.
    // 3. Try to extract a "RELATING TO …" clause from the description. Delaware
    //    legislation descriptions always begin with the full act title in ALL CAPS,
    //    e.g. "AN ACT TO AMEND TITLE 1 … RELATING TO PUERTO RICO DAY." If found,
    //    this clause (converted to Sentence Case) replaces both the bill-number
    //    title and the generic why-it-matters text, giving users a readable,
    //    specific description of the law.
    private NewsItem convertEntry(SyndFeed feed, SyndEntry entry) {
        NewsItem item = new NewsItem();

        item.id        = "rss-" + UUID.randomUUID();
        item.title     = safe(entry.getTitle());   // bill number e.g. "HB 311" — overridden below if RELATING TO found
        item.summary   = extractSummary(entry);
        item.body      = item.summary;

        ContentSource contentSource = new ContentSource();
        contentSource.name = feed.getTitle() != null ? feed.getTitle() : "RSS Feed";
        contentSource.url  = entry.getLink();
        item.contentSource = contentSource;

        item.communityId = defaultCommunityId;

        Date published = entry.getPublishedDate() != null
                ? entry.getPublishedDate()
                : entry.getUpdatedDate() != null
                    ? entry.getUpdatedDate()
                    : new Date();

        item.published = DATE_FMT.format(published);
        item.createdDate = item.published;
        item.updatedDate = item.published;

        item.active  = true;
        item.type    = "legislation";
        item.urgency = "standard";

        // Step 2: keyword classification
        String text = (item.title + " " + item.summary).toLowerCase();
        Classification cls = classifyLegislation(text);
        item.tags          = cls.categoryTags;
        item.resourceTags  = cls.resourceTags;
        item.whyItMatters  = cls.whyItMatters;

        // Step 3: extract "RELATING TO …" clause for a more readable title/why
        String relatingTo = extractRelatingTo(item.summary);
        if (relatingTo != null) {
            item.title         = relatingTo;
            item.whyItMatters  = relatingTo;
        }

        return item;
    }

    // =============================================================================
    // RELATING TO EXTRACTION
    // =============================================================================
    // WHY: Delaware legislation descriptions always contain a formal title in ALL
    // CAPS like "AN ACT TO AMEND … RELATING TO PAID LEAVE." The "RELATING TO"
    // clause is the most human-readable part. We extract it and convert to
    // Sentence Case so the UI can show "Relating to paid leave." instead of a
    // bill number or a generic category sentence.
    //
    // ALGORITHM — three terminators, earliest wins:
    //   1. First "." after "RELATING TO" — normal case where act title ends with a period.
    //   2. "This <Word>" boundary — Delaware descriptions follow the act title with
    //      "This Act…", "This Senate…", etc. When there is no period before this
    //      phrase, stop just before it and append a period.
    //   3. 120-character cap — hard safety net, truncates at last word boundary.
    //   Returns null if "RELATING TO" is not found, or if NEITHER terminator (1) nor
    //   (2) is found at all — the character cap (3) only trims an already-found
    //   match, it does not by itself create an end point. (Confirmed by test:
    //   RssFeedServiceTest.shouldTruncateRelatingToClauseAtCharacterCap needed a
    //   trailing period in its fixture; a fixture with no period and no "This X"
    //   phrase returns null, not a capped string.)
    private static final int RELATING_TO_MAX_CHARS = 120;

    private static String extractRelatingTo(String summary) {
        if (summary == null) return null;
        String upper = summary.toUpperCase();
        int start = upper.indexOf("RELATING TO");
        if (start < 0) return null;

        int end = Integer.MAX_VALUE;

        // Rule 1: first period after "RELATING TO" (include the period)
        int periodIdx = summary.indexOf('.', start);
        if (periodIdx >= 0) end = Math.min(end, periodIdx + 1);

        // Rule 2: "This <Word>" boundary — stop before it
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\bThis\\s+[A-Z]", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(summary);
        while (m.find()) {
            if (m.start() > start) {
                int boundary = m.start();
                while (boundary > start && Character.isWhitespace(summary.charAt(boundary - 1))) {
                    boundary--;
                }
                end = Math.min(end, boundary);
                break;
            }
        }

        if (end == Integer.MAX_VALUE || end <= start) return null;

        String raw = summary.substring(start, end).trim();

        // Rule 3: character cap — truncate at last word boundary
        if (raw.length() > RELATING_TO_MAX_CHARS) {
            raw = raw.substring(0, RELATING_TO_MAX_CHARS);
            int lastSpace = raw.lastIndexOf(' ');
            if (lastSpace > 0) raw = raw.substring(0, lastSpace);
        }

        if (raw.isEmpty()) return null;

        if (!raw.endsWith(".")) raw = raw + ".";

        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1).toLowerCase();
    }

    // =============================================================================
    // CLASSIFICATION
    // =============================================================================
    // WHY LinkedHashMap: insertion order matters — the first matched tag determines
    // which "why it matters" sentence is used when multiple tags match.
    //
    // HOW IT WORKS: Each entry's lowercase title+summary is tested against
    // keyword arrays. Any matching bucket adds its display tag to the result list.
    // If no bucket matches, a generic "Delaware Legislation" tag and fallback
    // sentence are returned.
    private static final class Classification {
        List<String> categoryTags;
        List<String> resourceTags;
        String whyItMatters;
        Classification(List<String> categoryTags, List<String> resourceTags, String whyItMatters) {
            this.categoryTags = categoryTags;
            this.resourceTags = resourceTags;
            this.whyItMatters = whyItMatters;
        }
    }

    private static final Map<String, String[]> TAG_KEYWORDS = new LinkedHashMap<>();
    static {
        TAG_KEYWORDS.put("housing",    new String[]{"housing", "rent", "landlord", "tenant", "evict",
                                                    "mortgage", "residential", "manufactured home",
                                                    "affordable rental", "shelter"});
        TAG_KEYWORDS.put("healthcare", new String[]{"health", "medical", "medicaid", "medicare",
                                                    "hospital", "clinic", "mental health", "prescription",
                                                    "nursing", "patient", "wellness", "behavioral health",
                                                    "opioid", "drug", "therapy", "physician", "care",
                                                    "insurance", "vaccination", "public health",
                                                    "drinking water", "long-term care", "school-based health"});
        TAG_KEYWORDS.put("food",       new String[]{"food", "nutrition", "snap", "hunger", "grocery",
                                                    "meal", "wic", "restaurant meals", "dietitian",
                                                    "farm", "agriculture"});
        TAG_KEYWORDS.put("employment", new String[]{"employ", "worker", "wage", "labor", "job",
                                                    "workplace", "paid leave", "unemployment",
                                                    "workforce", "occupational", "salary", "licensure"});
        TAG_KEYWORDS.put("utilities",  new String[]{"utility", "utilities", "electric", "energy",
                                                    "net meter", "solar", "water system"});
        TAG_KEYWORDS.put("disability", new String[]{"disability", "disabilities", "accessible", "accessibility",
                                                    "accommodation", "developmental disability",
                                                    "rehabilitation", "hearing", "blue envelope"});
        TAG_KEYWORDS.put("benefits",   new String[]{"benefit", "assistance", "subsidy", "aid",
                                                    "social service", "low-income", "poverty",
                                                    "state employee benefit", "child care",
                                                    "school-based", "voucher"});
        TAG_KEYWORDS.put("legal",      new String[]{"court", "justice", "civil right", "equal accommodation",
                                                    "protection", "eviction", "trafficking",
                                                    "stalking", "criminal", "juvenile"});
    }

    private static final Map<String, String> TAG_WHY = new LinkedHashMap<>();
    static {
        TAG_WHY.put("housing",    "This new law may affect your rights as a renter, homeowner, or manufactured-home resident in Delaware.");
        TAG_WHY.put("healthcare", "This new law may change what health services or coverage are available to you or your family.");
        TAG_WHY.put("food",       "This new law may affect food assistance programs or nutrition services in your community.");
        TAG_WHY.put("employment", "This new law may change your rights or benefits at work, including wages, leave, or licensing.");
        TAG_WHY.put("utilities",  "This new law may affect your electric, water, or energy bills.");
        TAG_WHY.put("disability", "This new law may expand services or protections for people with disabilities.");
        TAG_WHY.put("benefits",   "This new law may change assistance programs or benefits available to low-income Delawareans.");
        TAG_WHY.put("legal",      "This new law may affect your legal rights or access to the courts.");
    }

    private static Classification classifyLegislation(String text) {
        List<String> matched = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : TAG_KEYWORDS.entrySet()) {
            for (String kw : entry.getValue()) {
                if (text.contains(kw)) {
                    matched.add(entry.getKey());
                    break;
                }
            }
        }

        if (matched.isEmpty()) {
            return new Classification(
                List.of("Delaware Legislation"),
                List.of(),
                "Stay informed about new laws signed by the Governor of Delaware."
            );
        }

        List<String> categoryTags = matched.stream()
                .map(t -> Character.toUpperCase(t.charAt(0)) + t.substring(1))
                .collect(Collectors.toList());

        List<String> resourceTags = new ArrayList<>(matched);

        String why = TAG_WHY.get(matched.get(0));

        return new Classification(categoryTags, resourceTags, why);
    }

    // =============================================================================
    // SUMMARY EXTRACTION
    // =============================================================================
    // WHY three sources: RSS feeds vary — most use <description>, some use
    // <content:encoded>, and older feeds may use media extensions. Trying in order
    // handles all three without configuration.
    private String extractSummary(SyndEntry entry) {
        // 1. Standard <description>
        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
            return stripHtml(entry.getDescription().getValue());
        }

        // 2. <content:encoded>
        if (entry.getContents() != null && !entry.getContents().isEmpty()) {
            SyndContent content = entry.getContents().get(0);
            if (content != null && content.getValue() != null) {
                return stripHtml(content.getValue());
            }
        }

        // 3. Media module fallback
        for (SyndContent c : entry.getContents()) {
            if (c != null && c.getValue() != null) {
                return stripHtml(c.getValue());
            }
        }

        return "";
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        return html
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN (migration-specific additions)
// =============================================================================
// Package moved from org.firststep.backend.service to
// org.firststep.backend.news.service. Field-level changes, all mechanical
// consequences of NewsItem's migration onto CivicContent (see
// NewsItem_annotated.java) — no classification/extraction LOGIC changed:
// - item.headline assignments -> item.title (inherited field).
// - item.sourceName/item.sourceUrl -> a constructed ContentSource, assigned
//   to item.contentSource.
// - item.categoryTags -> item.tags (inherited field); item.resourceTags
//   stays unchanged (still NewsItem-specific).
// - Added item.communityId = defaultCommunityId (new @Value-injected field,
//   matching JsonResourceRepository/JsonNewsRepository's default-stamping)
//   and item.createdDate/updatedDate = item.published, so RSS-sourced items
//   satisfy the same "every CivicContent object carries a communityId"
//   invariant as JSON-loaded items, not just the static-file path.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES (migration-specific additions)
// =============================================================================
// - Independent of JsonNewsRepository — RSS-sourced news and static
//   JSON-sourced news are two separate paths, both producing NewsItem
//   objects that satisfy the same CivicContent contract.
// =============================================================================
