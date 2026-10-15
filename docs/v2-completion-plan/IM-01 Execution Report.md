# IM-01 execution report

**Disposition: Satisfied for the authorized editorial-expiry scope.**
**Execution date:** October 15, 2026 (America/New_York).
**Branch:** `copyofdev`. **Starting HEAD:** `2209f65f496f4e205b515e4ddf788e2fb035e35f`.
No commit, staging or push was performed; implementation remains reviewable in the working tree.

## Authority and protected starting state

This execution explicitly follows **IM-01-PC-01, PC-02, PC-03 and PC-04** in
[the approved decision record](IM-01%20Readiness%20Decisions%20PC-01%20to%20PC-04%20-%20Approved.md),
[the issued work order](IM-01%20Coding%20Agent%20Work%20Order.md), the Completion Contract,
approved P4/P5, Architecture Resolutions and the authoritative reconciled IM-01–IM-14 matrix.
The user separately authorized execution in this chat. The supplied
[IM-00 readiness report](IM-00%20Readiness%20Report%20-%20Revised.md) is historical baseline evidence.
These three supplied documents are retained verbatim, without rewriting their historical findings.

The environment initially had a clean `work` branch at main commit
`de6a55995d394a5583241e23d7cd714c5bb75951`. After read-only status and remote checks,
the existing checkout was switched to a local `copyofdev` branch at the supplied remote HEAD.
The working tree was clean there too. Thus IM-00's formerly uncommitted FL-008,
ORG-064, producer and image additions are already committed in this starting baseline.
There were no preexisting local edits to overwrite. No catalog record, image,
classification, relationship, lockfile or dependency declaration was changed.

## Inspection and baseline repair (before expiry implementation)

Inspected CivicContent, Community, all five ContentTypes and seven concrete content
classes; JsonResource/News/Flyer/Faq/ExpertAnswer/Article repositories and their
interfaces; RSS discovery and signed-bill sources; Article publication guards;
CategoryContent/CategoryPage/TopicPage/Category/Taxonomy, Navigation, Updates,
CommunityNotices, Home/Pathway, FindHelp, Organization and Search services and
controllers; existing tests, application properties, frontend manifest/lockfile,
catalog validators and the completion-plan documents.

Findings confirmed: news `expires`/`active` normalize to canonical metadata but
resident reads ignored expiry/status; other concrete classes inherit nullable
canonical fields and do not acquire editorial expiry from their other dates.
Article approval already withheld drafts. RSS retains separate classified and
ungated signed-bill caches. Stored repository reads are distinct from resident
service inventory. Featured selectors and a dedicated All-[Category]-Help surface
are not implemented; existing Topic browsing covers resources and flyers.

FL-008 is present, classified Community Support, associated in JSON with ORG-064
and producer `westside-grows-together`. No reclassification was needed.
Only two existing assertions changed in `EditorialStabilityTest`:
Community Support **61 → 62**, total **238 → 239**. These are protected **stored
category-placement counts**, including dual-classification, not unique-record
counts and not current discoverability counts. Existing comments about the earlier
historical baseline were not rewritten.

| Stage | Actual command | Result |
|---|---|---|
| Untouched copyofdev | `bash /workspace/.firststep-tools/im01-mvn test` | 371 tests; 2 failures; 0 errors/skipped; exactly the two count assertions |
| Reconciled, before expiry code | same | 371 passed; 0 failures/errors/skipped |
| After expiry implementation | same | **417 passed; 0 failures/errors/skipped** |

The local wrapper executes Maven 3.9.9 from `backend/`, using verified Temurin
25+36 (repository-pinned toolchain; Java 17 compile target), explicit cloud proxy
settings, system Java CA trust store, external Maven cache and toolchains XML.
Its Maven arguments are `-B -ntp -s /workspace/.firststep-tools/settings.xml
-t /workspace/.firststep-tools/toolchains25.xml
-Dmaven.repo.local=/workspace/.firststep-tools/m2 test`.
No signature, checksum or TLS verification was disabled.
Actual baseline failures and separate pass summaries are retained under
[evidence/im01](evidence/im01/01-original-baseline.txt).

## Implementation and integration

New production files under `shared/lifecycle/`:

- `EditorialLifecycle.java`: immutable assessment of ACTIVE, APPROACHING_EXPIRY,
  EXPIRED, editorial-expiry eligibility and editorial-attention requirement.
  It parses only canonical `expirationDate`. Missing/blank expiry is ACTIVE for
  this condition alone, without changing dates, verification or other metadata.
  A malformed nonblank date raises a parsing error rather than silently becoming
  a valid undated item. Independent inactive/withdrawn status is excluded from
  current inventory; no generic eligibility Boolean is persisted.
- `EditorialExpiryConfiguration.java`: injectable UTC Clock as an instant source;
  `app.editorial-expiry.approaching-days` (30), `default-zone`
  (America/New_York), and `community-zones.<community-id>` overrides. Wilmington
  is explicitly configured. Other ids inherit the configured deployment zone
  unless overridden; the server's implicit timezone is never used. No Community
  rollout, identity or cardinality change was introduced.

Calendar-date arithmetic begins approaching expiry at expirationDate minus 30
calendar days, retains eligibility through expirationDate and expires at the next
local midnight, including DST changes. Attention is independently true for
approaching/expired assessments; reporting and Admin presentation belong to IM-02.

Existing production changes:

| File/class | Change and downstream coverage |
|---|---|
| `resource/service/ResourceService.java` | Filter current `getAllResources/getAll`; supplies Get Help, Topic, category summary/preview, Search, Find Help, Organization offerings, navigation/counts and AI retrieval. Historical detail retains status guard. |
| `news/service/NewsService.java` | Filter current news before Category Policy Info, Updates, notices, Search, summaries, navigation and AI retrieval. |
| `flyer/service/FlyerService.java` | Filter current list and therefore carousel, Topic/category, Search, Updates, notices and counts. Detail retains status guard. No event-date eligibility. |
| `expert/service/FaqService.java` and `ExpertAnswerService.java` | Filter current inventories before supporting collections, Updates, navigation and homepage FAQ Originals; historical details retain status guards. |
| `originals/service/ArticleService.java` | Current approved inventory additionally applies expiry/status. Historical article detail remains approved-only and public-status-only; expiry does not indiscriminately turn it into 404. |
| `news/service/RssFeedService.java` | Filter both current discovery and signed-bill presentation getters; retain full internal caches, existing relevance distinction and no publication-age heuristic. Supplies category/Updates/navigation and homepage laws. |
| `news/repository/JsonNewsRepository.java` | Retain explicit canonical expirationDate/status when legacy fields are absent; legacy expires and active=false still normalize. Canonical withdrawal cannot be erased by active=true. |
| `application.properties` | Configure the single 30-day window and Community-zone policy. |

The calculation is applied at shared inventory boundaries, not reimplemented in
each destination. Existing classification, sector, Community filters, organization
relationships and article approval remain with their existing owners. Collection
and navigation counts consequently count filtered items. Repositories retain all
stored records, including expired ones. Existing test constructors now receive an
explicit fixed-clock lifecycle fixture; their unrelated assertions are unchanged.
Only the two PC-03 baseline assertions were revised.

## Acceptance evidence

All tests named below passed in the final full backend run.

| Work-order item | Evidence |
|---|---|
| 1 — exact lifecycle boundaries | `EditorialLifecycleTest.shouldUseThirtyCalendarDaysAndInclusiveNewYorkExpiry`: 5 instants spanning the exact window start and inclusive expiration-day end. `shouldHonorConfiguredWindow` verifies a 7-day override. |
| 2 — Clock, DST, other zone | `shouldPreserveLocalExpiryAcrossSpringDaylightSaving` (4), `shouldPreserveLocalExpiryAcrossAutumnDaylightSaving` (4), `shouldResolveEachCommunityZoneWithoutChangingTheAlgorithm`, `shouldUseConfiguredDeploymentZoneForUnlistedCommunities`; `EditorialExpiryApiTest.shouldApplyBoundCommunityTimezoneConfiguration` verifies actual Spring property binding with a test-only Tokyo Community id. |
| 3 — missing expiry by class | `shouldAllowMissingExpiryForEveryContentClassWithoutInventingDates`: RESOURCE, NEWS, FLYER, LAW, FAQ, Expert Answer, Article (7 cases); old publication/audit/event/session dates do not create expiry or verification. |
| 4 — independent guards | `shouldKeepStatusIndependentOfExpiryState` (7 cases), `EditorialProjectionTest.shouldRetainHistoricalRecordsAndKeepPublicationStatusIndependent`, existing ArticleService/controller tests, and two new JsonNewsRepository normalization tests. Drafts remain private; withdrawn items remain excluded despite missing expiry. |
| 5 — projections, counts, retention | Seven `EditorialProjectionTest` cases cover all seven collections, Topic/navigation counts, Updates/notices, Search/category previews, Find Help/Organization, homepage Originals/laws/flyers and stored/historical retention, using active/approaching/expired/inactive/withdrawn fixtures. Approaching items remain included. |
| 6 — real expired NP fixtures | `EditorialExpiryApiTest.shouldExcludeRealExpiredHousingPolicyFixturesFromCategoryAndGovernmentUpdates`: NP-001 (2026-06-30), NP-002 (2026-07-14), NP-006 (2026-08-07) retained in repository, EXPIRED at fixed October 15 and absent from Housing/government Updates. |
| 7 — stored versus current | Reconciled `EditorialStabilityTest` remains 239/62. Real API tests retain 10 news records but deliver 5; Housing Policy Info has 2 current items and Housing NEWS count is 3 including one approved Article. Current Housing total is 51 in the runtime check. |
| 8 — event independence | Missing-expiry seven-class tests and `shouldPreservePastEventFlyersAndTheirHistoricalDetailLinks` retain FL-001–FL-008, with no fabricated editorial date. Community meetings retain FL-002 while excluding editorially expired NP-009/010. This is not a claim of occurrence eligibility completion. |
| 9 — scope protection | No ContentType/model hierarchy, generic persisted residentEligible, event model, workflow, Admin dashboard, selector, ranking, catalog removal or new package was introduced. `app/data` and dependency/lockfile diffs are empty. |

Focused additions: 31 lifecycle cases, 7 synthetic projection cases, 6 real-data
Spring/MockMvc API cases and 2 news normalization cases = **46 new cases**.
The final suite contains **417 tests**. Intermediate new-test iterations corrected
test compilation/DTO-path assumptions and preserved the existing approved-Article
contribution to NEWS counts and the past-event FL-002 meeting; they did not weaken
production rules or existing assertions. Final results supersede those iterations.

## Other verification

- `npm ci` in `frontend/`: completed with the committed lockfile unchanged.
- `npm test`: **19 files, 126 tests passed**; no frontend behavior changed.
- `npx tsc --noEmit`: exit 0.
- `npm run build`: exit 0 (TypeScript and Vite production build).
- All six Python catalog validators: exit 0 (`validate_content_sources`,
  `validate_homepage`, `validate_navigation`, `validate_news`, `validate_organizations`,
  `validate_schema`). These were run on unchanged catalog inputs.
- Maven `-DskipTests package`: exit 0; packaging is separate from the passing
  417-test run and is not represented as another test run.
- Started the packaged app from the repository root on port 8081, with RSS initial
  delay extended and AI disabled. Ten local endpoint checks passed: current news,
  Housing, flyers, meetings, Home, Find Help, government/community Updates, Housing
  eviction-prevention Topic, and FL-008 detail. Runtime summary is retained in
  `evidence/im01/04-runtime-smoke.json`. Live RSS transport/AI were not required or
  tested. Fixed-clock regressions do not depend on runtime wall-clock checks.
- Touched Java production/test files have synchronized full annotated copies in
  `references/` and `references/tests/`, scoped to this package per `claude.md`.

## Deferred integration and limits (not reopened scope)

| Owner | Remaining responsibility |
|---|---|
| IM-02 | Consume `EditorialLifecycle.assess` for approaching/expired diagnostics and human attention signals; no dashboard or workflow here. |
| IM-03 / IM-04 | Expert classification integrity and explicit non-RESOURCE Get Help semantics remain unchanged. |
| IM-05 | Integrate the same tested `isCurrent/assess` contract into dedicated All-[Category]-Help and expanded Topic inventories when built. Existing resource/flyer Topic browsing is filtered now. Existing Topic navigation counts cover all classified types, while the Topic list covers RESOURCE/FLYER; this preexisting breadth difference was not redesigned. |
| IM-06 / IM-07 | Featured Topic/item selectors do not exist yet. Use current inventories and the shared assessment; approaching-expiry is attention, not automatic ranking priority. |
| IM-08 / IM-09 | Carry the rule into redesigned Category composition and controlled collections; existing implemented collections are integrated now. |
| IM-10 | Occurrence eligibility, date-only events, endsAt and unavailable occurrence dates; past flyers are not mass-expired. Current-event presentation is not complete under IM-01 alone. |
| IM-12 | Full 99-rule/R5 audit, Community partition/scoping and provenance/association audits. Existing blank-Community aggregation and source ownership were not refactored. |
| IM-13 | Combined event/editorial and J1–J7 release regression, manual responsive/accessibility quality gates beyond the existing automated frontend suite. |
| IM-14 | Final evidence/documentation reconciliation, including the existing Admin Guide. That guide's Library link was inaccessible and its text was not supplied; approved PC-01/02 fully supplied the implementation parameters. No rival guide was invented. |

**Final finding:** PC-01 editorial eligibility and current-inventory enforcement are
implemented and evidenced; PC-02 event separation, PC-03 baseline sequencing and
PC-04 audit ownership are preserved. IM-01 is satisfied within its approved
boundary. This is not acceptance of the downstream packages or a v2 release declaration.
