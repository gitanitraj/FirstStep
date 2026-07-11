# 02 — Information Flow

*How information moves through First Step, from source to community.*

See the diagram: [uml/information-flow-uml.md](uml/information-flow-uml.md).

## Sources

Information originates from two format classes and several actor types.

- **Structured sources** — > TODO: resource directories, RSS feeds, government
  websites, government APIs (future), partner organization data, databases.
- **Unstructured sources** — > TODO: community flyers, PDFs, images, event
  announcements, meeting notices, expert answers, community submissions.
- **Source actor types** (see Information Sources diagram) — > TODO: Government,
  Nonprofit, Community Organization, Grassroots Organizer, Resident (future),
  Expert.

## Stage 1 — Collect

> TODO: Inputs (which sources), outputs, and where provenance (`ContentSource`) is
> first attached.

## Stage 2 — Generate metadata

> TODO: How unstructured artifacts are turned into metadata *while preserving the
> original* (linked as `Media`).

## Stage 3 — Normalize

> TODO: Mapping heterogeneous inputs onto the one consistent knowledge model.

## Stage 4 — Enrich

> TODO: Categories, tags, summaries, citations, relationships, translations,
> AI-generated context. (v1's RSS classifier is a concrete example of enrichment.)

## Stage 5 — Deliver

The same knowledge, many channels.

> TODO: Web, Mobile, AI assistant, Newsletter, Social media, APIs — each a
> consumer of one knowledge model.

The Search channel is implemented: `GET /api/search?q=...&communityId=...`
(`search/service/SearchService`, `search/controller/SearchController`)
searches across Resource/NewsItem/Flyer and returns one community-aware,
ranked list. See `references/decisions.md` Decision 012.

The AI-assistant channel is named `AiAssistant` (interface) /
`SpringAiAssistant` (implementation) in code — see
`03-application-architecture.md`'s "AiAssistant (the AI seam)" section. The
diagram in [uml/information-flow-uml.md](uml/information-flow-uml.md) used
the placeholder name `CivicAssistantService`; that's reconciled to
`AiAssistant` there rather than keeping two names for the same seam.

## Provenance through the flow

> TODO: Where provenance is *attached* (Collect / Normalize) and where it is
> *surfaced* to the community (Deliver, via `Citation` → `ContentSource`).
