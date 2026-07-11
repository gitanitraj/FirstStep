package org.firststep.backend.pipeline.normalize;

/**
 * Stage 3 of the information pipeline: map a heterogeneous input onto the
 * one consistent knowledge model (a CivicContent subtype). See
 * docs/architecture/02-information-flow.md.
 *
 * Scaffolding only — not implemented or wired in yet.
 */
public interface Normalizer<T, R> {
    R normalize(T rawItem);
}
