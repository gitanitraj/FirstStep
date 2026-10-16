package org.firststep.backend.flyer.controller;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// FlyerController exposes the flyer slice's REST endpoints: GET /api/flyers
// and GET /api/flyers/{id}, wrapped in ApiResponse<T> — mirroring
// ResourceController exactly, per direct instruction.
// =============================================================================

import java.util.List;

import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.service.FlyerService;
import org.firststep.backend.shared.dto.ApiResponse;
import org.firststep.backend.shared.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FlyerController {

    private final FlyerService service;

    public FlyerController(FlyerService service) {
        this.service = service;
    }

    @GetMapping("/flyers")
    public ResponseEntity<ApiResponse<List<Flyer>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/flyers/{id}")
    public ResponseEntity<ApiResponse<Flyer>> getById(@PathVariable String id) {
        Flyer flyer = service.getById(id)
                .orElseThrow(() -> new NotFoundException("Flyer not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(flyer));
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Same ApiResponse<T>/NotFoundException wiring as ResourceController/
// NewsController/DecisionController — no new response pattern introduced.
// No seasonal-images-style extra endpoint here: that existing endpoint
// (ResourceController#getSeasonalImages) still serves the raw image file
// list for the existing carousel UI, unchanged and untouched by this slice —
// the two features coexist rather than one replacing the other in this pass.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Depends on FlyerService (constructor-injected).
// - Relies on GlobalExceptionHandler (shared.web) to turn a thrown
//   NotFoundException into a 404 ApiResponse envelope.
// - The frontend is expected to combine each Flyer's `image` (bare filename)
//   with the known static path (backend/src/main/resources/static/images/
//   seasonal/) to render it — this controller doesn't resolve that path
//   itself, matching "the frontend displays the image using the filename
//   stored in the Flyer" per direct instruction.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Rewiring the existing seasonal-images carousel (app.js) to consume this
//   new /api/flyers endpoint instead of /api/seasonal-images: explicitly out
//   of scope for this pass — the user asked for the backend slice only; the
//   frontend layout/carousel redesign is a separate, later piece of work.
// =============================================================================
