/*
 * Annotated reference: backend/src/main/java/org/firststep/backend/expert/controller/ExpertAnswerController.java
 * WHAT: Preserves the complete source below; see the explicit allowlists and optional exact-owner filters in the source.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Controllers adapt internal models only at the resident boundary; tests and evidence exercise real repositories plus labeled synthetic counterexamples.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
package org.firststep.backend.expert.controller;

import java.util.List;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.service.ExpertAnswerService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.firststep.backend.shared.dto.ResidentContent;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<ApiResponse<List<Object>>> getAll(@RequestParam(required = false) String communityId) {
        return ResponseEntity.ok(ApiResponse.success(service.getAll().stream()
                .filter(c -> communityId == null || communityId.equals(c.communityId))
                .map(ResidentContent::from).toList()));
    }

    @GetMapping("/expert-answers/{id}")
    public ResponseEntity<ApiResponse<Object>> getById(@PathVariable String id, @RequestParam(required = false) String communityId) {
        ExpertAnswer expertAnswer = service.getById(id)
                .filter(c -> communityId == null || communityId.equals(c.communityId))
                .orElseThrow(() -> new NotFoundException("Expert answer not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(ResidentContent.from(expertAnswer)));
    }
}
