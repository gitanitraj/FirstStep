/*
 * IM-01 annotated reference — full mirror of backend/src/main/java/org/firststep/backend/expert/service/ExpertAnswerService.java
 *
 * WHAT: Preserve the existing slice while enforcing the shared current-inventory boundary.
 * WHY: PC-01 requires one editorial lifecycle interpretation; repositories retain history.
 * INTERACTION: current service queries apply EditorialLifecycle before projection/counting;
 * classification, Community selection and article approval retain their existing owners.
 * ALTERNATIVE: filtering separately in every destination would duplicate date/status policy.
 * Event occurrence is deliberately separate (PC-02 / IM-10). See the IM-01 execution report.
 */
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
