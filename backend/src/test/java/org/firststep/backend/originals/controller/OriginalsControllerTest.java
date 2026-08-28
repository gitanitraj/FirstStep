package org.firststep.backend.originals.controller;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.originals.model.Article;
import org.firststep.backend.originals.model.EditorialReview;
import org.firststep.backend.originals.model.FlagDisposition;
import org.firststep.backend.originals.model.ReviewFlag;
import org.firststep.backend.originals.repository.ArticleRepository;
import org.firststep.backend.originals.service.ArticleService;
import org.firststep.backend.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The public contract for the Originals reading surface.
 *
 * <p>Two things are under test and only one of them is the happy path. The other
 * is that <b>internal editorial state cannot reach a reader</b> — not through the
 * payload, and not through the difference between two error responses.
 */
@WebMvcTest(OriginalsController.class)
@ContextConfiguration(classes = {OriginalsController.class, GlobalExceptionHandler.class,
        OriginalsControllerTest.TestConfig.class})
class OriginalsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** Every field populated, including the ones that must never be served. */
    private static Article article(String id, String reviewStatus) {
        Article a = new Article();
        a.id = id;
        a.title = "How to read a Delaware eviction notice";
        a.summary = "What the dates mean.";
        a.whyItMatters = "Tenants miss the window that matters.";
        a.body = "Full text of the article.";
        a.byline = "Admin";
        a.disclosure = "ai-assisted";
        a.publishDate = "2026-08-10";
        a.updatedDate = "2026-08-12";
        a.categoryTags = List.of("Housing", "Legal");
        a.subcategory = "Eviction Prevention";
        a.verified = true;
        a.communityId = "wilmington-de";
        a.generatedBy = "ai:secret-model-name";

        EditorialReview review = new EditorialReview();
        review.status = reviewStatus;
        review.reviewedDate = "2026-08-12";
        review.reviewer = "human:editorial";
        ReviewFlag flag = new ReviewFlag();
        flag.passage = "an internal passage nobody outside editorial should read";
        flag.issue = "advocacy";
        flag.reason = "internal reviewer reasoning";
        flag.recommendation = "remove";
        FlagDisposition disposition = new FlagDisposition();
        disposition.status = "overridden";
        disposition.date = "2026-08-12";
        disposition.actor = "human:editorial";
        disposition.reason = "internal override rationale";
        flag.disposition = disposition;
        review.flags = List.of(flag);
        a.editorialReview = review;
        return a;
    }

    @Configuration
    static class TestConfig {
        @Bean
        ArticleService articleService() {
            List<Article> all = List.of(article("OR-003", "approved"), article("OR-001", "flagged"));
            return new ArticleService(new ArticleRepository() {
                @Override
                public List<Article> findAll() {
                    return all;
                }

                @Override
                public Optional<Article> findById(String id) {
                    return all.stream().filter(a -> a.id.equals(id)).findFirst();
                }
            });
        }
    }

    @Test
    void shouldServeApprovedArticleWithItsFullText() throws Exception {
        mockMvc.perform(get("/api/originals/OR-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("OR-003"))
                .andExpect(jsonPath("$.data.body").value("Full text of the article."))
                .andExpect(jsonPath("$.data.whyItMatters").value("Tenants miss the window that matters."))
                .andExpect(jsonPath("$.data.byline").value("Admin"))
                .andExpect(jsonPath("$.data.disclosure").value("ai-assisted"))
                .andExpect(jsonPath("$.data.subcategory").value("Eviction Prevention"))
                .andExpect(jsonPath("$.data.categoryTags[0]").value("Housing"));
    }

    // ---- the serialization guard -------------------------------------------

    @Test
    void shouldNotSerializeAnyEditorialReviewDataInThePublicPayload() throws Exception {
        // A guard against a FUTURE edit adding a component to ArticleDetail.
        // Today the exclusion is structural — the record has no such field — so
        // this test asserts the property rather than the implementation, and it
        // greps the raw JSON rather than named paths so a RENAMED leak is caught
        // too.
        MvcResult result = mockMvc.perform(get("/api/originals/OR-003"))
                .andExpect(status().isOk())
                .andReturn();

        String payload = result.getResponse().getContentAsString();

        for (String forbidden : List.of(
                "editorialReview", "reviewer", "flags", "disposition", "overridden",
                "generatedBy", "secret-model-name", "verified", "communityId",
                "internal reviewer reasoning", "internal override rationale",
                "an internal passage nobody outside editorial should read")) {
            assertFalse(payload.contains(forbidden),
                    "public payload leaked internal editorial data: '" + forbidden + "' in " + payload);
        }
    }

    @Test
    void shouldExposeExactlyTheAgreedPublicFieldsAndNoOthers() throws Exception {
        // Pins the contract's SIZE. A new component added to ArticleDetail fails
        // here even if its name is not on the forbidden list above.
        MvcResult result = mockMvc.perform(get("/api/originals/OR-003")).andReturn();
        com.fasterxml.jackson.databind.JsonNode data = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString()).get("data");

        List<String> expected = List.of("id", "title", "summary", "whyItMatters", "body",
                "byline", "disclosure", "publishDate", "updatedDate", "categoryTags", "subcategory");

        List<String> actual = new java.util.ArrayList<>();
        data.fieldNames().forEachRemaining(actual::add);
        java.util.Collections.sort(actual);
        List<String> sortedExpected = new java.util.ArrayList<>(expected);
        java.util.Collections.sort(sortedExpected);

        assertEquals(sortedExpected, actual, "the public contract changed");
    }

    // ---- the boundary, through the URL --------------------------------------

    /** status + errorCode + errorMessage, with the id normalized away. */
    private String errorShapeOf(String id) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/originals/" + id)).andReturn();
        com.fasterxml.jackson.databind.JsonNode body = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString());
        return result.getResponse().getStatus()
                + "|" + body.get("success")
                + "|" + body.get("errorCode")
                + "|" + body.get("errorMessage").asText().replace(id, "ID");
    }

    @Test
    void shouldReturnNotFoundForAnUnapprovedArticle() throws Exception {
        mockMvc.perform(get("/api/originals/OR-001"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGiveIdenticalResponsesForUnapprovedAndNonexistentArticles() throws Exception {
        // The 404 must not become a disclosure that a draft exists at that id.
        // `timestamp` is excluded: it differs by microseconds between any two
        // responses and carries no information about the article.
        assertEquals(errorShapeOf("OR-999"), errorShapeOf("OR-001"),
                "an unapproved article must be indistinguishable from one that does not exist");
    }
}
