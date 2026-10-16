package org.firststep.backend.resource.repository;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// ResourceRepository is the resource slice's persistence seam — a small
// interface (findAll, findById) that ResourceService depends on instead of
// knowing how or where Resource data is actually stored.
// =============================================================================

import java.util.List;
import java.util.Optional;

import org.firststep.backend.resource.model.Resource;

public interface ResourceRepository {
    List<Resource> findAll();
    Optional<Resource> findById(String id);
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Resolves the docs' previously-open "Repository — a pattern, not a
// committed generic class" question (docs/architecture/03-application-architecture.md):
// the decision made here is per-slice repository interfaces, not one
// generic Repository<T>. This interface has exactly the two methods
// ResourceService/ResourceController actually call — no CRUD methods
// (save/delete/update) were added since nothing in this pass writes data
// back (storage is still read-only JSON files loaded at startup).
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - JsonResourceRepository is the only implementation in this pass, backed
//   by the same JSON-file loading mechanism v1's ResourceService used.
// - ResourceService depends on this interface (constructor-injected), not
//   on JsonResourceRepository directly — a future SQLite/Postgres-backed
//   implementation could replace JsonResourceRepository without touching
//   ResourceService or ResourceController at all.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - A single generic Repository<T> interface shared across all slices:
//   rejected per the docs' own framing of this as an open decision to
//   resolve, not a given — a generic interface would need to accommodate
//   very different query needs per slice (e.g. Resource has no update
//   methods yet, News's RSS-backed data has a different refresh model)
//   for no present benefit, since nothing is shared between them today
//   beyond "loads a list from JSON."
// =============================================================================
