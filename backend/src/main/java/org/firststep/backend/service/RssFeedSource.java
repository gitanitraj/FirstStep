package org.firststep.backend.service;

import org.firststep.backend.model.NewsItem;

import java.util.List;

public interface RssFeedSource {
    List<NewsItem> getRssItems();
}
