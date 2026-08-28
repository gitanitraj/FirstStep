package org.firststep.backend.originals.model;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deserializes the REAL {@code app/data/articles.json} onto the model.
 *
 * <p><b>Why this test exists.</b> Every model class here is annotated
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)}, which is right for
 * tolerating extra keys and dangerous for detecting missing ones: a field named
 * {@code dispositon} in the data, or renamed in Java, would be SILENTLY IGNORED.
 * Nothing would throw. Every flag would simply read as open, the article would
 * still be withheld, and the system would look entirely healthy while having lost
 * its review history.
 *
 * <p>No API surface exposes {@code editorialReview}, so a running application
 * cannot demonstrate this either — the boundary deliberately keeps review data
 * off public payloads. Unit tests over hand-built objects prove the LOGIC; this
 * proves THE LOGIC IS REACHED BY THE AUTHORED DATA.
 *
 * <p>It doubles as the only current check that articles.json is well-formed
 * against the model, since articles.json has no validator script. That gap is
 * recorded, not closed here.
 */
class ArticleDeserializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static List<Article> loadRealArticles() throws Exception {
        // Mirrors JsonArticleRepository.parse: records array, external data dir.
        JsonNode root = MAPPER.readTree(Path.of("../app/data/articles.json").toFile());
        JsonNode records = root.has("records") ? root.get("records") : root;
        return MAPPER.convertValue(records, new TypeReference<List<Article>>() {});
    }

    private static Article byId(List<Article> articles, String id) {
        return articles.stream().filter(a -> id.equals(a.id)).findFirst().orElseThrow();
    }

    @Test
    void shouldDeserializeEveryAuthoredArticleOntoTheModel() throws Exception {
        List<Article> articles = loadRealArticles();

        assertFalse(articles.isEmpty(), "articles.json must not be empty");
        for (Article a : articles) {
            assertNotNull(a.id, "every article needs an id");
            assertNotNull(a.title, a.id + " has no title");
            assertNotNull(a.body, a.id + " has no body");
            assertNotNull(a.editorialReview, a.id + " has no editorialReview - it could never be served");
        }
    }

    @Test
    void shouldReadEditorialReviewStatusFromAuthoredData() throws Exception {
        List<Article> articles = loadRealArticles();

        // The approved fixture proves the boundary admits as well as excludes.
        assertTrue(byId(articles, "OR-003").isPublishable(), "OR-003 is authored approved");
        assertFalse(byId(articles, "OR-001").isPublishable(), "OR-001 is authored flagged");
        assertFalse(byId(articles, "OR-002").isPublishable(), "OR-002 is authored flagged");
    }

    @Test
    void shouldReadFlagDispositionsFromAuthoredData() throws Exception {
        // The test that fails if `disposition` were misnamed on either side.
        Article rentEscrow = byId(loadRealArticles(), "OR-001");
        List<ReviewFlag> flags = rentEscrow.editorialReview.flags;

        long open = flags.stream().filter(ReviewFlag::isOpen).count();
        long dispositioned = flags.stream().filter(f -> !f.isOpen()).count();

        assertEquals(6, flags.size(), "Rent Escrow carries six flags");
        assertEquals(4, open, "four concerns remain outstanding");
        assertEquals(2, dispositioned, "one withdrawn, one resolved");
    }

    @Test
    void shouldDistinguishWithdrawnFromResolvedInAuthoredData() throws Exception {
        // The distinction must survive in the DATA, not just in the enum. This is
        // the recorded reviewer false positive.
        Article rentEscrow = byId(loadRealArticles(), "OR-001");

        ReviewFlag withdrawn = rentEscrow.editorialReview.flags.stream()
                .filter(f -> "unattributed-claim".equals(f.issue)).findFirst().orElseThrow();
        ReviewFlag resolved = rentEscrow.editorialReview.flags.stream()
                .filter(f -> "attribution-mismatch".equals(f.issue)).findFirst().orElseThrow();

        assertEquals(DispositionStatus.WITHDRAWN,
                withdrawn.disposition.resolvedStatus().orElseThrow(),
                "the inline-citation flag was a reviewer false positive");
        assertEquals(DispositionStatus.RESOLVED,
                resolved.disposition.resolvedStatus().orElseThrow(),
                "the date conflict was legitimate and has since been addressed");
    }

    @Test
    void shouldCarryCompleteDispositionsForEveryDispositionedFlag() throws Exception {
        // status, date, actor and reason travel together - an incomplete
        // disposition silently reopens the flag.
        Article rentEscrow = byId(loadRealArticles(), "OR-001");

        rentEscrow.editorialReview.flags.stream()
                .filter(f -> f.disposition != null)
                .forEach(f -> assertTrue(f.disposition.isComplete(),
                        "incomplete disposition on " + f.issue + " - it would silently reopen"));
    }

    @Test
    void shouldPreserveTheOriginalFindingOnDispositionedFlags() throws Exception {
        // Review history is evidence: dispositioning must not blank the finding.
        Article rentEscrow = byId(loadRealArticles(), "OR-001");

        rentEscrow.editorialReview.flags.stream()
                .filter(f -> f.disposition != null)
                .forEach(f -> {
                    assertNotNull(f.passage, "dispositioned flag lost its passage");
                    assertNotNull(f.issue, "dispositioned flag lost its issue");
                    assertNotNull(f.reason, "dispositioned flag lost its reason");
                });
    }
}
