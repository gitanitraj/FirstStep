package org.firststep.backend.expert.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.repository.ExpertAnswerRepository;
import org.firststep.backend.shared.lifecycle.EditorialLifecycle;
import org.springframework.stereotype.Service;

@Service
public class ExpertAnswerService {

    private final EditorialLifecycle lifecycle;

    private final ExpertAnswerRepository repository;

    public ExpertAnswerService(ExpertAnswerRepository repository, EditorialLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        this.repository = repository;
    }

    /** Current resident inventory; the repository retains expired and withdrawn records. */
    public List<ExpertAnswer> getAll() {
        return repository.findAll().stream().filter(lifecycle::isCurrent).toList();
    }

    /** Historical reading preserves the independent public-status restriction. */
    public Optional<ExpertAnswer> getById(String id) {
        return repository.findById(id).filter(lifecycle::isPublicStatus);
    }
}
