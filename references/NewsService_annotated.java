/*
 * IM-01 annotated reference — full mirror of backend/src/main/java/org/firststep/backend/news/service/NewsService.java
 *
 * WHAT: Preserve the existing slice while enforcing the shared current-inventory boundary.
 * WHY: PC-01 requires one editorial lifecycle interpretation; repositories retain history.
 * INTERACTION: current service queries apply EditorialLifecycle before projection/counting;
 * classification, Community selection and article approval retain their existing owners.
 * ALTERNATIVE: filtering separately in every destination would duplicate date/status policy.
 * Event occurrence is deliberately separate (PC-02 / IM-10). See the IM-01 execution report.
 */
package org.firststep.backend.news.service;

import java.util.List;

import org.firststep.backend.ai.service.DecisionAgentService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.shared.lifecycle.EditorialLifecycle;
import org.springframework.stereotype.Service;

@Service
public class NewsService implements DecisionAgentService.NewsServiceLike {

    private final EditorialLifecycle lifecycle;

    private final NewsRepository repository;

    public NewsService(NewsRepository repository, EditorialLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        this.repository = repository;
    }

    @Override
    /** Current resident inventory; the repository retains expired and withdrawn records. */
    public List<NewsItem> getAllNews() {
        return getAll();
    }

    public List<NewsItem> getAll() {
        return repository.findAll().stream().filter(lifecycle::isCurrent).toList();
    }

}
