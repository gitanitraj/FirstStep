package org.firststep.backend.flyer.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// FlyerService is the flyer slice's thin service layer between
// FlyerController and FlyerRepository — exposes getAll()/getById().
// =============================================================================

import java.util.List;
import java.util.Optional;

import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.springframework.stereotype.Service;

@Service
public class FlyerService {

    private final FlyerRepository repository;

    public FlyerService(FlyerRepository repository) {
        this.repository = repository;
    }

    public List<Flyer> getAll() {
        return repository.findAll();
    }

    public Optional<Flyer> getById(String id) {
        return repository.findById(id);
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Direct mirror of ResourceService's shape — thin delegation to the
// repository, no business logic of its own. Unlike ResourceService/
// NewsService, this does NOT implement a DecisionAgentService.*ServiceLike
// marker interface — Flyer isn't wired into the AI decision-aid's retrieval
// in this pass (out of scope; the user's ask was the backend slice only, no
// AI integration for flyers yet).
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - FlyerController calls getAll()/getById(id).
// - No other class depends on this one yet — Flyer isn't referenced from
//   DecisionAgentService, search, or any other slice.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Implementing a FlyerServiceLike interface now, anticipating future AI
//   integration: rejected as speculative — add it when DecisionAgentService
//   actually needs to retrieve flyers, not before.
// =============================================================================
