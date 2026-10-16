/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../notices/controller/CommunityNoticesController.java
 * Slice J. See references/decisions.md Decision 046.
 * =============================================================================
 *
 * WHAT IT DOES
 * ------------
 * Owns the /api/community-notices URL family and does nothing else: resolve the
 * path variable to a NoticeView, delegate, wrap in ApiResponse. No filtering, no
 * sorting, no counting — those belong to the service.
 *
 * WHY TWO MAPPINGS FOR ONE PAGE
 * -----------------------------
 * Spring will not match an empty path variable, so /api/community-notices and
 * /api/community-notices/{view} need separate handlers. Both return the same DTO
 * and both call the same service method; the landing route simply passes
 * NoticeView.OVERVIEW explicitly. The alternative — a required ?view= query
 * parameter — would have made the landing route's URL uglier and less shareable
 * for no gain.
 *
 * WHY AN UNKNOWN VIEW IS 404 AND NOT A REDIRECT
 * ---------------------------------------------
 * A view that EXISTS and is empty and a view that DOES NOT EXIST are different
 * facts, and they must not share a status code:
 *
 *     /api/community-notices/meetings      200, items: []   (nothing posted)
 *     /api/community-notices/newsletters   404              (no such view)
 *
 * Quietly rendering the landing page for a bad link tells a resident nothing
 * went wrong when something did, and hides the broken link from whoever
 * published it. Matches CategoryController's treatment of an unknown key.
 *
 * The exception message NAMES the unrecognized view. The controller test asserts
 * that exact string rather than merely a non-200 — per the Slice J testing rule,
 * a negative test must verify the intended failure path, not just some failure.
 *
 * HOW IT FITS: NotFoundException is translated to the JSON error envelope by
 * GlobalExceptionHandler, so this class never builds an error body itself.
 * ============================================================================= */

package org.firststep.backend.notices.controller;

import org.firststep.backend.notices.dto.CommunityNoticesPage;
import org.firststep.backend.notices.model.NoticeView;
import org.firststep.backend.notices.service.CommunityNoticesService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.firststep.backend.shared.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Owns the {@code /api/community-notices*} URL family.
 *
 * <pre>
 *   GET /api/community-notices          the landing state — counts + previews
 *   GET /api/community-notices/{view}   one discovery view — counts + items
 * </pre>
 *
 * <p><b>ONE endpoint shape for five routes</b>, because it is one page in five
 * states. The view is a path variable so the URL stays the source of truth: a
 * resident can share, bookmark or type any of the five and get the right state
 * without visiting the landing route first.
 *
 * <p>An unknown view is a 404 rather than a silent fall back to the landing page,
 * matching CategoryController. A view that EXISTS and is empty and a view that
 * DOES NOT EXIST are different facts, and quietly redirecting a bad link tells a
 * resident nothing went wrong when something did.
 */
@RestController
@RequestMapping("/api")
public class CommunityNoticesController {

    private final CommunityNoticesService service;

    public CommunityNoticesController(CommunityNoticesService service) {
        this.service = service;
    }

    @GetMapping("/community-notices")
    public ResponseEntity<ApiResponse<CommunityNoticesPage>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(service.getPage(NoticeView.OVERVIEW)));
    }

    @GetMapping("/community-notices/{view}")
    public ResponseEntity<ApiResponse<CommunityNoticesPage>> getView(@PathVariable String view) {
        NoticeView resolved = NoticeView.fromKey(view)
                .orElseThrow(() -> new NotFoundException("Unknown notices view: " + view));
        return ResponseEntity.ok(ApiResponse.success(service.getPage(resolved)));
    }
}
