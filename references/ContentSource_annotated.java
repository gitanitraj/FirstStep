package org.firststep.backend.shared.model;

// =============================================================================
// WHAT THIS CLASS DOES
// =============================================================================
// ContentSource captures provenance: where a piece of content came from
// (a dataset, an RSS feed, a manual entry), when it was retrieved, and a
// link back to it. It replaces the flat, ad hoc provenance fields v1 kept
// directly on Resource (source, retrieved) and NewsItem (sourceName,
// sourceUrl).
// =============================================================================

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentSource {
    public String id;
    public String name;
    public String type;
    public String url;
    public String retrieved;
}

// =============================================================================
// WHY THIS IMPLEMENTATION WAS CHOSEN
// =============================================================================
// Field names (id, name, type, url, retrieved) follow the domain-model UML
// exactly. type is a plain String (e.g. "dataset", "rss", "manual"), not an
// enum, matching the existing project convention of plain Strings for
// similarly open-ended fields (Resource.urgency, Resource.cost).
//
// There is no link to Media on this class (an earlier draft considered an
// originalArtifact: Media field) — the domain-model UML doesn't show that
// relationship, and no Flyer/Media-backed content exists yet to make that
// relationship concrete. Left for whoever builds the Flyer slice to decide.
// =============================================================================

// =============================================================================
// HOW IT INTERACTS WITH OTHER CLASSES
// =============================================================================
// - CivicContent.contentSource holds one of these per knowledge object.
// - Citation.contentSource is resolved (copied from the matching Resource's
//   or NewsItem's contentSource) by DecisionAgentService.resolveCitationSources
//   after the AI response is parsed — see DecisionAgentService_annotated.java.
// - JsonResourceRepository/JsonNewsRepository construct a ContentSource from
//   each record's flat source/retrieved (Resource) or sourceName/sourceUrl
//   (NewsItem) fields at load time.
// =============================================================================

// =============================================================================
// ALTERNATIVES CONSIDERED
// =============================================================================
// - Making `type` an enum: rejected for consistency with the rest of the
//   codebase's plain-String style for open-ended classification fields, and
//   because the full set of source types (dataset/rss/manual/...) isn't
//   fixed yet — a new Flyer/Expert source type would require an enum change
//   whereas a String just needs a new value.
// =============================================================================
