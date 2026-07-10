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
  preserving the original artifact). → **Demo.** *(Not started. Empty package
  scaffolding for `flyer`, `expert`, `search`, and a `pipeline` package with
  minimal per-stage marker interfaces — `Collector`, `MetadataExtractor`,
  `Normalizer`, `Enricher`, `Deliverer` — now exist as a landing point, per
  the vertical-slice migration's Step 7. No real logic, and Resource/News
  ingestion is deliberately NOT refactored to implement these interfaces
  yet — that refactor is future work once a second real case (e.g. Flyer)
  exists to validate the shape against.)*

> TODO: Add further milestones (e.g. repository pattern, SQLite, Spring AI swap,
> first non-Wilmington community). Each must end in a working demo.

## Future capabilities (backlog)

> TODO: Map each to the stage/seam it extends — personalized alerts, saved
> interests, multi-language support, voice assistance, SMS guidance, organization
> dashboards.
