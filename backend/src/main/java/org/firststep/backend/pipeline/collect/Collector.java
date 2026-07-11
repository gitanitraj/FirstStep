package org.firststep.backend.pipeline.collect;

import java.util.List;

/**
 * Stage 1 of the information pipeline: gather raw items from a source
 * (dataset file, RSS feed, future API/flyer upload/expert session).
 * See docs/architecture/02-information-flow.md.
 *
 * Scaffolding only — not implemented or wired into any real ingestion yet.
 * Resource/News loading still happens directly in their own repositories
 * (resource/repository/JsonResourceRepository, news/repository/
 * JsonNewsRepository, news/service/RssFeedService), not through this
 * interface. This is a landing point for a future pass that refactors that
 * ingestion into concrete pipeline stages once a second real case (e.g.
 * Flyer) exists to validate the shape against.
 */
public interface Collector<T> {
    List<T> collect();
}
