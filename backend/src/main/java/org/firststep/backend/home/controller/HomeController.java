package org.firststep.backend.home.controller;

import org.firststep.backend.home.dto.HomePayload;
import org.firststep.backend.home.service.HomeService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HomeController {

    private final HomeService service;

    public HomeController(HomeService service) {
        this.service = service;
    }

    @GetMapping("/home")
    public ResponseEntity<ApiResponse<HomePayload>> getHome(
            @RequestParam(required = false) String communityId) {
        return ResponseEntity.ok(ApiResponse.success(service.getHome(communityId)));
    }
}
