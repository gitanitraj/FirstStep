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

    public RssFeedService(CivicContentClassifier classifier) {
        this.classifier = classifier;
    }

    // WHY: Comma-separated URL list from application.properties so URLs can be
    // changed or added without recompiling.
    // Configured as: news.rss.urls=https://legis.delaware.gov/rss/RssFeeds/GovernorSignedLegislation
    @Value("${news.rss.urls:}")
    private String rssFeedUrls;

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
        if (rssFeedUrls == null || rssFeedUrls.isBlank()) {
            log.warn("RSS: No feed URLs configured");
            return;
        }

        List<NewsItem> allBills = new ArrayList<>();
        List<NewsItem> relevant = new ArrayList<>();

        for (String rawUrl : rssFeedUrls.split(",")) {
            String url = rawUrl.trim();
            if (url.isEmpty()) continue;

            try {
                SyndFeed feed = loadFeed(url);
                if (feed == null) continue;

                for (SyndEntry entry : feed.getEntries()) {
                    ConvertedEntry converted = convertEntry(feed, entry);
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
    private ConvertedEntry convertEntry(SyndFeed feed, SyndEntry entry) {
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

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN (fallback-title addition)
// =============================================================================
// Added in response to real user feedback on the deployed "Delaware's Newest
// Laws" feature: some titles were showing the bare bill number (e.g. "HB 14",
// "SB 45") because their descriptions never contain "RELATING TO" — that
// phrasing is specific to Acts that amend a Title of the Delaware Code.
// Resolutions and several Act types (appropriations, bond/capital
// improvements, one-off naming bills) use different formal structures
// entirely. Investigated by pulling real live-feed data (19 items were
// showing the generic fallback) and confirming every single one contained a
// "This Bill/Act/Resolution/Joint Resolution …" self-description — see
// references/decisions.md for the full investigation and the exact rules
// confirmed with the user.
//
// The two branches (bill/act vs. resolution) are NOT symmetric on purpose:
// the bill/act sentence is already normal-cased English lifted verbatim
// (case-preserved), while the resolution branch extracts and title-cases the
// ALL-CAPS formal title that precedes the "This Resolution" sentence — because
// that's what the two source shapes actually look like in the raw feed data,
// confirmed against real examples, not assumed.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES (fallback-title addition)
// =============================================================================
// - Called from convertEntry only when extractRelatingTo returns null — the
//   two extraction strategies are mutually exclusive per item, never both.
// - Shares toTitleCase with extractRelatingTo (including the Delaware
//   capitalization safety net) for the resolution branch only — the bill/act
//   branch deliberately does NOT call toTitleCase, since that text is already
//   correctly cased and running it through would incorrectly capitalize
//   ordinary sentence words.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED (fallback-title addition)
// =============================================================================
// - Keying the Resolution-vs-Bill branch off the bill NUMBER prefix (SJR/HJR
//   vs SB/HB) instead of the matched "This X" phrase: rejected for detecting
//   WHICH branch to use (the phrase itself is a more direct, robust signal —
//   confirmed it correctly handles an irregular bill number like "SS 1 for SB
//   119", which doesn't fit any clean prefix pattern but does contain "This
//   Act"). The bill number IS still used, but only for the narrower job of
//   choosing "Senate" vs "House" wording in the Resolution branch's prefix,
//   per the user's explicit instruction.
// - Leaving the "Purpose" AI-synopsis idea (reviewing a bill's full text and
//   summarizing it once an AI provider exists) unbuilt: confirmed with the
//   user as an intentional future item, not scaffolded here — no AI provider
//   is configured yet (see ai/service/SpringAiAssistant.java), and building
//   UI/data plumbing for it now would be speculative.
// - Using "everything before the matched This-phrase" as the formal title,
//   with no period check: this was the FIRST implementation, and it shipped
//   a real bug — confirmed against live feed data (not a hypothetical): some
//   resolutions have narrative background between the heading and their
//   self-description, and some mention "This Joint Resolution" a second time
//   later in the text, so the naive version matched the wrong occurrence and
//   swept multiple sentences into the title. Fixed by taking whichever
//   terminator (period or phrase start) comes first — see decisions.md
//   Decision 010 for the two real examples that exposed this.
// =============================================================================

// =============================================================================
// SLICE F1 UPDATE (Decision 032) — LAW CONTENT TYPE, AND AN ADMISSION
// =============================================================================
// WHAT CHANGED HERE
// -----------------------------------------------------------------------------
//   item.published      -> item.publishDate       (contract field name)
//   item.active = true  -> item.status = "active" (contract lifecycle field)
//   item.contentType = ContentType.LAW            (NEW)
//   item.tags         = cls.categoryTags          -> item.categoryTags
//   item.resourceTags = cls.resourceTags          -> item.tags
//
// The last two are the important pair. classifyLegislation() has always
// produced BOTH an editorial classification and a set of descriptive keywords —
// it just had nowhere correct to put them. categoryTags went into the shared
// `tags` field (the descriptive one) and the descriptive keywords went into a
// NewsItem-only `resourceTags` field. The contract gave each one its right home
// and the two assignments simply swapped.
//
// contentType = LAW is what lets signed legislation classify into the ordinary
// taxonomy (a housing bill is Housing content, on the Housing page) while still
// rendering with its own treatment. There is no "Legislation" category and
// there should not be one — see ContentType_annotated.java Section 1.
//
// WHAT IS STILL WRONG HERE (deliberately, until Slice F2)
// -----------------------------------------------------------------------------
// classifyLegislation() is doing EDITORIAL work inside a fetching service, and
// it emits a vocabulary that is not the taxonomy's:
//
//     emits                canonical taxonomy says
//     -----                -----------------------
//     Housing/Food/         (same — these are fine)
//     Employment/Utilities/
//     Legal
//     Healthcare            Health
//     Disability            (no such category — nearest is legal ▸ Disability Advocacy)
//     Benefits              (no such category — nearest is community-support ▸
//                            Financial Assistance)
//     Delaware Legislation  (not a category at all — it is a CONTENT TYPE,
//                            which is now what ContentType.LAW expresses)
//
// and four canonical categories (clothing, community-events,
// furniture-household, community-support) have no keywords at all, so RSS can
// never reach them.
//
// Decision 031 papered over the first row by adding "Healthcare" as an alias in
// the taxonomy. Slice F1 REMOVED that alias, on the principle that drift is
// normalized at the source rather than absorbed downstream. That leaves a known
// gap: as of F1, an RSS item tagged "Healthcare" matches no category.
//
// WHY THAT GAP IS SAFE TO CARRY: CategoryService reads newsService.getAll(),
// which is CURATED news only. RSS items reach UpdatesService alone. So no
// category page is affected — the drifted values are visible in /api/updates'
// categoryTags and nowhere else, exactly as they were before.
//
// THE OTHER KNOWN DEFECT — greedy substring matching:
//
//     if (text.contains(kw))     // raw substring, no word boundary
//
// "aid" matches *said* and *paid*; "care" matches *careful*; "farm" matches any
// *farmer*. This is why Decision 031 observed a wetlands bill coming back tagged
// ["Housing", "Food", "Utilities", "Benefits", "Legal"]. A word-boundary regex
// is the fix.
//
// ALL OF THE ABOVE IS SLICE F2's JOB, in shared/classification/:
//
//     RSS -> [extract] -> RssFeedService
//                            |
//                      CivicContentClassifier
//                       /              \
//              CategoryClassifier   TagClassifier
//                            |
//                   canonical taxonomy values
//                            |
//                     CategoryService
//
// After F2, RssFeedService EXTRACTS content and does not decide categories. The
// keyword tables, the boundary matching and the canonical mapping all move into
// the classifier, where every source (RSS, resources' raw directory categories,
// future feeds) shares one implementation. That is the architectural principle
// this slice is building toward:
//
//     Every CivicContent source classifies content using the SAME canonical
//     taxonomy.
// =============================================================================

// =============================================================================
// SLICE F2 UPDATE (Decision 033) — THIS SERVICE NO LONGER CLASSIFIES
// =============================================================================
// DELETED from this file: TAG_KEYWORDS (8 keyword arrays), TAG_WHY, the private
// Classification class, and classifyLegislation(). Roughly 90 lines. What
// replaced them at the call site is one line:
//
//     classifier.classify(item);
//
// The F1 annotation below predicted this and named the reason: "classifyLegislation()
// is doing EDITORIAL work inside a fetching service". A service whose job is
// network I/O and XML parsing had become the only place in the system that knew
// what a category was — and it knew a DIFFERENT set of categories from the
// taxonomy everything else used.
//
// Three defects went with it, all previously documented as known-and-deferred:
//
//   VOCABULARY DRIFT.  It emitted Healthcare / Disability / Benefits /
//   "Delaware Legislation" — none canonical. Decision 031 had papered over the
//   first by adding a "Healthcare" alias to the taxonomy; F1 removed the alias;
//   F2 removed the cause. Verified live: no drifted value appears in /api/updates
//   or /api/news/rss any more.
//
//   SUBSTRING MATCHING.  text.contains("aid") matched "said"/"paid". See
//   Tokenizer_annotated.java Section 1. Live max categories on one bill went from
//   5+ to 4, and that one bill (Office of Inspector General) plausibly does touch
//   four.
//
//   FOUR UNREACHABLE CATEGORIES.  clothing, community-events,
//   furniture-household and community-support had no keywords at all, so RSS
//   could never classify into them. They have vocabulary now — Community Support
//   picks up 44 live bills, Community Events 2.
//
// WHAT DELIBERATELY STAYED — and why it is not an inconsistency:
//
// WHY_BY_CATEGORY is law-specific EDITORIAL COPY ("This new law may affect your
// rights as a renter…"). It is not taxonomy vocabulary: it is a sentence about
// what a NEW LAW in that category means for a resident, which would make no
// sense on a Resource or a Flyer. Moving it into taxonomy.json alongside the
// shared vocabulary would put news-domain copy into the domain model. It was
// re-keyed from the old lowercase bucket names onto canonical category LABELS,
// and gained entries for the four categories that previously had none.
//
// THE FALLBACK CHANGED MEANING. "Delaware Legislation" is gone as a category
// tag. It described a content TYPE, not a subject — and ContentType.LAW (added
// in F1) expresses that properly. So an unclassifiable bill now carries EMPTY
// categoryTags and contentType LAW, which is the honest statement "this is a
// law, and we could not say what it is about". It keeps the generic
// why-it-matters sentence.
//
// That honesty has a visible cost worth recording: of 428 live bills, 253 are
// unclassified. Many genuinely are not civic-assistance content (pet stores,
// animal cruelty, the state flag). Some are misses — a bill titled "Relating to
// the Court of Chancery" scores 1 on "court", below MIN_SCORE, and is declined.
// The alternative is lowering the threshold, which is precisely how the
// five-category wetlands bill happened. Declining is the deliberate trade; the
// startup summary exists so the vocabulary can be improved with evidence.
//
// ORDERING NOTE: classify() is called BEFORE the "RELATING TO" title rewrite, so
// the classifier sees the raw bill text rather than the tidied clause. The raw
// description carries more signal.

// =============================================================================
// SLICE F2.1 UPDATE (Decision 034) — TWO FEEDS, TWO QUESTIONS
// =============================================================================
// This service now implements TWO interfaces and maintains TWO lists:
//
//   RssFeedSource.getRssItems()             relevance-gated  -> DISCOVERY
//   SignedLegislationSource.getSignedBills() ungated         -> PRESENTATION
//
// WHY THE SPLIT WAS NECESSARY, not merely tidy. Before it, one accessor served
// both. Adding a relevance gate would therefore have emptied the "New Delaware
// Laws" rotator of every bill the classifier could not categorize — roughly half
// of them — turning a factual legislative feed into an editorial selection
// without anyone deciding to. A discovery decision would have silently broken a
// presentation feature.
//
// The two answer genuinely different questions:
//
//   "What civic content did we admit?"     Categories, updates, search, AI.
//                                          Must exclude pet-store legislation.
//   "What has the Governor signed?"        The rotator. Must NOT exclude it —
//                                          the section is about legislation, not
//                                          about curated help.
//
// This keeps editorial taxonomy, source adaptation and legislation presentation
// as three independent concerns; none can change another by accident.
//
// THE GATE ITSELF is one line in fetchFeeds():
//
//     if (converted.classification().relevant()) { relevant.add(...); }
//
// Branching on relevant() and never on categoryTags — the engine owns the
// admission decision so it cannot drift across ingestion points.
//
// WHY ConvertedEntry EXISTS. convertEntry() must return both the item and the
// verdict, because classification has to happen exactly once: calling classify()
// again in fetchFeeds would double-count the startup summary that the keyword
// vocabulary is tuned from. A private two-field record is the honest way to
// return two things.
//
// WHY THE KEEP-LAST-GOOD GUARD KEYS ON allBills. A fetch that returns only
// irrelevant bills is a SUCCESSFUL fetch that admitted nothing — keying the
// guard on the relevant list would treat it as a network failure and preserve
// stale data indefinitely.
//
// TEST NOTE worth reading before editing RssFeedServiceTest: most fixtures there
// (money transmission, appropriations, resolutions, the state flag) are bills a
// resident has no use for, and they are CORRECTLY absent from getRssItems().
// Those tests exercise title extraction — a presentation concern — and read
// getSignedBills(). Only the two classification tests read the gated feed. That
// the two lists differ is the point.
//
// LIVE: 175 of 428 bills admitted; the rotator still shows 7, including "Pet
// Stores and Animal Welfare".
