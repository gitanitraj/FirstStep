package org.firststep.backend.flyer.repository;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.flyer.model.Flyer;

public interface FlyerRepository {
    List<Flyer> findAll();
    Optional<Flyer> findById(String id);
}
