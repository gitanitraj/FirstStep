package org.firststep.backend.category.selection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.firststep.backend.category.model.CategoryDefinition;
import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.news.service.NewsService;
import org.firststep.backend.news.service.RssFeedSource;
import org.firststep.backend.originals.service.ArticleService;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.lifecycle.EditorialLifecycle;
import org.firststep.backend.shared.model.CivicContent;
import org.springframework.stereotype.Service;

/** IM-06 metadata adapter: only existing current services, never raw repositories. */
@Service
public class FeaturedTopicInventory {
    private final ResourceService resources;
    private final FlyerService flyers;
    private final NewsService news;
    private final RssFeedSource rss;
    private final ArticleService articles;
    private final FaqService faqs;
    private final ExpertAnswerService experts;
    private final EditorialLifecycle lifecycle;
    private final TaxonomyService taxonomy;
    private final AuthoredTopicEvidence authored;
    public record Snapshot(List<CivicContent> items, List<String> classificationLimitations) {}

    public FeaturedTopicInventory(ResourceService resources, FlyerService flyers, NewsService news,
            RssFeedSource rss, ArticleService articles, FaqService faqs, ExpertAnswerService experts,
            EditorialLifecycle lifecycle, TaxonomyService taxonomy, AuthoredTopicEvidence authored) {
        this.resources = resources; this.flyers = flyers; this.news = news; this.rss = rss;
        this.articles = articles; this.faqs = faqs; this.experts = experts;
        this.lifecycle = lifecycle; this.taxonomy = taxonomy; this.authored = authored;
    }

    public Snapshot current(String communityId, CategoryDefinition category) {
        List<CivicContent> all = new ArrayList<>();
        all.addAll(resources.getAll());
        all.addAll(flyers.getAll());
        all.addAll(news.getAll()); // Curated precedence matches the existing Category projection.
        all.addAll(rss.getRssItems()); // Relevance-admitted current cache, not complete remote inventory.
        all.addAll(articles.getPublishable());
        all.addAll(faqs.getAll());
        all.addAll(experts.getAll());
        var limitations = new java.util.TreeSet<>(authored.loadLimitations());
        var distinct = new LinkedHashMap<String, CivicContent>();
        for (CivicContent item : all) {
            if (item.id != null && !item.id.isBlank() && communityId.equals(item.communityId)
                    && lifecycle.isCurrent(item)
                    && taxonomy.matchesCategoryTags(category, item.categoryTags)
                    && taxonomy.permitsExpertCategoryParticipation(item)
                    && item.subcategory != null && taxonomy.isTopicOf(category.key(), item.subcategory)) {
                if (authored.permits(item, category)) distinct.putIfAbsent(item.id, item);
                else limitations.add("unproven_authored_category_topic:" + item.id);
            }
        }
        return new Snapshot(List.copyOf(distinct.values()), List.copyOf(limitations));
    }
}
