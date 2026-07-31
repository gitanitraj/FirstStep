First Step is an editorial and technical platform.

# 04 — Editorial Principles

*The content and governance rules for what enters and leaves the knowledge base.*

These principles govern the human and AI judgment applied across the pipeline —
especially the **Enrich** and **Deliver** stages.

## Editorial Standards

-   Make civic information easier to understand without changing it.
-   Never advocate for a political position.
-   Attribute information to its source.
-   Distinguish facts from AI-generated summaries.
-   Preserve provenance through ContentSource and Citation.
-   Present context without changing underlying facts.
-   Encourage residents to connect with the originating organization
    rather than replacing it.
-   Accuracy before speed.
-   Build trust through transparency and traceability.

## Classification

**Editorial classification identifies the *primary home* of a CivicContent item.
Tags describe what the content is *about*. Cross-category relevance is expressed
through *relationships*, not multiple editorial classifications.**

| Concept | Field | Job |
| --- | --- | --- |
| Editorial classification | `category_tags` + `subcategory` | Where the item lives. Drives navigation. |
| Descriptive metadata | `tags` | What it is about. Drives search, filtering, AI enrichment. |
| Relationships | enrichment product | Cross-category relevance. |

Editorial classification is **deterministic and singular**: one `subcategory`,
chosen by an editor. A multi-valued subcategory was rejected — it complicates
navigation, validation and counting across the whole platform in order to serve a
minority of items. When an item is genuinely relevant beyond its primary home
(the disability-services flyer that is Legal ▸ Disability Advocacy but also
speaks to Community Support ▸ Information & Referral), that relevance belongs in
descriptive tags and in the relationship graph — not in a second classification.

**Tags must never determine navigation.** This is not a style preference: when
one field carries both meanings, every consumer must first ask what kind of
content it is holding before it can interpret the field, which is exactly the
special-casing the CivicContent contract exists to abolish.

### Automated classification is subordinate to editorial classification

> The classifier only classifies when editorial classification is absent.
> Hand-authored editorial classifications are authoritative and **immutable
> during ingestion**. Automated classification exists **solely to normalize
> unclassified content.**

"Immutable during ingestion" is precise: editorial classification can and should
change — an editor edits it. What is forbidden is the *pipeline* mutating it. The
rule applies **per field**, so an item with hand-authored `category_tags` and no
`subcategory` keeps the former untouched while the latter remains eligible.

It follows that **changes to how content is classified must result from
intentional editorial decisions, never from classifier behavior.** Tuning the
keyword vocabulary may change what *unclassified* content normalizes to; it must
never move content an editor has already placed.

Every source — Resources, News, RSS/Laws, Flyers and Expert content — classifies
into the same canonical taxonomy (`app/data/taxonomy.json`). Upstream label drift
is normalized **at the source**, never absorbed by widening the taxonomy's match
lists downstream, which is how a controlled vocabulary decays into a record of
every integration ever built.

## Civic Content

Every piece of CivicContent should answer:

-   What is available?
-   Who is eligible?
-   Where can someone learn more?
-   Who should they contact?

Every piece of NewsItem should have a headline and summary. Everypiece of NewsItem should answer:

-   Why does it matter?

## Translations & multi-language

> TODO: Standards for translated content and how the original/authoritative
> version is preserved.

## AI-generated context
AI summarizes but never invents.
> TODO: How AI-generated enrichment is labeled, bounded, and kept distinguishable
> from source content.

## Community submissions

> TODO: Intake, moderation, and provenance rules for resident/partner submissions.
