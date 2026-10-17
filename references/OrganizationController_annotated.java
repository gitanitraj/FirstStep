/*
 * WHAT: Complete annotated mirror of backend/src/main/java/org/firststep/backend/organization/controller/OrganizationController.java for IM-08.
 * WHY: Integrate approved selector outcomes with scoped, accessible resident navigation.
 * INTERACTION: Existing eligibility and collection services own admission; the destination
 * projection allowlists cards/links, while React owns responsive presentation and route scope.
 * ALTERNATIVES: Reimplementing selectors or deriving providers from source names would violate
 * predecessor contracts. Tests cover pass-through, actual catalog HTTP, and browser journeys.
 */
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
import org.springframework.web.bind.annotation.RequestParam;

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
    public ResponseEntity<ApiResponse<OrganizationPage>> getOrganization(@PathVariable String slug, @RequestParam(required = false) String communityId) {
        OrganizationPage page = (communityId == null ? organizationService.getBySlug(slug)
                : organizationService.getBySlug(slug, communityId))
                .orElseThrow(() -> new NotFoundException("Unknown organization: " + slug));
        return ResponseEntity.ok(ApiResponse.success(page));
    }
}
