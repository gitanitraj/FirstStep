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

# Decision 017

Step 4 of the homepage redesign roadmap — `AppLayout` + `Sidebar`. This is
the frontend's **first CSS** and its first real component structure; before
this pass `App.tsx` was the unstyled Step-3 proof (Decision 016). Three new
files (`frontend/src/index.css`, `components/AppLayout.tsx`,
`components/Sidebar.tsx`); `App.tsx` collapses to `return <AppLayout />`,
`main.tsx` imports the CSS.

**Palette:** `index.css` mirrors the warm-green/orange/cream vars from
`backend/src/main/resources/static/styles.css` (`--primary-color:#1a5c38`,
etc.). The layout classes (`.home-layout`, `.home-sidebar`,
`.category-checkbox`, …) are adapted from `references/CSSforNewDesign.md`
but **recolored** — that doc's blue/purple (`#0066cc`) was explicitly
rejected in favor of the existing civic identity (Decision 014's visual
direction).

**Sidebar owns its own fetch**, reusing `api/client.ts`'s `apiGet<T>`
(Decision 016's one piece of reusable infra) against `GET /api/categories`
and the existing `CategorySummary` type — no raw `fetch`, no new types.

**Two scope decisions confirmed with the user before building:**
1. **Checkboxes are local-toggle only** — a `useState<Set<string>>`
   toggles visibly, but drives nothing downstream. There is no content to
   filter yet (Steps 5–6) and the shared filter context is explicitly
   Step 7, so building the context now would be premature abstraction with
   no consumer.
2. **No router / no `SpaWebConfig` change this step** — `AppLayout` renders
   directly from `App.tsx`. Real client routes (result pages) belong to
   Step 6; `SpaWebConfig`'s catch-all widening (flagged in Decision 016)
   travels with them, not before. `react-router-dom` stays installed-but-
   unused.

**Deliberately NOT built this step, though the roadmap's original Step 4
line listed them** (flagged so it isn't mistaken for done): the
`CommunitySelector` (the `communityId` query param on `/api/categories` is
untouched — every community's categories show) and a toggle-driven **mobile
drawer** — the responsive `@media (max-width:768px)` block collapses the
sidebar to a horizontal wrap, but there is no open/close drawer control.
Both are reasonable follow-ups; neither was in scope for this pass.

**Verification:** `npm run build` (strict `tsc` + `vite build`) passes
clean — the real gate, since `noUnusedLocals` catches any import orphaned by
moving the category fetch out of `App.tsx`. `npm test` green (3 tests: the
retargeted `App.test.tsx` asserts the shell renders; new `Sidebar.test.tsx`
covers category rendering + checkbox toggle). **Live browser verification
DONE** — full Docker build (`docker compose up --build -d app`) served the
Step-4 build at `http://localhost:8080/app-next/`; headless-Chrome
screenshots at 1200px and 390px confirmed the sticky green header, all 10
live category counts, the warm-palette shell, and the mobile single-column
collapse (sidebar stacks above main — no drawer, as expected). NB: an
earlier claim in this session that live verification was "environment-
blocked (no JDK/Maven/Docker)" was WRONG — the toolchain was installed all
along, just off the default PATH; see [[firststep_build_toolchain]]'s
2026-07-19 note.

Main content area is a Step-5 placeholder.

# Decision 018

Step 5 of the homepage redesign — `MainContent` (Hero+AI, Important Updates,
CategoryPreviewList). **Split into 5a / 5b / 5c** at the user's direction
(their CLAUDE.md prefers small sequential prompts), each planned + built +
verified independently. **This decision covers 5a only** — the merged Hero +
AI guidance widget. Decisions banked with the user for the later slices:

- **5b Important Updates** — build a NEW backend `GET /api/updates` endpoint
  (controller+service+DTO+tests) that server-side merges News (`/api/news/rss`
  + `/api/news`) and Flyers sorted by date, so the client polls ONE URL.
  **Live-refresh = client polls every 5 min with change-diffing** (only
  setState when content actually changed). This will be the app's first
  `setInterval` + `useEffect` cleanup / `AbortController` pattern (none exists
  today). "Important Updates," never "Trending Now."
- **5c CategoryPreviewList** — consume `/api/categories` `latestItems`
  (`List<SearchResult>`, cap 3) + `latestPolicyUpdate` (a full `NewsItem`,
  not a summary DTO). Browse button inert until Step 6 routes exist. **CSS
  naming caution:** the reference doc's `.category-group-header` (flex row)
  collides with an existing `.category-group-header` (uppercase section
  heading) in `backend/styles.css` — use a distinct frontend class name.

**5a — what was built.** The old static demo had TWO separate blocks: a
text-only `.hero-section` and a lower `.ai-guidance-section` (question box +
chips). 5a MERGES them into one green→orange gradient hero
(`frontend/src/components/HeroGuidance.tsx`) carrying the AI flow inline:
a `<textarea>`, three toggle chips (🚨 Urgent → `urgent`; 🏠 Housing / 🛒
Essentials → `preferredCategories` Set), and a Get Help button (submit on
click or Enter). A thin `MainContent.tsx` composes the `.home-main` column
(HeroGuidance now; Important Updates 5b + previews 5c later) and replaced
Step 4's placeholder `<p>` in `AppLayout.tsx`.

**Reuse / new infra:** added `apiPost<TReq,TRes>` to `api/client.ts`
(mirrors `apiGet`'s envelope unwrap — first POST in the app), and AI DTO
types (`DecisionRequest/Response`, `DecisionStep`, `Citation`,
`ContentSource`) to `types/api.ts`. `POST /api/decide` already existed — no
backend change in 5a. Hero/widget CSS adapted from the WARM-palette
analogues in `backend/styles.css` (`.hero-section` gradient etc.), not the
reference doc's blue.

**The AI is a stub (known, deferred):** no Spring AI model-provider starter
is on the classpath, so `DecisionAgentService`'s `aiAssistant.generate()`
throws and the endpoint returns a graceful 200 fallback — `answerTitle:
"Unable to generate guidance"`, empty `steps`/`citations`, and `notes`
prefixed `"AI call failed: ..."`. 5a wires the full input→POST→render flow
anyway (the old demo did too) and renders the degraded state honestly.

**Deliberate deviation from the approved plan (UX fix, flagged to user):**
the plan said render the degraded `notes` directly. Live verification showed
that leaks a developer-facing string ("No ChatClient.Builder bean available.
Add a Spring AI model-provider starter…") to residents. Fixed by
distinguishing the provider-unavailable stub (notes starts with "AI call
failed") — which now shows a friendly "AI guidance is temporarily
unavailable — try browsing categories below." — from a LEGITIMATE "no
matches" answer whose `notes` is genuinely user-facing and IS shown. This
keeps the copy clean now AND correct once a real provider is wired.

**Verification:** `npm run build` (strict tsc) + `npm test` green (8 tests;
new `HeroGuidance.test.tsx` covers render, good-response render, the
provider-unavailable friendly notice + no-raw-error-leak, the legit no-match
notes path, and urgent:true in the POST body). Live: `docker compose up
--build -d app`, then the `run-firststep-app` Playwright driver pointed at
`APP_URL=/app-next/` filled the question, toggled Urgent, clicked Get Help,
and `wait-text "temporarily unavailable"` succeeded (it timed out against the
pre-fix build) — screenshot confirms the warm-palette hero + friendly
degraded card, no console errors.

Out of scope for 5a: Important Updates, CategoryPreviewList, `/api/updates`,
any polling, routing, filter context, wiring a real AI provider.

# Decision 019

**Governing architectural principle (set by the user during 5a review):
the backend aggregates & normalizes data; the frontend only displays it.**
This SUPERSEDES the banked-in-Decision-018 plan where the *client* was going
to merge `/api/news/rss` + `/api/flyers` and call `/api/categories` directly.
Cross-type aggregation, date selection, and source/url resolution now live
server-side. The principle governs Step 6+ too (results pages get
server-shaped data), not just Step 5.

Two endpoints embody it:
- **`GET /api/updates`** — the combined, normalized **Important Updates** feed
  (built in 5b, below). The *polled* endpoint for live-refresh.
- **`GET /api/home`** — the single initial-load aggregate for the homepage
  main column: `{ aiConfig, updates, categories }` (built in **5c**). SPA
  fetches once; browser stitches nothing. **`aiConfig` = static backend-owned
  config only** (suggested prompts, chip list, placeholder) — actual guidance
  stays the interactive `POST /api/decide`, which can't be pre-computed on
  load. **Scope:** `/api/home` feeds the main column now; the **Sidebar keeps
  its own `/api/categories` call until Step 7** (which lifts the fetch to a
  shared parent) — accepted minor cost: categories fetched twice on load until
  then.

**5b — Important Updates (DONE).** New backend `updates/` package:
`dto/UpdateItem` (a record: `type,id,title,summary,date,source,url,urgency` —
display-ready, camelCase, no `@JsonProperty` since these are new display
fields not domain models), `service/UpdatesService`, `controller/
UpdatesController` (`GET /api/updates`, mirrors `NewsController`). Aggregation:
merge curated News (`NewsService.getAll()`) + live RSS
(`RssFeedSource.getRssItems()`) + Flyers (`FlyerService.getAll()`); **dedupe
news by id** across curated+RSS (curated wins by insertion order via
`LinkedHashMap.putIfAbsent`); normalize each to `UpdateItem` (news date =
`published`, source/url = `contentSource.name`/`.url`, urgency = `urgency`;
flyer date = `eventDate` else `updatedDate`, source = `organization`, url/
urgency null); sort by `date` **descending, nulls last**
(`Comparator.nullsLast(reverseOrder())` — dates are `yyyy-MM-dd`, lexically
sortable); cap at **8** (`MAX_ITEMS`).

Frontend: `types/api.ts` gains `UpdateItem`; new `components/
ImportantUpdates.tsx` fetches `/api/updates` via `apiGet` on mount and renders
the feed (title, clamped summary, source · date, urgency badge for non-
"standard" news). **Live-refresh:** a `useEffect` `setInterval` polls every
**5 min** with **change-diffing** — a `useRef` holds the last serialized feed
(`JSON.stringify`) and `setUpdates` fires ONLY when it differs, so unchanged
polls cause no re-render/flicker. The effect returns a cleanup that
`clearInterval`s and flips a `cancelled` flag (the app's first interval
teardown / first polling pattern). Rendered under `<HeroGuidance />` in
`MainContent`. CSS adapted from the warm-palette `.resource-panel`/`.news-item`
in `backend/styles.css` (accent left-border rows, accent-underlined heading).

**Live-verification UX fix (discovered via the driven screenshot, not
predictable from data shape):** the RSS legislative item carries the ENTIRE
bill body as its `summary`, which blew one card to full-page height. Fixed
display-only with a 2-line `-webkit-line-clamp` on `.update-item-summary`
(keeps full data, just clamps the render) — consistent with "frontend
displays," rather than truncating server-side.

**Verification:** backend `mvn -Dtest=UpdatesServiceTest,UpdatesControllerTest`
green (7 tests: merge/sort, news+flyer normalization, event-date fallback,
id-dedupe, cap-at-8, null-dates-last, + `@WebMvcTest` endpoint shape).
Frontend `npm run build` + `npm test` green (10 total; new
`ImportantUpdates.test.tsx` proves the change-diffing via a `React.Profiler`
commit-count: an identical poll adds NO commit, a changed poll does + updates
the DOM — using `vi.useFakeTimers()` + `act(async () =>
advanceTimersByTimeAsync)`). Live: Docker rebuild → `run-firststep-app`
Playwright driver at `APP_URL=/app-next/` confirmed 8 real News+Flyer items in
date order, no console errors. (Polling live-change is hard to observe since
RSS only refreshes hourly — verified by the fake-timer unit test instead.)

Out of scope for 5b: `/api/home` + CategoryPreviewList (5c), Sidebar
consolidation (Step 7), routing (Step 6), real AI provider.

# Decision 020

Step **5c — `GET /api/home` consolidation + `CategoryPreviewList`** (DONE).
Completes Step 5 and realizes the single-request homepage architecture from
Decision 019: the SPA main column now loads from ONE `GET /api/home` call
instead of the client fetching several endpoints.

**Architectural framing (confirmed by user): `/api/home` is a
Backend-for-Frontend (BFF) endpoint, NOT a generic REST resource.** It is
shaped to serve exactly one view — the homepage — assembling precisely that
page's data in a single round trip. As the homepage grows, this endpoint grows
with it; the client never fans out or stitches data. This is deliberately
different from the granular resource endpoints (`/api/resources`,
`/api/flyers`, `/api/news`, …), which stay reusable and page-agnostic. Rationale
the user gave: a page-shaped BFF endpoint scales cleanly to BOTH the React web
app and a future mobile client (each view/client can have its own BFF
endpoint), while keeping every client intentionally **thin and display-only**.
This complements Decision 019's "backend aggregates, frontend displays" — the
BFF pattern is *how* that principle is applied per page. **Implication for Step
6+:** results/detail pages should get their own page-shaped aggregates rather
than having the client compose granular endpoints (`/api/updates` remains the
one carve-out: a lightweight polled SUBSET of `/api/home`, not a separate page).

**Backend — new `home/` package** (composition only, no duplicated logic):
- `dto/AiChip` (record `value,label,urgent`), `dto/AiConfig` (record
  `placeholder, suggestedPrompts, chips`), `dto/HomePayload` (record
  `aiConfig, updates, categories`).
- `service/HomeService` — holds the **static, backend-owned `AiConfig`**
  (placeholder + 3 suggested prompts + the 3 chips; chip `value`s match what
  `HeroGuidance` sends to `/api/decide`) and composes the EXISTING
  `UpdatesService.getUpdates()` (Decision 019) + `CategoryService.getAll()`.
- `controller/HomeController` — `GET /api/home?communityId=` (optional param,
  mirrors `CategoryController`).

**Frontend:**
- `types/api.ts` gains `AiChip/AiConfig/HomePayload`.
- `MainContent` becomes the orchestrator: fetches `/api/home` **once** on mount
  and distributes — `aiConfig` → `HeroGuidance`, `updates` → `ImportantUpdates`
  (seed), `categories` → `CategoryPreviewList`.
- `HeroGuidance` refactored to take `aiConfig?` and render backend-driven
  chips/prompts/placeholder; chip rendering unified (each chip's `urgent` flag
  decides whether it toggles `urgent` or `preferredCategories`). Keeps a
  `DEFAULT_AI_CONFIG` fallback so it still works standalone/in tests.
- `ImportantUpdates` refactored to accept optional `initialUpdates` — when
  seeded it skips the mount fetch (no double request) and just polls
  `/api/updates` for refresh; unseeded it self-fetches (standalone use).
- New `CategoryPreviewList` — one card per category (icon, count, 📢 latest
  policy update, latest item titles, inert **Browse** button).

**Design decisions worth recording:**
1. **Hero renders immediately; data sections wait for `/api/home`.**
   `MainContent` always renders `<HeroGuidance>` (with its default config) so
   the primary call-to-action never blocks on the network; when `/api/home`
   resolves, the real `aiConfig` takes over and Updates + Previews mount seeded.
   On error, the hero still works (default config) and a message covers the
   data sections.
2. **No double-fetch.** Because `ImportantUpdates` mounts only after
   `/api/home` resolves, it's always seeded and never also hits `/api/updates`
   on mount — it only polls thereafter.
3. **Browse is inert (`disabled`, `title="Full listings coming soon"`)** — real
   result pages/routes are Step 6. Consistent with the Step-4 "visible but not
   yet wired" precedent (sidebar checkboxes).
4. **Distinct CSS class names** (`.category-preview*`, `.previews-*`) to avoid
   the known `.category-group-header` collision with `backend/styles.css`
   (flagged in Decisions 018/019).
5. **Sidebar STILL calls `/api/categories` separately** — full consolidation
   (Sidebar reading from `/api/home` too) is deferred to **Step 7** (shared
   context), so categories are fetched twice on load until then. Accepted.

**Verification:** full backend suite **116 tests** green (new `HomeControllerTest`
wires the real `UpdatesService`+`CategoryService` with fake repos and asserts
the `{aiConfig, updates, categories}` shape); frontend **13 tests** green (new
`CategoryPreviewList.test` + `MainContent.test` asserting the single `/api/home`
call distributes to all three sections; `App.test` made path-aware since the
tree now fires both `/api/categories` and `/api/home`). Live: Docker rebuild →
`/api/home` returns aiConfig(3 chips/3 prompts)+8 updates+10 categories in one
call; `run-firststep-app` driver at `/app-next/` confirmed the full homepage
(hero w/ backend prompts, Important Updates, 10-card Browse-by-category grid
with policy lines) — no console errors. **Step 5 COMPLETE.**

Out of scope: routing/result pages (Step 6), Sidebar→/api/home consolidation
and shared filter context (Step 7), real AI provider.

# Decision 021

**MAJOR PIVOT — the homepage design was replaced completely by a new
civic-portal specification.** This supersedes the Step-5 homepage LAYOUT
(hero+AI / Important Updates / CategoryPreviewList) and the old Step 6–8
roadmap. The backend groundwork stands: `/api/categories`, `/api/updates`,
`/api/home`, `/api/decide`, the category taxonomy, and Decisions 014–020's
endpoint/architecture work all remain valid — only the homepage *presentation*
and the forward roadmap changed. The BFF principle (Decision 019/020,
[[firststep-bff-architecture]]) is unchanged and still governs.

**New homepage = 5 vertical sections** (trusted civic-information portal; avoid
excessive scrolling; key content above the fold; no oversized imagery/carousels
in the hero):
1. **Utility Bar** — narrow top strip: left = future social icons; center =
   always-available AI search ("Tell me what you need today…"); right = ARIA /
   accessibility controls.
2. **Hero** — logo (upper-left, links home from anywhere) + app name + tagline
   ("Your trusted guide to community resources, program updates and local
   information."); on the app-name row, **primary nav**: Housing Assistance,
   Community Info, Important Notices, Life Assistance (catchall).
3. **New Delaware Laws** — rotates ONE bill title at a time, 7 most recent
   signed bills.
4. **Resource Discovery** — two columns w/ subtle divider: **Organizations**
   (left, each → an Organization landing page aggregating all its content) |
   **Categories** (right, each → a Category page of topic-groups → topics →
   CivicContent).
5. **Community Information** — the flyer carousel.

**Four-level nav hierarchy:** Category → topic-groups → topic → CivicContent
(resources, news, policy, expert content — one consistent card design that
labels content type + source).

**Copy rules:** no Oxford commas; the Weekly Updates page is renamed
**"Important Notices"** (NOT "Important Notices Updates").

**Data reality (verified this session — governs later slices):** 229 resources
loaded (58 curated `resources.json` + 171 `resources.communities.json`). **178
distinct organizations, highly fragmented** (top has 6). **`subcategory` (the
"topic" level) exists ONLY on the 58 curated Housing/Clothing/Furniture
records; the other 171 have none.** Topic-group headers exist nowhere. Category
examples in the spec (Food Assistance, Transportation…) diverge from the 10-cat
taxonomy — reconcile in Discovery.

**Confirmed decisions (user):**
- **Frame-first** decomposition (this = Slice A).
- **Organizations = curated shortlist, NO "see all"** (First Step is a curated
  selection). Ranking metric TBD (likely policy/news-driven); **seed by resource
  count for now**.
- **Topic hierarchy full only where subcategory data exists** (Housing); other
  categories skip the topic level (→ content directly) until data lands.
- **Required data task (D0):** enrich `resources.communities.json` (171 records)
  to `resources.json`'s schema (add `subcategory` + curated fields) so topics
  generalize. Prereq for D/F.
- **AI need not be fully functional** — canned/sample responses acceptable
  (Utility Bar search wired in Slice B; `/api/decide` already returns canned
  text).

**New roadmap (supersedes old Step 6–8):** A. homepage frame + routing (DONE
below) · B. AI-search wiring · C. Delaware-Laws rotator · D0. data normalization
· D. Resource Discovery · E. Community carousel · F. Category/topic/content pages
· G. Organization pages · H. Important Notices page + Community Info page · I.
accessibility + mobile + polish.

## Slice A — homepage frame + routing foundation (DONE)
New `pages/{HomePage,StubPage}.tsx` + `components/{UtilityBar,SiteHero,PrimaryNav,
DelawareLawsFeature,ResourceDiscovery,CommunityInformation}.tsx`. Sections are
presentational scaffolds (real content in B–E). **React Router introduced at
last** (`react-router-dom` v6, installed since Step 3): `App.tsx` wraps
`<BrowserRouter basename="/app-next">` with `/` → HomePage and the four nav
destinations → `StubPage` ("Coming soon"). **`SpaWebConfig` widened** from two
exact `/app-next` forwards to the canonical depth-agnostic SPA fallback: a
`/app-next/**` resource handler with a `PathResourceResolver` that serves the
real file when it exists (JS/CSS/assets) else returns `index.html` — so client
routes and hard refreshes on them load at any depth. (Chosen over per-depth view
controllers because the coming Category→topic→content routes go 3–4 segments
deep.)

**Clean cut (per "replaces completely"):** removed
`components/{AppLayout,Sidebar,MainContent,CategoryPreviewList}.tsx` + their tests
(directly superseded). **Kept** `HeroGuidance.tsx` (AI → Slice B) and
`ImportantUpdates.tsx` (→ C/H) to repurpose — unrendered for now, so tree-shaken
from the bundle but still type-checked. `/api/home` temporarily has no consumer
until sections fill (fine). The old CSS for removed components remains in
`index.css` (dead, low-risk) alongside the new frame styles — flagged for later
cleanup.

**Verification:** frontend `npm run build` + `npm test` green (10 tests: frame
render via MemoryRouter+HomePage, PrimaryNav route targets, App routing under the
basename). Backend `mvn compile` clean. Live (Docker): `/app-next/` + deep-link
hard refreshes `/app-next/important-notices` (1-seg) and
`/app-next/category/housing-assistance` (2-seg) all return 200 + the SPA index,
while JS assets still serve as `text/javascript` (proves the resolver serves real
files, falls back only for routes). Playwright: home → click Important Notices →
stub "Coming soon" → click logo → home; no console errors. Screenshot
`step6a-frame-home.png`.

**Real brand assets wired (Slice A follow-up):** the placeholder compass emoji
was replaced with the actual logo — the green+orange **feet** at
`backend/.../static/images/First step logo feet.png`, copied into
`frontend/src/assets/logo-feet.png` and `import`ed (Vite bundles it +
rewrites the URL under the `/app-next/` base). Added `src/vite-env.d.ts`
(`/// <reference types="vite/client" />`) so `*.png` imports typecheck given the
explicit tsconfig `types` array. NB: the PNG has a WHITE background (renders as a
small white chip on the cream page) — a transparent version would sit cleaner.

**Reference from the OLD static demo (`static/index.html` + `styles.css`), for
upcoming slices:**
- **Accessibility/ARIA controls** the Utility Bar's right slot should hold (Slice
  I, or pull forward): a **Language toggle** (ES/EN — the demo has full Spanish
  i18n via `app.js`'s `t()`) and a **High Contrast** button that toggles a full
  `body.high-contrast` black/yellow theme (the CSS exists in `styles.css` to
  port). The Slice-A right slot is currently a disabled ♿ placeholder.
- **Flyer images** for the Community carousel (Slice E) live in
  `static/images/seasonal/` (7: Disability Info, Eviction Help, Fundraiser,
  Furniture, Health Fair, Volunteer, Youth) — pair with `flyers.json`.
- The old header's 🤖 "Get answers with AI" banner is the ancestor of the Utility
  Bar search (Slice B).

Out of scope for Slice A: AI wiring (B), Delaware-Laws data (C), Orgs/Categories
data + taxonomy reconcile (D0/D), carousel (E), deep pages (F–H), `/api/home`
reshape, accessibility/mobile (I).

# Decision 022

**Accessibility controls pulled forward from Slice I** (user asked to build them
now, after reviewing the old static demo which had them). Two controls live in
the Utility Bar's right slot, ported from the old `static/index.html` + `styles.css`:

- **Language toggle (EN/ES).** Lightweight i18n (user chose this over
  react-i18next — no new dependency, mirrors the old `app.js` `t()` approach):
  `i18n/dictionary.ts` (EN/ES key→string maps) + `i18n/I18nProvider.tsx`
  (context + `useI18n()` → `{lang, setLang, t}`; persists to `localStorage`;
  syncs `document.documentElement.lang`). **The context default is a working
  English `t()` so components render without a provider** (keeps the frame/nav
  unit tests provider-free and green). `App` is wrapped in `<I18nProvider>`.
  Frame components (`SiteHero` tagline/aria, `PrimaryNav` labels, `UtilityBar`
  placeholder, the three section titles + a generic `common.comingSoon`,
  `StubPage`) call `t()`. **Scope: UI-CHROME only** — a display concern, so it
  lives in the frontend. Translating CONTENT (resource data) would be a backend
  responsibility and is NOT done here (the old app didn't either).
- **High Contrast.** `hooks/useHighContrast.ts` toggles a `high-contrast` class
  on `document.body` (+ `localStorage`). The old `body.high-contrast` theme
  (black + `#ff0`) was REWRITTEN for the new frame classes (`index.css`) — the
  old rules targeted removed components, so they couldn't be copied verbatim.

**Verification gotcha worth remembering:** the first high-contrast screenshot
showed nav pills still white while cards went black. This was NOT a CSS bug —
`.primary-nav-item` has `transition: all 0.2s` (cards don't), so on toggle the
pills ANIMATE white→black and the screenshot/`getComputedStyle` caught them at
t≈0. Proven via CSSOM enumeration (rule present, top-level, matches, correct
specificity) + a delayed `getComputedStyle` returning `rgb(0,0,0)` after 600ms.
**Lesson: let CSS transitions settle before asserting visuals.** Minor polish
noted for later: theme toggles ideally shouldn't animate (suppress transitions
during a theme switch) — deferred to Slice I.

**Real brand logo** also wired this pass (see Decision 021 addendum): the
green+orange feet PNG replaced the compass emoji.

**`app.js` data-loading review (user directive: "frontend is for display,
backend is for business logic and aggregation").** The OLD static frontend did
substantial CLIENT-SIDE business logic that violates this — recorded so later
slices don't repeat it: Housing screen fetched ALL `/api/resources` then
`filter(category includes "housing")` + urgency filter (app.js:382); Essentials
`filter(cost === "free")` (app.js:414); News fetched `/api/news` + `/api/news/rss`
separately, merged, derived `[...new Set(flatMap(tags))]` and filtered by tag
(app.js:481,517); carousel parsed captions from filenames (app.js:806). The NEW
architecture already corrected most (`/api/categories`, `/api/updates`,
`/api/home` BFF). **Forward rule (reaffirmed, [[firststep-bff-architecture]]):**
Category pages (F) get a BFF returning the category's topics/content — not
fetch-all-and-filter; Important Notices (H) does tag grouping/filtering
server-side; the carousel (E) gets a BFF returning `{image, caption}` objects.

**Verification:** 12 frontend tests green (new `Accessibility.test.tsx`:
EN→ES toggle changes tagline + nav labels; high-contrast toggle sets the body
class + `aria-pressed`). Live (Docker): Spanish renders across the frame
(placeholder, tagline, nav, section titles, "Próximamente."); high contrast
renders black/`#ff0` throughout (nav confirmed black after transition). Both
persist via `localStorage`. Screenshots `step6a-spanish.png`,
`step6a-highcontrast.png`, `step6a-frame-logo.png`.

# Decision 023

Slice **D — Resource Discovery** (DONE). Fills the Slice-A two-column shell:
LEFT = curated **Organizations**, RIGHT = resource **Categories**, each a
navigation entry to a landing page (Organization pages = Slice G, Category pages
= Slice F; both stubs for now). Built on the **currently-loaded 229 records** —
data normalization **D0 deferred** to before F.

**Confirmed decisions:** reuse the **existing 10-category taxonomy** already in
`/api/home` (real counts) — refinement (Transportation/renames) tied to D0;
Organizations = a **curated shortlist, NO "see all"**, ranked by **resource
count as a PLACEHOLDER metric** (the eventual metric is expected to be
policy/news-driven — documented, not built).

**Backend (new `organization/` package, BFF-aggregated):** `dto/OrgSummary`
(record `name, slug, resourceCount`); `service/OrganizationService.
getCuratedShortlist()` groups `resourceService.getAll()` by `organization`
(skips null/blank), counts, ranks by count desc then name asc (stable), caps at
**8** (`MAX_ORGS`), and `slugify`s the name (lowercase, non-alphanumerics→hyphen)
for `/organization/{slug}` routing. `home/dto/HomePayload` gained
`organizations: List<OrgSummary>`; `HomeService` composes
`organizationService.getCuratedShortlist()` (its 3rd injected aggregator).

**Frontend:** `types/api.ts` gains `OrgSummary` + `organizations`. **`HomePage`
re-introduces the single `/api/home` fetch** (Slice A had removed `MainContent`
which used to do it) and distributes `organizations` + `categories` (+ `error`)
to `<ResourceDiscovery>`; the frame (Utility Bar + Hero) still renders instantly
while the payload loads. This restores the one-request BFF load and sets up C/E
(they'll consume `home.updates` etc.). `ResourceDiscovery` renders two `<Link>`
lists (org → `/organization/{slug}`, category → `/category/{key}`, with counts).
New `/organization/:slug` → `StubPage`. Discovery list CSS + high-contrast
overrides added.

**Verification:** backend `mvn -Dtest=OrganizationServiceTest,HomeControllerTest`
green (6: group/count, rank+tie-break, skip-blank, cap-8, slugify, + the endpoint
now asserts `organizations[0].name/slug`). Frontend `npm run build` + `npm test`
green (15; new `ResourceDiscovery.test`; the frame test now mocks `/api/home`
since HomePage fetches). Live (Docker): `/api/home` returns 8 ranked orgs with
slugs; the Discovery section shows orgs left + 10 categories right; clicking
Easter Seals routed to the Organization stub; no console errors. Screenshot
`step6d-discovery.png`.

Out of scope: D0 (categorize the broader dataset), real Organization (G) +
Category (F) pages, Laws rotator (C), carousel (E), AI wiring (B), taxonomy
refinement, non-count org ranking.

# Decision 024

Slice **C — New Delaware Laws rotator** (DONE). Directly below the Hero, rotates
ONE recently signed bill title at a time (7 most recent), reinforcing First Step
as a central news point.

**Backend (new `legislation/` package, BFF-aggregated):** `dto/LawItem` (record
`title, url, date`); `service/LegislationService.getRecentSignedBills()` reads
`RssFeedSource.getRssItems()` (the RSS feed IS the GovernorSignedLegislation
feed — 355 bills live), sorts newest-first (nulls last), caps at **7**
(`MAX_BILLS`), maps to `LawItem` (url from `contentSource.url`). A dedicated
service (not folded into HomeService) so the **Important Notices page (H)** can
reuse it for its legislation column. `HomePayload` gained
`delawareLaws: List<LawItem>`; `HomeService` composes it (4th aggregator).

**Frontend:** `types/api.ts` gains `LawItem` + `delawareLaws`. `HomePage` passes
`home.delawareLaws` to `DelawareLawsFeature`, now a **rotator**: `useState` index,
a `setInterval` auto-advancing every **5s** (`ROTATE_MS`) — **skipped when the
user prefers reduced motion** (`matchMedia('(prefers-reduced-motion: reduce)')`)
or there's <2 bills — subtle CSS fade (`@keyframes laws-fade`, keyed on index;
also disabled under the reduced-motion media query), and **dot buttons** for
manual navigation (accessible `aria-label`/`aria-current`). Each bill title links
to the bill (`target=_blank rel=noopener`). High-contrast overrides for the dots.

**Design choices (defaults, flagged to user):** 5s interval; fade animation;
reduced-motion respected; dot navigation; title links out.

**Verification:** backend `mvn -Dtest=LegislationServiceTest,HomeControllerTest`
green (3 legislation: newest-first+map, cap-7, missing-url; endpoint now asserts
`delawareLaws[0].title`; the `updates` assertion was made order-independent since
the fake RSS bill also merges into `updates` and can sort ahead by date).
Frontend `npm run build` + `npm test` green (19; new `DelawareLawsFeature.test`:
render+link+dots, auto-rotate via fake timers, dot-click jump, empty placeholder;
`App.test`'s HomePayload mock gained `delawareLaws: []` — tsc build enforced it).
Live (Docker): `/api/home` returns 7 bills newest-first (after the async RSS load
settles ~a few seconds post-boot); the rotator renders, dot-4 jumped to "Relating
to Family Trust Companies"; no console errors. Screenshots `step6c-laws-1/2.png`.

Out of scope: carousel (E), AI wiring (B), D0/deep pages (F–H), the Important
Notices page (H) that will reuse LegislationService.

# Decision 025

Slice **E — Community Information flyer carousel** (DONE). The homepage's bottom
section: a horizontal scroll-snap strip of flyer cards (image + caption).
Completes the visible civic-portal homepage (only AI-search wiring, B, remains).

**Backend (extends the flyer domain, BFF-aggregated):** `flyer/dto/FlyerCard`
(record `imageUrl, title, organization, eventDate`); `FlyerService.
getCarouselCards()` maps the loaded flyers → cards, filtering those without an
image, sorting by `eventDate` soonest-first (nulls last). **The imageUrl is
resolved AND URL-encoded server-side** — `"/images/seasonal/" +
UriUtils.encodePathSegment(image, UTF_8)` — because the seasonal images (bare
filenames like `Health Fair.jpg`, some with spaces) serve at that static path
only when encoded (`%20`). This keeps the frontend from knowing the path
convention or doing encoding (backend aggregates + shapes, frontend displays).
`HomePayload` gained `communityFlyers`; `HomeService` injects `FlyerService`
(5th aggregator) and composes it.

**Frontend:** `types/api.ts` gains `FlyerCard` + `communityFlyers`. `HomePage`
passes `home.communityFlyers` to `CommunityInformation`, now a **carousel**: a
horizontal `overflow-x: auto` + `scroll-snap` `<ul>` of fixed-width flyer cards
(240px, so the next card peeks and signals scrollability), each an `<img>` (alt =
title, `loading="lazy"`) + caption (title, org · date). **No auto-advance** — the
user explores by scrolling ("without overwhelming"). High-contrast overrides for
the cards.

**Data note:** the 7 flyers' `image` fields are bare filenames matching the 7
files in `static/images/seasonal/`, served at `/images/seasonal/<file>` by the
default Spring static handler (confirmed 200; spaces must be `%20`). The old
demo's `/api/seasonal-images` directory-listing endpoint + client-side
filename→caption parsing is NOT used — the flyer data already carries titles/orgs.

**Verification:** backend `mvn -Dtest=FlyerServiceTest,HomeControllerTest` green
(3 new carousel tests: URL-encode imageUrl, sort soonest-first, skip image-less;
endpoint asserts `communityFlyers[0].imageUrl == /images/seasonal/Health%20Fair.jpg`).
Frontend `npm run build` + `npm test` green (21; new `CommunityInformation.test`
asserts image src + caption; `App.test` mock gained `communityFlyers: []`). Live
(Docker): `/api/home` returns 7 encoded flyer cards event-date-sorted; the
carousel renders the real flyer images with a peeking next card; no console
errors. Screenshot `step6e-carousel.png`.

Out of scope: AI-search wiring (B), D0/deep pages (F–H).