package org.firststep.backend.shared.web;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// GlobalExceptionHandler is a single, application-wide translator from thrown
// exceptions to ApiResponse-wrapped HTTP error responses. It replaces v1's
// total absence of centralized error handling: previously an unhandled
// exception in NewsController/ResourceController fell straight through to
// Spring Boot's default error page.
// =============================================================================

import org.firststep.backend.shared.dto.ApiResponse;
import org.firststep.backend.shared.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", ex.getMessage()));
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// @RestControllerAdvice applies across every @RestController in the
// application automatically — no per-controller try/catch needed. Two
// handlers only: NotFoundException -> 404, and a catch-all Exception -> 500.
// Nothing more elaborate (e.g. Bean Validation error handling) was added,
// since no @Valid/Bean Validation exists anywhere in this codebase today —
// building that handling now would be speculative.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Applies to ResourceController, NewsController, DecisionController (and
//   any future @RestController) without those classes needing to reference
//   this class directly — Spring wires it in via component scanning.
// - Produces the same ApiResponse<T> envelope shape as the success path, so
//   frontend code (app.js) checking response.success can distinguish success
//   from error without a different parsing path for errors.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Per-controller try/catch instead of a global advice: rejected — would
//   reintroduce the exact per-controller inconsistency (different error
//   shapes, some controllers handling errors and some not) this class exists
//   to eliminate.
// =============================================================================
