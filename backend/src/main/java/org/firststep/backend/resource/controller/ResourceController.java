package org.firststep.backend.resource.controller;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.service.ResourceService;
import org.firststep.backend.shared.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<List<Resource>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/resources/{id}")
    public ResponseEntity<ApiResponse<Resource>> getById(@PathVariable String id) {
        Resource resource = service.getById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(resource));
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
