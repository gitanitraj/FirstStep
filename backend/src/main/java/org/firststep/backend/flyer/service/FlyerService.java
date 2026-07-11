package org.firststep.backend.flyer.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.flyer.model.Flyer;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.springframework.stereotype.Service;

@Service
public class FlyerService {

    private final FlyerRepository repository;

    public FlyerService(FlyerRepository repository) {
        this.repository = repository;
    }

    public List<Flyer> getAll() {
        return repository.findAll();
    }

    public Optional<Flyer> getById(String id) {
        return repository.findById(id);
    }
}
