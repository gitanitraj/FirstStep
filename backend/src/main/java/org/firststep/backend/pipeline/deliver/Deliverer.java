package org.firststep.backend.pipeline.deliver;

/**
 * Stage 5 of the information pipeline: expose the same knowledge through
 * a delivery channel (REST API, AI assistant, future Search/Newsletter/
 * mobile). See docs/architecture/02-information-flow.md.
 *
 * Scaffolding only — not implemented or wired in yet. The existing REST
 * controllers (resource/controller/ResourceController,
 * news/controller/NewsController, ai/controller/DecisionController) are
 * today's real delivery channels; they are NOT refactored to implement
 * this interface in this pass.
 */
public interface Deliverer<T, R> {
    R deliver(T item);
}
