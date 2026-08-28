/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../originals/controller/OriginalsController.java
 * Slice K. See references/decisions.md Decision 048.
 * =============================================================================
 *
 * WHAT IT DOES: serves one approved First Step Original article in full.
 *
 * THE ERROR RESPONSE IS PART OF THE BOUNDARY
 * ------------------------------------------
 * The subtle failure this controller avoids: a 404 that reads "not yet approved"
 * would ITSELF disclose that a draft exists at that id. The boundary would leak
 * through its own error handling, and a curious reader could enumerate ids to map
 * the editorial pipeline.
 *
 * So an unapproved article and a nonexistent one produce the SAME exception with
 * the SAME text, and a test asserts the two responses are identical. Telling them
 * apart is the future administrative layer's job, never a public reader's.
 *
 * That test initially failed on the response `timestamp`, which differs by
 * microseconds between any two calls. The assertion was narrowed to status +
 * errorCode + errorMessage rather than the raw body — the timestamp carries no
 * information about the article, and comparing it was over-broad rather than
 * strict.
 *
 * WHY THERE IS NO FILTERING HERE
 * ------------------------------
 * getPublishableDetail() has already applied BOTH the review boundary and the
 * public projection, so there is no condition in this class to forget. A
 * controller that re-checked approval would imply the service could not be
 * trusted to, and a controller that built its own projection could quietly
 * include a field ArticleDetail excludes.
 *
 * WHY THERE IS NO INDEX ENDPOINT
 * ------------------------------
 * No GET /api/originals. The homepage Originals panel is the index today, and an
 * endpoint with no caller would be speculative — the same rule that kept
 * getAwaitingReview() out of ArticleService.
 * ============================================================================= */

package org.firststep.backend.originals.controller;

import org.firststep.backend.originals.dto.ArticleDetail;
import org.firststep.backend.originals.service.ArticleService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.firststep.backend.shared.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The reading surface for First Step Original articles.
 *
 * <pre>
 *   GET /api/originals/{id}   one approved article, in full
 * </pre>
 *
 * <p><b>An unapproved article and a nonexistent one are indistinguishable
 * here</b>, deliberately and down to the message. A 404 that read "not yet
 * approved" would itself disclose that a draft exists at that id — the boundary
 * would leak through its own error handling. So both produce the same
 * {@link NotFoundException} with the same text, and telling them apart is the
 * future administrative layer's job rather than a public reader's.
 *
 * <p>Note what this controller does NOT do: it applies no filtering of its own.
 * {@code ArticleService.getPublishableDetail} has already applied the review
 * boundary AND the public projection, so there is no condition here to forget.
 *
 * <p>There is no {@code GET /api/originals} index. The homepage Originals panel
 * is the index today, and an endpoint with no caller would be speculative.
 */
@RestController
@RequestMapping("/api")
public class OriginalsController {

    private final ArticleService articleService;

    public OriginalsController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/originals/{id}")
    public ResponseEntity<ApiResponse<ArticleDetail>> getArticle(@PathVariable String id) {
        ArticleDetail article = articleService.getPublishableDetail(id)
                .orElseThrow(() -> new NotFoundException("Article not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(article));
    }
}
