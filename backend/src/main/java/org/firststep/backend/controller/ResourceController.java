package org.firststep.backend.controller;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.firststep.backend.model.Resource;
import org.firststep.backend.service.ResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ResourceController {

    private final ResourceService service;

    public ResourceController(ResourceService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/resources")
    public ResponseEntity<List<Resource>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/resources/{id}")
    public ResponseEntity<Resource> getById(@PathVariable String id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/seasonal-images")
    public ResponseEntity<List<String>> getSeasonalImages() {
        File dir = new File("backend/src/main/resources/static/images/seasonal");
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
