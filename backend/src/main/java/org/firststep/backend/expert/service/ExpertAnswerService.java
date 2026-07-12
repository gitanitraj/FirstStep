package org.firststep.backend.expert.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.expert.model.ExpertAnswer;
import org.firststep.backend.expert.repository.ExpertAnswerRepository;
import org.springframework.stereotype.Service;

@Service
public class ExpertAnswerService {

    private final ExpertAnswerRepository repository;

    public ExpertAnswerService(ExpertAnswerRepository repository) {
        this.repository = repository;
    }

    public List<ExpertAnswer> getAll() {
        return repository.findAll();
    }

    public Optional<ExpertAnswer> getById(String id) {
        return repository.findById(id);
    }
}
