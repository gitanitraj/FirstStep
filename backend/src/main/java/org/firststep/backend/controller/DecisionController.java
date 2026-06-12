package org.firststep.backend.controller;

import org.firststep.backend.dto.DecisionRequest;
import org.firststep.backend.dto.DecisionResponse;
import org.firststep.backend.service.DecisionAgentService;
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
    public DecisionResponse decide(@RequestBody DecisionRequest request) {
        // Feature flag: if AI is disabled (e.g., Ollama runtime unavailable), return graceful structured response.
        // This prevents the endpoint from hard-failing and keeps the UI polished.
        return agentService.decide(request);
    }
}

