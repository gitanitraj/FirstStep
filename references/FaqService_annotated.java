package org.firststep.backend.expert.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// FaqService is the FAQ slice's thin service layer between FaqController
// and FaqRepository — exposes getAll()/getById().
// =============================================================================

import java.util.List;
import java.util.Optional;

import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.repository.FaqRepository;
import org.springframework.stereotype.Service;

@Service
public class FaqService {

    private final FaqRepository repository;

    public FaqService(FaqRepository repository) {
        this.repository = repository;
    }

    public List<FAQ> getAll() {
        return repository.findAll();
    }

    public Optional<FAQ> getById(String id) {
        return repository.findById(id);
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Direct mirror of FlyerService/ExpertAnswerService — thin delegation, no
// business logic, no resolution of sourceExpertAnswerId (see
// FAQ_annotated.java for why that link is deliberately left unresolved in
// this pass).
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - FaqController calls getAll()/getById(id).
// - No dependency on ExpertAnswerService — the two slices are independent.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Having getById also resolve and embed the linked ExpertAnswer:
//   rejected as unneeded complexity for a stub pass — a client that wants
//   the full ExpertAnswer can call /api/expert-answers/{id} itself.
// =============================================================================
