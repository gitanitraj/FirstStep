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
