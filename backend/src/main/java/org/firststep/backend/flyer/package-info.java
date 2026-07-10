/**
 * Flyer vertical slice — scaffolding only, no implementation yet.
 *
 * A Flyer is an unstructured artifact (image/PDF) plus generated metadata,
 * extending the shared CivicContent base once built — see the "Flyer" TODO
 * in docs/architecture/01-domain-model.md and "Milestone 2" in
 * docs/architecture/03-application-architecture.md.
 *
 * Seven real flyer images already exist at
 * backend/src/main/resources/static/images/seasonal/ (served today via
 * resource/controller/ResourceController#getSeasonalImages, unrelated to
 * this slice). Their embedded metadata is standard image EXIF only — no
 * descriptive or resource-linking data — so building this slice will need
 * to decide what "extract metadata" actually means for them (filename-
 * derived data, authored companion metadata, or something else) rather
 * than assuming it's a simple parsing exercise. See
 * references/Media_annotated.java for the full note.
 */
package org.firststep.backend.flyer;
