package org.firststep.backend.ai.controller;

import org.firststep.backend.ai.dto.DecisionRequest;
import org.firststep.backend.ai.dto.DecisionResponse;
import org.firststep.backend.ai.service.DecisionAgentService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DecisionController {

    private final DecisionAgentService agentService;

    public DecisionController(DecisionAgentService agentService) {
        this.agentService = agentService;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/decide")
    public ResponseEntity<ApiResponse<DecisionResponse>> decide(@RequestBody DecisionRequest request) {
        // Feature flag: if AI is disabled (e.g., no provider configured), return graceful structured response.
        // This prevents the endpoint from hard-failing and keeps the UI polished.
        return ResponseEntity.ok(ApiResponse.success(agentService.decide(request)));
    }
}
