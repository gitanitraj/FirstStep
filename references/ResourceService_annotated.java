/*
 * IM-01 annotated reference — full mirror of backend/src/main/java/org/firststep/backend/resource/service/ResourceService.java
 *
 * WHAT: Preserve the existing slice while enforcing the shared current-inventory boundary.
 * WHY: PC-01 requires one editorial lifecycle interpretation; repositories retain history.
 * INTERACTION: current service queries apply EditorialLifecycle before projection/counting;
 * classification, Community selection and article approval retain their existing owners.
 * ALTERNATIVE: filtering separately in every destination would duplicate date/status policy.
 * Event occurrence is deliberately separate (PC-02 / IM-10). See the IM-01 execution report.
 */
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
