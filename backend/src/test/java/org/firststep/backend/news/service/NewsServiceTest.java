package org.firststep.backend.news.service;

import java.util.List;

import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.news.repository.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NewsServiceTest {

    private NewsRepository repository;
    private NewsService service;

    @BeforeEach
    void setUp() {
        repository = mock(NewsRepository.class);
        service = new NewsService(repository);
    }

    @Test
    void shouldReturnAllNewsFromRepositoryWhenGetAllIsCalled() {
        NewsItem item = new NewsItem();
        item.id = "n1";
        when(repository.findAll()).thenReturn(List.of(item));

        List<NewsItem> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals("n1", result.get(0).id);
    }
}
