package org.firststep.backend.resource.service;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.ai.service.DecisionAgentService;
import org.firststep.backend.resource.model.Resource;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.shared.lifecycle.EditorialLifecycle;
import org.springframework.stereotype.Service;

@Service
public class ResourceService implements DecisionAgentService.ResourceServiceLike {

    private final EditorialLifecycle lifecycle;

    private final ResourceRepository repository;

    public ResourceService(ResourceRepository repository, EditorialLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        this.repository = repository;
    }

    @Override
    /** Current resident inventory; the repository retains expired and withdrawn records. */
    public List<Resource> getAllResources() {
        return repository.findAll().stream().filter(lifecycle::isCurrent).toList();
    }

    // existing endpoint uses getAllResources
    public List<Resource> getAll() {
        return getAllResources();
    }

    /** Historical reading preserves the independent public-status restriction. */
    public Optional<Resource> getById(String id) {
        return repository.findById(id).filter(lifecycle::isPublicStatus);
    }
}
