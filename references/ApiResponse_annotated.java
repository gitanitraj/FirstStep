package org.firststep.backend.shared.dto;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// ApiResponse<T> is a single generic envelope wrapping every REST response
// this backend returns: ApiResponse<List<Resource>>, ApiResponse<List<NewsItem>>,
// ApiResponse<DecisionResponse>. It carries whether the call succeeded, the
// payload (on success), and a machine-readable error code plus message (on
// failure) — replacing the three different, inconsistent response shapes
// ResourceController/NewsController/DecisionController each used in v1.
// =============================================================================

import java.time.Instant;

public class ApiResponse<T> {
    public boolean success;
    public T data;
    public String errorCode;
    public String errorMessage;
    public String timestamp;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.timestamp = Instant.now().toString();
        return response;
    }

    public static <T> ApiResponse<T> error(String errorCode, String errorMessage) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.errorCode = errorCode;
        response.errorMessage = errorMessage;
        response.timestamp = Instant.now().toString();
        return response;
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// v1 had three controllers each doing something different: DecisionController
// returned a bare POJO with no ResponseEntity at all; NewsController returned
// ResponseEntity<List<NewsItem>> via .ok(...); ResourceController mixed
// ResponseEntity.ok(...) and ResponseEntity.notFound().build(). None of them
// carried a machine-readable error code, and there was no shared error
// envelope — an unhandled exception in NewsController/ResourceController
// would fall straight through to Spring Boot's default error page.
//
// This class wraps existing domain models directly (ApiResponse<List<Resource>>,
// not a new ResourceResponse DTO) — no new per-slice response DTO layer, since
// nothing about the current data requires a wire shape different from the
// domain model itself. Static success()/error() factories keep call sites at
// controllers to one line instead of manual field assignment everywhere.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - ResourceController/NewsController/DecisionController all wrap their
//   ResponseEntity body in ApiResponse.success(...).
// - GlobalExceptionHandler wraps error bodies in ApiResponse.error(...) so
//   thrown exceptions (NotFoundException, or anything unexpected) still
//   produce the same envelope shape as a successful response.
// - backend/src/main/resources/static/app.js was updated at every fetch call
//   site to read response.data instead of treating the raw fetch response as
//   the payload — this is the one place a JSON-shape regression would be
//   invisible to `mvn test` alone, since app.js has no automated test
//   coverage; verified manually by reading its parse code before this change.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Adding dedicated ResourceResponse/NewsResponse DTOs (matching the literal
//   ApiResponse<ResourceResponse> naming from the original request): rejected
//   for this pass — no existing controller-facing shape differs from the
//   domain model, and adding a DTO layer with nothing to decouple yet would
//   be an abstraction for a problem that doesn't exist. Wire shape = domain
//   model shape until something requires otherwise.
// - Wrapping /api/health and /api/seasonal-images too: left unwrapped — they
//   aren't part of the resource/news/decide pattern this class standardizes,
//   and app.js's seasonal-images consumer (line ~811) reads the raw array
//   directly with no other changes needed.
// =============================================================================
