package org.firststep.backend.resource.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.ai.service.DecisionAgentService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.springframework.stereotype.Service;

@Service
public class ResourceService implements DecisionAgentService.ResourceServiceLike {

    private final ResourceRepository repository;

    public ResourceService(ResourceRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Resource> getAllResources() {
        return repository.findAll();
    }

    // existing endpoint uses getAllResources
    public List<Resource> getAll() {
        return getAllResources();
    }

    public Optional<Resource> getById(String id) {
        return repository.findById(id);
    }
}
