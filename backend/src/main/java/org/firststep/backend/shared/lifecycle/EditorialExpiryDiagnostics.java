package org.firststep.backend.shared.lifecycle;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.firststep.backend.expert.repository.ExpertAnswerRepository;
import org.firststep.backend.expert.repository.FaqRepository;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.news.service.RssFeedService;
import org.firststep.backend.originals.model.Article;
import org.firststep.backend.originals.repository.ArticleRepository;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.shared.model.CivicContent;
import org.firststep.backend.shared.model.ContentType;
import org.springframework.stereotype.Service;

/** Internal, read-only stewardship view of stored records; never a resident DTO. */
@Service
public class EditorialExpiryDiagnostics {
    public record Context(String id, String title, String contentClass, ContentType contentType,
            String communityId, String contentSourceId, String expirationDate, String status,
            String articleReviewStatus, boolean statusAllowsPublication, Boolean articleApproved) {}
    public record Finding(Context context, EditorialLifecycle.Assessment assessment,
            long daysUntilExpiry, String reason) {}
    public record Problem(Context context, String reason) {}
    public record Report(Instant asOf, String communityId, int inspectedRecords, int cachedRssRecords,
            List<Finding> findings, List<Problem> problems) {}

    private static final Comparator<Context> ORDER = Comparator
            .comparing((Context c) -> value(c.communityId()))
            .thenComparing(c -> value(c.expirationDate()))
            .thenComparing(Context::contentClass)
            .thenComparing(c -> String.valueOf(c.contentType()))
            .thenComparing(c -> value(c.id()))
            .thenComparing(c -> value(c.title()))
            .thenComparing(c -> value(c.contentSourceId()))
            .thenComparing(c -> value(c.status()))
            .thenComparing(c -> value(c.articleReviewStatus()));

    private final EditorialLifecycle lifecycle;
    private final Clock clock;
    private final ResourceRepository resources;
    private final NewsRepository news;
    private final FlyerRepository flyers;
    private final FaqRepository faqs;
    private final ExpertAnswerRepository experts;
    private final ArticleRepository articles;
    private final RssFeedService rss;

    public EditorialExpiryDiagnostics(EditorialLifecycle lifecycle, Clock clock,
            ResourceRepository resources, NewsRepository news, FlyerRepository flyers,
            FaqRepository faqs, ExpertAnswerRepository experts, ArticleRepository articles,
            RssFeedService rss) {
        this.lifecycle = lifecycle;
        this.clock = clock;
        this.resources = resources;
        this.news = news;
        this.flyers = flyers;
        this.faqs = faqs;
        this.experts = experts;
        this.articles = articles;
        this.rss = rss;
    }

    /** Null/blank Community requests explicitly inspect all Communities, retaining each owner. */
    public Report collect(String communityId) {
        String scope = communityId == null || communityId.isBlank() ? null : communityId;
        List<CivicContent> stored = new ArrayList<>();
        stored.addAll(resources.findAll());
        stored.addAll(news.findAll());
        stored.addAll(flyers.findAll());
        stored.addAll(faqs.findAll());
        stored.addAll(experts.findAll());
        stored.addAll(articles.findAll());
        List<? extends CivicContent> cached = rss.getStoredRssItems();
        stored.addAll(cached);
        List<Finding> findings = new ArrayList<>();
        List<Problem> problems = new ArrayList<>();
        int inspected = 0;
        for (CivicContent item : stored) {
            if (scope != null && !scope.equals(item.communityId)) continue;
            inspected++;
            Context context = contextFor(item);
            try {
                var assessment = lifecycle.assess(item);
                if (assessment.editorialAttentionRequired()) {
                    String reason = assessment.state() == EditorialLifecycle.State.APPROACHING_EXPIRY
                            ? "Editorial expiry is approaching; verify accuracy and continuing usefulness."
                            : "Editorial validity has ended; review for authorized renewal or retained history.";
                    findings.add(new Finding(context, assessment, lifecycle.daysUntilExpiry(item).orElseThrow(), reason));
                }
            } catch (DateTimeParseException invalidDate) {
                // Isolate the record without weakening the resident assessment's fail-fast contract.
                problems.add(new Problem(context, "Invalid editorial expiration date; expected a valid ISO calendar date (YYYY-MM-DD). Investigate the source record; no lifecycle state was assigned."));
            }
        }
        findings.sort(Comparator.comparing(Finding::context, ORDER));
        problems.sort(Comparator.comparing(Problem::context, ORDER));
        int includedCache = (int) cached.stream().filter(c -> scope == null || scope.equals(c.communityId)).count();
        return new Report(clock.instant(), scope, inspected, includedCache, List.copyOf(findings), List.copyOf(problems));
    }

    private Context contextFor(CivicContent item) {
        Article article = item instanceof Article a ? a : null;
        return new Context(item.id, item.title, item.getClass().getSimpleName(), item.contentType,
                item.communityId, item.contentSource == null ? null : item.contentSource.id,
                item.expirationDate, item.status,
                article == null || article.editorialReview == null ? null : article.editorialReview.status,
                lifecycle.isPublicStatus(item), article == null ? null : article.isPublishable());
    }

    private static String value(String value) { return value == null ? "" : value; }
}
