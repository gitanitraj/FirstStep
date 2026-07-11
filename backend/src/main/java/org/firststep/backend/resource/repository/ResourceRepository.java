package org.firststep.backend.resource.repository;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.resource.model.Resource;

public interface ResourceRepository {
    List<Resource> findAll();
    Optional<Resource> findById(String id);
}
