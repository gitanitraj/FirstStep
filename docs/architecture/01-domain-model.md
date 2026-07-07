# 01 — Domain Model

*The knowledge model: what First Step knows about, and how those things relate.*

See the diagram: [uml/domain-model.md](uml/domain-model.md).

## Core Knowledge

The primary things the community cares about.

- **Resource** — > TODO: define (evolves from v1 `Resource`).
- **NewsItem** — > TODO: define (evolves from v1 `NewsItem`).
- **Flyer** — > TODO: define (unstructured artifact + generated metadata).
- **FAQ** — > TODO: define.
- **ExpertAnswer** — > TODO: define.

## Organizing entities

- **Community** — > TODO: define. First-class partition key; every Core Knowledge
  object carries a `communityId` from day one (even with only Wilmington today).
- **Category** — > TODO: define.
- **Tag** — > TODO: define.

## Supporting objects

- **ContentSource** — > TODO: define. Provenance link (Level 1: source
  attribution). Captures structured vs. unstructured origin, retrieval time, and a
  link to the preserved original artifact.
- **Citation** — > TODO: define (evolves from v1 `dto/Citation`). How a delivered
  answer points back to its `ContentSource`.
- **Location** — > TODO: define.
- **Contact** — > TODO: define.
- **Media** — > TODO: define. The preserved original artifact (PDF, image, flyer).

## Shared characteristics — the *conceptual* KnowledgeObject

Resources, News Items, Flyers, FAQs, and Expert Answers share common
characteristics (identity, `communityId`, provenance link, categories/tags,
timestamps, verification status).

> **NOTE — decision intentionally deferred.** `KnowledgeObject` is a **conceptual
> domain idea**, *not* a committed Java class. Whether these shared characteristics
> are ultimately realized as **inheritance**, **composition**, or **interfaces**
> is left open until an explicit decision is made. Do not presume inheritance in
> this document or downstream.

> TODO: Describe the shared characteristics as concepts, without choosing an
> implementation mechanism.

## Provenance depth

Level 1 (source attribution) for v2.

> TODO: Note the seam to deeper lineage (field-level / event-sourced) as a future
> option, without building it now.

## v1 → v2 mapping

How today's model seeds v2 (this is an *evolution*, not a throwaway).

| v1 | v2 |
| --- | --- |
| `Resource` (flat `source`/`retrieved`/`verified`) | `Resource` + `ContentSource` |
| `NewsItem` (`sourceName`/`sourceUrl`) | `NewsItem` + `ContentSource` |
| `dto/Citation` (`sourceType`/`id`/`label`) | `Citation` linked to `ContentSource` |
| `Resource.Location/Phone/Website` | `Location` / `Contact` |

> TODO: Extend the mapping and note migration implications.
