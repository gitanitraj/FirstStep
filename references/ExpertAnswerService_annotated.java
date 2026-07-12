package org.firststep.backend.expert.service;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// ExpertAnswerService is the expert-answer slice's thin service layer
// between ExpertAnswerController and ExpertAnswerRepository — exposes
// getAll()/getById().
// =============================================================================

import java.util.List;
import java.util.Optional;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.repository.ExpertAnswerRepository;
import org.springframework.stereotype.Service;

@Service
public class ExpertAnswerService {

    private final ExpertAnswerRepository repository;

    public ExpertAnswerService(ExpertAnswerRepository repository) {
        this.repository = repository;
    }

    public List<ExpertAnswer> getAll() {
        return repository.findAll();
    }

    public Optional<ExpertAnswer> getById(String id) {
        return repository.findById(id);
    }
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Direct mirror of FlyerService — thin delegation to the repository, no
// business logic. No *ServiceLike marker interface implemented — not
// wired into DecisionAgentService's AI retrieval or SearchService/
// CategoryService in this pass, per direct instruction (see
// references/decisions.md Decision 015).
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - ExpertAnswerController calls getAll()/getById(id).
// - No other class depends on this one yet.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Implementing a marker interface now, anticipating future Search/AI
//   integration: rejected as speculative, same reasoning FlyerService's
//   annotated reference already documents.
// =============================================================================
