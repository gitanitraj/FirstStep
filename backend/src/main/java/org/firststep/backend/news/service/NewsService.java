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
