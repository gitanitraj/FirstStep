package org.firststep.backend.expert.repository;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// ExpertAnswerRepository is the expert-answer slice's persistence seam —
// findAll/findById — that ExpertAnswerService depends on instead of
// knowing how/where expert answer data is stored. Mirrors
// FlyerRepository's shape exactly.
// =============================================================================

import java.util.List;
import java.util.Optional;

import org.firststep.backend.expert.model.ExpertAnswer;

public interface ExpertAnswerRepository {
    List<ExpertAnswer> findAll();
    Optional<ExpertAnswer> findById(String id);
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Same per-slice repository pattern already established for Resource/
// News/Flyer — findById included since ExpertAnswer, like Flyer, supports
// a detail-view lookup by id.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - JsonExpertAnswerRepository is the only implementation, backed by
//   app/data/expert-answers.json.
// - ExpertAnswerService depends on this interface, not
//   JsonExpertAnswerRepository directly.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - None — direct mirror of FlyerRepository.
// =============================================================================
