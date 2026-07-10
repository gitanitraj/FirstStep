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

- **Community** — First-class partition key. Fields: `id, name, city, state,
  zipCodes: List<String>, active`. Every `CivicContent` object carries a
  `communityId` from day one (even with only Wilmington today). Ships in this
  pass as a model class plus a default-stamping mechanism (every existing
  Resource/NewsItem gets `communityId = "wilmington-de"` from a config
  property, since none of today's data has one); no `CommunityController`/
  service with real query behavior yet — see `references/decisions.md`
  Decision 005.
- **Category** — > TODO: define.
- **Tag** — Realized as `CivicContent.tags: List<String>` (see below).

## Supporting objects

- **ContentSource** — Provenance link (Level 1: source attribution). Fields:
  `id, name, type, url, retrieved`. `type` is a plain String (`"dataset"`,
  `"rss"`, `"manual"`, …), matching the project's existing convention for
  open-ended classification fields. No link to `Media` yet — left for when
  Flyer is actually built.
- **Citation** — Evolves from v1 `dto/Citation` (moved to
  `shared.model.Citation`), same `sourceType, id, label` fields, plus a new
  `contentSource: ContentSource` (nullable — resolved by `DecisionAgentService`
  after retrieval; stays null if the AI cites an id that doesn't match a
  retrieved item). This is how a delivered answer points back to its
  `ContentSource`.
- **Location** — Fields: `label, address, city, state, zip, confidential`.
  Promoted verbatim from `Resource`'s nested static `Location` class to a
  top-level shared class.
- **Phone** / **Website** — Promoted verbatim from `Resource`'s nested static
  classes (`number, label` / `url, label`) to top-level shared classes.
- **Contact** — New composite: `phones: List<Phone>, websites: List<Website>,
  email`. For future slices (Expert, Flyer); `Resource` keeps its existing
  flat `phones`/`websites` lists rather than adopting `Contact` in this pass.
- **Media** — The preserved original artifact (PDF, image, flyer). Fields:
  `id, type, url, filename, mimeType`. Not yet wired to `ContentSource` or any
  real content — seven flyer images already exist at
  `backend/src/main/resources/static/images/seasonal/`, but their embedded
  metadata today is standard image EXIF only (no descriptive/resource-linking
  data), so this class is scaffolding for when that extraction work is
  actually scoped.

## Shared characteristics — `CivicContent`

`CivicContent` is a **committed abstract class** (resolving the previously-open
inheritance/composition/interfaces question — see `references/decisions.md`
Decision 005). Fields: `id, communityId, title, summary, verified, tags:
List<String>, contentSource: ContentSource, createdDate, updatedDate`.
`Resource` and `NewsItem` extend it today; `Flyer`, `FAQ`, and `ExpertAnswer`
will extend it once those slices are built.

`tags` is a single shared list, not left per-subtype — but this isn't a
lossless merge for every existing field: `NewsItem.categoryTags` maps onto
it, while `NewsItem.resourceTags` (a different concept — cross-references to
specific Resource ids, not a category classification) stays a
`NewsItem`-specific field rather than being folded in.

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
