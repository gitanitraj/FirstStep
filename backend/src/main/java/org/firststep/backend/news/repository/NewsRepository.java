package org.firststep.backend.news.repository;

import java.util.List;

import org.firststep.backend.news.model.NewsItem;

public interface NewsRepository {
    List<NewsItem> findAll();
}
