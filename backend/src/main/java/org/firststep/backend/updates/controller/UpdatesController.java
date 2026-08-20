package org.firststep.backend.updates.controller;

import java.util.List;

import org.firststep.backend.shared.dto.ApiResponse;
import org.firststep.backend.shared.exception.NotFoundException;
import org.firststep.backend.shared.model.Sector;
import org.firststep.backend.updates.dto.UpdateItem;
import org.firststep.backend.updates.dto.UpdatesPage;
import org.firststep.backend.updates.service.UpdatesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Owns the {@code /api/updates*} URL family.
 *
 * <pre>
 *   GET /api/updates            the raw feed  (polled by ImportantUpdates)
 *   GET /api/updates/{sector}   a whole page  (Latest Updates · Community Notices)
 * </pre>
 */
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

    /**
     * One sector's page, grouped by content type.
     *
     * <p><b>ONE endpoint serves both destination pages</b> — Latest Updates
     * ({@code government}) and Community Notices ({@code community}). They have
     * the same shape because the only thing separating them is who published the
     * content, so the difference belongs in a parameter rather than in a second
     * endpoint.
     *
     * <p>An unrecognised sector is a 404 rather than an empty page, matching
     * CategoryController's rule: a sector that EXISTS and is empty and a sector
     * that DOES NOT EXIST are different facts and must not look alike.
     */
    @GetMapping("/updates/{sector}")
    public ResponseEntity<ApiResponse<UpdatesPage>> getSectorPage(@PathVariable String sector) {
        Sector resolved = Sector.fromKey(sector)
                .orElseThrow(() -> new NotFoundException("Unknown sector: " + sector));
        return ResponseEntity.ok(ApiResponse.success(service.getPage(resolved)));
    }
}
