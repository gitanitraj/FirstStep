package org.firststep.backend.expert.repository;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.expert.model.ExpertAnswer;

public interface ExpertAnswerRepository {
    List<ExpertAnswer> findAll();
    Optional<ExpertAnswer> findById(String id);
}
