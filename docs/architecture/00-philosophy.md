# 00 — Philosophy

*The north star all other architecture documents reference.*

## Information over features

> TODO: State the core thesis — First Step is a program designed around
> **information flows**, not application features. Every capability (resource
> discovery, AI assistance, newsletters, …) exists to collect, organize, enrich,
> preserve, and deliver community information.

## The five pipeline stages

The organizing spine of the whole system. These stage names are used verbatim
across every other document.

1. **Collect** — gather from structured and unstructured sources.
2. **Generate metadata** — extract structure from unstructured artifacts *while
   preserving the original*.
3. **Normalize** — map everything onto one consistent knowledge model.
4. **Enrich** — add categories, tags, summaries, citations, relationships,
   translations, and AI-generated context.
5. **Deliver** — expose the same knowledge through many channels.

> TODO: Expand each stage with its intent and boundaries.

## Transparency & traceability

> TODO: State the commitment that every delivered piece of information traces back
> to its original source, and how that trust is surfaced to the community.

## Scale by community, not by app

> TODO: Explain horizontal scaling — First Step grows by *adding communities*, not
> by duplicating applications. `Community` is a first-class partition across the
> whole model.

## Definition of Done

Every implementation task in First Step must:

- [ ] Follow the architecture specification.
- [ ] Preserve traceability to the original source.
- [ ] Include tests.
- [ ] Update documentation if the domain changes.
- [ ] Remain deployable.
- [ ] Keep the demo functional.
- [ ] Not introduce Wilmington-specific assumptions unless intentionally scoped.
- [ ] Support future multi-community expansion where applicable.

> TODO: Add any project-specific clarifications or examples for each criterion.
