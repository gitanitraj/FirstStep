/*
 * Annotated reference: backend/src/main/java/org/firststep/backend/resource/controller/ResourceController.java
 * WHAT: Preserves the complete source below; see the explicit allowlists and optional exact-owner filters in the source.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Controllers adapt internal models only at the resident boundary; tests and evidence exercise real repositories plus labeled synthetic counterexamples.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
package org.firststep.backend.resource.controller;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.firststep.backend.shared.dto.ResidentContent;
import org.springframework.web.bind.annotation.RequestParam;
import org.firststep.backend.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ResourceController {

    private final ResourceService service;

    @Value("${app.seasonal.images.dir:backend/src/main/resources/static/images/seasonal}")
    private String seasonalImagesDir;

    public ResourceController(ResourceService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/resources")
    public ResponseEntity<ApiResponse<List<Object>>> getAll(@RequestParam(required = false) String communityId) {
        return ResponseEntity.ok(ApiResponse.success(service.getAll().stream()
                .filter(c -> communityId == null || communityId.equals(c.communityId))
                .map(ResidentContent::from).toList()));
    }

    @GetMapping("/resources/{id}")
    public ResponseEntity<ApiResponse<Object>> getById(@PathVariable String id, @RequestParam(required = false) String communityId) {
        Resource resource = service.getById(id)
                .filter(c -> communityId == null || communityId.equals(c.communityId))
                .orElseThrow(() -> new NotFoundException("Resource not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(ResidentContent.from(resource)));
    }

    @GetMapping("/seasonal-images")
    public ResponseEntity<List<String>> getSeasonalImages() {
        File dir = new File(seasonalImagesDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<String> files = Arrays.stream(dir.listFiles())
                .filter(f -> f.isFile() && f.getName().matches("(?i).*\\.(jpg|jpeg|png|gif|webp)"))
                .map(f -> "images/seasonal/" + f.getName())
                .sorted()
                .collect(Collectors.toList());
        return ResponseEntity.ok(files);
    }
}
