# Adding content

How to author a flyer, an announcement, or a First Step Original by hand, and
what has to be true before it will show up.

Two rules explain most of what follows:

1. **The producer decides where content lands.** `contentSource` → the registry →
   a sector → a destination page. Government content goes to Latest Updates;
   community content goes to Community Notices. Nothing else decides this.
2. **An unregistered producer is silently invisible.** The record stays valid and
   still appears in category pages and search — it just cannot claim a sector, so
   no sector page claims it. Register the producer first.

---

## Step 1 — Register the producer (once per organization)

`app/data/content-sources.json`

```json
{
  "id": "ncc-recorder-of-deeds",
  "name": "New Castle County Recorder of Deeds",
  "sector": "government"
}
```

| field | rule |
| --- | --- |
| `id` | lowercase-kebab-case, stable forever. Content references this, not the name. |
| `name` | Exactly how the organization should be shown to a resident. This is the only place attribution comes from. |
| `sector` | `government` · `community` · `first-step`. Nothing else. |
| `feedUrl` | Optional, RSS only. Omit for hand-authored producers. |

**Already registered** — reuse these rather than adding a near-duplicate:

```
government   city-of-wilmington · de-housing-authority · de-dhss
             wilmington-housing-authority · de-visually-impaired · de-legislature
community    wilmington-farmers-market · west-end-neighborhood-house
             ministry-of-caring · community-legal-aid · united-way-delaware
             westside-family-healthcare · de-volunteer-legal-services
first-step   first-step
```

> **Sector is about who PRODUCED the notice, not who runs the program.** A city
> announcement about a program administered by a nonprofit is still
> `government` — the city published it. The administering organization belongs in
> the body text, and probably deserves its own resource record.

---

## Step 2a — A flyer

`app/data/flyers.json` → the `records` array. Copy this whole block:

```json
{
  "id": "FL-008",
  "communityId": "wilmington-de",
  "title": "Transfer on Death Deeds — Free Information Session",
  "summary": "One sentence a resident can act on. What it is and who it is for.",
  "verified": true,
  "category_tags": ["Housing"],
  "subcategory": "Homeownership",
  "tags": ["Homeownership", "Free", "event"],
  "contentSource": {
    "id": "ncc-recorder-of-deeds",
    "type": "manual"
  },
  "createdDate": "2026-08-21",
  "updatedDate": "2026-08-21",
  "organization": "New Castle County Recorder of Deeds",
  "event_date": "2026-09-18",
  "location": {
    "label": "Louis L. Redding City/County Building",
    "address": "800 N. French Street",
    "city": "Wilmington",
    "state": "DE",
    "zip": "19801"
  },
  "image": "NCC Transfer on Death TODD.jpg"
}
```

### The three fields that decide where it appears

**`contentSource.id`** — must exist in the registry (Step 1). Its sector decides
whether the flyer is a Community Notice or a Latest Update.

**Exactly one notice kind in `tags`** — `event`, `meeting`, or `announcement`:

| kind | use it for |
| --- | --- |
| `event` | Something happening at a time and place a resident can attend |
| `meeting` | A public meeting, hearing, or input session |
| `announcement` | News with nothing to attend — a program opening, a policy, a waiting list |

Not `events`. Not two of them. The validator blocks both, and a flyer with no
kind is invisible on all four views.

> **`flyer` is NOT a kind.** Every flyer already appears in the Flyers view by
> its content type. The kind says what it is *about*, which is why a health-fair
> flyer shows in both Events and Flyers.

**`image`** — the exact filename in
`backend/src/main/resources/static/images/seasonal/`, including spaces and case.
Encoding is handled server-side. Omit the field entirely if there is no poster.

### The rest

| field | notes |
| --- | --- |
| `id` | `FL-###`, next unused number |
| `category_tags` | One or more taxonomy **categories** (see vocabularies below) |
| `subcategory` | One subcategory belonging to that category |
| `tags` | Free descriptive tags **plus** the one notice kind. Tags never drive navigation |
| `organization` | Display name — normally matches the registry `name` |
| `event_date` | `YYYY-MM-DD`. Drives sort order. Omit if the flyer has no date |
| `location` | Omit the whole object if there is no physical location |
| `verified` | `true` once you have confirmed the details are current |

---

## Step 2b — An announcement or news item

`app/data/news.json` → the `records` array. **News uses different field names
than flyers** — flat `source_id`, not a nested `contentSource`.

```json
{
  "id": "NP-011",
  "type": "program-change",
  "headline": "City of Wilmington launches Rent Escrow Program for tenants",
  "summary": "One sentence. What changed and who it affects.",
  "body": "Full text. Blank lines separate paragraphs.\n\nA second paragraph.",
  "why_it_matters": "One sentence on why a resident should care.",
  "published": "2026-08-21",
  "expires": "2026-12-31",
  "geography": "wilmington",
  "source_id": "city-of-wilmington",
  "source_url": "https://www.wilmingtonde.gov/...",
  "category_tags": ["Housing"],
  "resource_tags": ["housing", "tenant rights", "announcement"],
  "urgency": "standard",
  "author": "manual",
  "verified": true,
  "active": true
}
```

All seventeen fields are **required** — the validator rejects the record if any
is missing.

### Controlled vocabularies — these exact strings only

| field | allowed values |
| --- | --- |
| `type` | `policy-update` · `program-change` · `deadline` · `new-resource` · `general-news` |
| `geography` | `wilmington` · `delaware` · `both` |
| `urgency` | `emergency` · `time-limited` · `standard` |
| `author` | `manual` (hand-written) · `rss` · `api` |

`resource_tags` carries the notice kind, same rule as a flyer's `tags`: **exactly
one** of `event` / `meeting` / `announcement` for community-produced items.

`expires` is not decoration — a waiting-list notice or a deadline should expire.

---

## Step 2c — A First Step Original

There are two shapes here, and only one is publishable today.

**A question-shaped FAQ** works now. Author it in `app/data/faq.json` with:

```json
"contentSource": { "id": "first-step", "type": "manual" }
```

The `first-step` sector is what makes it an Original — same mechanism as every
other producer.

**A substantive editorial article does not have a home yet**, and should NOT be
compressed into FAQ shape to fit one. First Step has never displayed the full
text of anything: `body` is authored and validated, then dropped at the DTO
boundary, and every existing surface either links out to the producer or shows a
summary. An article is the first content First Step *hosts* rather than *points
at*, so it needs a reading surface that does not exist yet.

**Write the article anyway and hold it.** Preserving the content in its honest
form and deferring publication is the intended handling — see Decision 047 and
the Slice K proposal.

---

## Step 3 — Taxonomy vocabularies

`category_tags` must be one of these ten **categories**, spelled exactly.
`subcategory` must belong to the category you chose.

| Category | Subcategories |
| --- | --- |
| **Housing** | Emergency Shelter · Eviction Prevention · Homeownership · Public Housing · Rental Assistance · Senior Housing · Sober Living · Transitional Housing · Youth Housing |
| **Food** | Farmers Market · Food Pantry · Home-Delivered Meals · Prepared Meals |
| **Clothing** | Clothing Closet · Thrift Store · Vouchers |
| **Health** | Counseling & Therapy · Crisis Services · Medical Care · Substance Use Treatment · Trauma & Grief Support |
| **Employment** | Job Search Assistance · Vocational Training · Youth Employment |
| **Utilities** | *(none yet)* |
| **Legal** | Disability Advocacy · Eviction Prevention |
| **Community Events** | Arts & Music · Libraries · Parks & Outdoors · Senior Activities · Sports & Fitness · Youth Programs |
| **Furniture & Household** | Appliances · Furniture & Household Goods · Thrift Store · Vouchers |
| **Community Support** | Child Care & Early Learning · Community Celebrations · Education & Training · Financial Assistance · Information & Referral · Life Skills · Mentoring · Parenting Support · School Supplies · Support Groups · Transportation · Volunteer Opportunities |

If your content does not fit, **say so rather than forcing it** — a missing
subcategory is a taxonomy decision, not an authoring problem.

---

## Step 4 — Check your work

From the repository root:

```sh
for s in data-cleaning/scripts/validate_*.py; do python3 "$s" || echo "FAILED: $s"; done
```

All five must exit 0. The messages name the record and the rule, so read them
rather than guessing.

Then see it for real:

```sh
export PATH=/Applications/Docker.app/Contents/Resources/bin:$PATH
docker compose up --build -d app
```

- Community flyers → `http://localhost:8080/app-next/community-notices/flyers`
- Government flyers → `http://localhost:8080/app-next/updates`
- Announcements → `/community-notices/announcements` or `/updates`

**If a record validates but does not appear, the producer's sector is the first
thing to check** — that is the one failure the validators allow through by design.

---

## Common mistakes

| symptom | cause |
| --- | --- |
| Flyer missing from all four notice views | No notice kind, or two of them, in `tags` |
| Flyer missing from Community Notices but fine elsewhere | Producer is registered as `government` — it is in Latest Updates |
| Flyer missing everywhere sector-scoped | `contentSource.id` is not in the registry |
| Broken image | Filename does not match the file exactly, including spaces and case |
| Validator rejects `category_tags` | Not one of the ten categories, or wrong capitalization |
| Validator rejects `geography` or `author` | Free text where a controlled vocabulary is required |
