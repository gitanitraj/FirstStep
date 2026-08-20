First Step composes what it already knows.

# 05 — The Front Door

*The homepage information architecture: what each section is, which service
supplies it, and which slice owns what is missing.*

The **Front Door** is the resident's entry point — an eight-section homepage that
promotes the three mission pathways to the top level. This document is the
**scoping pass**, produced 2026-08-08 after Slice F. It is design documentation:
nothing here is built yet, and its job is to make the build decisions before code
makes them by accident.

---

## 1. What the Front Door is

**The Front Door is a composition layer, not a content architecture.**

Slice F established the foundation: the `CivicContent` contract, the canonical
taxonomy in `app/data/taxonomy.json`, and the four-level navigation hierarchy
(Category → Group → Topic → CivicContent) delivered end to end through
`/api/category/{key}` and `/api/category/{key}/{topic}`. The Front Door composes
that foundation into the resident experience. Slices G and H then fill in the two
destinations the homepage points toward.

**The architectural risk is that presentation and discovery needs get satisfied
by inventing new domain concepts rather than using the existing CivicContent
model.** Three resolutions follow from that, and they govern everything below:

| The homepage needs… | It gets… | It does **not** get |
| --- | --- | --- |
| A Seniors pathway | a controlled **discovery tag** derived from population and eligibility metadata | a canonical category · a ContentType |
| Community Information in four groups | existing CivicContent types plus distinguishing **metadata** | four new ContentTypes |
| First Step Originals | a **`ContentSource`** identity | an `Originals` domain class |

**The through-line: presentation may compose the model in new ways; it may not
add to it.** Every section in §6 is scoped against that test.

> **AMENDED by Decision 045.** The rule above is unchanged. What was too broad
> was its silence on *grouping*, which the Latest Updates page exposed as a
> legitimate boundary case:
>
> **Presentation may group or organize existing CivicContent by controlled
> metadata** — such as `contentType`, category, or `ContentSource` — **when that
> grouping represents a meaningful user-facing discovery model.** Such groupings
> must use **generic presentation components** rather than creating a component
> or domain concept per metadata value. **Empty groups are not rendered.**
>
> The distinction that makes this safe: grouping *reads* metadata the domain
> already owns. It adds no type, no field and no class. A `LawGroup` component
> beside a `NewsGroup` component would be the violation — not the grouping
> itself, but enumerating the metadata in code. One generic `UpdateGroup`
> renders every group, so a sixth ContentType costs nothing.

### 1.1 Why this is the risk

This is not a hypothetical. **The same failure already happened in this codebase
once and was paid down.**

**It has a precedent here.** Flyers once reached a category through a hardcoded
`includesFlyers` boolean on the Community Events category definition — a
*presentation* need ("show flyers on this page") satisfied by a *domain* special
case. Slice F1 deleted it and made flyers classify through the canonical taxonomy
like every other content type (see `Flyer.java`, `CategoryDefinition.java`, and
`references/CategoryService_annotated.java` §3). The Front Door asks three
structurally identical questions at once — Seniors, the Community Information
groups, Originals — so the same shortcut is available three times over.
A second instance is `UpdateItem.type`, a display-shaped field that reports
`"news"` for both curated news and signed legislation, and which now costs Slice H
a named retirement criterion (Decision 036).

**The contract only holds if ContentType stays behavioral.** `ContentType`'s own
contract states that it "determines how an item is PRESENTED … never WHERE it
appears," and `CivicContent`'s states that every type answers the same six
questions with the same fields. Adding Events / Meetings / Announcements would
take the enum from 5 values to 8 that answer those six questions **identically** —
types distinguished by nothing the system can act on. That empties the enum of
meaning for the types that earned it, and there is no cheap way back:
`contentType` is persisted in the data files and is the semantic identifier F5a
and F6 were built on.

**A new domain concept is not a local change here.** The verified blast radius of
a single new ContentType value:

- the `ContentType` enum
- `frontend/src/i18n/contentTypeLabel.ts` — an exhaustive
  `Record<ContentType, string>` that **fails the build** if a value is unhandled
- the `badge-${contentType.toLowerCase()}` CSS classes (five today)
- `UpdatesService`'s per-type mappers
- `EditorialStabilityTest`'s pinned baseline
- English and Spanish i18n keys
- and, per `CLAUDE.md`, a full annotated mirror for every file touched

Four new types is that cost four times over, for one homepage row.

**The project's own rules point the other way.** "Simplicity First — no
abstractions for single-use code; no flexibility that wasn't requested" makes a
domain class that exists solely to fill a homepage section the defined
anti-pattern. And the precedent for the alternative is already set: when Decision
036 found 193 of 429 classified items carried a category but no subcategory, the
*rejected* fix was making the classifier infer subcategories, and the *accepted*
one was **composition** — reaching that content through an aggregate instead.
Seniors, Originals and Community Information are the same shape of problem, and
they get the same shape of answer.

---

## 2. Five concepts, five questions

`01-domain-model.md` establishes three vocabularies (Taxonomy · Navigation ·
Content) and a three-question test for what may become a category. The Front Door
exercises two further axes, so the test extends:

| Question | Answered by | Drives |
| --- | --- | --- |
| What is it about? | `categoryTags` + `subcategory` | **Navigation placement** |
| What kind of thing is it? | `contentType` | Presentation |
| Who produced it? | `contentSource` | Attribution · Originals membership |
| Who is it relevant to? | derived discovery tag (e.g. `seniors`) | **Discovery pathway** |
| How is it found? | `tags` | Search, filtering, relationships |

Five concepts, kept distinct:

- **Category** — canonical editorial classification.
- **Subcategory** — canonical editorial classification within a category.
- **Tags** — descriptive metadata about the CivicContent.
- **ContentType** — behaviorally meaningful content type: Resource, News, Flyer,
  Law, Expert.
- **ContentSource** — who produced the content.

**This refines row three of `01-domain-model.md`'s table rather than replacing
it.** That row correctly identifies "Who is this for?" as a
population/eligibility facet. The Front Door adds the mechanism: the facet fields
remain the authoritative source evidence, and a **derived discovery tag** is
computed from them. See §8.

**A discovery pathway is a filtered view across the taxonomy, never a node within
it.** It yields a result set; it never creates a category page with its own topic
groups. This is what keeps "a first-class discovery path driven by tags metadata"
consistent with the load-bearing rule that tags never determine navigation —
**placement and filtering are different operations.**

---

## 3. Mission pathways

The three mission cards are **homepage pathways, not domain objects.** Each
answers one resident question and leads into a capability that already exists or
is owned by a named slice:

```
Discover        What is available?      → Resource Categories
                                            ↓ Group ↓ Topic ↓ CivicContent

Connect         Where do I go next?     → Find Help
                                            ↓ Organization directory / search
                                            ↓ Organization
                                            ↓ that organization's First Step resources

Stay Informed   What has changed?       → Important Notices
                                            ├── News / Policy Updates
                                            ├── Laws
                                            └── Community Information
```

These are the same three pillars the category page already implements (Decision
036 — Discover / Connect / Stay Informed). The Front Door promotes them to the
homepage rather than inventing them.

---

## 4. Global navigation

Five items, replacing today's four (`nav.housing`, `nav.community`,
`nav.important`, `nav.life` in `PrimaryNav.tsx`):

```
Discover · Find Help · Stay Informed · Community Information · About First Step
```

**Labels are provisional.** What is being fixed here is the *structure*; the
exact wording is refined during the Front Door UI work.

**No category appears in the global navigation.** Today's nav leads with "Housing
Assistance", which puts a single category at the same level as the pathways that
contain it. Categories live *inside* Discover, so categories and mission cards
never compete for primary navigation.

Note the asymmetry the list makes visible: **Discover, Find Help and Stay
Informed are mission-level navigation; Community Information is a content
collection.** They sit side by side because residents look for both, not because
they are the same kind of thing.

---

## 5. The eight sections

```
1. AI Guidance / Search       natural-language entry point
2. Global Navigation          Discover · Find Help · Stay Informed · Community Information · About
3. Mission Cards              Discover · Connect · Stay Informed
4. Important Changes in DE    the Delaware Laws scroll, RENAMED
5. Main Content Split         LEFT: Civic Resource Categories
                              RIGHT: First Step Originals
6. Community Information      Flyers · Events · Meetings · Announcements
7. Latest Updates             News · Policy Updates · Emergency Notices
8. Footer
```

Two changes from the current homepage are explicit in the spec: Row 4 is renamed
from "New Delaware Laws" to **"Important Changes in Delaware"**, and Row 5 splits
into a two-column layout.

---

## 6. Dependency map

```
Front Door
│
├── Hero / AI Search ─────────── HomeService.AI_CONFIG + POST /api/decide      ✅
├── Global Navigation ────────── static                                    ⚠ rewrite
├── Mission Cards ────────────── static routing only                          ➕ new
├── Important Changes ────────── LegislationService.getRecentSignedBills()     ✅
├── Resource Categories ──────── CategoryService.getAll() / NavigationService  ✅
│   └── Seniors pathway ──────── derived `seniors` discovery tag             ⚠ gap 3
├── First Step Originals ─────── FaqService, filtered by contentSource.id  ⚠ gap 1+2
├── Community Information ────── FlyerService.getCarouselCards()             ⚠ gap 4
├── Latest Updates ───────────── UpdatesService.getUpdates()                   ✅
└── Footer ───────────────────── static                                       ➕ new
```

| § | Section | Supplied by | Status | Owner | What is missing |
| --- | --- | --- | --- | --- | --- |
| 1 | AI Guidance / Search | `HomeService.AI_CONFIG`, `POST /api/decide` (`DecisionAgentService`), `SiteHero` | ✅ built | — | nothing |
| 2 | Global Navigation | static — `PrimaryNav.tsx` | ⚠ rewrite | Front Door | 4 items → 5, new destinations |
| 3 | Mission Cards | static routing only | ➕ new | Front Door | presentational component; **no service** |
| 4 | Important Changes in Delaware | `LegislationService.getRecentSignedBills()` → `DelawareLawsFeature` | ✅ built | Front Door | **rename only** |
| 5L | Civic Resource Categories | `CategoryService.getAll()` → `ResourceDiscovery` | ✅ built | Front Door | layout change only |
| 5L | └ Seniors pathway | derived `seniors` tag over `Resource.population` / `eligibility` | ⚠ gap 3 | future slice | the derivation itself |
| 5R | First Step Originals | `FaqService`, filtered by `contentSource.id` | ⚠ gaps 1+2 | future slice | `id` populated; no query exists |
| 6 | Community Information | `FlyerService.getCarouselCards()` (7 flyers) | ⚠ gap 4 | future slice | grouping metadata |
| 7 | Latest Updates | `UpdatesService.getUpdates()` → `ImportantUpdates` | ✅ built | Front Door | nothing |
| 8 | Footer | static | ➕ new | Front Door | content decision |

**Five of ten rows are supplied by services that already exist**, verified
against the codebase during scoping. That is the point of the exercise: the Front
Door is mostly re-composition, and the genuinely new work is concentrated in
three gaps and two destination slices.

---

## 7. Mission-card destinations

The spec left these unspecified. Resolved:

| Card | Copy | Button | Route | Supplied by | Status |
| --- | --- | --- | --- | --- | --- |
| **Discover** | "Search trusted housing, food, health, employment and community resources." | Explore Resources | `/discover` | `CategoryService.getAll()` | service exists; **route is new** |
| **Connect** | "Find organizations, programs, offices and services." | Find Help | `/find-help` | organization directory | **Slice G** |
| **Stay Informed** | "Read local news, community announcements and important updates." | View Updates | `/updates` | `UpdatesService.getUpdates()` | **Slice H** |

**`/discover` is a real page, not an anchor scroll into Row 5.** The global nav
item needs a destination, and an anchor is not one; making Discover a page means
the nav item and the mission card share it.

**Consequence, stated plainly: when the Front Door ships, two of its three
mission cards point at stub pages.** That follows directly from the intended
sequencing — the Front Door composes, then G and H fill in the destinations it
points toward — but it is better recorded now than discovered during
verification. `/organization/:slug`, `/community-info`, `/important-notices` and
`/life-assistance` are all `StubPage` today.

---

## 8. Seniors as a derived discovery pathway

> **Seniors answers "Is this relevant to seniors?" — not "Was this program
> created exclusively for seniors?"**

Seniors is a legitimate resident discovery need, not a presentation convenience.
It is also the section of this document most likely to be re-derived incorrectly
later, so the rule and its evidence are both recorded.

### 8.1 The model

**Source evidence stays authoritative.** `population`, `eligibility`,
`eligibility_age_min` and `eligibility_age_max` are the resident-facing
requirement and are never rewritten or replaced. A resident is told
*"Age 62 and older"* — the program's actual rule — not *"seniors"*.

**`seniors` is derived discovery metadata.** It is an *additional* controlled
classification computed during **Enrich**, the pipeline stage that already
produces derived metadata (`02-information-flow.md`), from explicit evidence in
the source. It makes content discoverable through the Seniors pathway; it does
not describe the program's eligibility.

**The Seniors page is a discovery page like any other.** It retrieves
CivicContent carrying the controlled tag and presents the results using the same
category and content-type structure as every other discovery page.
**No second Seniors query. No Seniors ContentType. No separate senior-resource
model.**

### 8.2 Evidence rules

| Source evidence | Assign `seniors`? | Observed in data |
| --- | --- | --- |
| `Seniors`, `Senior citizens` | Yes | tags only — **never** a `population` value |
| `Older adults`, `Older persons` | Yes — **subject to the collision guard** | HA-034, HA-035 |
| `Elderly` | Yes | HA-034, HA-035, SD-051, SD-052 |
| Age 65+ / 62+ / 60+ / 55+ | Yes | HA-027 (62), SD-039 (60), HA-036 (55) |
| **Age 50+** | **Yes — and flag the evidence source**, so the 50–54 band stays auditable without re-deriving everything | SD-095, SD-127 |
| A general age range that merely happens to include older adults | **No** | HA-010, HA-014 (`Age 18 and older`) |

The last row is the rule's real content. A general adult program does not become
senior-relevant because 68-year-olds are inside its range; a program becomes
senior-relevant when its eligibility **explicitly includes** elderly, senior or
older-adult residents, or when it is age-restricted at a senior threshold, or
when the provider describes it as senior programming.

### 8.3 Two constraints the data forces

**1 — A collision guard is required, and for a specific reason.**

The phrase rule `older adults?` matches the literal sentence:

> *"Age 18 and **older Adults** recovering from substance abuse, domestic
> violence…"* — HA-010, HA-014

The phrase is genuinely present; it spans the end of `"Age 18 and older"` and the
start of the next sentence. **No word-boundary anchoring fixes this** — this is
the same family of bug as `\blogo\b` matching inside `hero-logo` during the CSS
sweep (Decision 039). The guard excludes the pattern `\d+\s+and\s+older\s+adults?`
before term matching runs.

**HA-010 and HA-014 must be excluded by the collision guard, not by the age
gate.** The distinction matters: filtering them out on `age = 18` would appear to
work while encoding the wrong logic, and that same logic would wrongly drop
HA-034 and HA-035 — which are `Age 18 and older` **and** genuinely
senior-relevant, because their eligibility text reads *"includes elderly and
disabled persons."* A rule that cannot tell those two cases apart is the wrong
rule, however plausible its output looks.

**2 — Communities data carries no numeric age field.**

`eligibility_age_min` is populated in 20 of 58 records in `resources.json` and in
**0 of 171** in `resources.communities.json`. There, the threshold exists only
inside the prose `population` string (`"Age 50 and older"`). The implementing
slice must choose one of:

- the derivation parses the `population` string, or
- ingestion populates `eligibility_age_min` from it, and the derivation reads the
  structured field only.

The second is preferable — it fixes the missing structure once rather than
teaching every future consumer to parse prose — but it is a data-pipeline change
and is named here as a decision, not made here.

### 8.4 Verified reach today

**8 resources** carry senior evidence: 5 in `resources.json` (HA-016, HA-027,
HA-034, HA-035, HA-036) and 3 in `resources.communities.json` (SD-039, SD-095,
SD-127).

**0 non-resource CivicContent.** News (8), flyers (7), FAQs (6) and expert
answers (6) carry no senior evidence at all. The rule is CivicContent-wide by
design; only Resources exercise it today.

**Age evidence currently adds no record that term evidence does not already
find.** Every age-qualified record also carries a senior term in its tags. The
age rule is kept because it is the provider-independent signal — it will matter
for data that arrives with a threshold and no vocabulary — but its present value
is future-proofing, not coverage. Recording this prevents a later reader from
concluding the age rule is load-bearing when it is not yet.

Reproduce these counts:

```bash
python3 - <<'PY'
import json, re
TERM    = re.compile(r'(?<![\w-])(seniors?|senior citizens?|elderly|older adults?|older persons?)(?![\w-])', re.I)
COLLIDE = re.compile(r'\d+\s+and\s+older\s+adults?', re.I)
def agemin(i):
    v = i.get('eligibility_age_min')
    if isinstance(v, int): return v
    m = re.search(r'Age\s+(\d+)\s+and older', str(i.get('population') or ''), re.I)
    return int(m.group(1)) if m else None
for f in ['resources.json', 'resources.communities.json']:
    items = json.load(open('app/data/' + f))['records']
    for i in items:
        ev  = ' '.join(str(i.get(k) or '') for k in ('population', 'eligibility'))
        tg  = ' '.join(i.get('tags') or [])
        a   = agemin(i)
        why = []
        if a is not None and a >= 50:      why.append(f'age>={a}' + (' [50-54 FLAG]' if a < 55 else ''))
        if TERM.search(tg):                why.append('term:tags')
        if TERM.search(COLLIDE.sub(' ', ev)): why.append('term:eligibility')
        if why: print(f, i.get('id'), why)
PY
```

---

## 9. Gaps and owners

### Gap 1 — `ContentSource.id` is unpopulated everywhere

`ContentSource` declares `id, name, type, url, retrieved`. **`id` is `null` in
every record in every data file**, so Originals membership has no queryable key.

*Done means:* First-Step-produced records carry `id: "first-step"`,
`name: "First Step"`.

**Why `ContentSource` and not a new ContentType — the data already made this
distinction informally:**

| File | `contentSource.name` | `contentType` |
| --- | --- | --- |
| `faq.json` | `"First Step Curated FAQ"` | `EXPERT` |
| `expert-answers.json` | `"Delaware Volunteer Legal Services"` | `EXPERT` |

Two records, same content type, different producers — "we made this" versus "we
publish this". The distinction already exists in the data as an unstructured name
string. `contentSource.id` is the structured form of a fact the data is already
recording, not a new concept. **First Step Originals describes who created the
content, not a fundamentally different kind of CivicContent.**

```
CivicContent
    contentType
    ContentSource
        id:   first-step
        name: First Step
```

### Gap 2 — Originals content is editorially unclassified

FAQ **0 of 6** and expert answers **0 of 6** carry `category_tags` or
`subcategory`. `UpdatesService.getForCategory` therefore matches none of them:
they cannot reach a category page.

**This is a separate gap from Gap 1, and conflating them will cause rework.**
`contentSource` says *who made it*; only editorial classification says *where it
appears*. Originals needs both — identity to be collected into the section,
classification to be reachable from anywhere else.

**Row 5 RIGHT ships with what exists** (the 6 curated FAQs) and grows as
Originals are authored. The section proves the `ContentSource` mechanism against
live data rather than against a hypothetical. Community Briefings, YouTube, Data
Stories and newsletters do not exist as data today.

### Gap 3 — the Seniors derivation is not implemented

See §8. Nothing in the pipeline assigns the tag today.

### Gap 4 — Community Information has no grouping metadata

All 7 flyers carry `contentSource.type: "manual"`. Nothing distinguishes a
meeting from an announcement from an event.

**Community Information initially remains a presentation structure around
existing CivicContent.** Flyers, Events, Meetings and Announcements do not become
four new CivicContent types unless they actually behave differently in the
system; the grouping is expressed through metadata.

The implementing slice picks the distinguishing metadata and **must not reach for
`subcategory`** — that field is the topic level of the navigation hierarchy, and
using it for presentation grouping would put a display concern inside editorial
classification. That is the `includesFlyers` mistake in a new costume.

### Gap 5 — no organization directory capability

`OrganizationService.getCuratedShortlist()` and `getForCategory(categoryKey)`
exist, but there is no directory, no organization search, and no
`/api/organization/{slug}`. `/organization/:slug` is a `StubPage`.

**Owner: Slice G.** This is the destination behind Connect → Find Help.

### Gap 6 — no Important Notices page

**Owner: Slice H**, which already carries the `UpdateItem.type` retirement exit
criterion (Decision 036). This is the destination behind Stay Informed → View
Updates.

### Gap 7 — payload growth is the performance trigger

Baseline measured 2026-08-06, before any of this exists:
`/api/home` 55 KB / 49 ms · `/api/category/housing` 22 KB / 6 ms ·
DOMContentLoaded 38–52 ms · 191 DOM nodes.

Rows 5R, 6 and 7 put Originals, four Community Information groups and a Latest
Updates feed on one page. This is the moment the tech-debt register named for
revisiting performance budgets — not before, because until now nothing was slow
enough for a budget to guard anything.

---

## 10. Conventions that apply

- **Every Front Door component is built as a co-located CSS Module** —
  `components/X/{X.tsx, X.module.css}`. `index.css` is a quarantine at 91
  selectors and its count may only decrease. See
  [../frontend/css-architecture.md](../frontend/css-architecture.md).
- **The backend aggregates; the frontend displays.** New page-shaped data goes
  through a BFF endpoint rather than being stitched client-side (Decisions
  019/020).
- **A readiness check tests the slice's contract, not the application's overall
  health.** Gate a Front Door slice only on the dependencies that slice actually
  has.

---

## Related

- [01-domain-model.md](01-domain-model.md) — the three vocabularies, and the
  three-question test §2 extends.
- [02-information-flow.md](02-information-flow.md) — the Enrich stage that owns
  the Seniors derivation.
- [04-editorial-principles.md](04-editorial-principles.md) — classification rules
  the Front Door composes but never overrides.
- `references/decisions.md` Decision 041 — the reasoning behind this scoping pass.
