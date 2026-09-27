package org.firststep.backend.findhelp.controller;

import org.firststep.backend.findhelp.dto.FindHelpPage;
import org.firststep.backend.findhelp.service.FindHelpService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <pre>
 *   GET /api/find-help
 * </pre>
 *
 * <p>One page-shaped response, per the BFF principle: the backend aggregates and
 * the client displays. There is no {@code ?category=} parameter and there will
 * not be one — selecting a category leaves this page.
 */
@RestController
@RequestMapping("/api")
public class FindHelpController {

    private final FindHelpService findHelpService;

    public FindHelpController(FindHelpService findHelpService) {
        this.findHelpService = findHelpService;
    }

    @GetMapping("/find-help")
    public ResponseEntity<ApiResponse<FindHelpPage>> getFindHelp() {
        return ResponseEntity.ok(ApiResponse.success(findHelpService.getPage()));
    }
}
