package org.firststep.backend.legislation.service;

import java.util.ArrayList;
import java.util.List;

import org.firststep.backend.legislation.dto.LawItem;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.shared.model.ContentSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegislationServiceTest {

    private static NewsItem bill(String title, String published, String url) {
        NewsItem n = new NewsItem();
        n.title = title;
        n.published = published;
        if (url != null) {
            ContentSource cs = new ContentSource();
            cs.url = url;
            n.contentSource = cs;
        }
        return n;
    }

    private static LegislationService service(List<NewsItem> rss) {
        return new LegislationService(() -> rss);
    }

    @Test
    void shouldReturnBillsNewestFirstMappedToLawItem() {
        LegislationService service = service(List.of(
                bill("Older bill", "2026-01-01", "https://legis.example/1"),
                bill("Newer bill", "2026-06-01", "https://legis.example/2")));

        List<LawItem> bills = service.getRecentSignedBills();

        assertEquals("Newer bill", bills.get(0).title());
        assertEquals("https://legis.example/2", bills.get(0).url());
        assertEquals("2026-06-01", bills.get(0).date());
        assertEquals("Older bill", bills.get(1).title());
    }

    @Test
    void shouldCapAtSevenBills() {
        List<NewsItem> many = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            many.add(bill("Bill " + i, String.format("2026-01-%02d", i + 1), null));
        }
        assertEquals(7, service(many).getRecentSignedBills().size());
    }

    @Test
    void shouldTolerateMissingContentSourceUrl() {
        LegislationService service = service(List.of(bill("No url bill", "2026-05-01", null)));

        LawItem item = service.getRecentSignedBills().get(0);

        assertEquals("No url bill", item.title());
        assertEquals(null, item.url());
    }
}
