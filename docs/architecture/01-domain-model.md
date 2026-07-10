# 01 — Domain Model

*The knowledge model: what First Step knows about, and how those things relate.*

See the diagram: [uml/domain-model.md](uml/domain-model.md).

## Core Knowledge

The primary things the community cares about.

- **Resource** — `org.firststep.backend.resource.model.Resource`, extends
  `CivicContent`. Resource-specific fields: `category, subcategory,
  organization, parentOrganization, description, population, eligibility,
  eligibilityAgeMin, eligibilityAgeMax, eligibilityGender, locations:
  List<Location>, phones: List<Phone>, websites: List<Website>, county,
  accessMode: List<String>, cost, urgency, notes`. `title` (inherited from
  `CivicContent`) is populated from `organization` at load time — `app.js`
  has always rendered `organization` as the display title, and `organization`
  itself is kept (not renamed) since it's used throughout
  `DecisionAgentService`'s scoring logic. Backed by `JsonResourceRepository`
  (JSON-file storage, unchanged from v1's loading mechanism).
- **NewsItem** — `org.firststep.backend.news.model.NewsItem`, extends
  `CivicContent`. News-specific fields: `type, body, urgency, published,
  expires, geography, active, author, whyItMatters, resourceTags:
  List<String>`. `title` (inherited) is populated from `headline` (a direct
  rename); `tags` (inherited) is populated from `categoryTags`.
  `resourceTags` (cross-references to specific `Resource` ids — a different
  concept from a category classification) deliberately stays NewsItem-only,
  not folded into the shared `tags`. Two independent data paths both produce
  `NewsItem`: `JsonNewsRepository` (static `app/data/news.json`) and
  `RssFeedService` (live Delaware legislature RSS feed), served via
  `GET /api/news` and `GET /api/news/rss` respectively.
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

Level 1 (source attribution) for v2. Concrete example, now implemented:
`DecisionAgentService.resolveCitationSources` matches each AI-generated
`Citation.id` against the `Resource`/`NewsItem` actually retrieved for that
answer and copies its `contentSource` onto the citation — so a delivered
answer's citation traces back to a real `ContentSource` (or stays
unresolved, logged at DEBUG, if the model cited an id that was never
retrieved). This is item-level provenance (which source backs this citation),
not field-level (which source backs this specific claim within the item).

> TODO: Note the seam to deeper lineage (field-level / event-sourced) as a future
> option, without building it now.

## v1 → v2 mapping

How today's model seeds v2 (this is an *evolution*, not a throwaway).

| v1 | v2 |
| --- | --- |
| `Resource.source`/`.retrieved` (flat Strings) | `Resource.contentSource.name`/`.retrieved` (inherited `ContentSource`) |
| `Resource.organization` | `Resource.organization` (unchanged) + `Resource.title` (inherited, populated from it) |
| `Resource.verified` | inherited from `CivicContent`, unchanged |
| *(none)* | `Resource.communityId`, `.createdDate`, `.updatedDate` — new, defaulted at load time |
| `NewsItem.sourceName`/`.sourceUrl` (flat Strings) | `NewsItem.contentSource.name`/`.url` (inherited `ContentSource`) |
| `NewsItem.headline` | `NewsItem.title` (inherited, direct rename) |
| `NewsItem.categoryTags` | `NewsItem.tags` (inherited, direct rename) |
| `NewsItem.resourceTags` | unchanged, stays News-specific (not a shared field) |
| `dto/Citation` (`sourceType`/`id`/`label`) | `Citation` linked to `ContentSource` |
| `Resource.Location/Phone/Website` (nested statics) | `shared.model.Location/Phone/Website` (top-level, same fields) |

> TODO: Extend the mapping and note migration implications.
