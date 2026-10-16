package org.firststep.backend.pipeline.extractmetadata;

/**
 * Stage 2 of the information pipeline ("Generate metadata" in
 * docs/architecture/00-philosophy.md, "extract-metadata" in the
 * architecture UML): turn an unstructured artifact into structured data
 * while preserving the original (as Media). See
 * docs/architecture/02-information-flow.md.
 *
 * Scaffolding only — not implemented or wired in yet. Package named
 * "extractmetadata" (no hyphen) since Java package identifiers can't
 * contain one; the UML/docs' "extract-metadata" label refers to the same
 * stage.
 */
public interface MetadataExtractor<T> {
    T extract(T rawItem);
}
