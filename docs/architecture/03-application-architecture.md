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
CivicAssistantService
```

Errors are centralized through `shared/web/GlobalExceptionHandler`
(`@RestControllerAdvice`), which maps thrown exceptions
(`shared/exception/NotFoundException` → 404; anything else → 500) onto the
same `ApiResponse` envelope shape as a successful response.


## Repository — a *pattern*, not a committed generic class

> **NOTE — decision intentionally deferred.** The persistence layer is described
> as a **pattern**. Whether all knowledge lives in a single generic repository, or
> in several purpose-specific repositories, is **left open** until decided. Do not
> commit to a `Repository<T>` or any one structure here.

> TODO: Describe the repository pattern and the open question of single-vs-multiple
> repositories.

## CivicAssistantService (the AI seam)

> TODO: `CivicAssistantService` is the abstraction for AI assistance. Today it is
> backed by direct Ollama HTTP calls (v1 `OllamaService`); the intended v2
> implementation is **Spring AI**. Describe the seam and what stays stable across
> the swap.

## Technical stack

Intended implementations, reached through the service/repository patterns above —
not hard-wired.

| Concern | Intended implementation |
| --- | --- |
| Backend | Spring Boot |
| API | REST |
| Frontend | React |
| Storage (now) | JSON |
| Storage (next) | SQLite → PostgreSQL |
| AI | Spring AI (via `CivicAssistantService`) |
| Mobile | Future |

> TODO: Confirm/adjust; note anything explicitly rejected.

## Milestone Roadmap

Small, demonstrable increments. **Every milestone leaves First Step in a working,
demoable state** and satisfies the [Definition of Done](00-philosophy.md#definition-of-done).

- **Milestone 1** — Introduce `Community`; introduce `ContentSource`; keep JSON
  storage. → **Demo.**
- **Milestone 2** — Add flyer metadata extraction (unstructured → metadata while
  preserving the original artifact). → **Demo.**

> TODO: Add further milestones (e.g. repository pattern, SQLite, Spring AI swap,
> first non-Wilmington community). Each must end in a working demo.

## Future capabilities (backlog)

> TODO: Map each to the stage/seam it extends — personalized alerts, saved
> interests, multi-language support, voice assistance, SMS guidance, organization
> dashboards.
