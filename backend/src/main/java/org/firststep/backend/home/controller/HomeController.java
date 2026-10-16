package org.firststep.backend.home.controller;

import org.firststep.backend.home.dto.HomePayload;
import org.firststep.backend.home.service.HomeService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HomeController {

    private final HomeService service;

    public HomeController(HomeService service) {
        this.service = service;
    }

    /**
     * The homepage BFF — every section in one response.
     *
     * <p><b>No {@code communityId} parameter.</b> It was here to filter the
     * category tiles, and Slice H replaced those with authored pathways that are
     * the same for every resident. Nothing the homepage now returns is
     * community-scoped, and the frontend never sent it. A parameter that is
     * accepted and ignored is worse than one that is absent, so it is gone until
     * a section genuinely needs it. Granular endpoints keep theirs.
     */
    @GetMapping("/home")
    public ResponseEntity<ApiResponse<HomePayload>> getHome() {
        return ResponseEntity.ok(ApiResponse.success(service.getHome()));
    }
}
