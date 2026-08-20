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
import org.firststep.backend.shared.classification.CivicContentClassifier;
import org.firststep.backend.shared.classification.ClassificationResult;
import org.firststep.backend.shared.model.ContentSource;
import org.firststep.backend.shared.service.ContentSourceService;
import org.firststep.backend.shared.model.ContentType;
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
public class RssFeedService implements RssFeedSource, SignedLegislationSource {

    private static final Logger log = LoggerFactory.getLogger(RssFeedService.class);

    // Slice F2: this service EXTRACTS content and no longer decides categories.
    // Its keyword tables and classifyLegislation() moved into
    // shared/classification, where every source shares one implementation.
    private final CivicContentClassifier classifier;

    private final ContentSourceService contentSources;

    public RssFeedService(CivicContentClassifier classifier, ContentSourceService contentSources) {
        this.classifier = classifier;
        this.contentSources = contentSources;
    }

    // FEEDS COME FROM THE PRODUCER REGISTRY, NOT FROM A URL LIST.
    //
    // content-sources.json pairs each feedUrl with the producer id that publishes
    // it, so a feed cannot be added without declaring who it belongs to, and the
    // item's provenance is known before the first byte is parsed. The previous
    // `news.rss.urls` property carried URLs alone, which left identity to be
    // guessed at parse time from feed.getTitle() — a value the upstream publisher
    // can change at will. See Decision 045.

    @Value("${app.default-community-id:wilmington-de}")
    private String defaultCommunityId;

    // WHY: volatile ensures the lists are visible across threads when @Scheduled
    // writes them and the request thread reads them concurrently.
    //
    // TWO lists, because this service feeds two independent concerns (Slice F2.1):
    //   signedBills — EVERY bill. Legislation presentation (the Delaware Laws
    //                 rotator) shows what the Governor signed, ungated.
    //   rssItems    — only bills that passed relevance assessment and are
    //                 therefore CivicContent. Discovery: updates, categories,
    //                 search, AI retrieval.
    // One list served both before, so adding a relevance gate would have emptied
    // the rotator of every uncategorizable bill — a presentation feature broken
    // by a discovery decision.
    private volatile List<NewsItem> rssItems = List.of();
    private volatile List<NewsItem> signedBills = List.of();

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * A converted bill plus the engine's verdict on it. Two values because
     * conversion and admission are separate questions and classification must
     * happen exactly once — the alternative was classifying again in fetchFeeds,
     * which would double-count the startup summary.
     */
    private record ConvertedEntry(NewsItem item, ClassificationResult classification) {
    }

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
        Map<String, String> feeds = contentSources.feedUrls();
        if (feeds.isEmpty()) {
            log.warn("RSS: No feeds declared in content-sources.json");
            return;
        }

        List<NewsItem> allBills = new ArrayList<>();
        List<NewsItem> relevant = new ArrayList<>();

        for (Map.Entry<String, String> feedEntry : feeds.entrySet()) {
            String producerId = feedEntry.getKey();
            String url = feedEntry.getValue();

            try {
                SyndFeed feed = loadFeed(url);
                if (feed == null) continue;

                for (SyndEntry entry : feed.getEntries()) {
                    ConvertedEntry converted = convertEntry(producerId, entry);
                    allBills.add(converted.item());

                    // THE ADMISSION GATE. Branch on relevant() and never on
                    // categoryTags — the engine owns this decision so it cannot
                    // drift across ingestion points.
                    if (converted.classification().relevant()) {
                        relevant.add(converted.item());
                    }
                }

                log.info("RSS: Loaded {} entries from {}", feed.getEntries().size(), url);

            } catch (Exception ex) {
                log.error("RSS: Failed to load {} → {}", url, ex.getMessage());
            }
        }

        // WHY only replace when non-empty: a transient network failure during a
        // refresh should not wipe out the last good result. Keyed on allBills so
        // a fetch that returns only irrelevant bills still counts as a success.
        if (!allBills.isEmpty()) {
            signedBills = List.copyOf(allBills);
            rssItems = List.copyOf(relevant);
            log.info("RSS: {} of {} bills admitted as CivicContent", relevant.size(), allBills.size());
        }
    }

    /** Relevance-gated CivicContent. Discovery only. */
    @Override
    public List<NewsItem> getRssItems() {
        return rssItems;
    }

    /** Every signed bill, ungated. Legislation presentation only. */
    @Override
    public List<NewsItem> getSignedBills() {
        return signedBills;
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
    // 1. Map raw RSS fields (title, description, link, date) to NewsItem fields.
    // 2. Run keyword classification to assign civic category tags and a generic
    //    "why it matters" sentence.
    // 3. Try to extract a "RELATING TO …" clause from the description. Delaware
    //    legislation descriptions always begin with the full act title in ALL CAPS,
    //    e.g. "AN ACT TO AMEND TITLE 1 … RELATING TO PUERTO RICO DAY." If found,
    //    this clause (converted to Sentence Case) replaces both the bill-number
    //    headline and the generic why-it-matters text, giving users a readable,
    //    specific description of the law.
    private ConvertedEntry convertEntry(String producerId, SyndEntry entry) {
        NewsItem item = new NewsItem();

        item.id        = "rss-" + UUID.randomUUID();
        item.title     = safe(entry.getTitle());   // bill number e.g. "HB 311" — overridden below if RELATING TO found
        item.summary   = extractSummary(entry);
        item.body      = item.summary;

        // Identity is CONFIGURATION, stamped from the registry entry this feed
        // came from. feed.getTitle() is never used as provenance — an upstream
        // publisher renaming their feed must not silently re-attribute content.
        ContentSource contentSource = new ContentSource();
        contentSource.id   = producerId;
        contentSource.url  = entry.getLink();
        contentSources.resolveName(contentSource);
        item.contentSource = contentSource;

        item.communityId = defaultCommunityId;

        Date published = entry.getPublishedDate() != null
                ? entry.getPublishedDate()
                : entry.getUpdatedDate() != null
                    ? entry.getUpdatedDate()
                    : new Date();

        item.publishDate = DATE_FMT.format(published);
        item.createdDate = item.publishDate;
        item.updatedDate = item.publishDate;

        item.status  = "active";
        item.type    = "legislation";
        item.urgency = "standard";

        // Signed legislation is a NewsItem that PRESENTS as a Law. Content type
        // decides the treatment; classification below decides where it appears.
        // Keeping LAW here (rather than inventing a "Legislation" category) is
        // what preserves the dedicated Law experience without a second taxonomy.
        item.contentType = ContentType.LAW;

        // Step 2: classification is delegated. This service extracts content;
        // shared/classification decides relevance, the canonical category and the
        // descriptive tags, using the same vocabulary and the same engine as every
        // other source. Called before the title rewrite below so the classifier
        // sees the raw bill text, which carries more signal than the tidied clause.
        ClassificationResult classification = classifier.classify(item);
        item.whyItMatters = whyItMattersFor(item.categoryTags);

        // Step 3: extract "RELATING TO …" clause for a more readable headline/why
        String relatingTo = extractRelatingTo(item.summary);
        if (relatingTo != null) {
            item.title         = relatingTo;
            item.whyItMatters  = relatingTo;
        } else {
            // Step 3b: no "RELATING TO" clause (typically Resolutions, appropriations
            // Acts, and other bills whose formal title doesn't follow that phrasing) —
            // fall back to the bill's own "This Bill/Act/Resolution/Joint Resolution …"
            // self-description instead of leaving the raw bill number as the title.
            String fallbackTitle = extractFallbackTitle(item.title, item.summary);
            if (fallbackTitle != null) {
                item.title         = fallbackTitle;
                item.whyItMatters  = fallbackTitle;
            }
        }

        return new ConvertedEntry(item, classification);
    }

    // =============================================================================
    // RELATING TO EXTRACTION
    // =============================================================================
    // WHY: Delaware legislation descriptions always contain a formal title in ALL
    // CAPS like "AN ACT TO AMEND … RELATING TO PAID LEAVE." The "RELATING TO"
    // clause is the most human-readable part. We extract it and convert to
    // Title Case (with "Delaware" always forced capitalized as a proper noun)
    // so the UI can show "Relating to Paid Leave." instead of a bill number or
    // a generic category sentence.
    //
    // ALGORITHM — three terminators, earliest wins:
    //   1. First "." after "RELATING TO" — normal case where act title ends with a period.
    //   2. "This <Word>" boundary — Delaware descriptions follow the act title with
    //      "This Act…", "This Senate…", etc. When there is no period before this
    //      phrase, stop just before it and append a period.
    //   3. 120-character cap — hard safety net, truncates at last word boundary.
    //   Returns null if "RELATING TO" is not found or nothing remains after trimming.
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

        return toTitleCase(raw);
    }

    // =============================================================================
    // FALLBACK TITLE EXTRACTION (no "RELATING TO" clause)
    // =============================================================================
    // WHY: Resolutions (SJR/HJR) and some Acts (appropriations, bond bills, one-off
    // naming/designation bills) never contain "RELATING TO" — that phrasing is
    // specific to Acts amending a Title of the Delaware Code. These bills instead
    // self-describe with a "This Bill …"/"This Act …"/"This Resolution …"/"This
    // Joint Resolution …" sentence. Without this fallback, extractRelatingTo
    // returns null and the raw bill number (e.g. "HB 500") was the only title —
    // unhelpful compared to what's actually available in the same summary text.
    //
    // TWO DIFFERENT RULES, because the two source shapes are different:
    // - Bills/Acts ("This Bill …" / "This Act …"): this sentence is ALREADY
    //   well-formed, normally-cased English (not shouted caps) — e.g. "This Bill
    //   is the Fiscal Year 2027 Bond and Capital Improvements Act." So the fix
    //   just swaps the lead-in for "The bill" and keeps everything through the
    //   next period verbatim — no case conversion, which would only mangle text
    //   that's already correct.
    // - Resolutions ("This Resolution …" / "This Joint Resolution …", optionally
    //   with "House"/"Senate" inserted — e.g. "This House Joint Resolution …"):
    //   the FORMAL LONG TITLE precedes this sentence, in ALL CAPS (like the
    //   RELATING TO case) — e.g. "THE OFFICIAL GENERAL FUND REVENUE ESTIMATE FOR
    //   FISCAL YEAR 2027." That portion genuinely needs toTitleCase, and gets
    //   prefixed with "Senate Joint Resolution: " / "House Joint Resolution: "
    //   (based on the bill number's own SJR/HJR prefix, not the matched phrase —
    //   a bill numbered SJR could in principle say just "This Resolution", not
    //   "This Joint Resolution", and should still be labeled Senate Joint
    //   Resolution).
    //
    //   The formal title ends at whichever comes first: the summary's own first
    //   period (the normal case — the heading is its own sentence), or the start
    //   of the matched "This …" phrase itself (for the rare case where no period
    //   separates them at all). Just using "before the matched phrase" is NOT
    //   enough on its own: some resolutions have several sentences of narrative
    //   background between the heading and their self-description, and some
    //   summaries mention "This Joint Resolution" a SECOND time later in the
    //   text (e.g. "This Joint Resolution also requires …") — matching that
    //   later occurrence instead of the real one would sweep all of that
    //   background into the "title". Confirmed against real feed data that had
    //   exactly this problem before the earliest-terminator-wins fix.
    private static String extractFallbackTitle(String billNumber, String summary) {
        if (summary == null) return null;

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\bThis\\s+(?:House\\s+|Senate\\s+)?(Joint\\s+Resolution|Resolution|Bill|Act)\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(summary);
        if (!m.find()) return null;

        boolean isResolution = m.group(1).toLowerCase(Locale.ROOT).contains("resolution");

        if (isResolution) {
            int periodIdx = summary.indexOf('.');
            int end = (periodIdx >= 0) ? Math.min(periodIdx, m.start()) : m.start();

            String formalTitle = summary.substring(0, end).trim();
            if (formalTitle.isEmpty()) return null;

            String prefix = billNumber != null && billNumber.trim().toUpperCase(Locale.ROOT).startsWith("SJR")
                    ? "Senate Joint Resolution: "
                    : "House Joint Resolution: ";
            return prefix + toTitleCase(formalTitle + ".");
        } else {
            String afterPhrase = summary.substring(m.end()).trim();
            int periodIdx = afterPhrase.indexOf('.');
            if (periodIdx < 0) return null;
            String tail = afterPhrase.substring(0, periodIdx + 1);
            if (tail.isEmpty()) return null;
            return "The bill " + tail;
        }
    }

    // WHY: minor connector words stay lowercase in the middle of a title
    // (standard title-case convention) but are still capitalized as the
    // first or last word.
    private static final Set<String> TITLE_CASE_MINOR_WORDS = Set.of(
            "a", "an", "and", "as", "at", "but", "by", "for", "in", "nor",
            "of", "on", "or", "the", "to", "with");

    private static String toTitleCase(String sentence) {
        String[] words = sentence.toLowerCase(Locale.ROOT).split(" ");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) continue;
            String alphaOnly = word.replaceAll("[^a-zA-Z]", "");
            boolean keepLowercase = TITLE_CASE_MINOR_WORDS.contains(alphaOnly) && i != 0 && i != words.length - 1;
            if (!keepLowercase) {
                words[i] = capitalizeFirstLetter(word);
            }
        }
        String result = String.join(" ", words);

        // "Delaware" is a proper noun the general lowercasing above would
        // otherwise miss (e.g. mid-word inside "delaware's" or after a minor
        // word) — force it capitalized wherever it appears, as a safety net
        // on top of title case rather than relying on it alone.
        return result.replaceAll("(?i)\\bdelaware\\b", "Delaware");
    }

    private static String capitalizeFirstLetter(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (Character.isLetter(word.charAt(i))) {
                return word.substring(0, i) + Character.toUpperCase(word.charAt(i)) + word.substring(i + 1);
            }
        }
        return word;
    }

    // =============================================================================
    // WHY IT MATTERS (law-specific editorial copy)
    // =============================================================================
    // Slice F2 moved keyword classification OUT of this service into
    // shared/classification. What stays here is the one thing that is genuinely
    // legislation-specific rather than taxonomy vocabulary: the sentence telling a
    // resident why a NEW LAW in a given category might affect them. That copy is
    // about laws, not about the category in general, so it does not belong in
    // taxonomy.json alongside the shared vocabulary.
    //
    // Keyed by canonical category LABEL (not the old lowercase bucket names), and
    // LinkedHashMap because insertion order picks the sentence when a bill
    // classifies into several categories.
    private static final Map<String, String> WHY_BY_CATEGORY = new LinkedHashMap<>();
    static {
        WHY_BY_CATEGORY.put("Housing",     "This new law may affect your rights as a renter, homeowner, or manufactured-home resident in Delaware.");
        WHY_BY_CATEGORY.put("Health",      "This new law may change what health services or coverage are available to you or your family.");
        WHY_BY_CATEGORY.put("Food",        "This new law may affect food assistance programs or nutrition services in your community.");
        WHY_BY_CATEGORY.put("Employment",  "This new law may change your rights or benefits at work, including wages, leave, or licensing.");
        WHY_BY_CATEGORY.put("Utilities",   "This new law may affect your electric, water, or energy bills.");
        WHY_BY_CATEGORY.put("Legal",       "This new law may affect your legal rights or access to the courts.");
        WHY_BY_CATEGORY.put("Community Support", "This new law may change assistance programs or services available in your community.");
        WHY_BY_CATEGORY.put("Community Events",  "This new law may affect community programs, recreation, or public spaces near you.");
        WHY_BY_CATEGORY.put("Clothing",    "This new law may affect programs providing clothing and everyday essentials.");
        WHY_BY_CATEGORY.put("Furniture & Household", "This new law may affect programs providing furniture and household goods.");
    }

    private static final String GENERIC_WHY =
            "Stay informed about new laws signed by the Governor of Delaware.";

    // Picks the sentence for the first classified category, falling back to the
    // generic line. An unclassifiable bill keeps EMPTY categoryTags rather than a
    // "Delaware Legislation" pseudo-category — that string described a content
    // TYPE, and ContentType.LAW now expresses it properly.
    private static String whyItMattersFor(List<String> categoryTags) {
        if (categoryTags != null) {
            for (String tag : categoryTags) {
                String why = WHY_BY_CATEGORY.get(tag);
                if (why != null) {
                    return why;
                }
            }
        }
        return GENERIC_WHY;
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
