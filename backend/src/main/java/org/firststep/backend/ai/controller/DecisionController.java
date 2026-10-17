package org.firststep.backend.ai.controller;

import org.firststep.backend.ai.dto.DecisionRequest;
import org.firststep.backend.ai.dto.DecisionSummary;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<ApiResponse<DecisionSummary>> decide(@RequestBody DecisionRequest request, @RequestParam(required = false) String communityId) {
        // Feature flag: if AI is disabled (e.g., no provider configured), return graceful structured response.
        // This prevents the endpoint from hard-failing and keeps the UI polished.
        return ResponseEntity.ok(ApiResponse.success(DecisionSummary.from(communityId == null ? agentService.decide(request) : agentService.decide(request, communityId))));
    }
}
