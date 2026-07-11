package org.firststep.backend.flyer.repository;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// FlyerRepository is the flyer slice's persistence seam — findAll/findById —
// that FlyerService depends on instead of knowing how/where flyer data is
// stored. Mirrors ResourceRepository's shape exactly, per direct instruction.
// =============================================================================

import java.util.List;
import java.util.Optional;

import org.firststep.backend.flyer.model.Flyer;

public interface FlyerRepository {
    List<Flyer> findAll();
    Optional<Flyer> findById(String id);
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Same per-slice repository pattern already established for Resource/News —
// findById included (unlike NewsRepository, which only has findAll) since
// Flyer, like Resource, is expected to support a detail-view lookup by id.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - JsonFlyerRepository is the only implementation, backed by
//   app/data/flyers.json.
// - FlyerService depends on this interface, not JsonFlyerRepository directly.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - None — direct mirror of ResourceRepository, per direct instruction to
//   build this "exactly" like the Resource slice.
// =============================================================================
