# Decision 003

Housing Help returns many resource subtypes.

Observed during prototype testing.

Examples:
- Emergency shelter
- Transitional housing
- Rental assistance
- Domestic violence shelter

Conclusion:
A filter stage is necessary before showing results.

# Decision 004

Two filesystem reads used relative paths hardcoded to the repo root:
- ResourceService loaded `app/data/resources.json`
- ResourceController listed `backend/src/main/resources/static/images/seasonal`

This tied the app to being launched from the repo root and blocked deployment
from any other working directory (e.g. a container).

Conclusion:
Made both paths configurable via `@Value` properties, keeping the original
relative paths as defaults so existing behavior is unchanged:
- `app.data.dir` (default `app/data`)
- `app.seasonal.images.dir` (default `backend/src/main/resources/static/images/seasonal`)

A different working directory now overrides these via env/properties without
code changes. Chose `@Value` over `@ConfigurationProperties` to match the
existing style (OllamaService already injects `ollama.api.url` via `@Value`).

# Decision 005

Introducing the shared domain kernel (`shared/model/`) as part of the v2
vertical-slice migration.

`CivicContent` field list: `id, communityId, title, summary, verified, tags,
contentSource, createdDate, updatedDate` — taken directly from the project's
own domain-model UML (`docs/architecture/uml/domain-model-uml.md`), which
resolved the previously-open "KnowledgeObject: inheritance, composition, or
interfaces?" question in `01-domain-model.md` by drawing `CivicContent` as a
committed `<<abstract>>` class. `communityId` isn't shown as a boxed field in
the UML (Community is drawn as a relationship arrow instead), but it's kept
as a plain `String` field on the Java class — the diagram shows the
relationship, the code still needs a concrete FK under JSON-file storage.

`Contact` (new composite: phones, websites, email) does not replace
`Resource.phones`/`Resource.websites` in this pass. Those lists work fine
today; `Contact` ships in the shared kernel for Expert/Flyer to adopt once
built, per "don't refactor things that aren't broken."

# Decision 006

Bumped Spring Boot from 3.1.6 to 3.5.16 to add Spring AI (which requires
Spring Boot 3.3+). As of this decision, 3.5.16 is actually the final release
of the 3.5.x line (OSS end-of-life 2026-06-30) — Spring Boot 4.x is the
actively-patched line, paired with Spring AI 2.0 (which requires Boot 4).
Chose 3.5.16 anyway: it's a 4-minor-version jump from 3.1.6 with no
Jakarta/Boot-4 breaking changes to absorb, versus a much larger jump to Boot
4 (Spring Framework 7, changed auto-configuration/actuator behavior) that
this pass has not scoped time to audit. Spring AI 1.1.x still receives its
own security/dependency patches independent of Boot 3.5's own OSS EOL.

Added Spring AI's provider-agnostic `spring-ai-client-chat` dependency
(via the `spring-ai-bom` at 1.1.8) without any model-provider starter (no
Ollama, OpenAI, etc.) — no AI provider is available or subscribed to as of
this decision; the provider choice is explicitly deferred. `SpringAiAssistant`
(see `references/SpringAiAssistant_annotated.java`) takes an
`ObjectProvider<ChatClient.Builder>` so the application still boots cleanly
with zero providers configured; it throws `AiProviderNotConfiguredException`
only if `AiAssistant.generate()` is actually called, which
`DecisionAgentService`'s existing broad catch already handles by falling
back to a canned response — the same observable behavior v1 had when Ollama
was unreachable.

`docker-compose.yml`'s `ollama` sidecar container is left in place even
though the `app` service no longer reads `OLLAMA_API_URL` — removing the
sidecar is a separate infrastructure decision, not made here. Also removed
`app`'s `depends_on: [ollama]` — app startup was never actually blocked by
it (Compose's plain `depends_on` only orders container start, it doesn't
wait for health), but keeping it implied a dependency that doesn't exist;
removing it makes the compose file honestly reflect that `ollama` is a fully
optional, currently-unused sidecar.

Verified the whole stack end-to-end via `docker compose build && docker
compose up` once Docker Desktop was actually available in this environment
(it had to be installed from an existing but never-completed
`~/Downloads/Docker.dmg` — the CLI symlinks at `/usr/local/bin/docker*` were
pointing at a long-unmounted dmg volume from an earlier abandoned install
attempt). Findings from the real run:
- The app container starts and serves traffic within ~1 second — confirms
  it does not block on Ollama being present or healthy.
- `/api/decide` returns HTTP 200 with `ApiResponse.success` wrapping a
  fallback `DecisionResponse` whose `notes` field surfaces the real
  `AiProviderNotConfiguredException` message — the exact graceful-degradation
  path this pass was designed to produce.
- `/api/resources` (58 records) and `/api/news/rss` (349 live entries from
  the real Delaware legislature feed) both work correctly through the
  container. This means a separately-reported claim that "the RSS feed
  doesn't work in Docker" does not reproduce for the actual RSS endpoint.

# Decision 007

What's actually broken in the container (confirmed via the verified run
above, not assumed) is `/api/news` — the **static** news endpoint backed by
`app/data/news.json` — not the RSS feed. Container logs show: `Failed to
load news.json: app/data/news.json (No such file or directory)`. This is
the pre-existing inconsistency already noted during the News slice planning:
unlike `ResourceService` (which resolves its data path via the
`app.data.dir` property, overridden by the Dockerfile's `APP_DATA_DIR=/data`
env var), `NewsService` reads a hardcoded relative path
(`Path.of("app","data","news.json")`) that only resolves correctly when the
process's working directory is the repo root — true when running via `mvn
spring-boot:run` locally, false inside the container (`WORKDIR /app`, data
baked at `/data`).

Not fixed here — it predates this migration and isn't caused by it; fixing
it is folded into the News slice migration (Step 5), where `NewsService`'s
data-loading logic is being touched anyway. Recorded here so a future reader
doesn't mistake "the RSS feed is broken" (the original, imprecise report) for
what's actually true ("the static news list is broken; the live RSS feed
works fine").

# Decision 008

A real browser walkthrough of the deployed container (after Steps 1-5)
surfaced regressions this plan's curl-only verification had missed:
`app.js` still referenced `NewsItem`'s pre-migration flat field names
(`item.headline`, `item.sourceName`/`item.sourceUrl`/`item.source_name`/
`item.source_url`, `item.categoryTags`/`item.category_tags`) in every news
rendering function — `renderLawsColumn`, `renderNewsFilter`,
`renderNewsItems`, `loadSidebarNews`, `loadSidebarLaws`, and
`showNewsDetail`. These fields were renamed onto `CivicContent`
(`title`, `contentSource.name`/`.url`, `tags`) in Step 5, but Step 5's own
scope was backend-only per the plan — nobody went back through `app.js`'s
news-rendering code specifically for the *field renames* (Step 2's `app.js`
pass only handled the `ApiResponse` envelope unwrapping, which predated
Step 5's `NewsItem` field renames entirely). Symptom: "Latest Updates" and
"Delaware's Newest Laws" on the home page, the full "Weekly Updates" page,
and the single-story detail view all showed the literal text "undefined"
for titles and/or sources.

Fixed all six functions in `app.js` to read `item.title`,
`item.contentSource?.name`, `item.contentSource?.url`, and `item.tags`.

While investigating, also found: `renderLawsColumn`/`loadSidebarLaws`
displayed `item.why_it_matters || item.headline` — since `why_it_matters`
is *always* truthy (it's either the extracted "Relating to X" sentence or a
generic/keyword-based fallback sentence from `RssFeedService.classifyLegislation`),
the `|| item.headline` fallback never mattered, and the generic stock
message ("Stay informed about new laws signed by the Governor of
Delaware.") was being shown as if it were a headline whenever RELATING TO
extraction failed (confirmed against live feed data: 19 of 349 items).
Changed both to display `item.title` directly — it already holds the
extracted "Relating to X" sentence when available, or a sensible raw
fallback (the bill number, e.g. "HB 500") otherwise — never the generic
sentence.

Also fixed a real content-quality bug in `RssFeedService.extractRelatingTo`
(confirmed against live data, e.g. "Relating to delaware banks and trust
companies."): the old capitalization only capitalized the string's very
first letter and lowercased everything else, breaking embedded proper
nouns. Replaced with real title-case conversion (`toTitleCase`) plus a
`Delaware`-specific regex safety net, since the feed's content guarantees
that word appears constantly. See `references/RssFeedService_annotated.java`
for the full before/after.

**Explicitly deferred to after the full reconfiguration plan (Steps 6-8) is
complete**, per direct instruction: the resource-size ARIA filter buttons
don't work and should be *removed* (no fix has worked); there's no detail
view for Free/Low-Cost Essentials records. Neither is pre-existing-migration
fallout — both are separate, longer-standing product gaps.

# Decision 009

Removed the resource-size ARIA filter buttons (`#increase-text-button` "A+",
`#decrease-text-button` "A−") from `index.html`, along with their click
handlers and the `localStorage` font-size restore call in `app.js` — per
direct instruction, since no fix had gotten the feature to work and the
user chose removal over further debugging. Verified in a live browser: both
elements are gone from the DOM, no console errors, `.utility-button` CSS
class (still used by the remaining ES/contrast buttons) untouched.

# Decision 010

Extended `RssFeedService`'s title extraction to cover bills/resolutions with
no "RELATING TO" clause (previously falling back to the bare bill number,
e.g. "HB 500", "SJR 22"). Investigated by pulling all 19 real live-feed items
that were showing the generic classification fallback and confirming every
one contains a "This Bill/Act/Resolution/Joint Resolution …" self-description
— see `references/RssFeedService_annotated.java` for the full rule and code.

Two rules, confirmed with the user against real examples:
- **Bills/Acts** ("This Bill …"/"This Act …"): replace the lead-in with "The
  bill" and keep the sentence verbatim through the next period — this text
  is already normally-cased English in the source feed, not shouted caps, so
  no title-casing is applied (would incorrectly capitalize ordinary words).
- **Resolutions** ("This Resolution …"/"This Joint Resolution …"): the
  formal long title precedes this sentence in ALL CAPS (like the RELATING TO
  case) — extract it, title-case it, and prefix with "Senate Joint
  Resolution: " or "House Joint Resolution: " based on the bill number's own
  SJR/HJR prefix (HJR confirmed to get the same treatment as SJR, extending
  the user's explicit SJR instruction by direct parallel).

`why_it_matters` is synced to the new title in both cases, matching the
existing "Relating to X" behavior (confirmed with the user rather than
assumed).

**Deferred, confirmed with the user, not built in this pass**: an
AI-generated "Purpose" section synthesizing a bill's full text once an AI
provider is configured (none is, per Decision 006). No scaffolding added —
would be speculative without a provider to populate it.

**Follow-up bug found during live verification** (same session, deployed
container, real feed data): the first implementation matched "before the
matched This-phrase" too naively and produced two classes of bad titles:
1. `"This House Joint Resolution directs…"` — an infix word ("House"/
   "Senate") between "This" and "Joint Resolution" that the regex didn't
   handle, so it skipped past the real phrase to a coincidental SECOND
   mention later in the text (e.g. "This Joint Resolution also requires…"),
   sweeping the entire first paragraph into the title.
2. A resolution with several sentences of narrative background between its
   ALL-CAPS heading and its (correctly-matched, no infix) self-description —
   "before the matched phrase" swept all of that background in too.

Fixed both: the regex now optionally matches a "House "/"Senate " infix, and
the formal-title boundary is whichever comes first — the summary's own first
period, or the matched phrase's start (mirroring `extractRelatingTo`'s
existing "earliest terminator wins" design). Re-verified against the full
live 349-item feed: 0 items showing the generic fallback, and the remaining
long titles are all genuinely long single sentences from real Delaware
legislative text — not extraction artifacts. Two regression tests added
using the real bill text that exposed each bug.

# Decision 011

Built the Flyer vertical slice's backend (`flyer/{model,repository,service,
controller}`), per direct instruction: "static flyers.json + static images,
no OCR/AI, no real pipeline/ package wiring — just mirroring
ResourceService." First backlog item from the roadmap the user laid out
(Flyers → Search → Community multi-tenancy → Expert stubs → React frontend →
Mobile → Persistence) — chosen first because it's small, fully self-scoped,
and mirrors a pattern already proven three times (Resource/News/AI), giving
later work (layout redesign, React frontend) real data to build against
instead of placeholders.

Two deliberate deviations from the user's original class sketch (`private
String organization; private LocalDate eventDate; private Location
location; private String image;`):
- Public fields, not private+getters — matches every other domain class in
  this codebase (zero exceptions).
- `eventDate` is `String`, not `LocalDate` — every other date-shaped field
  in the codebase (`NewsItem.published/.expires`, `ContentSource.retrieved`,
  `CivicContent.createdDate/.updatedDate`) is a plain String, and the
  repositories' hand-built `ObjectMapper` instances don't have
  `JavaTimeModule` registered (Spring's autoconfigured one does, but these
  repositories deliberately don't use it, matching Resource/News). Using
  `LocalDate` would have been the first `java.time` usage in the domain
  model and required a new dependency/registration for one field.

`JsonFlyerRepository` mirrors `JsonResourceRepository`'s file-discovery
mechanism (external file at `app.data.dir`, classpath fallback, multi-shape
JSON support) but deliberately has NO field-mapping adapter — Resource/News
needed one to bridge v1's legacy flat JSON shape onto the new CivicContent
shape; Flyer has no legacy shape to bridge (it's brand new), so
`app/data/flyers.json` was authored to already match the Java class
directly. Only `communityId` defaulting was kept, since none of today's
data specifies one.

`app/data/flyers.json` has 7 records, one per the real flyer image already
at `backend/src/main/resources/static/images/seasonal/`. **The metadata
(organization, event date, location, summary) is manually authored for this
pass, not extracted from the images** — confirmed earlier (see
`references/Media_annotated.java`) that those images carry only standard
EXIF, no descriptive content. Flagging this explicitly so it isn't mistaken
for real extracted data later, when OCR/AI extraction is eventually built.

Not done, explicitly out of scope for this pass: wiring Flyer into
`DecisionAgentService`'s AI retrieval; rewiring the existing seasonal-images
carousel (`app.js`) to consume the new `/api/flyers` endpoint instead of the
old `/api/seasonal-images`; routing loading through the `pipeline/` package's
Collector/Normalizer interfaces (conceptually a natural fit, but that
package stays scaffolding-only until a case actually needs it, per Step 7).

# Decision 012

Built the Search vertical slice: `GET /api/search?q=...&communityId=...`,
searching across Resource/NewsItem/Flyer in one community-aware, ranked
list. Second item on the confirmed backlog (Community Flyers → **Search** →
Community multi-tenancy → Expert stubs → React frontend → Mobile →
Persistence). Unlike Flyer, this wasn't a copy of an established
single-slice pattern — it's the first genuinely new cross-cutting feature
since the v2 migration, so three real design forks were surfaced and
confirmed with the user before implementation, rather than picked silently:

**1. Result shape: unified ranked list, not grouped-by-type.**
`SearchResult{type, score, content}` in one list sorted by score, mixing
all three types — considered and rejected the alternative
(`{resources:[...], news:[...], flyers:[...]}`) because the entire point of
cross-type search is a real ranking (a highly-relevant Flyer should be able
to outrank a weakly-relevant Resource); grouping by type would just push
the interleaving work onto every future client instead of doing it once.
`content` is typed as the abstract `CivicContent`, not `Object` — Jackson
serializes a field's runtime type by default in this codebase (no
`MapperFeature.USE_STATIC_TYPING` is set anywhere), so every subtype's
extra fields (`Resource.organization`, `Flyer.image`, etc.) still serialize
correctly with no `@JsonTypeInfo` needed.

**2. Scoring logic: extracted to `shared/util/TextScore.java`, and
`DecisionAgentService` was refactored to use it too.** `DecisionAgentService`
already had a private `scoreMatch`/`safeLower` substring-scoring helper for
its own AI-prompt retrieval. The alternative — giving `SearchService` its
own independent copy, leaving `DecisionAgentService` completely untouched —
was presented as the lower-risk option (zero chance of destabilizing
tested, working code) but the user explicitly chose extraction instead, to
avoid long-term drift between two copies of the same logic. This is the
one place this pass touched pre-existing working code; the move was
byte-for-byte behavior-equivalent (same flat-5-points-per-field,
substring-containment, first-match-wins-for-lists semantics), and
`DecisionAgentServiceTest`'s full existing suite was re-run and confirmed
unchanged afterward. See `references/TextScore_annotated.java` and the
updated `references/DecisionAgentService_annotated.java`.

**3. Backend only this pass — no `app.js` wiring.** Matches how the Flyer
slice was done (endpoint + tests first, frontend later); a real search UI
is better built once in the upcoming React frontend (backlog item #5) than
built twice.

**Community-aware filtering is genuinely new to this codebase.** Every
repository (`JsonResourceRepository`, `JsonNewsRepository`,
`JsonFlyerRepository`) stamps the same default `communityId`
(`wilmington-de`) onto every record at load time, but until this slice,
nothing ever read it back — `communityId` was write-only metadata. Search
is the first place it's actually used as a filter, establishing the
community-aware plumbing ahead of the later multi-tenancy backlog item —
though it's inert today (single community, so every record always
matches). A missing `communityId` on a search request falls back to
`app.default-community-id` (same default used everywhere else), not "no
filter" — a search is always scoped to some community context by default.

**Incidental bug found and fixed via test failure, not anticipated in
advance:** `SearchController`'s `q` is this app's first required
`@RequestParam` (every other endpoint's parameters are path variables).
Spring's normal 400 for a missing required param was being swallowed by
`GlobalExceptionHandler`'s catch-all `Exception → 500` handler, since no
more specific handler existed for
`MissingServletRequestParameterException`. Added a dedicated handler for
it (400, `MISSING_PARAMETER`) — additive only, doesn't change the
catch-all's behavior for anything else. See
`references/GlobalExceptionHandler_annotated.java`.

Not done, explicitly out of scope for this pass: `app.js` search UI (see
above); result pagination (`PageResponse<T>` stays unwired, matching every
other endpoint — current dataset sizes don't need it); any fuzzy/TF-IDF
relevance scoring beyond `TextScore`'s existing flat substring-match
convention; search-by-category or other structured filters beyond `q`/
`communityId`.

# Decision 013

Community multi-tenancy, third item on the confirmed backlog. First pass
concluded (wrongly) that no multi-tenancy data existed, because it checked
`countyServed`/`county`, which is `"NCC"` for literally every record in
both `Service_Directory_cleaned.json` (603 records) and `resources.json`
(58 records) — zero variance. **User correction: Community means
incorporated city/town (Wilmington, Middletown) or unincorporated area
(Claymont, Belvedere), not county.** Re-pulling the distribution at
city/town level (parsed from `fullAddress`) showed real, substantial
variance the county-level check had completely hidden:

- Full DSCYF directory (603 records): Wilmington 364, **Newark 79, New
  Castle 45, Middletown 33, Bear 14, Claymont 8, Hockessin 7**, Greenville 4,
  Newport 4, Townsend 2, St. Georges 2, Smyrna 2, plus singletons and 33
  unparseable/missing addresses.
- Curated `resources.json` (58 records, what the app actually serves
  today): Wilmington 54, Newport 2, New Castle 2, Middletown 2, Bear 1 —
  almost entirely Wilmington, confirming this file really is the thin,
  Wilmington-focused curation `references/firststep_resource_data_lineage`
  (session memory) already flagged it as.

Three scope questions were confirmed with the user before building anything:

**1. Field uniformity: structural mapping only, narrowed to the 6
high-volume towns** (Newark, New Castle, Middletown, Bear, Claymont,
Hockessin — the bolded list above), not all 603 raw records and not full
manual curation of eligibility/cost/urgency/tags/verified (a separate,
larger project the raw DSCYF export simply doesn't have the source data
for). Executed as a one-time Python transform (not committed — see below)
producing `app/data/resources.communities.json`: 171 final records after
skipping 15 exact-organization-name duplicates of existing `resources.json`
entries (case-insensitive match — bounded, deterministic, not fuzzy).
Per-town: Newark 79, New Castle 39, Middletown 29, Bear 9, Claymont 8,
Hockessin 7. Field mapping — `organizationName`→`organization`,
`servicesDescription`→both `summary` and `description` (mirrors
`resources.json`'s own convention of duplicating that text),
`typeOfService`→`category` (Title-Cased, cosmetic only, not a real
taxonomy), `populationServed`→`population`, `fullAddress`→parsed
`locations[0]` (address/city/state/zip), `phone`/`website`→`phones[0]`/
`websites[0]` when non-blank, `countyServed`→`county`. Deliberately left
null (no source data, not invented): `subcategory`, `parentOrganization`,
`eligibility*`, `cost`, `urgency`, `notes`, `accessMode`, `tags`.
`verified: false` for all (honestly true — none of this is
human-reviewed). `source: "Delaware DSCYF Service Directory (raw,
structurally mapped)"` — deliberately distinct from the curated set's
source string so the two are never confused for the same provenance.
`retrieved: null` — no real per-record timestamp exists in the raw file;
not fabricated. Synthetic sequential ids `SD-001`..`SD-171`.

**The generation script itself is not committed** — same precedent as
`Service_Directory_cleaned.json`/`resources.json`, neither of which has a
committed regeneration script either. Only the output JSON is in the repo;
the mapping rules above are recorded here precisely enough to reproduce or
re-run the transform later if the source directory updates.

**Explicitly deferred, not silently dropped:** Greenville, Newport,
Townsend, St. Georges, Smyrna, and the singleton towns (all ≤4 records) —
real but low-volume, can be added the same way later. Full curation of
eligibility/cost/urgency/tags/verified for the 6 included towns also stays
deferred.

**2. Fixed a real, pre-existing bug:** `JsonResourceRepository`'s
`applyContentSourceAndDefaults` only ever guarded `if (resource.communityId
== null)` before stamping `app.default-community-id` — a guard that was
always true, since no source JSON has ever set `communityId` itself. This
silently mislabeled every non-Wilmington resource as `"wilmington-de"`,
including 5 of the original 58 curated records (2 New Castle, 2 Middletown,
1 Bear). Fixed by deriving `communityId` from `resource.locations[0].city`
via the new `shared/util/CommunitySlug.forCity(...)` (a general slugifier,
not a hardcoded town list — `"Wilmington"` → `"wilmington-de"`, matching
today's default exactly, so no existing Wilmington record's value changes),
falling back to `defaultCommunityId` only when no location/city exists at
all. This is the change that makes `/api/search`'s `communityId` filter
actually do something — previously every record shared one community
value, so filtering by it was a no-op regardless of what data existed.
`JsonResourceRepository.init()` was also extended to load
`resources.communities.json` alongside `resources.json` (extracted the
existing external-then-classpath-fallback body into a shared `loadFile`
helper, called twice — a direct refactor required to support two files,
not speculative).

**3. No new Community CRUD API.** `Community.java` stays the inert struct
it's been since Milestone 1 — no `CommunityRepository`/`Service`/
`Controller`, no `communities.json` listing file, no `GET /api/communities`
discovery endpoint. Just accurate `communityId` values flowing through so
`/api/search?communityId=newark-de` (etc.) is now meaningful. A real
Community API is a separate, later decision if/when a client actually
needs to discover available communities rather than already knowing one.

# Decision 014

Category taxonomy + `GET /api/categories` — Step 1 of the 8-step homepage
redesign roadmap (see `docs/architecture/03-application-architecture.md`'s
Milestone Roadmap; the full 8-step sequence is recorded there). The
redesign brief (`references/CSSforNewDesign.md`) calls for a persistent
sidebar organized around 7 categories (Housing, Food, Clothing, Health,
Employment, Utilities, Legal), each showing a resource count and "latest"
items, plus a category-to-policy-update link.

**Same shape of lesson as Decision 013's county-vs-community finding,
repeated at the category level**: `Resource.category` is uncontrolled free
text, and the two live data files use two disjoint vocabularies with zero
overlap (`resources.json`: 3 category strings; `resources.communities.json`:
~21 strings). Mapping everything onto the requested 7 categories left ~117
of 229 resources (about half) unmapped — "Recreational" alone is 53
records, the single largest category in the entire directory, second only
to "Housing Assistance." A field that looks like it should carry real
structure needed direct investigation before any sidebar could be built on
top of it — checking real data distribution before building UI on an
assumed taxonomy is now a repeated, confirmed lesson in this project.

**Resolution, by direct instruction — 10 categories, 100% resource
coverage:**

| key | label | matches `Resource.category` | matches News tags | includes |
|---|---|---|---|---|
| `housing` | Housing | Housing Assistance, Housing | housing | |
| `food` | Food | Food Program | food | |
| `clothing` | Clothing | Clothing & Incidentals | — | |
| `health` | Health | Healthcare/Medical, Mental Health, Substance Use | healthcare | |
| `employment` | Employment | Employment | employment | |
| `utilities` | Utilities | — | utilities | 0 resources today (accepted) |
| `legal` | Legal | Advocacy | legal | |
| `community-events` | Community Events | Recreational | — | all `Flyer` records |
| `furniture-household` | Furniture & Household | Furniture & Household Items | — | |
| `community-support` | Community Support | Resource Information, Education/Training, Parenting Education, Financial Support, Support Group, Early Childhood/Pre-K, Volunteer, Mentor, Life Skills, Transportation, Child Care, Before/After School Care, Entertainment | — | catch-all; frontend uses `tags`/`subcategory` for finer filtering within it |

Two categories added beyond the original 7: **Community Events** absorbs
the 53-record "Recreational" bucket and — deliberately, by direct
instruction — is also the first time `Flyer` content joins the category
taxonomy at all (Flyers were previously only reachable via `/api/flyers`
or `/api/search`, never through category browsing). **Furniture &
Household** got its own category rather than being folded into the
catch-all, since "Furniture & Household Items" was already a single, clean
category string (6 records) with no reason to blend it into a bucket it
didn't need. **Community Support** is the deliberate catch-all for the
remaining 13 leftover category strings (~64 records).

**Design**: `category/model/CategoryDefinition` is a static registry (not
a DB table — 10 entries, rarely changes), same pattern as
`RssFeedService`'s existing `TAG_KEYWORDS` constant map.
`category/service/CategoryService` composes `ResourceService`/
`NewsService`/`FlyerService` (same discipline as `SearchService`) and
returns `category/dto/CategorySummary`, whose `latestItems` field reuses
`search/dto/SearchResult` rather than inventing a duplicate
polymorphic-list wrapper. Policy-update matching uses
`NewsItem.resourceTags` (lowercase, machine-matching field) rather than
`.tags` (Capitalized, display field) — both are produced from the same
matched-bucket list in `RssFeedService.classifyLegislation()`, differing
only in casing.

**UI guideline logged for later frontend steps, not a backend change**:
`Resource.updatedDate` is set from the JSON load/retrieved date, not real
edit-history tracking. `CategoryService` sorts by it internally for
"latest items" (a reasonable recency proxy), but per direct instruction
**the frontend must not display it to users as "last updated"** — that
would imply a freshness guarantee the data doesn't have. Applies when
Step 6/8 of the redesign roadmap builds the actual resource cards.

**"Important Updates," not "Trending Now"** — by direct instruction, this
is the name to use for the redesign's recency/urgency civic-content
section (Step 5 of the roadmap), correcting the working name used in the
initial critique of the redesign brief.

**Explicitly logged as a separate, deferred idea, not part of this
roadmap**: real Exiftool/AI-based metadata extraction for `Flyer` records,
suggested as a way to eventually auto-populate Community Events content
from flyer images. Ties back to Decision 011's original OCR/AI deferral —
still a good idea, still not built.

# Decision 015

Expert stubs — Step 2 of the homepage redesign roadmap. Per direct
instruction, Expert content "feeds `CivicContent` and is the basis for FAQ
answers," so it was built immediately after the category taxonomy, not
deferred behind the frontend steps.

Investigation before designing anything confirmed **nothing about this
feature was defined anywhere beyond the name** — `expert/package-info.java`
was scaffolding-only, and `docs/architecture/01-domain-model.md` listed
`FAQ` and `ExpertAnswer` as two separate, still-`> TODO: define.`
`CivicContent` subtypes, positioned right after `Flyer`. No field-list
sketch existed in any doc, decision, or reference file. Three things were
confirmed with the user before building:

**1. Build both `FAQ` and `ExpertAnswer`, not one.** The domain model doc
already committed to two separate subtypes; this pass defines both rather
than picking one and leaving the other's TODO unresolved.

**2. `ExpertAnswer` fields**: `question, answer, expertName,
expertCredentials, expertOrganization, expertContact (shared.model.Contact),
sessionDate`. `expertContact` reuses the existing `Contact` composite
(phones/websites/email) rather than flat fields — the shared-kernel docs
(`references/Contact_annotated.java`) explicitly anticipated Expert as
Contact's first real adopter since the original migration; this is that
adoption completing, not a new design decision. `FAQ` was left deliberately
simpler by design (see below) — no individual expert attribution.

**`FAQ` fields (proposed, not separately specified by the user, confirmed
at plan review)**: `question, answer, sourceExpertAnswerId` (nullable,
references an `ExpertAnswer.id`). `sourceExpertAnswerId` directly reflects
the stated "ExpertAnswer is the basis for FAQ answers" relationship as a
plain id reference — not a resolved/embedded object (nothing auto-resolves
it in this pass; a client wanting the full `ExpertAnswer` calls
`/api/expert-answers/{id}` itself), and not a JPA relationship (there's no
database, everything is JSON-file-backed).

**Both models use inherited `tags`, not a new `category`/topic field**:
Decision 014 just spent real effort discovering that `Resource.category`
was uncontrolled free text needing a whole mapping layer to become useful.
Introducing a second free-text category field on `ExpertAnswer`/`FAQ`
would repeat that exact problem from scratch. `tags` is already the
established free-form topic-labeling field on `CivicContent`.

**3. Standalone slice only — no `Search`/`Category` wiring.** Matches the
"backend slice first" precedent from Flyer (Decision 011) and Search
(Decision 012). `ExpertAnswer`/`FAQ` are not yet searchable via
`/api/search` and don't appear in `/api/categories` counts — becomes its
own small follow-up once real content volume exists.

**Implementation**: both models are direct, confirmed file-for-file
mirrors of the `Flyer` slice's shape (public fields, no field-mapping
adapter since data is authored directly in the target shape including a
full `contentSource` object per record, `communityId`-defaults-only-if-null,
external-file-then-classpath-fallback loading) — `expert/model/
{ExpertAnswer,FAQ}`, `expert/repository/{ExpertAnswerRepository,
JsonExpertAnswerRepository,FaqRepository,JsonFaqRepository}`,
`expert/service/{ExpertAnswerService,FaqService}`,
`expert/controller/{ExpertAnswerController,FaqController}`. Data:
`app/data/expert-answers.json` (6 hand-authored Delaware civic-info Q&A
entries — housing/tenant rights, SNAP benefits, employment rights,
Medicaid, eviction notice periods, LIHEAP heating assistance) and
`app/data/faq.json` (6 entries, 3 carrying a real `sourceExpertAnswerId`
back to a matching `expert-answers.json` entry to demonstrate the link,
3 standalone). `GET /api/expert-answers`, `/api/expert-answers/{id}`,
`GET /api/faqs`, `/api/faqs/{id}` — same `ApiResponse<T>`/
`NotFoundException` conventions as every other endpoint.

Not done, explicitly out of scope for this pass: `Search`/`Category`
wiring (see above); resolving/validating `sourceExpertAnswerId` at load
time; any real "monthly session" intake workflow (this is static,
hand-authored stub content, matching how Flyer's data was hand-authored,
not a live ingestion pipeline).

# Decision 016

Step 3 of the homepage redesign roadmap — React frontend project
scaffold. Pure tooling/pipeline this pass, no real UI (Sidebar/Hero/
Important Updates are Steps 4-5).

**React + Vite + TypeScript, not Next.js.** Confirmed with the user: a
client-side SPA is the right fit since Spring Boot already is the API —
Next.js's SSR/file-routing/API-routes would add real config surface for
no benefit today (would only be justified if SEO for public resource
pages becomes an explicit future goal). Vite produces a pure static
`dist/` folder, which is the deciding factor for fitting the existing
deploy model with zero changes to it.

**Served at a new path, `/app-next/`, not replacing the root demo.** The
existing `index.html`/`app.js` (resource browsing, AI widget, news)
keeps serving unchanged at `/`. The new app has no real UI yet, so
serving it separately preserves "every milestone leaves First Step in a
working, demoable state" (the project's Definition of Done) rather than
taking the live demo offline mid-rebuild. **A later step (likely part of
Step 8, or its own small follow-up) must flip the routing** once the new
app is functionally equivalent-or-better — flagging this now so it isn't
forgotten; `/app-next/` is explicitly a temporary path, not a permanent
second app.

**Docker integration — new Stage 0, one new `COPY` line, everything else
unchanged**: `backend/Dockerfile` gained a `node:24-alpine` stage that
runs `npm ci && npm run build` against the new `frontend/` project, then
the existing Maven stage does
`COPY --from=frontend-build /frontend/dist src/main/resources/static/app-next`
right before `COPY backend/src src` → `mvn package` — the React build
output rides into the jar exactly like the hand-written static files
already do, via Maven's standard `src/main/resources` → classpath
bundling. No changes needed to the runtime stage or `docker-compose.yml`
— confirmed via investigation that neither treats `static/` as a distinct
serving concern (it was never singled out before this pass either). Node
version (`24`) matches what's already installed on the dev machine
([[firststep_build_toolchain]]).

**`frontend/api/client.ts`** is the one piece of real reusable
infrastructure this scaffold produces — a small typed wrapper unwrapping
the backend's `ApiResponse<T>` envelope (`success`/`data`/`errorMessage`),
directly mirroring `shared/dto/ApiResponse.java`. Every later step's
components need exactly this, so it's built now rather than being
premature abstraction.

**`App.tsx` is deliberately a bare proof, not a real page**: fetches
`GET /api/categories` and renders the raw list, unstyled. This is the
actual verification goal of a "scaffold" step — proving React build →
Docker → Spring Boot serving → real API call succeeds end-to-end, not
building anything Steps 4-8 will just replace.

**`react-router-dom` is installed but unused** — no real routes exist
yet, but every later step needs real client-side routing (category pages,
resource detail), so it's added now rather than as a churny mid-roadmap
dependency addition.

Not done, explicitly out of scope for this pass: any real Sidebar/Hero/
Important Updates/card UI (Steps 4-6); flipping `/app-next/` to be the
served-at-`/` app; CSS/styling of any kind (the placeholder page is
intentionally unstyled).

**Follow-up (same day, post-Step-3 design review):** confirmed the
React+Vite+TS choice remains sound given the project updates regularly
(flyers, policy updates, RSS) — the frontend fetches live via `fetch()`
with nothing baked into the JS bundle at build time, so content-update
frequency is fully decoupled from frontend build/deploy frequency (this
is precisely why Next.js SSG/ISR was rejected above). **By direct
instruction, live-update-without-manual-reload for new RSS
items/Flyers is now an explicit requirement of Step 5 (Important
Updates)**, not a deferred nice-to-have — client-side polling
(mirroring `RssFeedService`'s own hourly-poll design) is the recommended
mechanism over WebSockets/SSE, since no backend push infrastructure
exists and isn't justified by an hourly-or-slower update cadence. Final
polling interval/diffing mechanism to be settled in Step 5's own
dedicated design pass, not here.