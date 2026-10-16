# 01 — Domain Model

*The knowledge model: what First Step knows about, and how those things relate.*

See the diagram: [uml/domain-model-uml.md](uml/domain-model-uml.md).

## When to introduce a domain class

**Do not introduce a new domain class simply because a content type has different
*data*. Introduce a new domain class only when it has different *business
behavior*.**

This rule exists to prevent a specific and tempting form of overengineering: a
class-per-shape hierarchy that grows a type every time a field differs, and whose
subclasses are then indistinguishable in behavior.

The v2 types earn their existence:

| Type | The behavior that justifies it |
| --- | --- |
| **Resource** | Eligibility, locations, contacts, and organizations — it must be matched against a person's circumstances and connected to a provider. |
| **Flyer** | Image-based media — the artifact *is* the content, and it is preserved and rendered rather than read. |
| **ExpertAnswer** / **FAQ** | Curated knowledge — attributed, reviewed, and answering a question rather than describing a service. |
| **NewsItem** | Time-based editorial content — it publishes, expires, and has a "why it matters". |

A **Law** does *not*. It has the same fields and the same behavior as a NewsItem;
only its presentation differs. That is why `LAW` is a value of
`shared.model.ContentType` and not a `LawItem` class — a class whose only
distinguishing feature is its own type tag models nothing. See
`references/decisions.md` Decision 032.

The corollary: **content type describes presentation, category describes
subject.** If a proposed category answers "what format is this?" rather than
"what is this about?", it is a content type, not a category.

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
- **Flyer** — extends `CivicContent`. Adds `organization: String, eventDate:
  String, location: Location, image: String` (bare filename, e.g.
  "Youth.jpg" — the frontend combines it with the known static image path
  to render it). Backed by `JsonFlyerRepository` reading a static
  `app/data/flyers.json` authored directly in the Flyer JSON shape (no
  legacy-format adapter needed, unlike Resource/News). Served via
  `GET /api/flyers` and `GET /api/flyers/{id}`. Current data (7 records) is
  manually authored metadata for the 7 existing images at
  `backend/src/main/resources/static/images/seasonal/` — not extracted via
  OCR/AI, which remains a future milestone (see "Milestone 2" in
  `03-application-architecture.md`).
- **FAQ** — extends `CivicContent`. Adds `question: String, answer: String,
  sourceExpertAnswerId: String` (nullable — a plain id reference to the
  `ExpertAnswer` it was curated from, if any; not auto-resolved). Deliberately
  simpler than `ExpertAnswer` — no individual expert attribution. Backed by
  `JsonFaqRepository` reading static `app/data/faq.json`. Served via
  `GET /api/faqs` and `GET /api/faqs/{id}`. Not yet wired into `Search` or
  the category taxonomy — see `references/decisions.md` Decision 015.
- **ExpertAnswer** — extends `CivicContent`. Adds `question: String,
  answer: String, expertName: String, expertCredentials: String,
  expertOrganization: String, expertContact: Contact, sessionDate: String`
  — the first real adopter of the shared `Contact` composite
  (phones/websites/email), anticipated for this purpose since the original
  migration. Backed by `JsonExpertAnswerRepository` reading static
  `app/data/expert-answers.json`. Served via `GET /api/expert-answers` and
  `GET /api/expert-answers/{id}`. Current data (6 records) is hand-authored
  Delaware civic-info Q&A, not from a real "monthly session" intake
  process. Not yet wired into `Search` or the category taxonomy — see
  `references/decisions.md` Decision 015.

## Organizing entities

- **Community** — First-class partition key, at **city/town granularity, not
  county** (every record in both Service Directory data sources shares the
  same county, New Castle County — county has zero variance and is not a
  useful partition; incorporated towns like Wilmington/Newark/Middletown, or
  unincorporated areas like Claymont, are the real unit). Fields: `id, name,
  city, state, zipCodes: List<String>, active`. Every `CivicContent` object
  carries a `communityId`, derived from its own location's city via
  `shared.util.CommunitySlug.forCity(...)` (e.g. `"Newark"` →
  `"newark-de"`) with `app.default-community-id` (`"wilmington-de"`) as the
  fallback when no location exists — see `references/decisions.md`
  Decision 013. `Community.java` itself remains an inert model class with
  no `CommunityRepository`/`Service`/`Controller` and no discovery
  endpoint — communityId values are real and derived, but nothing yet lets
  a client enumerate which communities exist.
- **Category** — a 10-entry taxonomy (Housing, Food, Clothing, Health,
  Employment, Utilities, Legal, Community Events, Furniture & Household,
  Community Support), not a stored/CRUD entity. Loaded from
  `app/data/taxonomy.json` by `category/service/TaxonomyService` — the hand-
  mirrored `CategoryDefinition.ALL` registry this document once described was
  deleted in Slice F1, because two copies of a vocabulary that must agree is a
  drift bug waiting to happen. `GET /api/categories`
  (`category/service/CategoryService`) aggregates counts, latest items,
  and linked policy updates per category, reusing `search.dto.SearchResult`
  for the polymorphic Resource/Flyer item list. See
  `references/decisions.md` Decision 014 for why the taxonomy has 10
  entries, not the originally-proposed 7.
- **Tag** — Realized as `CivicContent.tags: List<String>` (see below).

## Three vocabularies

The system uses three distinct vocabularies, and most confusion about "where
does this content go?" comes from collapsing them:

```
Taxonomy (Editorial)        Category  →  Subcategory     what First Step KNOWS
Navigation (Presentation)   Group     →  Topic           how residents DISCOVER it
Content                     CivicContent                 the things themselves
```

**A navigation Topic references an editorial Subcategory.** "Housing ▸ Rental
Assistance" is simultaneously an editorial subcategory and a navigation topic
*because navigation references it* — Topic is a pointer into the taxonomy, not a
fourth vocabulary of its own. That is why `validate_navigation.py` can check
every authored topic against the taxonomy, and why `NavigationService` counts a
topic by reading each item's `subcategory` field.

The two artifacts have deliberately different lifecycles (Decision 029):
`taxonomy.json` is the stable domain model that everything validates against;
`navigation.json` is editorial presentation, regenerable — a future AI navigation
generator rewrites that one file and nothing else.

| Layer | Field | Job |
| --- | --- | --- |
| Category | `category_tags` | Which category page an item appears on |
| Subcategory | `subcategory` | Which topic within it — **browse only** |
| Tags | `tags` | Search, AI, relationships — **never placement** |

**Content carrying a category and no subcategory is fully classified**, not
half-classified. Resources and flyers have topics because an editor gave them
one; news, legislation and expert content generally do not, and the classifier is
conservative by design and will not invent one. Such content is reached through a
category page's *updates* rather than its *topics* — see 03's "the category page
is an aggregate read model."

### What makes something a category?

Ask what question it answers. The test has three outcomes, and only one of them
is a category:

| The proposed thing answers… | It is a… | Example |
| --- | --- | --- |
| *"What is this about?"* | **Category** | Housing, Food, Legal |
| *"What format is this?"* | **ContentType** | `LAW`, `FLYER`, `EXPERT` |
| *"Who is this for?"* | **population / eligibility facet** | Seniors, veterans, families with children |

The middle row is why `LAW` is a contentType rather than a category (Decision
032): a housing bill is *about* housing and *formatted as* legislation, and
forcing it to choose would lose one of those facts.

The third row is the same mistake in a different dimension, and the fields for it
already exist and are unused: `Resource` carries `population`,
`eligibility_age_min`, `eligibility_age_max` and `eligibility_gender`, with 43
distinct population values in the loaded data.

**Facets and categories compose.** "Housing resources for seniors" is the Housing
category *filtered by* an eligibility facet — one item, two independent
descriptions, no contradiction. Promoting "Seniors" to a category would force
every senior housing resource to choose between two homes and split the housing
page in half. A facet answers a question the category never asked.

This applies whenever the information architecture grows: a proposed new category
must survive all three rows of the table, not just sound plausible in a nav list.

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
