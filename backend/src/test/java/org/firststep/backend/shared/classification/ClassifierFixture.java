package org.firststep.backend.shared.classification;

import org.firststep.backend.category.service.TaxonomyService;

/**
 * Builds a real classifier wired to the REAL app/data/taxonomy.json, for the many
 * tests that only need a repository to construct.
 *
 * <p>A real classifier rather than a mock, for the same reason F1's tests use a
 * real TaxonomyService: these tests then fail if the actual vocabulary drifts,
 * which is signal worth having. A mock would make every repository test pass
 * regardless of whether classification works at all.
 *
 * <p>Surefire's working directory is {@code backend/}, so the project data
 * directory is one level up.
 */
public final class ClassifierFixture {

    private ClassifierFixture() {
    }

    public static CivicContentClassifier real() {
        return new CivicContentClassifier(
                new CategoryClassifier(new TaxonomyService("../app/data")),
                new TagClassifier());
    }
}
