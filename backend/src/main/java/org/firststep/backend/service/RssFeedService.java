package org.firststep.backend.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.firststep.backend.model.NewsItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class RssFeedService {

    @Value("${news.rss.urls:}")
    private String rssFeedUrls;

    private List<NewsItem> rssItems = Collections.emptyList();

    @Scheduled(fixedDelayString = "${news.rss.refresh-interval:3600000}", initialDelay = 5000)
    public void fetchFeeds() {
        if (rssFeedUrls == null || rssFeedUrls.isBlank()) return;

        List<NewsItem> fetched = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (String feedUrl : rssFeedUrls.split(",")) {
            String url = feedUrl.trim();
            if (url.isEmpty()) continue;
            try {
                SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
                for (SyndEntry entry : feed.getEntries()) {
                    NewsItem item = new NewsItem();
                    item.id = "rss-" + UUID.randomUUID();
                    item.headline = entry.getTitle() != null ? entry.getTitle().trim() : "";
                    item.summary = entry.getDescription() != null
                            ? stripHtml(entry.getDescription().getValue()) : "";
                    item.body = item.summary;
                    item.sourceName = feed.getTitle() != null ? feed.getTitle() : url;
                    item.sourceUrl = entry.getLink();
                    item.published = entry.getPublishedDate() != null
                            ? sdf.format(entry.getPublishedDate()) : sdf.format(new Date());
                    item.active = true;
                    item.type = "general-news";
                    item.urgency = "standard";
                    item.categoryTags = List.of("Community", "Updates");
                    item.whyItMatters = "Stay informed about news and updates in your community.";
                    fetched.add(item);
                }
                System.out.println("RSS: loaded " + feed.getEntries().size() + " entries from " + url);
            } catch (Exception e) {
                System.err.println("RSS fetch failed for " + url + ": " + e.getMessage());
            }
        }

        if (!fetched.isEmpty()) {
            rssItems = Collections.unmodifiableList(fetched);
        }
    }

    public List<NewsItem> getRssItems() {
        return rssItems;
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", "").replaceAll("&nbsp;", " ")
                   .replaceAll("&amp;", "&").replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">").trim();
    }
}
