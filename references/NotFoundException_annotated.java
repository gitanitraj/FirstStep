package org.firststep.backend.shared.exception;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// NotFoundException signals that a requested item doesn't exist (e.g. GET
// /api/resources/{id} for an id that isn't in the data set). Thrown by
// controllers, turned into a 404 ApiResponse envelope by GlobalExceptionHandler.
// =============================================================================

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// A plain unchecked RuntimeException with a single message constructor — the
// smallest possible shape that GlobalExceptionHandler can catch and map to
// HTTP 404. It's the only custom exception type this pass introduces, since
// nothing else in the current code paths throws in a way that needs its own
// dedicated handling; adding more exception types now would be speculative.
//
// Replaces ResourceController.getById()'s previous
// Optional.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build())
// pattern with .orElseThrow(() -> new NotFoundException(...)), so the 404
// path goes through the same ApiResponse envelope as every other error.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - ResourceController.getById() throws this when ResourceService.getById(id)
//   returns Optional.empty().
// - GlobalExceptionHandler.handleNotFound(...) catches it and returns
//   ApiResponse.error("NOT_FOUND", ex.getMessage()) with HTTP 404.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - A checked exception: rejected — would force every caller up the stack to
//   declare or catch it, when in practice it's only ever meant to propagate
//   straight to GlobalExceptionHandler.
// =============================================================================
