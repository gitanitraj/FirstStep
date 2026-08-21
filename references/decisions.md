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

# Decision 026

Slice **B — AI-search wiring** (DONE). **Completes the homepage.** The Utility
Bar's AI search (UI-only since Slice A) now posts to `/api/decide` and shows the
answer in a dismissible dropdown panel. Per the user, canned/degraded responses
are acceptable — no AI provider is wired, so `/api/decide` returns its graceful
"Unable to generate guidance" body, which the panel renders as a friendly notice.

**Repurposed the Step-5a hero AI logic** (the `HeroGuidance` component kept
unrendered for exactly this): extracted the result rendering + degraded detection
into a new presentational `components/AiResultCard` (answerTitle/steps/citations,
OR — when steps+citations are empty — a friendly "temporarily unavailable" line
for the provider-unavailable stub [notes prefixed "AI call failed"] vs. the AI's
own `notes` for a legit no-match). **`HeroGuidance.tsx` + its test + its
annotated mirror were deleted** (fully superseded — the AI moved to the Utility
Bar; the new homepage has no hero widget).

**UtilityBar** now owns the search state: enter a query → `apiPost<DecisionRequest,
DecisionResponse>('/api/decide', { userQuery, urgent: false, preferredCategories:
[] })` (a plain "what do you need" box — the old urgency/category chips are gone),
and renders a **dropdown result panel** absolutely positioned under the centered
search (`.utility-center` is now `position: relative`). Panel states:
loading/error/`AiResultCard`. **Dismiss** via close button, Escape, or click
outside (document listeners added only while open). Accessible: `role="region"`,
`aria-live="polite"`. New i18n keys (`search.thinking/error/unavailable/sources/
close/resultsLabel`, EN+ES) — the canned/degraded copy is translated (answer
content from the backend is not). High-contrast panel overrides added.

**Design choice (default, flagged):** results in a dismissible dropdown panel
(not a modal), keeping the search "always available without dominating".

**Verification:** frontend `npm run build` + `npm test` green (20 tests; net −1
vs. prior: removed HeroGuidance's 5, added `AiResultCard.test` [3: provider-
unavailable friendly line + no raw-error leak, legit no-match notes, full
answer/steps/sources] + `UtilityBar.test` [1: Enter posts the plain query and
the panel shows the canned notice, then closes]). Live (Docker): typed a query,
pressed Enter → panel showed "AI guidance is temporarily unavailable", no console
errors. Screenshot `step6b-ai-search.png`.

**HOMEPAGE COMPLETE** (Utility Bar w/ AI search + a11y · Hero + primary nav ·
Delaware Laws rotator · Orgs|Categories discovery · flyer carousel), all from the
single `/api/home` BFF + the interactive `/api/decide`. Remaining project work:
**D0** (data categorization) → deep pages **F/G/H**.

Out of scope: D0/deep pages (F–H), wiring a real AI provider (`/api/decide`
stays a graceful stub).

# Decision 027

**D0.1 + D0.2 — canonical taxonomy SSOT + AI enrichment pipeline** (tooling; not
yet run against the data). Splits the domain model into two deliberately separate
layers (per the user's refinement): a **stable, hand-maintained canonical
taxonomy** (`Category → Subcategory`) and a **generated navigation** layer (D0.4,
`Group → Topics`) built on top of it. This decision covers the first two pieces.

**D0.1 — `app/data/taxonomy.json` is the single source of truth.** One file holds
the canonical `Category → [Subcategory]` vocabulary for all 10 display categories,
each with `matchCategories` (the raw source-category strings, mirroring
`CategoryDefinition.java`'s raw→display mapping). `validate_schema.py` was rewired:
its previously-hardcoded 3-category `VALID_CATEGORIES`/`CATEGORY_TO_SUBCATEGORY`/
`VALID_SUBCATEGORIES` are now loaded from `taxonomy.json` via `_load_taxonomy()`
(same variable names, so `validate_record()` is unchanged). The 58 curated
`resources.json` records still pass 58/58. **`Eviction Prevention` is dual-placed**
in both `housing` and `legal` (confirmed by the user — one seasonal flyer is an
eviction-prevention legal resource; topics are content-agnostic and a subcategory
may sit under more than one category).

**D0.2 — `data-cleaning/scripts/enrich_resources.py`** proposes the fields the 171
structurally-mapped `resources.communities.json` records lack (`subcategory`,
`cost`, `urgency`, `tags` — see Decision 013 for why they're empty). **Two-phase,
human-in-the-loop by design — the model never writes the data file directly:**
`propose` writes a reviewable proposals file under `data-cleaning/proposals/`
(each entry `approved: true`, flip to false to drop, or edit values in place);
`apply` merges ONLY approved proposals, filling empty fields (never clobbering
existing non-null values). **Subcategory is constrained** — a per-display-category
JSON-schema enum built from `taxonomy.json`, so the model can only pick a valid
canonical subcategory; categories with no subcategories omit the field.
cost/urgency are best-effort enums; tags are free topic strings that feed
navigation. A separate `--task flyers` pass tags each flyer with 1–3 canonical
topics (constrained to the full 44-subcategory vocabulary) and unions them into
the flyer's existing `tags`, so flyers fold into the same navigation topics.
Claude API `claude-opus-4-8`, structured outputs (`output_config.format`).
**No `anthropic` SDK** — the user declined to install it, so `propose` POSTs to the
Messages API over raw HTTPS via the Python stdlib (`urllib`), zero third-party
deps; structured outputs are a request parameter so nothing about the constrained
design changes. `propose` needs `ANTHROPIC_API_KEY`; `apply` never calls the API.

**Verification:** no-API smoke test — all 21 raw categories present in the data
resolve through `matchCategories`; per-category enums build (health = 5 subs); the
empty-category path drops `subcategory`; the flyer schema constrains to 44 topics.
The enrichment script is **not** run here — the actual (billed) per-category passes
are D0.3.

Out of scope (this decision): running enrichment (D0.3), the navigation generator
(D0.4), and integration/`--strict` validation of the enriched file (D0.5).

# Decision 028

**D0.3 — enrichment run (rules method) + flyer tagging, applied.** All 171 records
and 7 flyers are now enriched, human-reviewed, and merged into the data files.

**Pivot: rules method instead of AI.** The user set up an Anthropic API key but the
account had a $0 credit balance (the Console "free plan" covers the Workbench UI,
not programmatic Messages-API calls — there is no free API tier), and chose not to
purchase credits. So `enrich_resources.py` gained a **`--method {rules,ai}`** switch
(**rules is the default**). The rules proposer is offline, deterministic, dependency-
free, and writes the SAME proposals shape as the AI path, so review/apply are
unchanged. Two tiers: (1) `RAW_DEFAULT` maps raw source categories that ARE a
subcategory (e.g. "Support Group" -> "Support Groups") — this alone covers all of
community-support, because the DSCYF directory's raw category already is the
subcategory there; (2) a `SUBCATEGORY_KEYWORDS` lexicon disambiguates the broad
categories (Recreational, Mental Health, Housing, Food, Employment, Legal). The AI
path is retained for a future funded key.

**Result: 169/171 (98%) auto-assigned a canonical subcategory** on the first pass;
community-support (58), community-events (53), health (32), housing (7), legal (3)
were 100% covered. **2 gaps + 1 dual-topic were reviewed by the user:** SD-020
(Habitat ReEmployeAbility) -> Job Search Assistance; SD-041 (Our Daily Bread) ->
Food Pantry; SD-078 (Terry Children's, "crisis stabilization") kept
`Counseling & Therapy` as subcategory **plus** a `Crisis Services` tag — the same
content-agnostic dual-topic pattern as Eviction Prevention (a record has one
canonical subcategory but surfaces under multiple navigation topics via tags).

**Flyers (7) tagged with canonical topics unioned into existing hand tags.** Flyer
keyword-matching is lower-confidence (flyers describe intent, not services — FL-003,
a volunteer-recruitment flyer, mis-hit "food banks/shelters" and was corrected to
Volunteer Opportunities), so all 7 were human-reviewed. **Two user-chosen free-form
tags** are not canonical subcategories — `Student Support` (FL-004 back-to-school
drive) and `Furniture/Household Goods` (FL-007 furniture giveaway) — which is fine:
flyer tags have always been free-form and navigation topics gather by tag OR
subcategory.

**Data note (furniture):** the loaded 171-record `resources.communities.json` has
**no** furniture-household resources, so FL-007 is currently the only content in
that category. But the full 603-record source (`Service_Directory_cleaned.json`,
`services[]`, `typeOfService` field) **does** have ~11 furniture services (Salvation
Army, Habitat ReStore, Catholic Charities, Sunday Breakfast Mission) — they'd
populate that category if the loaded dataset expands beyond the curated subset
(Decision 013).

Out of scope (this decision): navigation generation (D0.4) and `--strict` /
integration verification of the enriched file (D0.5).

# Decision 029

**D0.4 + D0.5 — navigation as its own artifact, and both data files validating
clean. D0 IS COMPLETE; Slice F is unblocked.**

**The design decision (user, during planning): navigation is a PRESENTATION model,
not domain vocabulary, so it lives in its own file.** Decision 027 had planned
either a `groups` key inside `taxonomy.json` or a `navigation.generated.json`
produced by analyzing content. The user pushed back on both: topic groups are part
of the navigation experience, not the domain model. Two artifacts, two lifecycles —
`taxonomy.json` (Category → Subcategory, stable, what everything validates against)
and **new `app/data/navigation.json`** (Group → Topics, editorial, expected to
evolve). **The payoff is the long-term one:** when navigation generation becomes
AI-assisted, only `navigation.json` is regenerated — the domain taxonomy stays
stable, the backend keeps aggregating live content, and the frontend is untouched
because it renders whatever navigation model it is handed. A **`source` field**
(`hand-authored` now, `ai-generated` later) carries provenance so the **filename
stays stable**: the AI swap changes content, never wiring.

**Structure is authored; counts are computed at runtime.** `navigation.json` stores
group labels and topic names — never counts. Baking counts into a data file would
duplicate the aggregation the backend already does (`OrganizationService.
getCuratedShortlist()`) and go stale against loaded data, violating Decision 019's
*backend aggregates and normalizes; frontend only displays*. **Slice F's
`NavigationService` does the counting**, using the standing content-agnostic rule
(`resource.subcategory == topic OR content.tags contains topic`).

**Only two categories are grouped** — housing (8 topics) and community-support (11).
A category absent from `navigation.json` renders a **flat topic list**; a group
header above legal's single topic is noise, not hierarchy. Housing: Need Help Right
Away · Find a Place to Live · Help Staying Housed. Community Support: Get Help ·
Family and Children · Learning and Skills · Connect with Others. Every subcategory
is placed exactly once; `Eviction Prevention` stays dual-placed across categories
(housing groups it, legal is flat).

**New `data-cleaning/scripts/validate_navigation.py`** — one validator per data
artifact is this repo's convention (`validate_schema.py` → resources,
`validate_news.py` → news), and this gate is exactly what a future AI generator's
output must pass. Enforces: every key exists in the taxonomy; every topic is a
subcategory of its category (catches taxonomy-rename drift); **group topics cover
every subcategory** (the subtle one — otherwise a topic can hold content and be
unreachable from the UI, a bug nothing else would surface); no topic in two groups;
no duplicate labels. Reports (never errors) flat categories and topics with no
loaded content. `taxonomy.json`'s `note` was corrected to describe the split; the
file is otherwise **structurally unchanged**.

**D0.5 — the enriched file had never been validated, and failed 171/171.** Not a
data-quality problem: six fields were **absent as keys** where the curated 58 carry
them as explicit `null` (`eligibility`, `eligibility_age_min/max`,
`eligibility_gender`, `access_mode`, `notes`, plus `cost` on 144), and the ID rule
hardcoded the curated `CI/FH/HA` prefixes, rejecting every `SD-` id. Fixes:

- **`normalize_resources.py` gained 3 lines** — it already filled 11 of those keys
  via `record.get(...)`; `eligibility`, `eligibility_gender` and `cost` were simply
  never touched. Run in place on `resources.communities.json`, this cleared all
  1176 missing-field errors. Values land as `null` (the honest value — the DSCYF
  directory never stated them), and `record.get` preserved the 27 costs the D0.3
  enrichment assigned. **Verified: 0 pre-existing values changed, 9 keys added,
  `meta` identical, 171 records aligned by id.** The curated file was left alone.
- **`validate_schema.py`**: `SD` added to the ID prefixes; a **`--input` flag**
  (it was hardcoded to `resources.json` and could not validate the communities
  file at all).
- **The `meta.note`** on `resources.communities.json` was corrected — it still
  claimed subcategory/tags/cost were unpopulated, which D0.3 had changed.

**Duplicate detection was wrong, and the finding was that there are NO duplicates.**
The rule keyed on organization+address and flagged 37 pairs. Investigation showed
every one is the **same service listed under multiple raw source categories** — the
DSCYF directory does this deliberately (Learning Tree Academy under Before/After
School Care, Child Care AND Early Childhood/Pre-K; BCCS and RI International under
both Mental Health and Substance Use), and the curated thrift/voucher records are
dual-listed across Clothing and Furniture (CI-004/FH-002, CI-013/FH-006). **Nothing
was deleted.** The key became organization+address+**category**+subcategory, and
`detect_duplicates()` now returns **(errors, warnings)** — two tiers, because a key
collision means two different things: **identical summary → a true duplicate
(ERROR); different summary → one organization running several programs at one site
(WARNING)**. The two survivors are legitimate: Saint Anne's (Youth Group vs Sunday
School in a Box) and Brandywine Valley SPCA (three volunteer programs). Both files
now **exit 0**. Note this also fixed a pre-existing bug — `resources.json` had been
exiting 1 on 3 of these false positives all along.

**Warnings are accepted as documented debt** (user's call): 42 curated + 171
communities warnings remain (`notes` empty, `parent_organization` null, long source
summaries). `--strict` stays aspirational — clearing it means authoring content for
~342 fields, which is editorial work, not a data pass.

**Verification:** `validate_schema.py` on both files → **exit 0** (58 passed, 171
passed, 0 failures). `validate_navigation.py` → **exit 0**, and both validators were
**negative-tested**: a bogus topic, an omitted topic, a topic in two groups, a
duplicate label, an unknown key and an empty groups[] each fail as intended; an
identical cloned record raises the duplicate ERROR while a reworded clone raises
only the WARNING. **127 backend tests green.** Live (Docker): `/api/home` category
counts **unchanged** — housing 44 · health 32 · clothing 15 · food 12 ·
furniture-household 6 · employment 6 · legal 3 · utilities 0 · community-support 58 ·
community-events 60 (53 resources + 7 flyers) = 229 resources; `GET /api/resources/
SD-004` round-trips with `eligibility: null`, proving in-place normalization did not
break deserialization.

Annotated mirror: `references/validate_navigation_annotated.py` (new script).
`validate_schema.py` and `normalize_resources.py` have no annotated mirrors — a
pre-existing gap, recorded here rather than by adding two ~450-line files.

Out of scope (this decision): `NavigationService`, the category BFF endpoint and
any frontend — all Slice F.

# Decision 030

**`validate_news.py` de-hardcoded — and the cleanup exposed that half the curated
news never surfaces under any category.**

**The problem:** `VALID_CATEGORY_TAGS` was a hardcoded 4-string set
(`Clothing & Incidentals`, `Furniture & Household Items`, `Housing Assistance`,
`General`) from the original 3-category curated data. It predates the 10-category
taxonomy (Decision 014) and never got the `taxonomy.json` rewiring
`validate_schema.py` received in D0.1, so it rejected every newer tag and the file
had been **failing validation (exit 1) unnoticed**.

**Structural finding that shaped the fix — `category_tags` drives nothing.**
Tracing both tag fields through the backend: `category_tags` → `NewsItem.tags`,
exposed on `/api/news` but rendered by no component and used in no aggregation.
The field that actually associates a news item with a category is **`resource_tags`**,
matched case-insensitively against `CategoryDefinition.matchNewsTags()` in
`CategoryService.matchesAnyTag()` to pick each category's `latestPolicyUpdate`. So
the validator had been policing the inert field with a stale vocabulary while the
functional field went unchecked.

**Three changes:**

1. **Vocabulary loaded from `taxonomy.json`** via `_load_taxonomy_tags()` (same
   pattern as `validate_schema.py`'s `_load_taxonomy()`). Allowed = display
   **labels** + canonical **subcategories** + `General`, so an item can be filed at
   category or topic level. **Raw source category strings are deliberately NOT
   allowed** — "Housing Assistance" is DSCYF directory vocabulary, not a
   user-facing label.
2. **`taxonomy.json` gained `matchNewsTags` per category** (`housing`, `food`,
   `healthcare`, `employment`, `utilities`, `legal`; empty for the other four),
   mirroring `CategoryDefinition.java` exactly as `matchCategories` already does.
   This is what lets a Python validator check a rule that previously existed only
   in Java. The file's compact formatting was preserved (line-targeted insert, not
   a JSON rewrite).
3. **New reachability WARNING**: if none of a record's `resource_tags` match any
   category's `matchNewsTags`, the item loads fine and is simply never surfaced
   anywhere — a silent failure no other check could catch.

**Data normalized (8 lines in `news.json`, formatting untouched):** Housing
Assistance→Housing (×5), Food Access→Food (×2), Utility Assistance→Utilities,
Dental Assistance + Medical Assistance→Health, and `Free` dropped (a cost, not a
category). Safe because nothing renders these tags.

**THE FINDING — 4 of 8 curated news items are unreachable** (the new warning fires
on each): NP-001, NP-002, NP-006 are housing news whose `resource_tags` are
fine-grained slugs (`rental-assistance`, `vouchers`, `SRAP`, `WHA`) that never
include the literal token `housing`; **NP-008 tags `health` while the category
matches on `healthcare`**. `matchesAnyTag` is exact `equalsIgnoreCase` — no
substring, no stemming — so one token off means invisible. Live proof: Housing's
`latestPolicyUpdate` comes only from NP-005 (the one housing item that happens to
carry the token), and Health shows **none** despite having a Medicaid dental item.
**Not fixed here** — it is a live-behavior change, and there are two candidate
paths: widen `matchNewsTags` (code, also affects RSS classification) or add the
category token to each item's `resource_tags` (data only). Left for the user.

**Verification:** `validate_news.py` → **exit 0**, 8/8 passed (was 8/8 FAILED).
127 backend tests green. Live (Docker rebuild): `/api/home` identical — same three
categories carry a `latestPolicyUpdate` (housing, food, utilities), 236 category
resource counts unchanged; `/api/news` returns the normalized tags. The other two
validators still exit 0.

Noted, not acted on: `taxonomy.json` now has **four** separate loaders
(`validate_schema.py`, `validate_news.py`, `validate_navigation.py`,
`enrich_resources.py`), each a handful of lines. A shared module would be DRYer;
refactoring three working scripts was out of scope for this cleanup.

# Decision 031

**`category_tags` is now the authoritative field for news categorization
(user's architectural call).** Decision 030 surfaced that 4 of 8 curated news items
reached no category, and offered two fixes — widen the matching rules or add
category tokens to `resource_tags`. **The user rejected both**: the mismatch is a
prototype artifact, not a data problem. `category_tags` **is** the editorial
classification of a NewsItem and should drive navigation; `resource_tags` stays
descriptive metadata for search, filtering and AI retrieval and must not be
overloaded with category meaning.

**Three distinct concepts in the V2 CivicContent model, one field each:**

| Field | Purpose |
|---|---|
| `category_tags` | Editorial classification and navigation |
| `resource_tags` (future: `tags`) | Search, filtering, AI retrieval |
| `status` / `expirationDate` | Content lifecycle |

**Backend refactor (blast radius was small — `matchNewsTags` had exactly ONE
usage):**

- **`CategoryDefinition`**: `matchNewsTags` → **`matchCategoryTags`**, repopulated
  with **editorial display-cased values** (was lowercase machine tokens). Each
  category holds its own label plus any alias an upstream source emits: **health =
  `["Health", "Healthcare"]`** because RSS-classified legislation says "Healthcare"
  where the taxonomy says "Health". The four categories that previously had an
  EMPTY list (clothing, community-events, furniture-household, community-support)
  now carry their label, so they can surface a policy update if one is ever filed
  under them.
- **`CategoryService`**: matches `n.tags` (category_tags) instead of
  `n.resourceTags`. The `matchNewsTags().isEmpty()` short-circuit was **removed** —
  no list is empty anymore, so it was dead code. (Correcting the old annotated
  note while removing it: it claimed an empty list would "vacuously match every
  news item," which is backwards — the inner loop never runs, so nothing matches.
  The guard was only ever an efficiency nicety.)
- **`UpdateItem` gained `categoryTags`**, populated from `n.tags` for news and
  `null` for flyers — a Flyer has no editorial classification field, and its own
  `tags` are content descriptors, so promoting them would re-introduce exactly the
  conflation this decision removes. **No grouping behavior changed**; this carries
  the classification through so the Weekly Updates page (Slice H) can group
  server-side without another DTO change.
- **`taxonomy.json`** mirrors the rename and its note now states plainly that
  `resource_tags` are never used for categorization.
- **`validate_news.py`**: the reachability warning added in Decision 030 was
  **retargeted onto `category_tags`** (warning on the wrong field would have been
  worse than not warning). Aliases are accepted as valid `category_tags`. The
  `resource_tags` check is now just a type/emptiness check with a comment saying
  why there is no vocabulary to check it against.

**Behavior change — the defect is fixed by design, not by patching data.** Live
`/api/home`: **health now shows NP-008** (Medicaid dental — previously none), and
housing resolves to **NP-006** rather than NP-005 because all four housing items
are finally eligible and the most recent wins. Food (NP-003) and utilities (NP-004)
unchanged. All 8 curated items are now reachable. `/api/updates` carries
`categoryTags` for news (RSS items included — one live bill shows
`["Healthcare", "Benefits", "Legal"]`, exercising the alias path) and `null` for
flyers.

**Verification:** **132 backend tests green** (127 + 5 new). New tests lock in the
contract: `shouldIgnoreResourceTagsWhenAssociatingNewsWithCategory` (an item with
`resourceTags=["housing"]` but `category_tags=["Food"]` must NOT reach housing),
`shouldMatchCategoryTagAliasWhenSourceUsesDifferentLabel`,
`shouldReturnNullPolicyUpdateWhenOnlySubcategoryTagIsPresent`,
`shouldCarryEditorialCategoryTagsForNewsItems`, `shouldLeaveCategoryTagsNullForFlyers`.
All four validators exit 0. Live verified via Docker rebuild. Annotated mirrors
synced: `CategoryDefinition_annotated.java`, `CategoryService_annotated.java`,
`UpdatesService_annotated.java`.

**Left unmapped deliberately:** RSS emits `Disability`, `Benefits` and
`Delaware Legislation`, which match no category. They only affect future Weekly
Updates grouping, never category pages (`CategoryService` reads
`newsService.getAll()`, which is curated news only — RSS reaches `UpdatesService`
alone). **Observed while verifying, flagged not fixed:** `classifyLegislation`'s
keyword matching is greedy — a wetlands bill came back tagged
`["Housing", "Food", "Utilities", "Benefits", "Legal"]`. Slice H will inherit that
noise if it groups RSS items by category.

**TECHNICAL DEBT ITEM (user-directed, do AFTER Slice F):** extract a shared
taxonomy loader. `taxonomy.json` now has four small, stable, independently
exercised loaders (`validate_schema.py`, `validate_news.py`,
`validate_navigation.py`, `enrich_resources.py`). Deliberately left alone to avoid
mixing an architectural refactor into functional work. A second, smaller item:
rename `resource_tags` → `tags` in the CivicContent model.
# Decision 032

**Slice F1 — the CivicContent contract is formalized, and `taxonomy.json`
finally becomes the single source of truth it has claimed to be since Decision
027.**

**The user's architectural direction, stated once so later slices can cite it:**
every CivicContent object — Resource, News item, Flyer, Expert content, Law, or
whatever comes next — **must answer the same set of questions with the same
fields**. That uniformity is what lets search, navigation, AI guidance, category
pages and the future mobile app treat all civic information consistently instead
of carrying a branch per type.

| Question | Field |
|---|---|
| What kind of content is this? | `contentType` |
| What is it about? | `category_tags` + `subcategory` |
| How can it be found? | `tags` |
| Where did it come from? | `contentSource` |
| Who is it for? | `communityId` |
| When is it relevant? | `publishDate`, `expirationDate`, `status` |

Three rules follow from it, and they govern everything after this decision:
**Category and Subcategory are editorial classification and drive navigation.
Tags are descriptive metadata and drive search, filtering, AI enrichment and
related-content discovery — tags must NEVER determine navigation. Content Type
determines how content is presented, not where it appears in the hierarchy.**

## The conflation this fixes

Tracing the contract through the code turned up the concrete defect it exists to
prevent. `JsonNewsRepository` was loading news.json's **editorial**
`category_tags` into `CivicContent.tags` — the same inherited field that holds
**descriptive** metadata on Resource and Flyer:

```java
item.tags = node.get("category_tags");   // one field, two meanings
```

So `tags` meant "which category is this" for a NewsItem and "what words describe
this" for everything else, and every consumer had to know which type it held
before it could read the field. Decision 031 saw the symptom (news items
unreachable from their categories) and fixed the routing; F1 removes the cause by
giving the two concepts two fields. This also closes 031's logged tech-debt item
(`resource_tags` → `tags`) as a side effect rather than a separate rename.

Correct mapping now: `category_tags` → `categoryTags` (editorial),
`resource_tags` → `tags` (descriptive).

## `taxonomy.json` is now actually loaded

`CategoryDefinition.ALL` — a hardcoded ten-entry list in Java, hand-mirrored
against `taxonomy.json` — **is deleted**. New **`TaxonomyService`** loads the
file and serves the vocabulary; `CategoryDefinition` is now just the record
Jackson binds each entry to. Decisions 030 and 031 had each paid the cost of the
duplication by hand; a source of truth nothing consumes is a comment.

Cheaper than expected: `taxonomy.json` already carried `matchCategories`,
`matchCategoryTags` and `subcategories` for all ten. Only **`icon`** had to be
added (10 lines). The file now also holds `subcategories` where the Java constant
never did — which is why the backend could not offer a topic level before now.

**Loaded in the constructor, not on `ApplicationReadyEvent`** like the content
repositories. Content can load whenever; *vocabulary must exist before anything
that uses it*. Constructor loading makes it a Spring dependency, so ordering is
enforced by the container rather than by listener luck — which matters because
F2's classifier will normalize resources *through* the taxonomy at load. **A
missing taxonomy is fatal**, unlike a missing content file: ten silently empty
categories rendering a plausible-looking homepage is far more expensive to
diagnose than a startup failure.

## Flyers classify like everything else; `includesFlyers` is gone

Flyers were the only content type with **no editorial classification at all**.
They reached a category through a hardcoded boolean:

```java
List<Flyer> matched = definition.includesFlyers() ? flyers : List.of();
```

Every flyer into Community Events regardless of subject — so the eviction-rights
session, the health fair and the furniture giveaway all filed under events, and a
tenant browsing Housing for eviction help found nothing. **`flyers.json` gained
`category_tags` + `subcategory` on all 7 records; `includesFlyers` is deleted.**
The Flyer *class* needed no new fields — both are inherited from the contract —
which is the contract paying for itself.

**The user's catch:** asked whether I had checked `images/seasonal/`. I had not.
`Eviction Help.png` is FL-002, so **housing ▸ Eviction Prevention is not an empty
topic** as I had claimed. Nor is Utilities empty: NP-004's `category_tags`
include `Utilities`. Both "dead spots" from the F-scope were wrong.

FL-002 also proves the model — `Eviction Prevention` is declared under **both**
housing and legal, so one `subcategory` value correctly places it under both.
FL-005 strains it: genuinely Legal (Disability Advocacy) *and* Community Support
(Information & Referral), but `subcategory` is singular, so Disability Advocacy
was chosen as primary. A multi-valued subcategory was rejected — it changes the
contract for all five types and 229 resources to serve one flyer; the enrichment
relationship graph is the intended answer to "this also relates to that".

**Two flyer subcategories are judgment calls worth a second look:** FL-004
(Back-to-School Supply Drive) → `Education & Training`, and FL-007 (Free
Furniture Giveaway) → `Starter Kits`.

## RSS: LAW is a content type, not a category

`RssFeedService` now sets `contentType = LAW`. Legislation classifies into the
ordinary taxonomy — a housing bill is Housing content, on the Housing page —
while still rendering with its own treatment. **The general rule: if a proposed
category answers "what format is this?" rather than "what is this about?", it is
a content type.** This is why `contentType` is a per-instance *field* rather than
an abstract method: a Law is a `NewsItem` with the same fields and behavior, so a
`LawItem` subclass would exist only to return its own type tag.

**The `Healthcare` alias was removed** from health's `matchCategoryTags`.
Decision 031 added it so the taxonomy could absorb the RSS classifier's drifted
vocabulary; that direction is how vocabularies rot — each integration widens the
lists until "canonical" means "whatever anyone has ever emitted". Normalization
now happens **at the source**. Matching is case-insensitive (a casing slip is a
typo) but not fuzzy (a different word is a different vocabulary).

**Known gap carried deliberately into F2:** the classifier still emits
`Healthcare`, `Disability`, `Benefits` and `Delaware Legislation`, and has no
keywords for four canonical categories. Safe because `CategoryService` reads
curated news only — RSS reaches `UpdatesService` alone, so no category page is
affected. Also confirmed the cause of 031's greedy-matching observation:
`text.contains(kw)` is **raw substring, no word boundary**, so `"aid"` matches
*said*/*paid* and `"care"` matches *careful*. Word-boundary matching plus the
canonical mapping both belong in F2's `shared/classification/`
(`CivicContentClassifier`, `CategoryClassifier`, `TagClassifier`), after which
`RssFeedService` extracts content and does not decide categories.

## `validate_navigation.py` was implementing the countermanded rule

`count_topics()` credited a topic when **any descriptive tag matched a topic
name**, and `count_flyer_tags()` credited flyer topics from free-form tags. That
is metadata driving navigation, it inflated counts so topic counts did not sum to
category counts, and it made the empty-topic warning unreliable. Both now use
`subcategory` only; `count_flyer_tags()` → **`count_flyer_topics(label_to_key)`**
returning `(category_key, topic)` tuples like `count_topics()`.

## Verification

**155 backend tests green** (was 132). New: `TaxonomyServiceTest` (13, run
against the REAL `taxonomy.json` so they fail if the file drifts) and
`CivicContentTest` (5, locking the contract). Changed tests that encode reversed
decisions: `shouldMatchCategoryTagAliasWhenSourceUsesDifferentLabel` →
`shouldNotMatchNonCanonicalCategoryLabel` (plus a case-insensitivity test),
`shouldIncludeFlyersInCommunityEventsCount` →
`shouldCountFlyersMatchingTheirEditorialCategoryTags`,
`shouldLeaveCategoryTagsNullForFlyers` →
`shouldCarryEditorialCategoryTagsForFlyers`. Added
`shouldPlaceFlyerInEveryCategoryItIsEditoriallyClassifiedUnder` and
`shouldNotCountFlyerWithNoEditorialClassification`.

**All four validators exit 0.**

**Live (Docker rebuild)** — counts moved exactly as predicted by the flyer
reclassification: community-events **60 → 54** (53 resources + FL-001 only),
housing **44 → 45**, legal **3 → 5**, health **32 → 33**, furniture-household
**6 → 7**, community-support **58 → 61**; food/clothing/employment/utilities
unchanged. **229 resources + 9 flyer placements = 238.** `latestPolicyUpdate`
unchanged for every category (housing NP-006, food NP-003, health NP-008,
utilities NP-004), confirming the news path did not regress while the flyer path
changed. `/api/updates` now returns `categoryTags: ["Housing","Legal"]` for FL-002
where it previously returned `null`. A resource round-trips with
`contentType: "RESOURCE"` and its inherited `subcategory`.

**Deliberate seam — resources are the one type NOT yet carrying canonical
`categoryTags`.** `Resource.category` still holds the raw DSCYF string and
`CategoryService` still translates it through `matchCategories`. Normalizing that
is classification, and F2 does it for every source at once; doing it inline here
would mean writing a second classifier that F2 deletes.

Also unchanged: `CategorySummary.resourceCount` still counts resources + flyers,
not news. The direction that **topic pages count all classified CivicContent**
applies to the topic/category BFF endpoints, which land in F3/F4.

Annotated mirrors synced: `CivicContent`, `ContentType` (new), `TaxonomyService`
(new), `CategoryDefinition`, `CategoryService`, `Resource`, `NewsItem`, `Flyer`,
`ExpertAnswer`, `FAQ`, `JsonNewsRepository`, `RssFeedService`, `UpdatesService`,
`validate_navigation`.

**Revised Slice F plan** (was four sub-slices; the contract and classifier work
made it six): **F1 contract + taxonomy loading (this decision)** · F2
`shared/classification/` + canonical RSS · F3 `NavigationService` + counts · F4
`/api/category/{key}` BFF · F5 CategoryPage · F6 `/category/:key/:topic` +
shared ContentCard. The **cross-category relationship graph** — an Enrich-stage
product analyzing canonical categories, subcategories, tags and semantic
similarity, persisted as metadata so it stays deterministic and cheap to serve —
is scoped **after** F6: it is additive to the pages, not load-bearing for them.

# Decision 033

**Slice F2 — a shared classification engine. Every CivicContent source now
classifies into the canonical taxonomy through one implementation, at ingestion.**

**The user's direction:** don't fix individual callers, and don't just swap
`text.contains()` for a word-boundary regex. Introduce a `shared.classification`
package where `CategoryClassifier` determines canonical category and subcategory
and `TagClassifier` assigns descriptive tags, both operating on tokenized text.
Move the keyword vocabulary into `taxonomy.json`. After F2, `CategoryService`
operates purely on the unified domain model.

## Taxonomy corrections (Part 1)

Two subcategory changes, **organizing around resident needs rather than
organizational service terminology**:

- `furniture-household`: **`Starter Kits` → `Furniture & Household Goods`** —
  better represents the actual range (ReStore, Catholic Charities, furniture
  banks) and matches the words residents use. Affected FH-005 and FL-007.
- `community-support`: **new `School Supplies`** — a supply drive is recurring
  seasonal assistance, not an educational service. FL-004 moved off
  `Education & Training`. `community-support` is grouped, so `navigation.json`
  also gained it under **Family and Children** (an unplaced topic fails
  `validate_navigation.py`).

**FL-005 stays `Legal` ▸ `Disability Advocacy`, singular.** Its Community Support
relevance lives in descriptive tags and, later, the relationship graph. A
multi-valued subcategory was rejected: it complicates navigation, validation and
counting platform-wide to serve a minority of items.

## The engine

`shared/classification/` — `Tokenizer`, `Classification`, `CategoryClassifier`,
`TagClassifier`, `CivicContentClassifier`.

**Tokenization, not regex.** Whole-word matching is now *structural* — there is
no way to express a substring match through the API, so the bug cannot return via
a caller who forgets a `\b`. Phrases match only contiguously, and weigh their
token count (a two-word phrase is stronger evidence than a single word).

**`singularize()` was necessary and is deliberately not a stemmer.** Real text
says "TENANTS AND LANDLORDS"; the taxonomy says "tenant". Without normalization
the vocabulary would need every word twice. Rules are conservative with explicit
guards (`gas`, `bus`, `business`, `status`, `analysis` untouched) because
over-stemming manufactures false matches — trading a substring bug for a stemming
bug is not progress. Applied to keywords *and* text so the two meet in the middle.

**Two tiers, and `matchCategories` survived.** The brief said legacy translation
could be removed; that was right about the **query layer** and wrong about the
**mapping table**, so the two were separated. `CategoryService`'s request-time
filtering is gone. `matchCategories` was promoted to **tier 1**: a hand-curated
mapping of a known upstream vocabulary with 100% coverage of all 229 resources.
Replacing a correct deterministic mapping with keyword guessing would trade a
right answer for a likely one. **"Legacy" describes where code lives, not whether
it is correct.** Tier 1 short-circuits — a mapped resource is never keyword-scored.

**Tier 2 scoring:** `MIN_SCORE = 2` (one stray word is never enough) plus a
`RELATIVE_FLOOR = 0.5` — a category is kept only if it scores at least half the
leader. Chosen over a hard "max 3" cap, which is worse in both directions: it
truncates a genuinely four-category item *and* still admits three bad matches for
an item deserving none. A relative floor scales with the evidence.

**Confidence is used, not stored.** It is not a field on `CivicContent` — that
would be a category error (confidence is a property of the *act* of classifying,
and a hand-authored classification has no meaningful confidence value). It gates
the threshold and drives a startup summary.

## The classification policy (user's formulation, adopted verbatim)

> The classifier only classifies when editorial classification is absent.
> Hand-authored editorial classifications are authoritative and **immutable
> during ingestion**. Automated classification exists **solely to normalize
> unclassified content.**

"During ingestion" is precise — an editor changing the data file is exactly how
classification *should* change; the *pipeline* must not. **The rule applies per
FIELD, not per item**: NP-001 has `category_tags` but no `subcategory`; per-item
logic would leave it permanently topic-less.

**The corollary was verified empirically, not just asserted.** Mid-slice the
keyword vocabulary was tuned (legal and community-support gained terms) and the
live category counts **did not move by a single record** — 238 before, 238 after
— because every editorially-placed item is immutable. Only previously
unclassified RSS bills moved (134 → 175 of 428). *Changes must result from
intentional editorial decisions, never from classifier behavior.*

## RSS stops classifying

Deleted from `RssFeedService`: `TAG_KEYWORDS`, `TAG_WHY`, the private
`Classification` class, `classifyLegislation()` — ~90 lines, replaced by
`classifier.classify(item)`. Called **before** the "RELATING TO" title rewrite so
the classifier sees raw bill text.

`WHY_BY_CATEGORY` stayed: it is law-specific **editorial copy**, not taxonomy
vocabulary — a sentence about what a new law means for a resident makes no sense
on a Resource. Re-keyed onto canonical labels, with entries added for the four
categories that previously had none.

**`"Delaware Legislation"` is gone as a category tag.** It described a content
TYPE, and `ContentType.LAW` expresses that properly, so an unclassifiable bill
now carries **empty `categoryTags` + `contentType: LAW`** — honestly
uncategorized rather than filed under a pseudo-category.

## A bug I introduced and caught during live verification

Tier 1 initially returned the raw source category as `evidence`, which
`TagClassifier` then merged into descriptive tags — pushing **"Housing
Assistance" / "Before/After School Care" into `tags` on all 229 resources**. A
category name in the one field that must never hold one, plus DSCYF vocabulary
polluting search. Tier 1 now returns **empty evidence**; provenance is already on
`Resource.category`. Locked in by
`shouldNotLeakUpstreamSourceVocabularyIntoDescriptiveTags`.

## Verification

**205 backend tests green** (was 155), clean build. New: `TokenizerTest` (16),
`CategoryClassifierTest` (14), `CivicContentClassifierTest` (14),
`TagClassifierTest` (7). `ClassifierFixture` wires a *real* classifier to the
real taxonomy — a mock would make every repository test pass whether or not
classification works.

**Test-fixture change worth noting:** `CategoryServiceTest` and
`CategoryControllerTest` build `Resource` objects directly, bypassing ingestion,
so they now classify in the helper — which keeps them covering the seam (a raw
directory string still reaches the right category) rather than hand-setting
canonical tags.

**All validators exit 0. Frontend 20 green.**

**Live (Docker), the two-part invariant both halves holding:**
- **Category counts frozen:** housing 45 · food 12 · clothing 15 · health 33 ·
  employment 6 · utilities 0 · legal 5 · community-events 54 ·
  furniture-household 7 · community-support 61 = **238**, identical to F1 and
  identical again after keyword tuning.
- **Topic counts: exactly the three intentional moves** — `Starter Kits` 2→0,
  `Furniture & Household Goods` 0→2, `Education & Training` 9→8,
  `School Supplies` 0→1. No fourth change.

**Defects fixed, measured on 428 live bills:** no `Healthcare` / `Disability` /
`Benefits` / `Delaware Legislation` anywhere; max categories on one item **4**
(was 5+), and that one plausibly is four-topic; the four previously unreachable
categories now classify (Community Support 44 bills, Community Events 2).
`SD-004` returns `category_tags: ["Community Support"]` with clean tags.

**Honest cost:** 253 of 428 bills unclassified. Many genuinely aren't
civic-assistance content (pet stores, the state flag). Some are misses — "Relating
to the Court of Chancery" scores 1 on "court", below `MIN_SCORE`. Lowering the
threshold is precisely how the five-category wetlands bill happened. Declining is
the deliberate trade; the startup summary exists so the vocabulary improves with
evidence — that is how the mid-slice tuning pass was diagnosed.

Architecture docs updated: `04-editorial-principles.md` (classification section),
`01-domain-model.md` ("when to introduce a domain class" — *different behavior,
not different data*; a Law does not qualify), `02-information-flow.md` (Stage 4
Enrich, with the relationship graph named as the next Enrich product).

Annotated mirrors: 5 new + `CategoryService`, `CategoryDefinition`,
`RssFeedService` and all 5 repositories. **Pre-existing gap noted, not fixed:**
`JsonResourceRepository_annotated.java` omits a 12-line validation method — it
predates F2 (verified against HEAD) and expanding scope to fix it was out of
scope for this slice.

**Known wrinkle:** `shared.classification` depends on
`category.service.TaxonomyService`. The vocabulary is really shared
infrastructure that lives in the category package for historical reasons; moving
it was churn F2 did not need. Recorded rather than hidden.

**Next: F3** — `NavigationService` (groups + topics + counts by editorial
classification only), then F4 `/api/category/{key}` BFF, F5 CategoryPage, F6
topic route + shared ContentCard. The relationship graph follows F6.

# Decision 034

**Slices F2.1 + F3 — source adaptation leaves the taxonomy, relevance becomes an
explicit admission decision, and `NavigationService` lands as a read model.**

## The governing principle, now written down

> **Classification is an ingestion concern, not a query concern.** By the time
> content becomes CivicContent, its editorial classification has already been
> determined — allowing every downstream service to operate on a unified domain
> model instead of reinterpreting source-specific data.

Three things in the codebase are that principle applied, which is why the doc
names them: F2 removed `CategoryService`'s request-time translation; F2.1 moved
source adaptation into the engine; F3's read model is only *possible* because of
it. All three are in `03-application-architecture.md`, alongside the engine's
**four responsibilities** (adapt source vocabularies · determine editorial
classification · generate descriptive tags · determine relevance), applied
consistently to every ingestion source. **Confidence is the measure supporting
the fourth, not a fifth responsibility** — which is why it lives on
`ClassificationResult` and never on `CivicContent`.

## Part A — `matchCategories` was a source adapter in the wrong file

**The user's call:** `matchCategories` is a deterministic source adapter and
belongs in the classification engine, not the canonical editorial taxonomy.

`taxonomy.json` was carrying `"Housing Assistance"`, `"Before/After School Care"`,
`"Early Childhood/Pre-K"` — **DSCYF's words in the file that defines what First
Step's categories are.** Moved to new **`app/data/source-mappings.json`**, loaded
by **`SourceMappingService`**. Same reasoning as Decision 029's
taxonomy/navigation split: two artifacts because two lifecycles and two owners.
An editor changes the taxonomy; adopting a provider changes the mappings.

**Keyed by source**, not flat, which buys two things the moment a second provider
exists: **provenance** (a merged flat list loses whose word was whose) and
**correctness** (two providers can use the same string for different things; a
flat map forces one to win silently). The cost is that content must carry its
source identity — read from **`ContentSource.id`**, a field the model has always
declared for exactly this and never populated. `JsonResourceRepository` stamps
`"dscyf-directory"` at load. The alternative, a `sourceId` parameter on
`classify()`, would have five of six ingestion points passing null forever.

**A missing mappings file is NOT fatal**, unlike a missing taxonomy. The rule
generalizing both: *fail fast when the alternative is a plausible wrong answer;
degrade when the alternative is a less precise right one.*

`taxonomy.json` now contains **no vendor vocabulary at all** — the test of
whether the split was drawn in the right place. Three Python scripts repointed.

## Part B — Relevance, and `ClassificationResult`

**First Step is not a legislative tracker.** Of 428 signed Delaware bills, ~175
are civic information a resident might need; the rest are pet stores, animal
cruelty and the state flag. All 428 previously entered and flowed into
`/api/updates`.

`Classification` → **`ClassificationResult`**, gaining `relevant` and `reason`.
**Both collections stay ordered Lists** (per the user): `categoryTags` because
multi-category is intentional — `Eviction Prevention` is under both Housing and
Legal — and `tags` because tag order is an editorial decision.

**Relevance is set by the engine, never inferred by a caller.** No
`RelevanceAssessor` class — a service whose body is `!categoryTags.isEmpty()` is
ceremony — but the concept is *visible on the result* rather than re-derived.
Every caller could compute it correctly today; it is forbidden because a business
question answered at six ingestion points will eventually be answered six ways.
Verified by grep: no caller reads `categoryTags` to decide admission.

The record stays **immutable** despite the sketched `setRelevant(...)`: a
record's canonical constructor *is* "set it while producing the result", and the
requirement was that the engine decide, not that the field be mutable.

`reason` is what makes conservative-by-design auditable —
`"evidence below threshold (score 1 < 2)"` is a vocabulary task,
`"no category keywords matched"` is a different one; without it both look
identical from outside.

**Two subtleties, each with a test:** content an editor already placed returns
`editorial()` without re-judgement, and an item with `category_tags` but no
`subcategory` (curated news) is **still relevant** — relevance is about
*admission*, not completeness. Getting that backwards would have dropped every
curated news item.

### The rotator stays independent

`RssFeedService` now implements two interfaces: **`RssFeedSource`**
(relevance-gated → discovery) and new **`SignedLegislationSource`** (all bills →
presentation). `LegislationService` reads the second, so the "New Delaware Laws"
rotator still shows what the Governor signed — **including "Pet Stores and Animal
Welfare"**, verified live. Before the split, one accessor served both, so adding
a relevance gate would have silently emptied the rotator of half its content: a
presentation feature broken by a discovery decision, invisibly (seven bills would
still appear, just a different seven).

The test suite makes the split legible: the fixtures that broke were all *title
extraction* tests, which now read `getSignedBills()`; only the two classification
tests read the gated feed.

## Part C — Two principles, one of them enforced

**Conservative by design** — prefers leaving content unclassified over assigning
an incorrect category; accuracy improves through vocabulary, never through lower
thresholds. *A resident who finds nothing has a gap; a resident who finds the
wrong thing has been misled, and stops trusting the rest.*

**The Editorial Stability Invariant is now a test, not prose.**
`EditorialStabilityTest` pins per-category counts for both protected classes —
hand-authored `category_tags` **and** deterministic source mappings — and asserts
every resource is placed by mapping rather than inference.

**Negative-tested, and the result is the argument for the whole invariant.**
Removing one mapping did not make 37 housing resources disappear; it **silently
redistributed them** into community-support 61→76, health 33→37, clothing 15→16,
housing 45→30. Plausible-looking and completely wrong. Both the Java test and
`validate_schema.py` failed loudly with actionable messages.

The invariant deliberately does **not** freeze automatically classified content —
freezing that would forbid the engine from improving.

## Part D — F3: `NavigationService`

**A read model, not a business model** (user's framing). It reads exactly two
fields — `categoryTags` and `subcategory` — and never consults text, tags,
keywords or content type. Handed an unclassified item it counts nothing.

There is no fallback, deliberately: a fallback would be an editorial rule wearing
a convenience disguise, and "where does this appear?" would have two answers in
two places. `shouldNotClassifyUnclassifiedContent` pins it with a resource
deliberately stuffed with housing language;
`shouldNotUseDescriptiveTagsToPlaceContent` covers the subtler version.

Loads `navigation.json` (first Java reader). **Decision 029's "a category absent
from navigation.json renders a flat topic list" is now structural** — `build()`
returns groups or flat topics, never both. Counts cover **all** classified
CivicContent with a per-type breakdown, and topic counts are **scoped to their
category**, so `Eviction Prevention` counts correctly under Housing and Legal
independently. Empty topics are returned rather than hidden — suppressing them
would conceal exactly what `validate_navigation.py` exists to surface.

**No endpoint** (F4), and `/api/home` untouched — which is what keeps the
Editorial Stability Invariant trivially checkable across this slice.

## Verification

**225 backend tests green** (was 205), clean build. All validators exit 0.

**Live (Docker):**
- **Category counts frozen at 238** — housing 45 · food 12 · clothing 15 ·
  health 33 · employment 6 · utilities 0 · legal 5 · community-events 54 ·
  furniture-household 7 · community-support 61. Moving `matchCategories` to a
  source adapter is a refactor; nothing moved.
- **Rotator independence:** `delawareLaws` returns 7 bills including
  "Pet Stores and Animal Welfare" and "Animal Cruelty".
- **Relevance gate:** `/api/news/rss` carries 175 bills, **0 unclassified**.
- **Startup:** taxonomy (10 categories, 45 subcategories) · source mappings
  (1 source, 24 mappings) · navigation (2 grouped categories) · "175 of 428 bills
  admitted as CivicContent".

**Process note, recorded because it nearly cost real work:** a sync script
written as `open(path,'w').write(build(path))` truncated five annotated mirrors —
Python opens (and empties) the file before evaluating the argument that reads it.
Recovered from HEAD; the fix is to build all content first and write only after
every transform succeeds. Mirrors are now machine-verified against production
(19 files, 0 drift).

**Next: F4** — `GET /api/category/{key}` as a thin BFF over
`NavigationService.getByKey()`, then F5 CategoryPage, F6 topic route + shared
ContentCard. The relationship graph follows F6.

# Decision 035

**Slice F4 — `GET /api/category/{key}`, the category page's BFF.** A thin
pass-through over the F3 read model, plus one finding the endpoint made visible
that F5 has to answer.

## The endpoint

```
GET /api/category/{key}?communityId=…  →  ApiResponse<CategoryNavigation>
```

Added to the existing `CategoryController`, which now owns the whole
`/api/category*` URL family: `/api/categories` serves the homepage's discovery
tiles, `/api/category/{key}` serves the category page.

**Three "don't build it yet" calls, all the same rule — an abstraction needs a
second use before it earns its name:**

1. **No `CategoryPageService`.** The BFF pattern says a page gets one
   page-shaped endpoint so the client stays a display layer; it does not say
   every endpoint needs a service. `NavigationService` *is* the aggregator, so an
   intervening service would forward a call and nothing else. `HomeService`
   earns its existence by composing five aggregators plus static config — here
   there is one source and the composition step is empty.
2. **No `CategoryPagePayload` wrapper.** `CategoryNavigation` is already exactly
   the page: key, label, icon, totalCount, countsByType, groups, topics. A
   one-field wrapper deepens the JSON for nothing. Adding it later, when a second
   top-level field exists, is a small honest change.
3. **No `NavigationController`.** Rejected on two grounds: the codebase's
   convention is one controller per URL family (`ResourceController` owns
   `/resources`, `/health`, `/seasonal-images`), and `navigation` is the read
   model's package — F3 kept delivery concerns out of it deliberately.

**404, not an empty payload, for an unknown key.** `getByKey` returns
`Optional.empty()`; the controller throws `NotFoundException`, matching
Resource/Flyer/Expert controllers. Returning an empty `CategoryNavigation` would
make `/category/hosuing` render a real-looking, permanently empty page. This is
the mirror image of `TopicNavigation`'s rule that empty topics are *returned*
rather than hidden — both follow from **never let "nothing here" and "no such
thing" look alike.**

## The finding: ~47% of category content has no topic

The endpoint reports `totalCount` and per-topic counts side by side, and the two
do not meet:

| Category | total | reachable via topics | no topic |
| --- | --- | --- | --- |
| housing | 73 | 45 | **28** |
| health | 84 | 33 | **51** |
| legal | 41 | 5 | **36** |
| utilities | 22 | 0 | **22** |
| community-support | 105 | 60 | **45** |

Cause, confirmed directly: **all 175 admitted RSS bills carry a category and no
`subcategory`** (`/api/news/rss`: 175 admitted, 0 with a subcategory), while all
229 resources have one. That is not a defect — it is the designed consequence of
the earlier decision to put **category-level keywords only** in `taxonomy.json`.
The classifier has no subcategory-level evidence to work from and, being
conservative by design, assigns nothing rather than guessing.

Verified there is **no navigation reachability gap**: both grouped categories
cover their taxonomy subcategories exactly (housing 9/9, community-support
12/12), and no topic count exceeds its category total anywhere.

**This is an editorial question, and F5 must not answer it by accident.** Three
options, in the order I'd rank them:
1. The category page shows topic tiles **and** a recent-content list scoped to
   the category, so untopiced content is reachable without inventing a topic.
2. An explicit "More in this category" bucket for items with no subcategory.
3. Accept it — laws reach residents through Important Notices (Slice H), and
   category topics are for resources.

Whichever is chosen, the rule from F3 holds: **NavigationService must not infer a
topic for this content.** The fix, if one is wanted, is subcategory-level
vocabulary in the taxonomy — an editorial change, not a classifier change.

## Verification

**228 backend tests green** (was 225), clean build. Three new controller tests:
full page shape in one response, 404 on an unknown key, `communityId` passed
through to the read model. The read model is mocked there on purpose — routing
and envelope are the controller's job; aggregation is `NavigationServiceTest`'s,
against real data.

All three validators exit 0. **20 frontend tests green** (untouched — no
frontend work in F4; TypeScript types land with the page in F5 rather than
sitting unused now).

**Live (Docker):**
- All 10 category keys resolve; unknown key → 404 with the standard envelope.
- **Editorial Stability Invariant holds — 238**, unmoved.
- Grouped vs flat is correct end to end: housing returns 2+ groups and an empty
  flat list, food returns 4 flat topics and no groups.
- Cross-check against the homepage: housing `RESOURCE 44 + FLYER 1 = 45`, food
  `RESOURCE 12` — the same numbers `/api/home` reports, from a different code
  path. The larger totals are the news, laws and expert content F3 added, exactly
  as that slice predicted.

**Next: F5** — `CategoryPage` replacing the stub at `/category/:key`, which needs
the untopiced-content decision above. Then F6 topic route + shared ContentCard;
the relationship graph follows F6.

# Decision 036

**Slice F5a — the category page becomes an aggregate read model. Coverage grows
by composition, not by inference.**

## The problem F4 exposed

Putting `totalCount` beside the per-topic counts showed they do not meet: housing
45 of 73, health 33 of 84, legal 5 of 41, utilities **0 of 22**. The cause is
structural, and measured:

| Content type | count | has `subcategory` |
| --- | --- | --- |
| Resource | 229 | **229** |
| Flyer | 7 | **7** |
| News | 8 | 0 |
| Signed legislation | 175 | 0 |
| Expert answers / FAQ | 12 | 0 |

**193 of 429 classified items carry a category and no topic.**

## The user's direction, and why it is the right call

> Do not attempt to increase topic coverage by making the classifier infer
> subcategories. That's a Version 3 feature. The classifier should remain
> conservative and only assign editorial classifications supported by the
> taxonomy.

Instead: **a category page serves two complementary purposes — helping residents
browse resources, and helping them understand what has changed.** The topicless
items are not a gap in the first purpose; they are the second. That reframing
turns a coverage problem into a page-design problem, and page design is where it
belongs.

The rejected alternative had already been priced. F2.1's negative test showed
what guessing costs: removing one source mapping silently redistributed 37
housing resources into three other categories — plausible-looking and completely
wrong. Loosening the classifier to fill topics would have been the same trade.

## Three vocabularies, now named (`01-domain-model.md`)

```
Taxonomy (Editorial)        Category  →  Subcategory     what First Step KNOWS
Navigation (Presentation)   Group     →  Topic           how residents DISCOVER it
Content                     CivicContent                 the things themselves
```

**A navigation Topic references an editorial Subcategory.** "Housing ▸ Rental
Assistance" is both at once *because navigation references it* — Topic is a
pointer into the taxonomy, not a fourth vocabulary. That is why
`validate_navigation.py` can check topics against the taxonomy and why
`NavigationService` counts a topic by reading `subcategory`.

It follows that **content with a category and no subcategory is fully
classified**, not half-classified.

## Three pillars

| Pillar | Question | Field |
| --- | --- | --- |
| **Discover** | What is available? | `groups` / `topics` |
| **Connect** | Where do I go or contact next? | `organizations` |
| **Stay Informed** | What has changed? | `updates` |

## Composition, with every service keeping its job

```
CategoryPageService
  ├── NavigationService.getByKey()          → metadata + groups/topics
  ├── UpdatesService.getForCategory()       → news + law + flyer + expert
  └── OrganizationService.getForCategory()  → orgs ranked within the category
```

**`NavigationService` is untouched — verified, not asserted.** `git diff` on the
whole navigation package is empty and its 14 tests needed zero edits. That was
the design constraint: composition happens one layer up so the read model never
learns about pages.

**`UpdatesService` gained the category-scoped merge rather than a second merger
being written.** Its javadoc has claimed since Decision 019 to be "the single
place cross-type merging happens", and contradicting a documented invariant to
save ten lines of loop is a bad trade. It gained `ExpertAnswerService`,
`FaqService` and `TaxonomyService`; the homepage feed is behaviourally unchanged
(still 8 items, still no expert content). The reuse that mattered was the private
`toUpdateItem` mappers — date selection and source/url resolution — not the loop.

**Resources are excluded from the feed**, which is the load-bearing exclusion: a
resource is a standing service, not an event, and excluding them is what makes
the two halves complementary rather than overlapping.

**`UpdateItem` gained `contentType`** so a page can badge a LAW differently from
curated NEWS — `type` reports "news" for both. Nothing is inferred; every
CivicContent subtype already knows its own type. **Recorded as debt:** `type` and
`contentType` overlap and `type` survives only because the shipped homepage reads
it. Slice H rebuilds that feed and is where they converge.

**The F2.1 feed split paid off again.** The category feed reads `RssFeedSource`
(gated, classified); `SignedLegislationSource` could not serve it even if asked,
because an unclassified bill has no category to be scoped to.

## When orchestration earns a service

F4 refused a `CategoryPageService` — one source, empty composition step, so it
would have forwarded a call. F5a built it — three sources, real composition. **The
rule did not change; the facts did.** Worth keeping as the worked example that
"no abstractions for single-use code" is a test rather than a taste.

Reshaping the shipped endpoint was safe because `/category/:key` is still
`StubPage`. After F5b it stops being free.

`CategoryNavigation` was **not** modified — reshaping the read model's contract to
suit a page is exactly the coupling this slice exists to prevent. `CategoryPage`
projects `metadata` + `groups` + `topics` as siblings rather than nesting the
whole record, which would have repeated key/label/icon/counts twice in one
payload.

`CategoryMetadata` has **no `description`** and `lastUpdated` derives from the
updates feed only — never `Resource.updatedDate`, a load-date proxy that must not
be shown as a freshness guarantee. Category descriptions are deferred to the
future Admin project, because `taxonomy.json` is an editorial artifact and its
prose is written by editors.

## Verification

**253 backend tests green** (was 228), clean build. All three validators exit 0.
**20 frontend tests green** with no frontend edits.

**The coverage identity, measured live across all ten categories —
`browse + topicless == totalCount`, exactly:**

| | total | browse | topicless | shown |
| --- | --- | --- | --- | --- |
| housing | 73 | 45 | 28 | 6 |
| health | 84 | 33 | 51 | 6 |
| legal | 41 | 5 | 36 | 6 |
| utilities | 22 | 0 | 22 | 6 |
| community-support | 105 | 60 | 45 | 6 |

**Utilities is the sharpest demonstration:** 0 resources and 0 topics — a
literally empty page before this slice — now carries 22 signed bills, none of
them placed by a guess.

**Live:** `/api/category/housing` returns all three pillars, with `RESOURCE 44 ·
NEWS 5 · FLYER 1 · LAW 20 · EXPERT 3` and an updates feed carrying all four
content types. Food returns flat topics and no groups; unknown key still 404s.
**Editorial Stability Invariant holds — `/api/home` still totals 238.**
`/api/updates` still returns 8 items with no EXPERT content.

## Exit criterion — Slice H retires `UpdateItem.type`

The `type` / `contentType` overlap introduced here is **temporary and time-boxed
to Slice H**, not an accepted permanent shape. Recording the end state now, so
the overlap cannot become permanent by nobody deciding:

> **Slice H retires `UpdateItem.type`. `contentType` becomes the single semantic
> identifier for CivicContent. Any presentation labels or badges are derived from
> `contentType` by the frontend.**

```
ContentType
  RESOURCE
  NEWS
  LAW
  FLYER
  EXPERT
```

The two fields are not equivalent and never were — `type` reports `"news"` for
both curated news and signed legislation, which is precisely the conflation
`contentType` exists to remove. Keeping both is a migration state: `type` is read
by the shipped homepage feed, and removing a field mid-slice would have broken a
working page to tidy a DTO.

**Why the labels move to the frontend.** `"news"`/`"flyer"`/`"expert"` are display
strings living in a domain DTO — a presentation decision the backend has no
business making. Once `contentType` is the only identifier, the backend states
*what a thing is* and the client decides how to render it. That is the same
separation the CivicContent contract already draws between `contentType`
(presentation) and `category_tags` (placement).

**Done means:** `UpdateItem.type` deleted; `UpdatesService`'s four mappers no
longer pass a literal; `frontend/src/types/api.ts` drops `type`; every consumer
branches on `contentType`; the F5a tests asserting `type` are updated rather than
deleted.

## Governance rule (new, set by the user with this decision)

> **Annotated mirrors must always match production before a slice is considered
> complete, but they do not have to be updated during unrelated feature work.**

This replaces "keep annotated reference copies synchronized immediately whenever
production code changes" in `CLAUDE.md`, which has been amended.

**The obligation is scoped to the files a slice touches.** Modify a source file
and its mirror must be in sync before the slice is done — F5a's seven were, and
were machine-verified. Pre-existing drift in files the slice did not touch is
**not that slice's blocker**; it is debt owned by whichever slice next touches
those files. Report it, do not fix it opportunistically.

The rule earns its place because the old one made mirrors a tax on all work
rather than a record of the work being done: any slice touching any file could be
held up by drift it did not cause and had no context to repair correctly.

**Known drift under the new rule, reported and deliberately not fixed:**

| Mirror | Gap | Owed by |
| --- | --- | --- |
| `DecisionAgentService_annotated` | missing the whole 199-line `decide()` | next AI slice |
| `FlyerService_annotated` | missing `getCarouselCards()` (Slice E) | next flyer slice |
| `HomeService_annotated` | missing 3 of 5 aggregators (Slices C/D/E) | next homepage slice |
| `ExpertAnswer_annotated` | import order only | next expert slice |

Verification is now mechanical: a script strips comments and blank lines from
both sides and compares. 79 mirrors checked, 4 drifted, all pre-existing.

**Next: F5b** — the React `CategoryPage` replacing the stub: Current Updates +
Browse sections, TS types, CSS, vitest. Then F6 (`/category/:key/:topic` + shared
`ContentCard`), then the relationship graph.

# Decision 037

**Slice F5b — the React CategoryPage. `/category/:key` stops being a stub, and the
F5a aggregate finally has a consumer.**

## Why this came before the Front Door redesign

The user delivered a comprehensive homepage re-architecture (8 sections, Mission
Cards, "First Step Originals") mid-slice, and chose to finish F5b first. The
reasoning that settled it: **the Front Door's Row 5 LEFT is "Civic Resource
Categories", so
category pages survive the pivot** — and its Mission Cards are the same three
pillars F5a already built (Discover → resources, Connect → organizations, Stay
Informed → updates). The redesign *reinforces* the aggregate rather than
superseding it. Meanwhile F5a had left a shipped BFF with no consumer, which its
own notes flagged as the point where reshaping stops being free.

The Front Door spec is captured verbatim, unscoped, with its open questions
recorded —
including that **"Seniors" is a population, not a category.** `Resource` already
carries `population`, `eligibility_age_min/max` and `eligibility_gender` (43
distinct population values), so "Seniors" is an eligibility facet. Making it a
category would answer *"who is this for?"* rather than *"what is this about?"* —
the same test that kept `LAW` a contentType.

## The page

Order confirmed by the user: **Current Updates → Browse → Organizations**, badges
as **plain type names** (Law · News · Flyer · Expert) rather than event wording,
because "New Law" is a claim about time the badge cannot verify.

Two extracted components, not three. The split is by whether there is real
rendering *logic*: `CategoryUpdates` owns the label map and link-out rule,
`CategoryBrowse` owns the grouped/flat branch, and organizations are ~12 inline
lines reusing `.discovery-item`. A third component would be an abstraction over a
single use with nothing inside it.

**`/category/:key/:topic` is declared now**, pointing at `StubPage`, so the topic
links this page renders resolve to "Coming soon" rather than falling through to
the not-found route — the precedent Slice D set.

## The Slice H exit criterion, honoured at its first opportunity

**F5b reads `contentType` and never `type`.** Reading `type` here would be a bug,
not a style choice: it reports `"news"` for both curated news and signed
legislation, so the feed would render `[News] Relating to Rent Increases.` and a
resident could not tell a change in the law from an announcement.

The label map lives in the frontend, translated per locale — the "presentation
labels are derived from `contentType` by the frontend" half of Decision 036's exit
criterion. **When Slice H deletes `type`, this page needs no edit.** Pinned by a
page test whose fixture has `type` and `contentType` *deliberately disagreeing*.

The map is typed `Record<ContentType, string>`, so adding a content type without a
label **fails the build** rather than rendering `undefined`.

## Three defects found, and where each was caught

1. **`tsc` caught what vitest could not.** Adding `contentType` to `UpdateItem`
   orphaned two fixtures in `ImportantUpdates.test.tsx`. Vitest passed anyway
   (esbuild strips types), but `"build": "tsc && vite build"` means **the Docker
   build would have failed.** Typecheck is a separate gate from the test run.
2. **"1 flyers"** — caught in live browser verification, not by any test.
   Fixed with explicit `.one` / `.plural` keys rather than appending an "s": no
   suffix rule survives Spanish (`ley` → `leyes`). Regression test added.
3. **A CSS collision that made the page look broken.** `.category-browse`
   rendered ~240px wide beside two 1152px sections, because index.css **already
   had** a `.category-browse` rule — the "Browse" *button* from
   `CategoryPreviewList`, retired in Slice A. Its `align-self: flex-start` applied
   silently to a full-width section.

   **Dead CSS is not inert.** A retired component's styles keep matching, so a new
   class name is not safe just because no component uses it — grep the stylesheet,
   not just the components. This is the second time (Step 5c needed "distinct
   `.category-preview*` CSS to avoid a `.category-group-header` collision").
   **Fixed by renaming the new section to `.category-topics`, not by deleting the
   stale rule** — the dead-CSS sweep is a tracked END-OF-REDESIGN TODO and
   deleting unrelated code mid-slice is out of scope.

   Only a rendered-width measurement could catch this. The DOM was correct, so
   every unit test passed. Worth remembering when judging what live verification
   is for.

## An upstream failure worth recording

Mid-verification the Delaware feed returned malformed XML (`Invalid XML: Error on
line 40: element type "link" must be terminated`) and `RssFeedSource` fell to 0
items — so laws vanished from every category page. **Not caused by F5b, which
changes no Java.** Fetching the feed directly returned *valid* XML (`xmllint`
clean, line 40 a `<pubDate>`), so the backend had received a different, transient
response.

Two things this exposed, neither fixed here:
- **`@Scheduled(fixedDelay = 1 hour)` means a boot-time fetch failure leaves the
  app lawless for an hour.** There is no retry/backoff on failure.
- **The app degrades gracefully** — the error is caught, pages still render, and
  category counts simply exclude laws. That is the right behaviour and it worked.

Restarting the container refetched successfully (**179 of 432 bills admitted**).
Recorded as a resilience question for a later slice, not a defect of this one.

## Verification

**34 frontend tests green** (was 20), **`tsc --noEmit` clean**, **253 backend
tests green** (unchanged — F5b touches no Java).

**Live (Docker + headless Chromium):**
- Housing: `44 resources · 20 laws · 5 news items · 3 expert answers · 1 flyer`;
  badges FLYER/LAW/LAW/LAW/LAW/EXPERT; three group headings; topic href
  `/app-next/category/housing/emergency-shelter`; 8 organizations.
- Food renders **0 groups, 4 flat topics**. Utilities renders **6 updates and the
  no-topics line** — the page that was literally empty before F5a.
- Unknown key shows "We couldn't find that category." with a way home; the raw
  backend message is not shown.
- **Spanish:** Actualizaciones Actuales · Explorar Recursos · Organizaciones,
  badges FOLLETO / LEY / EXPERTO.
- **High contrast:** sections `rgb(0,0,0)`, badges `rgb(255,255,0)`. Every badge
  collapses to the same yellow — the LABEL carries the distinction, not the
  colour, which is why text badges were chosen over colour-only indicators.
- Only failing request is the deliberate `404 /api/category/nope`.
- Deep-link reload of a two-segment route still served by `SpaWebConfig`.

**Next: F6** — `/category/:key/:topic` + its BFF + a shared `ContentCard`. Then
the Front Door scoping pass, then the relationship graph.

# Decision 038

**Three tracked debt items, a measured performance baseline, a Version 3 backlog,
and an information-architecture rule. Documentation only — no production code
changed.**

Raised by the user reviewing Slice F5b. Recorded rather than fixed, in this
project's existing pattern: the tech-debt memory carries the actionable item,
this file carries the reasoning. (`gh` is not installed and the repo's GitHub
issues would need manual creation; the memory is also what actually gets re-read
at the start of each slice.)

## A correction to what I reported in F5b

I said the RSS failure meant "no retry, so a bad boot-time fetch leaves the app
lawless for an hour." The retry half is right; the rest was wrong. Reading
`RssFeedService.fetchFeeds()`:

```java
// WHY only replace when non-empty: a transient network failure during a
// refresh should not wipe out the last good result.
if (!allBills.isEmpty()) { signedBills = …; rssItems = …; }
```

**A last-good guard already exists.** What F5b hit was the one case it cannot
cover — the failure was on the **boot fetch**, when both lists were still
`List.of()`, so the guard faithfully preserved nothing.

| The ask | Actual state |
| --- | --- |
| retry with exponential backoff | **Missing.** The real fix — a cold start has nothing to fall back on. |
| keep last successful snapshot | **Partially exists** — in memory, across refreshes. Missing: surviving a restart. |
| distinguish stale from no data | **Missing entirely.** No `lastSuccessfulFetch` exists, and nothing is surfaced. |

## Debt 1 — ad hoc pluralization

F5b's `CategoryPage.tsx` picks `n === 1 ? 'one' : 'plural'`. It fits English and
Spanish and breaks on a third locale: Russian has 4 plural forms, Arabic 6.
**The plural rule is a property of the locale**, so it belongs in localization
rather than in a ternary.

**Fix: `Intl.PluralRules`** — built into every modern browser, driven by the same
CLDR data ICU uses, zero dependencies:

```ts
const suffix = new Intl.PluralRules(lang).select(n);  // 'one' | 'other' | 'few' | …
```

Keys become CLDR categories, so **`contentType.law.plural` → `.other`**; the
current name is not a CLDR category and would mislead a translator. Only locales
that need `.few`/`.many` grow those keys.

**Deliberately split from the framework question.** `Intl.PluralRules` is *debt* —
correcting a hack in shipped code. A full ICU/i18next stack is a *new capability*
and goes on the Version 3 backlog. Bundling them would leave the cheap correct
fix waiting on the expensive one.

## Debt 2 — RSS resilience (a production availability concern)

Three parts, in the order they pay off:

1. **Retry with backoff.** The next attempt today is `fixedDelay` = 1 hour. A few
   retries at increasing intervals would have turned the observed outage into
   seconds — the feed was valid on the very next request.
2. **Persist the last good snapshot** so it survives a restart, not only a failed
   refresh. Turns "lawless until the feed recovers" into "serving yesterday's
   laws".
3. **Distinguish stale from absent** — record `lastSuccessfulFetch` and let the
   UI say **"as of <date>"** instead of silently showing fewer laws. This is the
   part with an editorial consequence: a resident seeing no housing laws cannot
   tell whether none exist or the feed is down, and this project's standard is
   "build trust through transparency."

   It has a home already built. `CategoryMetadata.lastUpdated` is the same *kind*
   of fact and `CategoryUpdates` already renders it as "Latest 2026-07-25". A
   feed-level "as of" should reuse that pattern rather than invent a second one.

**Stated plainly: the failure was graceful.** The error was caught, pages
rendered, counts simply excluded laws, nothing crashed. The gap is honesty about
degradation, not stability.

## Debt 3 — no performance or regression checks exist

There is no perf tooling of any kind: no Lighthouse, no bundle analysis, no
budgets, no CI. Answering the four questions with measurements taken 2026-08-06
rather than assumptions:

| Question | Answer |
| --- | --- |
| **Render speed** | Not measured anywhere. Today: DOMContentLoaded **38–52 ms**, **191 DOM nodes**. |
| **Filtering speed** | **No client-side filtering exists** — by design the BFF filters server-side. `/api/category/housing` **22 KB / 6 ms**; `/api/home` **55 KB / 49 ms**. |
| **Mobile layout** | Media queries exist and are now verified at 375 px: **no horizontal overflow**, 16 px padding applied, section head stacks to column, every update title fits. |
| **Scrolling** | No virtualization and none needed — 6 updates, ≤12 topics, 8 organizations. Page height 2729 px at 375 px wide. |

**Recording the gap, not building a budget yet.** Nothing is slow, so a budget
today guards nothing. The **Front Door redesign** is when this changes — First
Step Originals, four Community Information card groups and a Latest Updates feed
all land on one page, which is when payload and DOM size actually grow. The
numbers above are the "before".

## The Version 3 backlog, and a naming collision

"Version 3" had been used as a place to defer things three times with no list, so
nothing could be reviewed as a set. Now consolidated in
`memory/firststep_version3_backlog.md`:

| Deferred to Version 3 | Also recorded at | Why |
| --- | --- | --- |
| **Subcategory inference** | `04-editorial-principles.md`, `03-application-architecture.md`, Decision 036 | The classifier stays conservative; coverage grows by composition, not inference. |
| **Admin function + category descriptions** | `04-editorial-principles.md`, Decision 036 | `taxonomy.json` prose is editorial and belongs to editors, not a code slice. |
| **Full ICU / i18next localization** | this decision | Needed when translation leaves developers, a third locale appears, or interpolation is required. |

It is a **separate list from tech debt**, because deferred capability is not debt.
Debt is something wrong in shipped code; these are things deliberately not built.
Mixing them makes the debt list look unfixable and the roadmap invisible.

**Naming collision, half of which this session introduced.** "V3" meant two
unrelated things: the product release above, and the 8-section homepage redesign
that Decision 037 called "homepage V3" (the *third homepage design*). "V3's Row 5
LEFT is Civic Resource Categories" and "subcategory inference is a Version 3
feature" are sentences about different things, and left alone someone reasonably
concludes the redesign ships with subcategory inference.

**The redesign is now the "Front Door" redesign** — the user's own phrase ("Slice
H is building the front door to the CivicContent ecosystem"), unambiguous and
more descriptive than a version number. **"Version 3" is reserved for the product
backlog and nothing else.**

## The category / facet distinction, preserved

"Seniors" is a **population, not a category**. `Resource` already carries
`population`, `eligibility_age_min`, `eligibility_age_max` and
`eligibility_gender`, with 43 distinct population values in the loaded data — so
the facet exists and is unused, not missing.

This generalizes the existing contentType heuristic rather than inventing a rule.
`01-domain-model.md` now carries the complete test: *what is this about?* → a
Category · *what format is this?* → a ContentType · *who is this for?* → a
population/eligibility facet. Facets and categories **compose**: "Housing
resources for seniors" is a Housing category filtered by an eligibility facet.
Making Seniors a category would force every senior housing resource to choose
between two homes — Decision 032's mistake in a new dimension.

Flagged against the Front Door spec's Row 5 category list, which is where it will
next come up.

**Next: F6** — `/category/:key/:topic` + its BFF + a shared `ContentCard`. Then
the Front Door scoping pass.

# Decision 039

**CSS architecture — design tokens, a token-driven theme, and incremental CSS
Modules. Phases 0–2 done; Phases 3–4 are conventions for later slices.**

## The diagnosis was wrong, and correcting it changed the plan

The two collisions were **not** two unrelated components sharing a global class
name. In both, one side was **not a component at all** — it was a deleted
component's CSS that outlived it. Both classes had **zero `.tsx` references**:

| Collision | Class | Owner |
| --- | --- | --- |
| Step 5c | `.category-group-header` | retired |
| Slice F5b | `.category-browse` | `CategoryPreviewList`, retired in Slice A |

**39% of `index.css` was orphaned this way.** The mechanism: component deleted →
CSS left behind → the name *looks* free (grep the components, find nothing) → a
new component claims it → silent inheritance.

That matters because scoping and hygiene fix different halves. Scoping makes the
bug class impossible; **deleting CSS with its component would have prevented both
actual instances.** The phased plan handles live CSS as pages get rewritten but
never reaches CSS whose component is already gone — hence Phase 0.

## Tailwind rejected, CSS Modules chosen

1. **The high-contrast theme.** 55 rules of `body.high-contrast .foo` — an
   accessibility feature with its own test. Survives Modules almost verbatim;
   under Tailwind every one becomes a custom variant on every element.
2. **The palette is shared with the backend's static `styles.css`.** CSS custom
   properties can serve two stylesheets in two build systems; a Tailwind config
   cannot.
3. **"Its own identity, not a framework's defaults"** — the stated goal argues
   against adopting an opinionated scale.
4. **Zero config in Vite** vs adding PostCSS to a Dockerfile already running a
   Node stage into a Maven build.

**On "dozens of reusable components": not yet.** 10 components + 3 pages, none
used more than once. The trajectory justifies the work — F6 adds the first shared
component and the Front Door roughly doubles the count — but *migrating at 13
rather than 25* is the honest argument, not the current size.

## Phase 0 — clean before restructuring

`styles/tokens.css` extracted (the 15 existing `:root` properties, unchanged).
**44 orphan rule blocks removed**, all from `HeroGuidance`, `CategoryPreviewList`,
`AppLayout` and `Sidebar`. **167 → 123 selectors.**

**Two false positives the first pass produced, both caught before deleting:**
- `badge-resource|law|news|flyer|expert` are built dynamically
  (`` `badge-${contentType.toLowerCase()}` ``) and never appear as literals. A
  naive "grep for the class name" sweep would have deleted all five.
- `\blogo\b` matched **inside** `hero-logo`, because `-` is a word boundary.
  Hyphen-aware boundaries (`(?<![\w-])…(?![\w-])`) were needed.

Scope was deliberately held: no consolidating, renaming or specificity tidying.

## Phase 1 — the `styles/` structure

`tokens.css` (`:root` only) · `base.css` (resets, typography, `.visually-hidden`)
· `themes.css` (high contrast). `index.css` **124 → 91 selectors**.

Ownership rule: **a class that styles a component never appears in any of the
three.** `.section-placeholder` — used by 7 components — was deliberately *left*
in `index.css` rather than promoted to `base.css`, because it is a shared
component style, not a base utility. Phase 3 decides what it becomes.

## Phase 2 — the theme becomes tokens

`body.high-contrast` now redefines `--surface`, `--bg-lighter`, `--bg-light`,
`--border-color` and the three text tokens. **30 → 20 blocks.** A new `--surface`
token replaced the literal `white` in six panel rules — the value those rules
always wanted.

**Two tokens deliberately NOT flipped, and the reason is a real finding:**
- **`--primary-color` is overloaded.** It is the utility bar's *background*
  (wants `#000`) and heading *text* (wants `#ff0`). No single value satisfies
  both. Splitting it into surface/ink roles is a token redesign — **tracked, not
  done here.**
- **`--warning-color`** is only ever a background behind white text; flipping it
  to `#ff0` would make `.update-urgency` yellow-on-white.

## What the visual gate caught — the most valuable result

**A rule can look redundant by value while doing SPECIFICITY work.**

`body.high-contrast .category-update-title { color: #fff }` was removed as
redundant, since `--text-primary` is already `#fff`. It was not redundant: an
update title is an `<a>` when the source has a URL, and `body.high-contrast a`
(0,1,2) outranks the component's own `.category-update-title` (0,1,0). Every
linked title turned **yellow**, which also collapsed the hover state into a
no-op.

Caught by pixel-diffing screenshots: four 16px bands per page, spaced ~103px,
`rgb(15,15,15) → rgb(15,15,0)` — anti-aliased white text becoming yellow. **No
unit test could see this**; the DOM was identical. Restored with the reasoning in
a comment.

## Two accepted visual changes, both accessibility fixes

Phases 0 and 1 were **byte-identical across all 16 combinations** (4 pages × 2
viewports × 2 themes). Phase 2 left **14 of 16 identical**; the two that differ
are both homepage/high-contrast, and both are latent defects being fixed:

| Element | Was (high contrast) | Now |
| --- | --- | --- |
| `.flyer-card-image` background | `rgb(247,234,220)` — **cream** | `rgb(34,34,34)` |
| `.flyer-card-meta` colour | `rgb(156,163,175)` — **gray on black** | `rgb(255,255,255)` |

`.flyer-card` and `.flyer-card-title` had overrides; `.flyer-card-image` and
`.flyer-card-meta` were simply forgotten. **That is the argument for token-driven
theming in one example:** the per-component approach requires remembering every
element and missed two; the token approach covers them by construction. **Light
mode is unchanged in every case.**

## Phases 3–4 — conventions, not work

**Phase 3:** every new slice uses co-located CSS Modules
(`components/ContentCard/{ContentCard.tsx,ContentCard.module.css}`). Existing
components stay put until a slice touches them, so no component is migrated only
to be rewritten by the Front Door. **F6 is the first application.**

**Phase 4:** the Front Door redesign deletes rules from `index.css` as it
rewrites pages.

`:global()` is reserved for `:root`, `html` and `body.high-contrast`. Phase 2 is
what makes that affordable — with the theme token-driven, components need no
escape hatch at all.

## The `index.css` contract

**End state: an import manifest and nothing else.** During migration it may also
hold not-yet-moved component rules — **it is a QUARANTINE, not a home: rules only
ever leave it.**

**Never allowed from Phase 3 onward: a global rule for a component built after
the convention was adopted.** That is the likeliest regression, because it is
always the path of least resistance in the moment.

**Enforced as a ratchet — the selector count may only decrease. 167 → 91.**

## Verification

**34 frontend tests green · `tsc --noEmit` clean · 253 backend tests green**
(untouched — the change is frontend-only).

**Zero orphaned selectors remain**, verified with hyphen-aware boundaries across
`.tsx`, `.ts` and `index.html`, with the five dynamic `badge-*` classes correctly
retained. **Light mode byte-identical on all 8 combinations at every phase.**

**Follow-ups recorded, not done:** split the overloaded `--primary-color`;
`.section-placeholder`'s 7-component ownership; the 17 classes shared across 2+
components, which is the list Phase 3 has to answer.

**Next: F6** — `/category/:key/:topic`, with `ContentCard` as the first component
built in the new structure.

# Decision 040

**Slice F6 — the topic page. The navigation hierarchy is complete, and
`ContentCard` is the first component built on the CSS Modules convention.**

`GET /api/category/{key}/{topic}` + `/category/:key/:topic`. Category → topic
group → topic → **CivicContent** (Decision 021) now exists end to end; the topic
links `CategoryBrowse` has rendered since F5b resolve to a real page instead of a
stub.

## The fact that shaped the slice

**Only resources (229/229) and flyers (7/7) carry a `subcategory`.** News, signed
legislation and expert answers carry none, so they can never appear on a topic
page. That is not a limitation — it is the same measurement that made the
category page an aggregate (Decision 036), seen from the other side: **browse
reaches what has a topic, the category page's updates feed reaches what does
not.** Together they cover everything, which is the coverage identity from 036
restated as two pages.

## `ContentItem`, and why it is not `UpdateItem`

A new normalized display DTO in `shared/dto`. F5a argued hard for ONE cross-type
merger, so the divergence needs justifying:

| | answers | resources |
| --- | --- | --- |
| `UpdateItem` | "what changed?" | **excluded by design** |
| `ContentItem` | "what is this?" | included |

Forcing resources into a DTO named *update* would repeat the exact naming
confusion (`type` meaning two things) that Decision 036 is retiring. They overlap
in ~7 fields, and that is **acknowledged debt, not an oversight**.

**Intended end state: `ContentItem` becomes the single display DTO and
`UpdateItem` disappears alongside `type` in Slice H**, at which point the updates
feeds return `ContentItem`s sorted by date. `ContentItem` already has no `type`
field — it was defined after the exit criterion, so there is nothing in it for
Slice H to remove.

## The CSS convention, proven

`components/ContentCard/{ContentCard.tsx, ContentCard.module.css}` — co-located,
per Decision 039. Verified live rather than assumed:

- **Classes are scoped:** rendered as `_card_1kq6h_8`, `_badge_1kq6h_29
  _badgeRESOURCE_1kq6h_44`. A collision with a deleted component's CSS is now
  structurally impossible for this component.
- **Themed for free:** high contrast measured at `rgb(0,0,0)` surface and
  `rgb(255,255,0)` border with **zero `:global()`** and zero theme rules of its
  own. Every colour is a token, so Phase 2's redefinition does the work. That is
  the payoff of 039 Phase 2, demonstrated on the first component to depend on it.
- **The ratchet held: F6 added ZERO rules to `index.css`** (91 selectors,
  unchanged).

`i18n/contentTypeLabel.ts` was extracted once `ContentCard` became a **second**
consumer beside `CategoryUpdates` — the same "earn it on the second use" rule the
backend services follow.

**What stayed global, deliberately:** `home-body`, `stub-page`, `stub-back`,
`section-placeholder`. Phase 3's rule is that existing styles move when a slice
touches *them*, not when a new page happens to use them. Migrating those here
would have meant duplicating them into a module or refactoring four other
components.

## Two defects found by looking at the page

Both were on **every card** of the first real render, both in this component's
own logic, and **neither was a rendering error** — the DOM was exactly what the
code asked for. They were judgement errors, and only a screenshot surfaces those:

1. **Title/organization duplication.** Directory records frequently name a
   resource after its provider, so ten cards each read "American Red Cross" then
   "American Red Cross · Wilmington".
2. **"Standard" urgency rendered as a chip.** `urgency: "standard"` means
   ordinary, so every non-urgent resource wore a badge announcing it was not
   urgent. `ImportantUpdates` already skipped it.

Both fixed and pinned by tests, so the judgement survives the next refactor.

## A readiness check tests the slice's contract, not the app's health

The upstream Delaware RSS feed served malformed XML again mid-verification, and
my readiness loop — which waited for `LAW > 0` — **spun for ten minutes**.

**Topic pages contain no laws.** The check was gated on a dependency the slice
does not have. The user's rule, recorded because it generalizes the mistake:

> **A readiness check should test the slice's contract, not the application's
> overall health.**

Corollaries applied here: F6 was **not** gated on RSS or LAW availability; the
feed failure is **documented separately** as the existing ingestion-reliability
debt (item 5); and **no RSS fix was introduced into F6** merely to make a
readiness check pass. Mixing an availability fix into a page slice would have
made both harder to review and neither easier to reason about.

**The debt is now demonstrably real rather than theoretical — it has cost
verification time twice.** Same failure both times: a boot fetch fails, there is
no retry, and `fixedDelay` means the next attempt is an hour away. Fetching the
feed directly returned valid XML on both occasions.

## Confidential Location Modeling — a data-model completeness item

**Classified by the user as data-model completeness, NOT privacy remediation:**

> Preserve the distinction between an unknown address and a deliberately
> unpublished address during ingestion. Until the domain model supports that
> distinction, validation must prevent confidential locations from carrying an
> address. No consumer should infer, generate or expose an address for a
> confidential location.

I initially called this "a privacy gap … a protected address being dropped".
**That was wrong twice over** — wrong on the facts, and wrong in category. The
user corrected both. Checked against the data:

```json
HA-006  { "label": "Primary", "address": null, "city": "Wilmington",
          "state": "DE", "zip": null, "confidential": true }
```

**The address is already null.** In the DSCYF source, `confidential` is the value
*in place of* an address — the organization does not publish its location — not a
flag concealing an address that exists. **First Step is not exposing a protected
address**, and nothing here says otherwise.

**The real issue is loss of meaning at ingestion.** `shared/model/Location` does
not map `confidential`, so two different source facts collapse into one null:

| Source meaning | After ingestion | Count |
| --- | --- | --- |
| address unknown | `address: null` | 6 |
| **deliberately unpublished** | `address: null` | 1 (HA-006) |

The risk is therefore **forward-looking, not current**: a later geocoding step, a
detail page or a map would have no way to know HA-006 must never acquire an
address. The domain model should express *an address that may be unavailable or
confidential*, rather than inferring or displaying one.

**Recorded as data-model completeness, not fixed here** (user's call — it does
not block F6). The fix is a model that carries an address which may be
*unavailable or confidential* rather than merely absent, plus deciding what every
consumer does with it.

**Why the classification matters and is not pedantry:** filed as privacy
remediation this reads as an incident — something leaked, someone should be told.
Filed as completeness it reads as what it is: the model cannot yet express a
distinction the source makes. The two get different urgency, different reviewers
and different write-ups, and only one of them is accurate.

**Verified now, so the invariant holds while the model catches up:**
`validate_schema.py` gained an ERROR when a confidential location carries an
address. It was negative-tested — planting an address on a confidential location
fires it — so it is a live guard rather than an inert rule. All three validators
still exit 0. `ContentItem` also carries **no address field at all**, so a browse
card cannot leak one by construction.

## Verification

**271 backend tests green** (was 253, +18) · **48 frontend green** (was 34, +14) ·
`tsc --noEmit` clean · **92 mirrors checked, no drift in any F6-touched file**.

**API:** topic counts match the category page exactly (Emergency Shelter 10,
Transitional Housing 11, Food Pantry 9). `legal/eviction-prevention` returns the
dual-classified flyer, and `housing/food-pantry` **404s** — proving topics are
category-scoped rather than global.

**Live**, desktop and 375px, light and high contrast: 10 cards, breadcrumb
`🏠 Housing › Emergency Shelter` → `/category/housing`, no horizontal overflow, 0
failed requests, click-through from the category page lands on the topic page.
Category page unchanged in both themes (3 sections, 3 groups, 9 topic links, 8
organizations).

A pixel regression check against the F5b baseline was **not** meaningful this run
— with the feed down those pages legitimately show fewer items — so the category
page was verified structurally instead. Stated plainly rather than reported as a
pass.

**Next: the Front Door scoping pass** ([[firststep-front-door-spec]]), with its
open questions already recorded.

# Decision 041

**The Front Door is scoped as a composition layer over existing capabilities, not
as a new content architecture.**

A scoping pass, not a build. Output:
**[docs/architecture/05-front-door.md](../docs/architecture/05-front-door.md)** —
the canonical Front Door information architecture. This entry records the
*reasoning*; the doc records the *result*.

## The risk being managed

**Presentation and discovery needs get satisfied by inventing new domain concepts
rather than using the existing CivicContent model.** The Front Door asks three
structurally identical questions at once — Seniors, the Community Information
groups, and First Step Originals — and each has a cheap wrong answer that looks
like a domain concept.

This is not hypothetical. **It already happened here and was paid down.** Flyers
once reached a category through a hardcoded `includesFlyers` boolean on the
Community Events category definition: a *presentation* need satisfied by a
*domain* special case. F1 deleted it. `UpdateItem.type` is the second instance,
and it costs Slice H a named retirement criterion (Decision 036).

The through-line adopted: **presentation may compose the model in new ways; it
may not add to it.**

## Three resolutions

| Need | Resolution | Rejected |
| --- | --- | --- |
| Seniors pathway | controlled **discovery tag** derived from population/eligibility | a category · a ContentType |
| Community Information ×4 | existing types + distinguishing **metadata** | four new ContentTypes |
| First Step Originals | a **`ContentSource`** identity | an `Originals` domain class |

### Why `ContentSource` and not a new ContentType

**The data was already making this distinction — informally.**

| File | `contentSource.name` | `contentType` |
| --- | --- | --- |
| `faq.json` | `"First Step Curated FAQ"` | `EXPERT` |
| `expert-answers.json` | `"Delaware Volunteer Legal Services"` | `EXPERT` |

Same content type, different producers: "we made this" versus "we publish this".
`ContentSource` already declares an `id` field — and it is `null` in every record
in every data file. So the mechanism exists and is unused; `contentSource.id =
"first-step"` is the structured form of a fact the data already records, not a
new concept. **Originals describes who created the content, not a different kind
of CivicContent.**

The counter-argument that lost: a new ContentType is not a local change here.
Verified blast radius of one enum value — the enum, `contentTypeLabel.ts` (an
exhaustive `Record<ContentType, string>` that *fails the build* if unhandled),
five `badge-*` CSS classes, `UpdatesService`'s mappers,
`EditorialStabilityTest`'s pinned baseline, EN + ES i18n keys, and a full
annotated mirror per file touched. Four new types is that cost four times, for
one homepage row — and the resulting values would answer CivicContent's six
questions *identically*, which empties the enum of meaning for the types that
earned it.

### Why `seniors` is derived metadata over preserved evidence

**Seniors answers "Is this relevant to seniors?" — not "Was this program created
exclusively for seniors?"** It is a resident discovery need, so it is
first-class; it is not a statement about the program, so it is not taxonomy.

The shape: **source evidence stays authoritative** (`population`, `eligibility`,
`eligibility_age_min/max` are the resident-facing requirement and are never
rewritten — a resident is told *"Age 62 and older"*, not *"seniors"*), and
`seniors` is an **additional** controlled tag computed during **Enrich**, the
stage that already produces derived metadata.

This refines `01-domain-model.md`'s third row rather than contradicting it. That
row correctly calls "who is this for?" a population/eligibility facet; what was
missing was the mechanism connecting the facet to a discovery path. **A discovery
pathway is a filtered view across the taxonomy, never a node within it** — which
is how "a first-class path driven by tags metadata" stays consistent with "tags
never determine navigation." Placement and filtering are different operations.

Consequences accepted: no second Seniors query, no Seniors ContentType, no
separate senior-resource model. The Seniors page is a discovery page like any
other, presenting results with the same category and content-type structure.

## The finding that changed the rule

**Free-text phrase matching produces a false positive that no word-boundary rule
fixes.** `older adults?` matches the literal sentence:

> *"Age 18 and **older Adults** recovering from substance abuse…"* — HA-010, HA-014

The phrase genuinely spans the end of `"Age 18 and older"` and the start of the
next sentence. Same family as `\blogo\b` matching inside `hero-logo` during the
CSS sweep (Decision 039) — **the boundary is not where it looks.**

The important part is *how* those two records get excluded. Filtering them on
`age = 18` appears to work and is wrong: HA-034 and HA-035 are also
`Age 18 and older` and **are** senior-relevant, because their eligibility reads
*"includes elderly and disabled persons."* So the exclusion must come from a
**collision guard** (`\d+\s+and\s+older\s+adults?` stripped before term matching),
not from the age gate. A rule that cannot separate those two cases is the wrong
rule however plausible its output looks — which is why the doc negative-tests it.

Two further data facts recorded: `eligibility_age_min` is 20/58 in
`resources.json` and **0/171** in `resources.communities.json` (the threshold
lives only in the prose `population` string there — a decision the implementing
slice must make), and **age evidence currently adds no record that term evidence
does not already find.** The age rule is future-proofing, not coverage, and
saying so prevents a later reader treating it as load-bearing.

Verified reach: **8 resources**, 0 non-resource CivicContent. The doc ships the
script that reproduces the list.

## Scoping answers recorded

- **Age 50+ → include, and flag the evidence source**, so the 50–54 band stays
  auditable without re-deriving. *Note for honesty:* under the final rule this
  threshold changes zero records today — SD-095 and SD-127 both carry `Seniors`
  tags and enter on term evidence regardless. The decision bought clarity, not
  coverage.
- **First Step Originals ships with what exists** — the 6 curated FAQs — proving
  the `ContentSource` mechanism against live data rather than a hypothetical.
- **Community Information stays presentation** over existing CivicContent.

## Sequencing consequence, stated up front

Mission-card destinations are resolved: Discover → `/discover`
(`CategoryService` exists, route is new), Connect → `/find-help` (**Slice G**),
Stay Informed → `/updates` (**Slice H**). `/discover` is a real page rather than
an anchor scroll, because the global nav item needs a destination and an anchor
is not one.

**Therefore: when the Front Door ships, two of its three mission cards point at
stub pages.** That follows directly from the intended order — the Front Door
composes, then G and H fill in the destinations it points toward — but it is
recorded now rather than discovered during verification.

## Gaps, with owners

1. `ContentSource.id` unpopulated everywhere — Originals has no queryable key.
2. Originals content unclassified — FAQ **0/6**, Expert **0/6** carry
   `category_tags`. **Separate from gap 1:** contentSource says *who made it*,
   editorial classification says *where it appears*. Conflating them causes rework.
3. Seniors derivation not implemented.
4. Community Information grouping metadata missing — all 7 flyers are
   `contentSource.type: "manual"`. The implementing slice **must not reach for
   `subcategory`**; that is the topic level of navigation, and using it for
   presentation grouping is the `includesFlyers` mistake in a new costume.
5. No organization directory — **Slice G**.
6. No Important Notices page — **Slice H**.
7. Payload growth is the performance trigger (baseline 2026-08-06: `/api/home`
   55 KB / 49 ms, 191 DOM nodes).

## Verification

Doc-only by construction: no `backend/`, `frontend/`, `app/data/` or test file
changed, so **no annotated mirror is in scope for this slice**. Every service
named in the dependency map was confirmed to exist; every "already built" claim
resolves to a real endpoint or component; every count in the doc ships with the
command that produced it, and the Seniors script was extracted from the doc and
run to confirm it reproduces all 8 records.

**Next: designing the actual homepage**, then Slice G and Slice H to fill in the
two destinations the Front Door points toward.

# Decision 042

**Slice H — Homepage Composition. The front door is built, and the complexity
stays behind it.**

Decision 041 set the domain guardrails; it deliberately did not define the visual
homepage. This slice does. Canonical spec:
**[docs/architecture/05-front-door.md](../docs/architecture/05-front-door.md)**.

**Scope: the homepage only.** Destinations stay stubs, each getting its own
slice. Six of the seven Community Resources pathways already resolve to real
Slice F category pages, so the front door is not pointing at nothing.

## The constraint that drove every decision

**The original First Step homepage was a question and four icon cards.** That
restraint is the target, and it is why the strongest force in this slice was
subtraction:

| | before | after |
| --- | --- | --- |
| top-of-page bars | 2 (UtilityBar + SiteHero) | **1** (three-zone header) |
| homepage search boxes | 2 (strip + section) | **1** |
| `index.css` selectors | 91 | **50** |
| homepage components with data | 4 | 4 (different ones) |

**Detailed search, filtering, browsing and full CivicContent presentation belong
on destination pages.** Applied to me as well: no extra sections were added
beyond the eight specified.

## The header came from the original, not from a new design

v1's `index.html` has `.header-content` = branding · `.ai-banner-header`
("Have questions? Get answers with AI.") · `.header-utilities` (`ES`, `⊕`), with
a separate `.main-nav` beneath. Slice H restores that shape.

**That resolved "ARIA" without guessing.** The spec's
`First Step | About | Housing | Community | Updates | ARIA` is not six nav links:
*First Step* is the branding zone, *About…Updates* is the nav row, and **ARIA is
the utilities cluster** — where v1 and `UtilityBar` both already put the
accessibility controls. They were never relocated; they were named. `ARIA` is a
`role="group"`, and a test asserts it contains two buttons and **no link**.

The centre banner is a **teaser, not a second search box** — an anchor on the
homepage, a route change elsewhere (a category page has no AI section, so an
anchor there would be dead). Both forms are tested.

## Three needs, three resolutions — none of them a new domain concept

**Community Resources → an authored list** (`app/data/homepage.json`), a sibling
editorial artifact to `navigation.json`. Seven pathways, because *which* seven of
ten categories a resident sees first is an editorial judgement and one of the
seven is not a category at all. **Labels and icons are RESOLVED from
taxonomy.json, never authored** — two files holding one label is the drift bug
Decision 032 removed.

`kind` carries the guardrail into the data and then into the URL:

```
category  → /category/{key}     canonical taxonomy category
discovery → /discover/{key}     controlled query over CivicContent metadata
```

Routing Seniors to `/category/seniors` would assert a taxonomy entry that must
never exist, and the "obvious fix" would be to create it. Separate namespaces
make the wrong fix unappealing. Pinned by a test phrased as the thing that must
not happen.

**First Step Originals → `contentSource.id`.** The data was already making the
distinction informally: `faq.json` says `"First Step"`, `expert-answers.json`
says `"Delaware Volunteer Legal Services"`, and **both are `contentType: EXPERT`**.
Same kind, different producer. The `id` field existed and was null everywhere;
this slice populates it on the six FAQs. Future briefings and data stories appear
**with no code change** — the payoff of identifying by provenance rather than
enumerating types. The filter is a private method in `HomeService`, not an
`OriginalsService`: one source today, and this codebase introduces the service at
the second (the F4 → F5a rule).

**Community Information → pathways, and a gap dissolved.** Front Door gap 4 said
the four groups need distinguishing metadata that does not exist (all 7 flyers
are `contentSource.type: "manual"`). **The homepage does not need to group
anything — it needs to offer a way in.** Events, Meetings and Announcements are
three links to `/community`; the grouping question moves to the destination page
where it belongs.

Worth generalizing: **a data-model gap can be a symptom of asking a page to do a
destination's job.** Before adding a field to satisfy a screen, check whether the
screen should have been asking. No flyer metadata changed; no ContentType was
added.

## What left the payload

`categories` → replaced by `communityResources` (seven label+icon pathways, not
ten summaries with counts and previews). `organizations` → **removed entirely**;
organizations moved behind Connect → Find Help, where a directory can do them
justice. `OrganizationService.getCuratedShortlist()` is consequently unused —
**left in place for Slice G rather than deleted**, and flagged here so it does
not become mystery dead code.

`getHome()` also lost its `communityId`. Nothing the homepage returns is
community-scoped anymore and the frontend never sent it; a parameter that is
accepted and silently ignored is worse than one that is absent.

## Rebalanced Slice H exit criterion

**The `UpdateItem.type` retirement (Decision 036) moves to the Latest Updates
page work**, not "Slice H" by name. That criterion belongs with the work that
actually touches the updates feed and its consumers. It remains non-optional.

## CSS: the ratchet, and the specificity trap in reverse

`index.css` **91 → 50**, as five components retired and their rules left with
them. Eight new components are co-located CSS Modules.

**`ContentCard`'s zero-`:global()` record did not survive, for a documented
reason.** `body.high-contrast a` has specificity (0,1,2); a CSS Module class has
(0,1,0). Every new component with links needed an explicit
`:global(body.high-contrast)` block or its labels would turn yellow and hover
states collapse — the same trap Decision 039 found by *deleting* a rule, met here
by *omitting* one. `:global()` on a theme selector is the one sanctioned use
(`docs/frontend/css-architecture.md`).

The root cause is the tracked `--primary-color` overload: brand INK (wants `#ff0`)
and brand SURFACE (wants `#000`) in one token. **Splitting it would delete most
of those blocks** — the debt item is now concrete rather than theoretical.

Dead rules were removed from `themes.css` **only where the target class no longer
exists**, since a rule for a live element can be doing specificity work while
looking redundant by value.

**The sweep produced five false positives**, two of them a new kind worth
recording alongside the known ones (dynamic `` `badge-${…}` `` classes; `\b`
matching inside hyphens): **`.discovery` matched prose** ("a discovery pathway")
and **`.community-title` matched an `id` attribute**, not a `className`. Grep for
`className="x"`, not for `x`.

## Verification

277 backend (+6) · 63 frontend · `tsc --noEmit` clean · 4 validators exit 0,
the new one negative-tested on all four of its rules · `index.css` ratchet 91 → 50 ·
live at 1280px and 375px in both themes · the six category pathways clicked, not
assumed.

**Next: the destination pages** — Latest Updates (carrying the `UpdateItem.type`
retirement), Community, Discover, and Slice G's organization directory behind
Find Help.

# Decision 043

**Slice H polish pass — the front door reviewed on screen, and corrected.**

Everything here came from looking at the rendered page rather than from a plan.
Recorded because most of it is the kind of thing a test suite cannot see.

## "ARIA" was a misreading, and the fix is to remove rather than rename

Slice H put the word **ARIA** in the header as a label for the language and
contrast buttons, on the reading that the spec's
`First Step | About | Housing | Community | Updates | ARIA` made it the utilities
cluster. **ARIA is Accessible Rich Internet Applications** — the W3C spec. v1 had
some ARIA usage that was ultimately removed, and none is being reintroduced yet.

Removed: the visible chip and its `role="group" aria-label="ARIA"` wrapper. The
two buttons already carry their own accessible names, so the wrapper was ARIA
for its own sake — **no ARIA beats decorative ARIA**, which is the actual rule.

Also removed as redundant: an `aria-label` on a plain `<div>` (does nothing
without a role) and one on a list already named by the heading above it.

**Kept, deliberately:** `aria-labelledby` tying each section to its heading,
`aria-live` on the AI answer, `aria-pressed` on the contrast toggle,
`aria-hidden` on decorative emoji and the new arrows. These do real work for
screen-reader users; removing them would be an accessibility regression on a
service whose audience includes people who most need it.

A test pins the removal *as a prohibition*: no "ARIA" text, no `role="group"`.

## Two destination pages, split by who produced the content

The homepage's Important Updates feed becomes **two** pages, not one:

| Route | Page | Producer |
| --- | --- | --- |
| `/updates` | **Latest Updates** | **government** — agencies, officials, programs |
| `/community-notices` | **Community Notices** | **non-government** — churches, nonprofits, community groups |

A church offering a free meal and a state agency changing SNAP eligibility are
both "notices", and collapsing them would flatten the difference a resident most
needs: *who is telling me this, and what does that imply about it?*

**That split is a `ContentSource` distinction — the third time ContentSource has
answered a question that looked like it wanted a new ContentType** (after First
Step Originals in 042). It is becoming the default answer to "these are the same
shape but different in kind", and worth checking first next time.

A merged feed on the front door could not honour it, so `updates` left
`HomePayload` and `ImportantUpdates` left the homepage. **`/api/updates` still
serves the feed** for those pages; `ImportantUpdates.tsx` is retained unrendered
as their starting point, flagged like `getCuratedShortlist()`.

## Layout: full width, and a magazine sidebar

**Full width.** No centred max-width column. Every top-level section indents by
one new token, `--page-gutter: clamp(18px, 4vw, 64px)`, so they stay aligned as
the viewport grows. The AI form and its answer panel keep a 760px measure —
a search field spanning 2000px is unusable.

**First Step Originals became a fixed 340px sidebar**, ruled off with a left
border, uppercase kicker heading and ruled rows rather than cards. A FIXED track,
not a fraction: a magazine sidebar should stay put while the main column absorbs
the extra width, and a fractional track would widen both until it stopped reading
as a sidebar.

## Corrections made from the render

- **Mission cards** lost their emoji and their italic questions ("What is
  available?"). The emoji were decoration; the question restated the sentence
  directly beneath it.
- **"Important Changes in Delaware" → "New Laws in Delaware"**, undoing 042's
  rename. The broader name promised policy changes and deadlines while the
  section only ever renders signed legislation — a small lie on every page load.
  The chips beneath it carry the broader remit, and they are explicitly a teaser
  for the destination.
- **The laws box no longer resizes as it rotates.** `min-height: calc(1.45em*3)`
  holds the longest title (a full House Joint Resolution); short ones leave the
  space empty. It was growing and shrinking every five seconds and shoving the
  columns below it around.
- **Category cards gained an arrow in a circle.** A bordered box does not say
  "clickable" on its own, least of all to a resident who is not a confident web
  user. `aria-hidden` — the whole card is the link and the label already names it.
- **Community Information lost its three pathway cards.** Events, Meetings and
  Announcements were three links stacked above a "See all" link to the same page
  — one destination offered four times. Flyers alone now. **The gap-4 reasoning
  survived their removal unchanged**, which suggests it was about the shape of
  the page rather than those three cards.
- **Flyers switched from `object-fit: cover` to `contain`.** They are POSTERS —
  `cover` cropped "FREE LEGAL HELP IS AVAILABLE FOR DELAWARE RENTERS" mid-line.
  Letterboxing costs polish and keeps the flyer readable, which is the only
  reason to show an image instead of a title.
- **~370px of dead space** under the resource cards, because the sidebar ran far
  longer. Closed by enlarging the cards and clamping sidebar summaries to two
  lines — even rows read better in a narrow track anyway.

## Verification

277 backend · 63 frontend · `tsc` clean · 4 validators exit 0 · `index.css`
ratchet **held at 50** (this pass added no global rules) · EN/ES key parity
checked mechanically (77 = 77) · live at 1280 and 375 in both themes, no
horizontal overflow, console clean.

**Next: the destination pages.** Latest Updates (government) and Community
Notices (community) are now distinct, and the `UpdateItem.type` retirement
belongs with whichever is built first.

# Decision 044

**Front door design pass — larger type, real elevation, a contained page, and
the AI search removed.**

Driven by review against two reference sites the user named: **newarkde.gov** and
**sanjoseca.gov**.

## The AI search is gone, not moved

It was powered by an Ollama agent that is no longer wired in. **An entry point
that cannot answer is worse than none** — a prominent "ask us anything" box on a
civic service homepage is a promise, and a resident in difficulty is the wrong
person to disappoint. Both the search section and the header banner were removed.

`AiSearch.tsx`, `AiResultCard.tsx` and `POST /api/decide` are retained unrendered
for whenever AI is decided. **Added to the Version 3 backlog.**

Consequence worth stating: the front door no longer serves "intentional
discovery" directly. Discover → Explore Resources carries that job until the
search returns.

## What the reference sites were actually doing

Three things, and none of them is decoration:

1. **Contained content with generous margins.** Neither site runs edge to edge.
   Content now caps at `--page-max: 1600px` and centres. On a 1280–1512 laptop
   that IS full width; past it, line lengths and a 7-across card grid stop
   becoming unreadable. This revises 043's edge-to-edge decision after seeing the
   references.
2. **Real elevation.** The old `--shadow-sm` was a single 5%-opacity 1px shadow —
   so faint every card looked painted on, which is what "the cards lay flat"
   was describing. All three shadow tokens are now TWO layers, a tight contact
   shadow plus a soft ambient one, which is what makes a card read as resting on
   a surface rather than printed on it.
3. **Larger type.** Root size 16px → **17px** via `html { font-size: 106.25% }`,
   plus targeted increases on section headings (1.35 → 1.75rem) and body copy.
   Set as a percentage, not px, so a resident who has raised their browser
   default still wins. **This is an accessibility decision before an aesthetic
   one** — the audience skews older and lower-vision.

**Section background BANDS were considered and rejected** (user's call): the
separation is carried by elevation and whitespace instead.

## First Step Originals gets a tinted panel

Warm `--bg-light` ground, a 4px accent top rule and a resting shadow, rather than
the bare left border it had. A second WHITE surface beside the white category
cards would have read as one more card; the tint says "different kind of content"
before a word is read. That is a component treatment, not a page band, so it
coexists with the no-bands decision.

In high contrast the tint cannot survive — `--bg-light` is `#222` there, which
against a `#000` page is a distinction nobody can see — so the accent rule and
border carry the separation instead.

## The laws box: a measured fix replacing a guessed one

043 gave `.bill` a `min-height` of 3 lines. Measured in the browser, that was
**exactly right at 1280px and reserved a dead line at 1600px** — and it would
have needed re-measuring for any longer title the feed publishes.

Replaced with a **ghost stack**: every title renders into the same CSS grid cell
(`grid-area: 1/1`) with only the current one `visible`. The cell measures the
tallest title at whatever width it has, for whatever data arrives. Hidden titles
are out of the accessibility tree by virtue of `visibility: hidden`.

Verified by clicking every dot at two widths: **one distinct box height at each**,
and the heights DIFFER between widths (300px vs 271px) — which is the proof it is
measuring content rather than repeating a constant.

## Verification, and one honest gap

277 backend · 58 frontend · `tsc` clean · 4 validators exit 0 · `index.css`
ratchet held at **50** · EN/ES parity 74 = 74 · no horizontal overflow at 1280 or
375 in either theme · console clean.

**The Delaware RSS feed is down again — the THIRD occurrence of tech-debt item 5**
(`Invalid XML: The element type "link" must be terminated`). Per the standing
readiness rule this did not gate the slice. The rotator was verified against
INJECTED laws via a request intercept, which tests this slice's contract rather
than Delaware's uptime. The live page currently shows the section's degraded
state, which is itself correct behaviour.

## Decision 044 addendum — brand-tinted contrast, and what measuring it exposed

The user asked for "a bit more contrast", a **green shadow** behind the mission
cards, and a **green** wash behind First Step Originals. Doing it properly turned
up two accessibility failures that had been sitting in the palette.

**Brand-tinted elevation.** `--shadow-brand-sm/md`, built from `--primary-color`
(`rgba(26, 92, 56, …)`) rather than near-black. A green shadow on a cream page
reads as part of the identity instead of grey haze, and carries more apparent
contrast at the same opacity because it differs in HUE as well as value.

**Both first attempts were too weak, and only measurement showed it:**

- The wash at **8%** composited to `#e9e0c9` — a warm khaki, indistinguishable
  from the cream page. Over a ground this saturated, a light green tint simply
  disappears. Raised to **18%** → `#d3d2ba`, a sage that actually reads as green.
- The shadow at **13%/10%** was invisible at 2× zoom. Raised to **22%/16%**.
  Verified by cropping the card's BOTTOM edge, where a downward shadow actually
  falls — the first crop caught the top edge and showed nothing, which would have
  been easy to misread as "the shadow is broken".

### Two WCAG failures found by checking rather than looking

Neither was introduced by this change; both were already shipping.

| Token | Was | Measured | Now | Now measures |
| --- | --- | --- | --- | --- |
| `--text-light` | `#9ca3af` | **2.54:1 on white** — fails AA on every date and meta line | `#6f6a64` | 5.36 white · 4.61 page |
| accent as TEXT | `#e07b39` | **2.26:1** on the Originals panel | `--accent-ink: #8f4009` | 4.71 on the wash |
| `--text-secondary` | `#6b7280` | 3.04 on the new wash | `#57534e` | 7.63 white · 6.57 page · 4.98 wash |

`--accent-color` is unchanged and still does the fills and rules — it is a
perfectly good fill colour and a poor text colour, which is why the split exists.
The greys also moved from COOL blue-greys to WARM stone tones, which suit a
cream-and-green palette rather than fighting it.

**The binding constraint was the wash, not white.** Deepening the Originals panel
to make it visibly green is what pushed the accent text under AA — so the
readable-accent fix is a direct consequence of the green request, not an unrelated
tidy-up.

**Every text token now passes AA (≥4.5:1) on every ground it actually appears
on**, verified with a contrast script rather than by eye. Non-text separation
(panel vs page, 1.13) is deliberately low: a wash is a cue, not a control.

High contrast is unaffected — the green shadows are invisible on black, which is
harmless, and the Originals panel keeps its explicit `#000` + yellow-border
override.

# Decision 045

**Slice I — the updates destinations, and the provenance model that made them
possible. Amends Decision 041 and closes Decision 036.**

`/updates` had four entry points and was a stub — the largest dead end on the
site. Building it required answering a question the data could not: **who
published this?**

## The split is by PRODUCER, and nothing in the data said so

| Route | Page | Producers |
| --- | --- | --- |
| `/updates` | Latest Updates | government — agencies, officials, programs |
| `/community-notices` | Community Notices | non-government — churches, nonprofits, community groups |

Three dead ends were checked before building anything:

- **`contentSource.type` encodes FORMAT**, not sector — `manual`,
  `expert-session`, `faq`.
- **`contentType` cannot carry it either.** Wilmington Housing Authority publishes
  BOTH a news item and a flyer, and both are government. A rule of the form
  "flyers are community" would have been wrong, and wrong in a way that only
  showed up on inspection of the data.
- **`contentSource.name` is not a key.** The same agency appeared as *"Delaware
  DHSS"* and *"Delaware Health and Social Services"*; RSS items took their name
  from `feed.getTitle()` at runtime, so it was not authored at all.

**Four `type`-ish fields now exist and only one was deleted.** Recorded because
the next person will meet all four: `contentType` (what the content IS),
`NewsItem.type` (`deadline`/`policy-update` — domain data), `contentSource.type`
(ingestion format), and `SearchResult.type` (a search projection). The registry
field is called **`sector`**, never `type`, for exactly this reason.

## The provenance model

`app/data/content-sources.json` — 14 producers. **Identity normalizes around a
stable id; a single registry owns producer metadata; records reference the
producer by id and never duplicate its attributes.** `name` resolves from the
registry at load (the Normalize stage, in the repositories), which is what
collapsed the two DHSS spellings into one agency.

**RSS provenance is configuration, never a runtime value.** The registry entry
carries `feedUrl`, so `RssFeedService` reads its feed list from the registry and
stamps each item with that entry's id. A feed cannot be added without declaring
who publishes it. `news.rss.urls` was retired from `application.properties` —
**deployment-visible**, and the reason is that a URL alone left identity to be
guessed from a feed title the upstream publisher can change.

## THE FAILURE BOUNDARY — the most important decision here

> **Provenance resolution is a CAPABILITY, not a VALIDITY GATE.**

An unknown `contentSource.id` means the item **cannot participate in
sector-scoped views, and nothing else.** It is still valid CivicContent —
browsable, searchable, classifiable, present on its category and topic pages.

**The codebase already worked this way.** Decision 036 established that content
with a category but no `subcategory` is *fully* classified, not half-classified —
it simply cannot appear on a topic page. A missing optional dimension narrows
**where** content can appear; it never invalidates the content.

**Exclude-and-log, not throw-at-startup.** Three arguments, the third decisive:

1. A civic service must not go offline because one JSON record has a typo.
2. `validate_content_sources.py` is the real gate — build-time, blocking. Runtime
   is defense in depth, and defense in depth must not be the thing that breaks.
3. **Throwing would violate the boundary itself.** Failing startup on an
   unresolvable id makes provenance a global validity requirement for all
   CivicContent — precisely what the boundary forbids. Exclude-and-log is not a
   weaker form of failing fast; it is the behaviour the architecture implies.

Observability, since an ERROR line is easy to lose: a **startup summary** on
`ApplicationReadyEvent` — `14 producers, 1 UNRESOLVED reference(s) [...]`.
Deliberately **not** on `/api/health`, which returns a bare `"OK"` and lives in
`ResourceController`; reshaping a liveness probe to carry editorial diagnostics
would give a resource controller a reason to know about content sources.

**Verified live, not assumed.** A record was planted referencing
`de-department-of-education`: absent from both sector pages, **still present on
`/api/category/housing` and in `/api/search`**, validator exit 1, startup summary
naming it.

## Amending Decision 041 — metadata-driven grouping is permitted

Latest Updates exposed a boundary case 041's wording was too broad to express.
The core rule is unchanged; a permission is added:

> **Presentation may group or organize existing CivicContent by controlled
> metadata** — `contentType`, category, `ContentSource` — **when that grouping
> represents a meaningful user-facing discovery model.** Such groupings must use
> **generic presentation components** rather than a component or domain concept
> per metadata value. **Empty groups are not rendered.**

The distinction that makes it safe: grouping **reads** metadata the domain
already owns. It adds no type, field or class. The violation would not be the
grouping — it would be `LawGroup` beside `NewsGroup`, enumerating the metadata in
code. One generic `UpdateGroup` renders every group, so a sixth ContentType costs
nothing here. **Empty groups cannot reach the client at all**: the server never
builds one, so the rule holds by construction rather than by a frontend guard.

Written into `docs/architecture/05-front-door.md`, which is canonical.

## Closing Decision 036: `UpdateItem.type` is DELETED

Not deprecated, not unused — **absent**, on every surface: model, services,
serialized payload, Java fixtures, frontend types, components and tests. Verified
by absence rather than by assertion, surface by surface.

One test lost its point in a way worth recording. `CategoryUpdates.test` used to
render `RESOURCE` carrying `type: 'news'` — a decoy proving the badge read the
right field. **That disagreement is no longer expressible**, because there is now
one identifier. The test kept its name and lost its decoy.

## Verification

289 backend (+11 ContentSourceService, +1 failure boundary) · 63 frontend ·
`tsc` clean · **5 validators exit 0**, the new one negative-tested on each rule ·
live sector split confirmed on real data, including both counter-examples ·
`index.css` ratchet unchanged.

**Next: the remaining destinations** — Slice G's organization directory behind
Find Help, then Discover, Community and About. Community Notices still has **no
homepage entry point**, which is the first thing to fix now that it is real.

---

# Decision 046 — Community Notices: four views, three kinds, one page

**Slice J.** Slice I made `/community-notices` real and left it a flat feed with
**no homepage entry point**. This slice turns it into a destination: four
resident-facing discovery views, reachable by URL, reachable from the homepage.

## The data-model question, surfaced rather than invented around

The instruction was explicit: *if an existing field cannot reliably distinguish
Events, Meetings, Announcements or Flyers, identify that as a data-model question
during scoping rather than silently inventing a frontend-only classification.*

It cannot. Measured against the real data before writing any code:

| View | Derivable from existing fields? | Items |
| --- | --- | --- |
| **Flyers** | yes — `contentType = FLYER` | 5 |
| **Announcements** | yes — `contentType = NEWS` + community sector | 3 |
| **Events** | only via `event_date` — but every dated community item **was** a flyer, so Events would have been the same five items under a second name, and all five dates had passed | 0 upcoming |
| **Meetings** | **no.** "meeting" appeared in **0 of 21 records**, and no field could have expressed one | 0 |

Two of the four views were underivable, and one of the two derivable ones would
have been a duplicate. A frontend heuristic here would have been guessing.

## The answer: a controlled kind vocabulary in `tags`

```json
{ "version": 1, "noticeKinds": ["event", "meeting", "announcement"], "categories": [ … ] }
```

**No new field, no new ContentType, no new domain class.** The kinds ride in the
existing `tags` array — the same mechanism as the Seniors discovery tag — and the
vocabulary lives in `taxonomy.json`, which already declares itself the single
source of truth for controlled vocabulary. A seventh data file would have brought
a seventh loader and a seventh validator with it.

It is genuinely **editorial, not presentational**: a church posting a notice knows
whether it is a fundraiser or a public meeting. First Step records what the
producer already knows. That is what keeps this inside Decision 045's amendment
rather than outside Decision 041.

### Three kinds, not four — and the asymmetry is the point

**"Flyer" is not a kind of notice. It is a form a notice takes.** It stays on
`contentType`, an axis that already existed. Adding a fourth kind would have
encoded the same fact twice and let the two copies disagree.

```
events         tags contains "event"        (any contentType)
meetings       tags contains "meeting"      (any contentType)
announcements  tags contains "announcement" (any contentType)
flyers         contentType = FLYER          (any kind)
```

## Views are LENSES, not buckets

The asymmetry above is exactly why the views **overlap by design**. A health-fair
flyer carries kind `event` and appears in **both** Events and Flyers, because
"what is happening?" and "what posters are up?" are different questions about the
same item. On live data, `events ∩ flyers = {FL-001, FL-004, FL-006}`.

This is not a defect to reconcile. Category and topic pages already overlap the
same way. A partition would have forced a false choice onto every item that
legitimately answers two questions.

`TaxonomyService.noticeKindOf` returns empty for **zero kinds and for more than
one** — never guessing which of two wins. Two kinds is an authoring error the
validator blocks; if one silently won, the record would look correctly filed on
whichever page it landed on.

## One page architecture, five routes

```
/community-notices                 overview — four cards + a preview of each
/community-notices/events          the same page, events view
/community-notices/meetings        …
/community-notices/announcements   …
/community-notices/flyers          …
```

**The URL is the source of truth.** The active view comes from `useParams`, never
from component state — the pattern `CategoryPage` and `TopicPage` already use. All
five routes work when typed, bookmarked, shared or reached with the back button,
without passing through the landing route first.

**One BFF endpoint**, `GET /api/community-notices/{view}`, page-shaped. `counts`
rides on **every** response because the four nav cards render on every route; a
separate counts call would have made the nav fill in after the page had drawn.

**An unknown view is a 404 naming the view**, not a quiet fall back to the landing
page. A view that EXISTS and is empty and a view that DOES NOT EXIST are different
facts and must not share a status code.

**The landing route is a destination, not a redirect.** It answers "what kinds of
community information can I find here?" with counts and a real sample of each. A
page that only routed onward would be a menu wearing a destination's URL.

### What differs between views, and why

Only the sort, and each choice answers a resident question:

- **Events / Meetings / Flyers** — soonest first. A past event at the top is
  useless; a flyer is a poster *for* something, and that something has a date.
- **Announcements** — newest first. Nothing to attend, so recency is the whole
  signal.

Plus one presentation departure: **Flyers gets a gallery grid.** The image IS the
content — the dates, the phone number, the languages it is printed in are all on
the poster — so a list of titles throws away the thing worth browsing.
`object-fit: contain` per Decision 044: `cover` cropped "FREE LEGAL HELP IS
AVAILABLE FOR DELAWARE RENTERS" mid-line. A **grid, not a carousel**: a carousel
is a preview device for someone who has not yet decided, and wrong on a page
someone reached by asking for flyers.

## Sector scoping is unchanged from Slice I

Every view is scoped to `Sector.COMMUNITY` through `ContentSourceService.isInSector`,
untouched. **Community-produced information is not the same thing as community
resources**, and a government flyer is not a community notice no matter how it is
tagged — Wilmington Housing Authority's flyer is correctly tagged, resolvable, and
absent from all four views.

An unresolvable producer is excluded from every view and **stays valid CivicContent
everywhere else** — the Slice I failure boundary doing its job here unchanged.
Provenance resolution remains a capability, not a validity gate.

## The homepage entry point

The row below the two-column split, previously **Community Information**, is now
**Community Notices** and links to the destination. The rename carries meaning:
the row above it (Community *Resources*) is services a resident can **use**; this
is what organizations are **telling** the neighborhood. Two different questions,
and the labels now say so.

### The rename turned working code into wrong code

Found by looking at the rendered page, not by any test. `getCarouselCards()` had
never been sector-scoped, so on live data the row showed:

| | | |
| --- | --- | --- |
| FL-005 | Disability Services & Benefits Info Fair | **government** |
| FL-007 | Free Furniture Giveaway | **government** |
| FL-004 | Back-to-School Supply Drive Fundraiser | community |

Two thirds of a row labeled *Community Notices*, linking to a destination that
correctly excludes both. A resident who saw the furniture giveaway and clicked
"See all community notices" would not have found it.

That was not wrong before this slice — under the vaguer heading "Community
Information" it was merely imprecise. **A rename can convert correct code into
incorrect code without touching it, because the label is part of the contract.**

Fixed by scoping `getCarouselCards()` to `Sector.COMMUNITY`, which is where the
row's other three content rules already lived. FlyerService gained a
ContentSourceService dependency and nine test files gained a constructor
argument — the honest price of keeping one rule in one place. Nothing is lost:
both government flyers already appear in Latest Updates, verified on live data
before the filter was added.

## Governance: a testing rule, written down because it cost real time

> **Negative tests must verify the intended failure path, not merely assert that
> invalid input produces some failure.** When validation rules overlap, isolate
> the rule under test or assert its specific diagnostic.

Added to `CLAUDE.md`. Slice I is the case study: a validator rule shipped **dead**
and its own negative test passed anyway, because an earlier rule caught the input
first and the exit code was non-zero as expected. The test asserted failure, not
the failure it was written for.

Applied here: the three new validator rules are each negative-tested against
**their own message**, and the controller's 404 test asserts
`"Unknown notices view: newsletters"` rather than merely a non-200.

## Known data-model gap, recorded rather than papered over

**News items carry no `event_date`.** An event-tagged news item therefore sorts by
its *publish* date in the Events view. Today that affects one record (NP-003) and
the ordering is not visibly wrong, but the field genuinely means something
different from the one beside it. Noted as debt; the fix is a real date model on
news, not a cast in the sort.

## Verification

309 backend · 73 frontend · `tsc` clean · 5 validators exit 0 ·
all five routes loaded **directly** · lens overlap and sector scoping asserted on
live data · `index.css` ratchet unchanged at 50.
