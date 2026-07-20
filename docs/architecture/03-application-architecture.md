# 03 — Application Architecture

*How the software implements the information flow.*

## Layered view

```
React SPA
  ↓
REST Controllers
  ↓
ApiResponse<T> / PageResponse<T>  (shared/dto — the generic response envelope
                                    every controller wraps its payload in;
                                    PageResponse<T> is scaffolded, not yet
                                    wired into any endpoint)
  ↓
Services
  ↓
Repository pattern
  ↓
Data (JSON / RSS / Flyers  →  SQLite / Postgres)
  ↓
AiAssistant / SpringAiAssistant
```

Errors are centralized through `shared/web/GlobalExceptionHandler`
(`@RestControllerAdvice`), which maps thrown exceptions
(`shared/exception/NotFoundException` → 404; anything else → 500) onto the
same `ApiResponse` envelope shape as a successful response.


## Repository — per-slice interfaces, not one generic class

**Decided** (resolves the previously-open question): each vertical slice owns
its own repository interface — `resource/repository/ResourceRepository`,
`news/repository/NewsRepository` — rather than a single generic
`Repository<T>`. Each interface has only the methods its slice's
service/controller actually call (e.g. `ResourceRepository` has no
save/update/delete, since storage is still read-only JSON loaded at
startup). Storage stays JSON-file-backed for this pass
(`JsonResourceRepository`, `JsonNewsRepository`) — a future SQLite/Postgres
swap only requires a new implementation of the same interface; services and
controllers don't change.

## AiAssistant (the AI seam)

`AiAssistant` (interface, `ai/service/AiAssistant.java`) is the abstraction for
AI assistance — one method, `generate(String prompt, double temperature)`.
`SpringAiAssistant` (`ai/service/SpringAiAssistant.java`) is its implementation,
using Spring AI's provider-agnostic `ChatClient` API. This replaces v1's direct
Ollama HTTP calls (`OllamaService`, deleted in this pass).

As of this pass, **no AI provider is configured** — no Spring AI model-provider
starter (e.g. `spring-ai-starter-model-ollama`) is on the classpath, since none
is available or subscribed to yet. `SpringAiAssistant` stays bootable by taking
an `ObjectProvider<ChatClient.Builder>` rather than a hard dependency; it
throws `AiProviderNotConfiguredException` only if actually called, which
`DecisionAgentService`'s existing fallback handling absorbs — the same
"AI guidance unavailable" behavior v1 had when Ollama was unreachable.

What stays stable across a future provider swap: `DecisionAgentService` and
every other caller depend only on the `AiAssistant` interface. Adding a
provider (e.g. `spring-ai-starter-model-ollama`) to `backend/pom.xml` should be
the only change needed to make `SpringAiAssistant` actually call out — its
internal logic already uses Spring AI's generic `ChatOptions`, not any
provider-specific options class.

## Technical stack

Intended implementations, reached through the service/repository patterns above —
not hard-wired.

| Concern | Intended implementation |
| --- | --- |
| Backend | Spring Boot 3.5.16 |
| API | REST |
| Frontend | React |
| Storage (now) | JSON |
| Storage (next) | SQLite → PostgreSQL |
| AI | Spring AI 1.1.8 (via `AiAssistant`/`SpringAiAssistant`) — no model provider configured yet |
| Mobile | Future |

> TODO: Confirm/adjust; note anything explicitly rejected.

## Milestone Roadmap

Small, demonstrable increments. **Every milestone leaves First Step in a working,
demoable state** and satisfies the [Definition of Done](00-philosophy.md#definition-of-done).

- [x] **Milestone 1** — Introduce `Community`; introduce `ContentSource`; keep JSON
  storage. → **Demo.** *(Completed — Steps 1-6 of the vertical-slice migration:
  shared domain kernel, `ApiResponse<T>`, `AiAssistant` seam, Resource/News/AI
  slices, real `Citation` → `ContentSource` resolution. Verified via a live
  Docker deployment and headless-browser walkthrough, not just `mvn test`.)*
- **Milestone 2** — Add flyer metadata extraction (unstructured → metadata while
  preserving the original artifact). → **Demo.** *(Not started — this specific
  milestone is about OCR/AI-driven extraction, still deferred. What DID land:
  a Flyer backend slice (`flyer/` — model/repository/service/controller)
  serving 7 real flyer images via `GET /api/flyers`, but with manually
  AUTHORED metadata (`app/data/flyers.json`), not extracted — see
  `references/decisions.md` Decision 011. `expert` and a `pipeline` package
  with minimal per-stage marker interfaces — `Collector`, `MetadataExtractor`,
  `Normalizer`, `Enricher`, `Deliverer` — remain scaffolding only, per the
  vertical-slice migration's Step 7. Resource/News/Flyer ingestion is
  deliberately NOT refactored to implement these interfaces yet — that
  refactor is future work once a real case demands it.)*
- [x] **Search MVP** — `GET /api/search?q=...&communityId=...` searching
  across Resource/NewsItem/Flyer in one community-aware, ranked list. →
  **Demo.** *(Completed — `search/` slice: `SearchService`/
  `SearchController`, scoring via the newly-extracted `shared/util/
  TextScore` (also adopted by `DecisionAgentService`). Backend only, no
  `app.js` UI yet. See `references/decisions.md` Decision 012.)*
- [x] **First non-Wilmington communities** — real `communityId` values
  derived from actual location data, `/api/search?communityId=...`
  meaningful across multiple towns. → **Demo.** *(Completed —
  `shared/util/CommunitySlug` derives `communityId` from each resource's
  location city; `resources.communities.json` (171 records, structurally
  mapped from the real DSCYF directory) adds real data for Newark, New
  Castle, Middletown, Bear, Claymont, and Hockessin alongside the existing
  Wilmington-curated set. No new Community CRUD API — see
  `references/decisions.md` Decision 013.)*

### Homepage redesign — 8-step roadmap

Redesigning the homepage/results pages into a trusted-civic-guide
experience (`references/CSSforNewDesign.md`), built as a real React
frontend. Each step gets its own dedicated design pass before being built,
mirroring how the original vertical-slice migration was planned.

- [x] **Step 1 — Category taxonomy + `GET /api/categories`.** → **Demo.**
  *(Completed — 10-category taxonomy in `category/model/CategoryDefinition`
  (100% resource coverage — see `references/decisions.md` Decision 014 for
  why 2 categories were added beyond the original 7). `category/service/
  CategoryService` aggregates counts, latest items (reusing `search/dto/
  SearchResult`), and policy-update linkage per category, community-aware.)*
- [x] **Step 2 — Expert stubs.** → **Demo.** *(Completed — two new
  `CivicContent` subtypes, `expert/model/{ExpertAnswer,FAQ}`, backed by
  hand-authored `app/data/{expert-answers,faq}.json`. `ExpertAnswer` is the
  first real adopter of the shared `Contact` composite. `FAQ.
  sourceExpertAnswerId` links back to the expert answer it was curated
  from. `GET /api/expert-answers` and `GET /api/faqs` (+ `/{id}`). Not yet
  wired into `Search`/`Category` — see `references/decisions.md`
  Decision 015.)*
- [x] **Step 3 — React frontend project scaffold.** → **Demo.**
  *(Completed — `frontend/` (React + Vite + TypeScript, not Next.js —
  Spring Boot already is the API). `backend/Dockerfile` gained a
  `node:24-alpine` build stage; the React `dist/` output rides into the
  jar at `static/app-next/` exactly like the existing hand-written static
  files do — no runtime-stage or `docker-compose.yml` changes needed.
  Served at `/app-next/`, NOT replacing the root demo yet (that flip is a
  later step, once the new app is functionally ready). `App.tsx` is a
  bare proof fetching real `/api/categories` data — no real UI yet, that's
  Steps 4-6. See `references/decisions.md` Decision 016.)*
- [x] **Step 4 — `AppLayout` + `Sidebar`.** → **Demo.**
  *(Completed — the frontend's first CSS + visual shell: sticky header,
  two-column (sidebar + main) grid, and a `Sidebar` of category-filter
  checkboxes fed live from `/api/categories` (reusing the `apiGet` wrapper
  and `CategorySummary` type). Existing warm green/orange/cream palette
  (`frontend/src/index.css`, mirroring `static/styles.css`), NOT
  `CSSforNewDesign.md`'s blue/purple. **Scope (confirmed with user):**
  checkboxes are **local-toggle only** — they visibly toggle but drive
  nothing downstream; real filtering is Step 7's shared filter context.
  Routing + `SpaWebConfig` widening **deferred to Step 6** (no real routes
  exist yet). `main` area is a Step-5 placeholder. **Deferred from this
  step, not built:** `CommunitySelector` wiring (the `communityId` param on
  `/api/categories`) and a toggle-driven mobile drawer — the responsive CSS
  collapses the sidebar to a horizontal wrap, but there's no drawer control.
  See `references/decisions.md` Decision 017.)*
- **Step 5 — `MainContent`: Hero + AI widget merge, Important Updates,
  `CategoryPreviewList`.** ("Important Updates," not "Trending Now" — see
  Decision 014.) **Split into 5a / 5b / 5c** (per the small-sequential-prompts
  preference — each planned/built/verified on its own):
  - [x] **Step 5a — Hero + AI guidance widget (merged).** → **Demo.**
    *(Completed — the old demo's text-only hero + separate AI section merged
    into one green→orange gradient hero carrying the AI question flow inline:
    question box, 🚨 Urgent / 🏠 Housing / 🛒 Essentials chips, Get Help →
    `POST /api/decide`, result rendered below. New `frontend/src/components/
    {HeroGuidance,MainContent}.tsx`, `apiPost` added to `api/client.ts`, AI DTO
    types added to `types/api.ts`. The AI provider is a stub — `/api/decide`
    returns a graceful degraded body — so the widget shows a friendly "AI
    guidance is temporarily unavailable" notice (the raw provider error is
    detected via its "AI call failed" prefix and suppressed; a legitimate
    "no matches" `notes` is still shown). Verified live via Docker + a driven
    submit. See `references/decisions.md` Decision 018.)*
  - [x] **Step 5b — Important Updates.** → **Demo.**
    *(Completed — new backend `updates/` package: `GET /api/updates` merges
    curated News + live RSS + Flyers server-side into normalized `UpdateItem`s
    (deduped by id, sorted date-desc nulls-last, capped 8). New
    `ImportantUpdates.tsx` renders the feed and **live-refreshes via a 5-minute
    poll with change-diffing** (a `useRef` serialized snapshot suppresses no-op
    re-renders; effect cleanup `clearInterval`s — the app's first polling
    pattern). Client-side polling, not WebSockets/SSE. Establishes the
    **backend-aggregates / frontend-displays** principle (Decision 019) that
    also governs Step 6+. Summary line-clamped after a long RSS body was found
    to blow out a card in live verification. 7 backend + 10 frontend tests
    green; verified live via Docker + Playwright driver. See Decision 019.)*
  - **Step 5c — `GET /api/home` consolidation + `CategoryPreviewList`.** Build
    `home/` package (`GET /api/home` = `{ aiConfig, updates, categories }`,
    composing `UpdatesService` + `CategoryService` + static `aiConfig`).
    `MainContent` fetches `/api/home` **once** and distributes: `aiConfig` →
    `HeroGuidance` (refactor 5a to take chips/prompts as props), `updates` →
    `ImportantUpdates` (seed for first paint; keep polling `/api/updates`),
    `categories` → new `CategoryPreviewList` (Browse inert until Step 6 routes).
    Sidebar consolidation deferred to Step 7. See Decision 019.
- **Step 6 — Results pages + progressively-detailed cards.** Three
  density tiers; no backend gap, every field already exists.
- **Step 7 — Shared filter context wiring** across AI guidance, Important
  Updates, category previews, and search results.
- **Step 8 — Interaction polish + full verification.** Existing warm
  green/orange/cream palette throughout (not `CSSforNewDesign.md`'s
  blue/purple), responsive behavior, full live verification.

Also logged, explicitly deferred, not part of this sequence: real
Exiftool/AI-based metadata extraction for Flyers (ties back to Decision
011's original OCR/AI deferral).

> TODO: Add further milestones (e.g. repository pattern, SQLite, Spring AI swap,
> a real Community discovery API, full curation of the community-directory
> resources' eligibility/cost/urgency/tags). Each must end in a working demo.

## Future capabilities (backlog)

> TODO: Map each to the stage/seam it extends — personalized alerts, saved
> interests, multi-language support, voice assistance, SMS guidance, organization
> dashboards.
