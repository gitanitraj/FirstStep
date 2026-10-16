package org.firststep.backend.ai.controller;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// DecisionController exposes POST /api/decide — the AI decision-aid
// endpoint, wrapping DecisionAgentService.decide() in the standard
// ApiResponse<T> envelope.
// =============================================================================

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

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Package move only (controller -> ai/controller), plus the Step 2
// ApiResponse wiring already in place. DecisionAgentService.decide() never
// throws (its own internal try/catch always returns a DecisionResponse, even
// on failure — see DecisionAgentService_annotated.java) so this controller
// has no error path of its own to handle; every response is HTTP 200 with
// success:true, even when the underlying answer is "AI is currently
// unavailable." That's intentional: it's the AI decision-aid's own
// graceful-degradation contract, not a controller-level error.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Depends on DecisionAgentService (constructor-injected).
// - @CrossOrigin(origins = "*") is unique to this controller among the three
//   REST controllers — carried over unchanged from v1, not re-evaluated as
//   part of this migration.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - None specific to this step — see ResourceController_annotated.java /
//   NewsController_annotated.java for the equivalent reasoning applied to
//   the sibling slices.
// =============================================================================
