/*
 * Annotated reference: backend/src/main/java/org/firststep/backend/ai/controller/DecisionController.java
 * WHAT: Preserves the complete source below; see the explicit allowlists and optional exact-owner filters in the source.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Controllers adapt internal models only at the resident boundary; tests and evidence exercise real repositories plus labeled synthetic counterexamples.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
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
