## Definition

**First Step brings trusted public information together in one place,
presenting it in a way that is easy to discover, understand, and explore
without changing the underlying facts.**

Rather than replacing government agencies or nonprofit organizations,
First Step connects residents with verified information from those
organizations while providing the context needed to navigate available
resources confidently.

## Mission

Help residents understand what is available, what has changed, and where
they can go or who they need to contact to take the next step.

## Three Architectural Layers

1.  **Shared Domain** --- defines what First Step knows.
2.  **Business Capabilities** --- defines what First Step does.
3.  **Information Pipeline** --- defines how information becomes
    CivicContent.

## The five pipeline stages

The organizing spine of the whole system. These stage names are used verbatim
across every other document.

1. **Collect** — gather from structured and unstructured sources.
2. **Generate metadata** — extract structure from unstructured artifacts while preserving the original.
3. **Normalize** — map everything onto one consistent knowledge model.
4. **Enrich** — add categories, tags, summaries, citations, relationships,
   translations, and AI-generated context.
5. **Deliver** — expose the same knowledge through many channels.

## Scale by community, not by app

First Step grows by *adding communities*, not by duplicating applications. `Community` is a first-class partition across the
whole model.

Community
──────────────────────────────
id
name
city
state
zipCodes : List<String>
active

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


