package org.firststep.backend.shared.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// Media represents a preserved original artifact — a PDF, image, or flyer —
// that a piece of content was derived from. It exists so the "preserve the
// original" principle in docs/architecture/00-philosophy.md has a concrete
// home once unstructured sources (flyers) are actually ingested.
// =============================================================================

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Media {
    public String id;
    public String type;
    public String url;
    public String filename;
    public String mimeType;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Minimal field set (id, type, url, filename, mimeType) — enough to point at
// and describe a stored file, nothing more. No relationship to ContentSource
// is wired in yet (an earlier draft considered ContentSource.originalArtifact:
// Media), because the domain-model UML doesn't show that link and no real
// Flyer content exists yet to make it concrete. Left for the Flyer slice to
// decide when it's actually built.
//
// Seven real flyer images already exist at
// backend/src/main/resources/static/images/seasonal/ (served today via
// ResourceController.getSeasonalImages()), but their embedded metadata is
// standard image EXIF only (dimensions, DPI, creation timestamp, generating
// software) — no descriptive text or resource-linking data. This class does
// not attempt to model or extract that yet; it's scaffolding for when that
// work happens.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - Not referenced by any other class yet. Ready for the Flyer slice (or a
//   future ContentSource field) to adopt once that work is scoped.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Wiring Media onto ContentSource now (originalArtifact: Media): rejected
//   for this pass — speculative until a real Flyer/Media-backed content type
//   exists to exercise the relationship, and the UML doesn't show it.
// =============================================================================
