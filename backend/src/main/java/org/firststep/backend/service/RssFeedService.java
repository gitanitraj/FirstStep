package org.firststep.backend.service;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.firststep.backend.model.NewsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class RssFeedService implements RssFeedSource {

    private static final Logger log = LoggerFactory.getLogger(RssFeedService.class);

    @Value("${news.rss.urls:}")
    private String rssFeedUrls;

    private volatile List<NewsItem> rssItems = List.of();

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");

    @Scheduled(
            fixedDelayString = "${news.rss.refresh-interval:3600000}",
            initialDelay = 5000
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

    private NewsItem convertEntry(SyndFeed feed, SyndEntry entry) {
        NewsItem item = new NewsItem();

        item.id = "rss-" + UUID.randomUUID();
        item.headline = safe(entry.getTitle());
        item.summary = extractSummary(entry);
        item.body = item.summary;

        item.sourceName = feed.getTitle() != null ? feed.getTitle() : "RSS Feed";
        item.sourceUrl = entry.getLink();

        Date published = entry.getPublishedDate() != null
                ? entry.getPublishedDate()
                : entry.getUpdatedDate() != null
                    ? entry.getUpdatedDate()
                    : new Date();

        item.published = DATE_FMT.format(published);

        item.active = true;
        item.type = "general-news";
        item.urgency = "standard";
        item.categoryTags = List.of("Community", "Updates");
        item.whyItMatters = "Stay informed about news and updates in your community.";

        return item;
    }

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

        // 3. Media module fallback (WITN22 sometimes uses this)
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
