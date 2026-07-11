package org.firststep.backend.news.service;

import java.util.List;

import org.firststep.backend.ai.service.DecisionAgentService;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.repository.NewsRepository;
import org.springframework.stereotype.Service;

@Service
public class NewsService implements DecisionAgentService.NewsServiceLike {

    private final NewsRepository repository;

    public NewsService(NewsRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<NewsItem> getAllNews() {
        return getAll();
    }

    public List<NewsItem> getAll() {
        return repository.findAll();
    }

}
