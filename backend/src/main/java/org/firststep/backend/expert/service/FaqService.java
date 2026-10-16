package org.firststep.backend.expert.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.repository.FaqRepository;
import org.firststep.backend.shared.lifecycle.EditorialLifecycle;
import org.springframework.stereotype.Service;

@Service
public class FaqService {

    private final EditorialLifecycle lifecycle;

    private final FaqRepository repository;

    public FaqService(FaqRepository repository, EditorialLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        this.repository = repository;
    }

    /** Current resident inventory; the repository retains expired and withdrawn records. */
    public List<FAQ> getAll() {
        return repository.findAll().stream().filter(lifecycle::isCurrent).toList();
    }

    /** Historical reading preserves the independent public-status restriction. */
    public Optional<FAQ> getById(String id) {
        return repository.findById(id).filter(lifecycle::isPublicStatus);
    }
}
