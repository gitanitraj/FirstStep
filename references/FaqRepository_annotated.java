package org.firststep.backend.expert.repository;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// FaqRepository is the FAQ slice's persistence seam — findAll/findById —
// that FaqService depends on instead of knowing how/where FAQ data is
// stored. Mirrors FlyerRepository's shape exactly.
// =============================================================================

import java.util.List;
import java.util.Optional;

import org.firststep.backend.expert.model.FAQ;

public interface FaqRepository {
    List<FAQ> findAll();
    Optional<FAQ> findById(String id);
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Same per-slice repository pattern already established for Resource/
// News/Flyer/ExpertAnswer.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - JsonFaqRepository is the only implementation, backed by
//   app/data/faq.json.
// - FaqService depends on this interface, not JsonFaqRepository directly.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - None — direct mirror of FlyerRepository.
// =============================================================================
