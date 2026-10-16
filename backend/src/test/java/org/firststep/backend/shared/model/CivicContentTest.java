package org.firststep.backend.shared.model;

import java.util.List;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.news.model.NewsItem;
import org.firststep.backend.resource.model.Resource;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Locks in the CivicContent contract: every content type answers the same
 * questions with the same fields, and the editorial/descriptive split holds.
 */
class CivicContentTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldReportItsOwnContentTypeForEverySubclass() {
        assertEquals(ContentType.RESOURCE, new Resource().contentType);
        assertEquals(ContentType.NEWS, new NewsItem().contentType);
        assertEquals(ContentType.FLYER, new Flyer().contentType);
        assertEquals(ContentType.EXPERT, new ExpertAnswer().contentType);
        assertEquals(ContentType.EXPERT, new FAQ().contentType);
    }

    @Test
    void shouldAllowNewsItemToPresentAsLawWithoutChangingItsClass() {
        // Signed legislation is a NewsItem whose contentType is LAW. This is why
        // contentType is a per-instance field rather than an abstract method.
        NewsItem bill = new NewsItem();
        bill.contentType = ContentType.LAW;

        assertEquals(ContentType.LAW, bill.contentType);
    }

    @Test
    void shouldBindCategoryTagsFromSnakeCaseJsonKey() throws Exception {
        Flyer f = mapper.readValue(
                "{\"id\":\"FL-002\",\"category_tags\":[\"Housing\",\"Legal\"],"
                        + "\"subcategory\":\"Eviction Prevention\",\"tags\":[\"Free\"]}",
                Flyer.class);

        assertEquals(List.of("Housing", "Legal"), f.categoryTags);
        assertEquals("Eviction Prevention", f.subcategory);
        assertEquals(List.of("Free"), f.tags);
    }

    @Test
    void shouldKeepEditorialAndDescriptiveClassificationInSeparateFields() {
        // The bug this contract exists to prevent: one field meaning "editorial
        // classification" for one content type and "descriptive metadata" for
        // another (JsonNewsRepository used to load category_tags into tags).
        NewsItem n = new NewsItem();
        n.categoryTags = List.of("Housing");
        n.tags = List.of("rental-assistance", "eviction");

        assertEquals(List.of("Housing"), n.categoryTags);
        assertEquals(List.of("rental-assistance", "eviction"), n.tags);
    }

    @Test
    void shouldLeaveSubcategoryNullWhenSourceDoesNotClassifyToTopicLevel() throws Exception {
        NewsItem n = mapper.readValue("{\"id\":\"NP-001\",\"category_tags\":[\"Housing\"]}", NewsItem.class);

        assertNull(n.subcategory);
    }
}
