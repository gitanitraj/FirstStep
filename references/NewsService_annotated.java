package org.firststep.backend.news.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// NewsService is the news slice's thin service layer between NewsController
// and NewsRepository — exposes getAll(), and implements
// DecisionAgentService.NewsServiceLike so the AI slice can fetch news
// without depending on this concrete class.
// =============================================================================

import java.util.List;

import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.service.DecisionAgentService;
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

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Same pattern as ResourceService: v1 did its own JSON loading directly;
// this version delegates entirely to NewsRepository (constructor-injected).
// Still implements DecisionAgentService.NewsServiceLike, the same
// pre-existing ai-slice-owned coupling direction discussed in
// ResourceService_annotated.java — carried over unchanged, not restructured
// in this pass.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - NewsController calls getAll() for the static /api/news endpoint.
// - RssFeedService/RssFeedSource are a SEPARATE path for /api/news/rss —
//   NewsService has no relationship to them; NewsController depends on both
//   independently.
// - DecisionAgentService calls getAllNews() through the NewsServiceLike
//   interface.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - None beyond what's covered in ResourceService_annotated.java — this
//   class mirrors that one's shape and reasoning exactly.
// =============================================================================
