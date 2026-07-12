package org.firststep.backend.expert.repository;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.expert.model.FAQ;

public interface FaqRepository {
    List<FAQ> findAll();
    Optional<FAQ> findById(String id);
}
