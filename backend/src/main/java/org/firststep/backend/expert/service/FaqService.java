package org.firststep.backend.expert.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.expert.model.FAQ;
import org.firststep.backend.expert.repository.FaqRepository;
import org.springframework.stereotype.Service;

@Service
public class FaqService {

    private final FaqRepository repository;

    public FaqService(FaqRepository repository) {
        this.repository = repository;
    }

    public List<FAQ> getAll() {
        return repository.findAll();
    }

    public Optional<FAQ> getById(String id) {
        return repository.findById(id);
    }
}
