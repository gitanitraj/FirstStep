package org.firststep.backend.pipeline.enrich;

/**
 * Stage 4 of the information pipeline: add categories, tags, summaries,
 * citations, relationships, translations, or AI-generated context to an
 * already-normalized item. See docs/architecture/02-information-flow.md.
 *
 * Scaffolding only — not implemented or wired in yet. The closest existing
 * analogue is news/service/RssFeedService's keyword classification
 * (classifyLegislation/extractRelatingTo), called out in the docs as "a
 * concrete example of enrichment" — it is NOT refactored to implement this
 * interface in this pass; see references/decisions.md for why the pipeline
 * package stays scaffolding-only for now.
 */
public interface Enricher<T> {
    T enrich(T item);
}
