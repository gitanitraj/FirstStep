/*
 * IM-01 annotated test reference — full mirror of backend/src/test/java/org/firststep/backend/news/service/NewsServiceTest.java
 * WHAT: Existing regression test with explicit fixed-clock service dependency.
 * WHY: prove behavior using deterministic time and preserve unrelated test expectations.
 * INTERACTION: see individual test names and fixtures; only lifecycle dependencies were
 * added to existing service construction. No assertion was weakened to hide expiry failures.
 * ALTERNATIVE: machine-clock fixtures would make future runs depend on the date of execution.
 */
package org.firststep.backend.news.service;

import org.firststep.backend.shared.lifecycle.LifecycleFixture;

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
        service = new NewsService(repository, LifecycleFixture.fixed());
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
