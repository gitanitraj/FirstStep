package org.firststep.backend.updates.controller;

import java.util.List;

import org.firststep.backend.shared.dto.ApiResponse;
import org.firststep.backend.updates.dto.UpdateItem;
import org.firststep.backend.updates.service.UpdatesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UpdatesController {

    private final UpdatesService service;

    public UpdatesController(UpdatesService service) {
        this.service = service;
    }

    @GetMapping("/updates")
    public ResponseEntity<ApiResponse<List<UpdateItem>>> getUpdates() {
        return ResponseEntity.ok(ApiResponse.success(service.getUpdates()));
    }
}
