# First Step v2 — Architecture

This directory holds the **v2 architecture specification**: the design that
reorganizes First Step around **information flows** rather than application
features.

**Scope:** design documentation only. This is a *parallel v2 design* — v1 (the
working Spring Boot + Docker demo) keeps running and is migrated incrementally.
Nothing here changes v1 code.

## Documents

| Doc | Purpose |
| --- | --- |
| [00-philosophy.md](00-philosophy.md) | The information-over-features thesis, the five pipeline stages, and the project-wide **Definition of Done**. |
| [01-domain-model.md](01-domain-model.md) | The knowledge model: Core Knowledge, organizing entities, and Supporting objects. |
| [02-information-flow.md](02-information-flow.md) | How information moves through Collect → Generate metadata → Normalize → Enrich → Deliver. |
| [03-platform-architecture.md](03-platform-architecture.md) | How the software implements the flow, plus the **Milestone Roadmap**. |
| [04-editorial-principles.md](04-editorial-principles.md) | Content and governance rules for what enters and leaves the knowledge base. |

## Diagrams

UML diagrams live in [uml/](uml/):
- [Domain Model](uml/domain-model.md)
- [Information Flow](uml/information-flow.md)
- [Information Sources](uml/information-sources.md)

## Key references

- **Definition of Done** — every implementation task must satisfy it. See
  [00-philosophy.md](00-philosophy.md#definition-of-done).
- **Milestone Roadmap** — small, demoable increments. See
  [03-platform-architecture.md](03-platform-architecture.md#milestone-roadmap).

> TODO: Add a one-paragraph summary of the v2 vision once 00-philosophy is authored.
