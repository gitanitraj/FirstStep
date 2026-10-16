package org.firststep.backend.expert.controller;

import java.util.List;

import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.service.FaqService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.firststep.backend.shared.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FaqController {

    private final FaqService service;

    public FaqController(FaqService service) {
        this.service = service;
    }

    @GetMapping("/faqs")
    public ResponseEntity<ApiResponse<List<FAQ>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/faqs/{id}")
    public ResponseEntity<ApiResponse<FAQ>> getById(@PathVariable String id) {
        FAQ faq = service.getById(id)
                .orElseThrow(() -> new NotFoundException("FAQ not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(faq));
    }
}
