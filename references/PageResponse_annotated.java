package org.firststep.backend.shared.dto;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// PageResponse<T> is a shape for a paginated result set (content, page, size,
// totalElements). It exists as scaffolding only in this pass — no controller
// returns it yet.
// =============================================================================

import java.util.List;

public class PageResponse<T> {
    public List<T> content;
    public int page;
    public int size;
    public long totalElements;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Named in the application-architecture UML (docs/architecture/uml/
// application-architecture-uml.md) alongside ApiResponse<T> in shared/dto.
// It's added now so the shared-kernel shape matches the UML, but it is
// deliberately NOT wired into any endpoint: no list endpoint (getAll for
// Resource or News) paginates today — every list returns everything in
// memory — and adding real pagination logic (page/size query params, slicing
// in the repository layer) is new feature scope beyond this pass's job of
// restructuring existing code into vertical slices.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Nothing references this class yet. It's ready for a future pass to wire
//   into ResourceController.getAll()/NewsController.getAll() once real
//   pagination is actually scoped and requested.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Wiring real pagination into getAll() now: rejected as speculative scope
//   creep — no current data set is large enough to need it (Resource has 58
//   records total), and building the query-param/slicing logic without a
//   concrete requirement would be exactly the "flexibility that wasn't
//   requested" the project's conventions warn against.
// =============================================================================
