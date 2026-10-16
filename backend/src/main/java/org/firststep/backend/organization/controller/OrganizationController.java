package org.firststep.backend.organization.controller;

import org.firststep.backend.organization.dto.OrganizationPage;
import org.firststep.backend.organization.service.OrganizationService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.firststep.backend.shared.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <pre>
 *   GET /api/organization/{slug}
 * </pre>
 *
 * <p><b>The path variable is a SLUG, and this is the only place a slug is
 * accepted.</b> Everything downstream works in stable IDs. Resolving the slug
 * here, once, at the system's edge, is what keeps a display-derived string from
 * leaking inward and becoming identity again.
 *
 * <p>An unknown slug is a 404 naming it, matching CategoryController and
 * CommunityNoticesController. Note what that covers: an organization that was
 * RENAMED has a new slug, so its old URL 404s. That is a real consequence of
 * shipping identity without alias history, and it is the honest behaviour —
 * better than resolving stale URLs by guessing at a name.
 */
@RestController
@RequestMapping("/api")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/organization/{slug}")
    public ResponseEntity<ApiResponse<OrganizationPage>> getOrganization(@PathVariable String slug) {
        OrganizationPage page = organizationService.getBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Unknown organization: " + slug));
        return ResponseEntity.ok(ApiResponse.success(page));
    }
}
