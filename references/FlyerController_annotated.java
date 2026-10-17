/*
 * Annotated reference: backend/src/main/java/org/firststep/backend/flyer/controller/FlyerController.java
 * WHAT: Preserves the complete source below; see the explicit allowlists and optional exact-owner filters in the source.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Controllers adapt internal models only at the resident boundary; tests and evidence exercise real repositories plus labeled synthetic counterexamples.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
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
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api")
public class FlyerController {

    private final FlyerService service;

    public FlyerController(FlyerService service) {
        this.service = service;
    }

    @GetMapping("/flyers")
    public ResponseEntity<ApiResponse<List<FlyerSummary>>> getAll(@RequestParam(required = false) String communityId) {
        return ResponseEntity.ok(ApiResponse.success(service.getAll().stream().filter(c -> communityId == null || communityId.equals(c.communityId)).map(f -> FlyerSummary.from(f, service)).toList()));
    }

    @GetMapping("/flyers/{id}")
    public ResponseEntity<ApiResponse<FlyerSummary>> getById(@PathVariable String id, @RequestParam(required = false) String communityId) {
        Flyer flyer = service.getById(id)
                .filter(c -> communityId == null || communityId.equals(c.communityId))
                .orElseThrow(() -> new NotFoundException("Flyer not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(FlyerSummary.from(flyer, service)));
    }
}
