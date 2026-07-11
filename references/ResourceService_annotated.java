package org.firststep.backend.resource.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// ResourceService is the resource slice's thin service layer between
// ResourceController and ResourceRepository — exposes getAll()/getById(),
// and also implements DecisionAgentService.ResourceServiceLike so the AI
// slice can fetch resources without depending on this concrete class.
// =============================================================================

import java.util.List;
import java.util.Optional;

import org.firststep.backend.ai.service.DecisionAgentService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.springframework.stereotype.Service;

@Service
public class ResourceService implements DecisionAgentService.ResourceServiceLike {

    private final ResourceRepository repository;

    public ResourceService(ResourceRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Resource> getAllResources() {
        return repository.findAll();
    }

    public List<Resource> getAll() {
        return getAllResources();
    }

    public Optional<Resource> getById(String id) {
        return repository.findById(id);
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// v1's ResourceService did its own JSON loading directly; this version
// delegates entirely to ResourceRepository (constructor-injected) — the
// service layer no longer knows or cares that storage is JSON files. This is
// the concrete result of introducing the repository pattern: a future
// SQLite/Postgres-backed ResourceRepository implementation would need zero
// changes here.
//
// Still implements DecisionAgentService.ResourceServiceLike (a marker
// interface nested inside DecisionAgentService, now in the ai slice as of
// Step 6's consolidation) — this is a pre-existing coupling direction
// (resource slice implements a type owned by the ai slice) carried over
// unchanged from v1, not something this pass restructures. It's a known
// smell — worth a future look — but "move code as-is" took priority over
// fixing unrelated design issues encountered along the way.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - ResourceController calls getAll()/getById(id).
// - DecisionAgentService calls getAllResources() through the
//   ResourceServiceLike interface (decoupled from this concrete class).
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Removing the ResourceServiceLike implementation and having
//   DecisionAgentService depend on ResourceRepository directly: would cut
//   out this pass-through service entirely, but changes ai-slice's
//   dependency shape beyond what "move code as-is" calls for — left as a
//   possible future simplification, not made here.
// =============================================================================
