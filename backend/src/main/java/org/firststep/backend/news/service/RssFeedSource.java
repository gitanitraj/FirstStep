package org.firststep.backend.news.service;

import org.firststep.backend.news.model.NewsItem;

import java.util.List;

public interface RssFeedSource {
    List<NewsItem> getRssItems();
}
