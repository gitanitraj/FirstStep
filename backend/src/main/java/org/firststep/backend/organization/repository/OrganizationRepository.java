package org.firststep.backend.organization.repository;

import java.util.List;
import java.util.Optional;

import org.firststep.backend.organization.model.Organization;

/**
 * Access to the organization registry.
 *
 * <p>Two lookups, and the split matters: {@link #findById} is how CONTENT
 * resolves its organization, {@link #findBySlug} is how a URL does. Content must
 * never resolve by slug — that would reintroduce the display-string identity the
 * registry exists to remove.
 */
public interface OrganizationRepository {

    List<Organization> findAll();

    /** Canonical lookup. What resources use. */
    Optional<Organization> findById(String id);

    /** Public-URL lookup. What the route uses, and nothing else. */
    Optional<Organization> findBySlug(String slug);
}
