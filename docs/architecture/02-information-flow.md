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

**Classification is the first implemented Enrich product.**
`shared.classification` normalizes every source into the canonical taxonomy at
ingestion:

```
source text ─▶ Tokenizer ─▶ CategoryClassifier ─┬─▶ canonical category_tags
                                                └─▶ TagClassifier ─▶ descriptive tags
```

- **`CivicContentClassifier`** is the single entry point every ingestion path
  calls. It extracts each content type's prose and enforces the classification
  policy (see [04-editorial-principles.md](04-editorial-principles.md#classification)):
  it fills editorial fields **only when absent**, per field.
- **`CategoryClassifier`** works in two tiers — an exact match against a
  category's `matchCategories` (upstream source vocabulary, deterministic), then
  scored keyword/phrase evidence from `taxonomy.json`'s `keywords`.
- **`TagClassifier`** assigns descriptive tags from matched evidence, additively.
  Never category names.
- **`Tokenizer`** gives whole-word and contiguous-phrase matching. It replaced
  raw substring search, under which `"aid"` matched *said* and one bill
  classified into five categories at once.

Vocabulary lives in `app/data/taxonomy.json`, so tuning classification is a data
change, not a code change. Classification runs **at load, in memory** — the data
files stay as authored, and a vocabulary change takes effect on restart with no
migration.

Every source uses this one engine: `JsonResourceRepository`, `JsonNewsRepository`,
`JsonFlyerRepository`, `JsonExpertAnswerRepository`, `JsonFaqRepository` and
`RssFeedService`. `RssFeedService` in particular now *extracts* content and does
not decide categories — it previously held its own keyword tables and emitted a
private vocabulary (`Healthcare`, `Benefits`, `Delaware Legislation`) that the
taxonomy did not recognize.

> TODO — the next Enrich product: **the relationship graph.** Cross-category
> relevance ("this flyer relates to that law") is computed from canonical
> categories, subcategories, tags and semantic similarity, then *persisted as
> metadata* so it stays deterministic and cheap to serve. It is the mechanism for
> cross-category relevance; a second editorial classification is not.

> TODO: summaries, citations, translations, AI-generated context.

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
