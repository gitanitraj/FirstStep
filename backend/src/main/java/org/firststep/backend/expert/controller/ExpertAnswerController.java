package org.firststep.backend.expert.controller;

import java.util.List;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.firststep.backend.shared.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExpertAnswerController {

    private final ExpertAnswerService service;

    public ExpertAnswerController(ExpertAnswerService service) {
        this.service = service;
    }

    @GetMapping("/expert-answers")
    public ResponseEntity<ApiResponse<List<ExpertAnswer>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/expert-answers/{id}")
    public ResponseEntity<ApiResponse<ExpertAnswer>> getById(@PathVariable String id) {
        ExpertAnswer expertAnswer = service.getById(id)
                .orElseThrow(() -> new NotFoundException("Expert answer not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(expertAnswer));
    }
}
