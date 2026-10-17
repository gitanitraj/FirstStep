package org.firststep.backend.flyer.controller;

import java.util.List;

import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.dto.FlyerSummary;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.firststep.backend.shared.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FlyerController {

    private final FlyerService service;

    public FlyerController(FlyerService service) {
        this.service = service;
    }

    @GetMapping("/flyers")
    public ResponseEntity<ApiResponse<List<FlyerSummary>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll().stream().map(f -> FlyerSummary.from(f, service)).toList()));
    }

    @GetMapping("/flyers/{id}")
    public ResponseEntity<ApiResponse<FlyerSummary>> getById(@PathVariable String id) {
        Flyer flyer = service.getById(id)
                .orElseThrow(() -> new NotFoundException("Flyer not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(FlyerSummary.from(flyer, service)));
    }
}
