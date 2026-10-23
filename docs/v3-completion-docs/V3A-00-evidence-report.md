# First Step V3A-00 — Evidence Completion Report (Revision 1)

Audit date: October 23, 2026. Authority: [approved work order](evidence_work_order.txt) and the V3A-00 Audit Supplement and Report Correction instruction. Application code, tests, dependencies, catalog records and approved V3A scope were not edited. New files are audit documentation, probes and evidence only.

## Revision record

| Revision | Content |
|---|---|
| R0 | Original report. Preserved byte-for-byte as [evidence/V3A-00-evidence-report.r0.md](evidence/V3A-00-evidence-report.r0.md) (hash in [supplement-sha256.json](evidence/supplement-sha256.json)). |
| R1 | This document. Adds the supplemental browser audit and corrects R0 where its conclusions went beyond its evidence. All R0 evidence files are retained unchanged; all 66 entries in [sha256.json](evidence/sha256.json) still match. |

Changes are tagged inline. **[R1 added]** marks new evidence or findings. **[R1 corrected]** marks an R0 statement that is replaced, with the reason. Untagged text is carried forward from R0 unchanged in substance.

R1 corrections in brief:

1. `/find-help` was a live, unaudited P08 surface. It is now audited.
2. The nonempty Events journey was rated Established without evidence. It has now been demonstrated with a controlled fixture; the live-catalog journey remains Unverified.
3. Government Updates community scoping was listed as a functional gap. It is now an open architectural requirement.
4. OR-003 and the 503 article message are now registered as explicit findings instead of being described in prose.
5. A new content-integrity finding: four of six published flyers show posters for different events.
6. The port-8080 reset is now explained.
7. Disposition: **Verification Incomplete → Escalation Required** (§10).

## 1. Executive Summary

The current `copyofdev` baseline is `a4ed4aeb0ff70bcbb34e164cd9ec87df695245d3`. It descends from the previously inspected `bb4d55ee0074452223e517ede48a1ed30f5e4e1a`. The intervening commit adds the First Step Admin Guide only, with no application change. HEAD and the clean tracked tree were reconfirmed at the end of R1.

These resident surfaces contain working functionality: Category, Topic, All Help, Notices, Updates, Organization and **[R1 added]** Find Help. The homepage needs structural redesign, and its Discover entry leads to a placeholder. Article delivery works.

**[R1 corrected]** R0 described editorial readiness as "incomplete". The precise finding is that the only publicly served First Step Original (OR-003) is a test fixture, and it appears on the homepage. Separately, **[R1 added]** four of the six published community flyers display poster artwork for a different event, organization and date than their caption. The Flyers page tells residents that "the flyer itself carries the details", so these are resident-facing integrity defects, not presentation gaps.

V3A should build on the existing capabilities. It should not assume universal completion or universal redevelopment.

**Disposition: Escalation Required [R1 corrected from Verification Incomplete].** All verification an auditor can execute in this environment is now complete or explicitly recorded as not testable. What remains needs project authority:

- The controlled V3A documents (acceptance contract, V3A-01–09 framework, Homepage/Housing design decisions) are not in the repository, so official clauses cannot be assessed.
- Two content-integrity issues need editorial decisions.

See §10.

## 2. Environment & Baseline Verification

| Item | Recorded environment |
|---|---|
| Repository | `/Users/composedmedia/Documents/Projects/FirstStep` |
| Branch / SHA | `copyofdev` / `a4ed4aeb0ff70bcbb34e164cd9ec87df695245d3` (reconfirmed after R1) |
| Initial tree | Only untracked `docs/v3-completion-docs/`; no tracked modifications |
| Java | Oracle JDK 25.0.2; Maven compiles Java 17 bytecode with the required JDK 25 toolchain |
| Maven | 3.9.16 at `/Users/composedmedia/tools/apache-maven-3.9.16/bin/mvn`; no wrapper; `mvn` not on PATH |
| Node / npm | 26.7.0 / 11.19.0; package has no engines constraint; Docker uses Node 24 |
| Frontend | React 18, TypeScript, Vite; installed dependencies used without installation |
| Configuration | Complete `app/data`, editorial manifest and original seasonal media; no DB required |
| App location | Current SPA `/app-next/`; `/` is the retained legacy demo. Work-order routes are SPA-relative |

See [environment evidence](evidence/working-tree.txt), [Java](evidence/java-version.txt), [Maven](evidence/maven-version.txt), [Node](evidence/node-version.txt), [npm](evidence/npm-version.txt).

Documented commands are in `docs/authoring/v2-release-operations.md`:

- Combined packaging: `docker compose up --build -d app`.
- From the repository root: `java -jar backend/target/firststep-backend-0.1.0.jar --app.data.dir=app/data --app.seasonal.images.dir=backend/src/main/resources/static/images/seasonal`.
- Frontend development: `cd frontend && npm run dev -- --host 127.0.0.1`. Vite forwards `/api` to port 8080.
- Production build: `cd frontend && npm run build`.

### Port-8080 limitation and runtime classes [R1 corrected]

R0 recorded that "port 8080 listened but returned connection resets" and worked around it with `-Djava.net.preferIPv4Stack=true --server.port=18080`. R1 found the cause.

- The R0 8080 JVM started at 13:17:34 from the **pre-existing** jar ([runtime-approved.log](evidence/runtime-approved.log), line 41).
- The approved `mvn -B verify` then ran `spring-boot:repackage`. At 13:18:53 it logged "Replacing main artifact …firststep-backend-0.1.0.jar" ([backend-verify-approved.log](evidence/backend-verify-approved.log), lines 22756–22763). That overwrote the archive underneath the running JVM.
- The same JVM's shutdown later failed with `NoClassDefFoundError: ch/qos/logback/classic/spi/ThrowableProxy`. That is the signature of classes being loaded lazily from a replaced archive.
- The sandbox's network restrictions were also in force at that time.
- **Reproduction:** the current verified jar (sha256 `e890d43a…11a8`) was started on plain 8080 with no IPv4 flag. It served `/api/home` and `/api/find-help` with HTTP 200 on both `localhost` and `127.0.0.1` ([supplement-runtime-8080.log](evidence/supplement-runtime-8080.log)).

**Conclusion:** the reset was an artifact of the audit environment (a jar replaced under a live JVM, plus the sandbox), not an application defect. R1 browser checks used the standard configuration: jar on 8080 and Vite's own proxy, with no in-memory forwarding except the fixture and fault scenarios in §4A.

Verification classes, which must not be conflated:

| Class | Status |
|---|---|
| Development runtime (verified jar + Vite dev server) | **Exercised** in R0 and R1 |
| Locally built jar serving the SPA | **Not applicable as built.** A Maven-only jar contains no `static/app-next`, as the runbook states. Its `/app-next/` paths returned HTTP 500 (see D-10) |
| Combined Docker production package (SPA baked into the jar, same-origin serving, deep-link fallback) | **NOT TESTED** in V3A-00. Last verified in v2 IM-13 |

## 3. Automated Test Results

Unchanged from R0. These are the counts from this audit's execution, and they are preserved. R1 did not re-run the suites, because no code changed.

| Exact command | Result | Executed / passed / failed / errors / skipped | Evidence |
|---|---|---|---|
| `npm test -- --run` | FAIL, Node environment | 162 / 145 / 17 / — / 0 | [raw frontend log](evidence/frontend-test.log) |
| `NODE_OPTIONS=--no-experimental-webstorage npm test -- --run` | PASS | 162 / 162 / 0 / 0 / 0; 23 files | [compatible frontend log](evidence/frontend-test-compatible.log) |
| `npm run build` | PASS | TypeScript compilation and Vite bundle; no test count | [build log](evidence/frontend-build.log) |
| `/Users/composedmedia/tools/apache-maven-3.9.16/bin/mvn -B verify` (sandbox) | FAIL, environment | 631 / 343 / 0 / 288 / 0 | [sandbox Maven log](evidence/backend-verify.log) |
| Same Maven command, approved outside sandbox | PASS | 631 / 631 / 0 / 0 / 0; build/package/verify successful | [accepted Maven log](evidence/backend-verify-approved.log) |
| `python3 -m unittest discover -s data-cleaning/tests -v` | PASS | 16 / 16 / 0 / 0 / 0 | [Python suite](evidence/python-0.log) |

The raw frontend failures include the Accessibility controls, SiteHeader and CategoryDestination tests, with `TypeError: Cannot read properties of undefined (reading 'clear')` and downstream duplicate/missing DOM queries. The Node setting documented in the v2 IM-07–IM-13 evidence resolves them without code changes; it is now logged as tooling issue T-01. Maven's sandbox run reports `Could not initialize plugin: interface org.mockito.plugins.MockMaker`. Full names and messages remain in the raw logs.

All six established validator commands passed: `validate_content_sources.py`, `validate_homepage.py`, `validate_navigation.py`, `validate_news.py`, `validate_organizations.py` and `validate_schema.py`, all under `data-cleaning/scripts/`. `validate_schema.py --input app/data/resources.communities.json` also passed ([commands and exit codes](evidence/python-commands.json)).

Passing validation does not establish that resources, external destinations or **[R1 added]** flyer artwork are substantively true. The validators passed while C-02 (§6B) is present.

## 4. Resident Page Implementation Inventory [R1 revised]

Routes are relative to `/app-next`. R0 browser evidence: [probe](evidence/browser-audit.mjs), [route results](evidence/browser-results.json), [payloads](evidence/api.json); screenshots `page-<width>-<index>.png`. R1 evidence: [supplement-audit.mjs](evidence/supplement-audit.mjs), [supplement-results.json](evidence/supplement-results.json); screenshots `supp-<scenario>-<width>.png`. Every listed route was opened directly at 1280 and 375 px. No uncaught page errors and no horizontal document overflow were recorded in either pass.

| Page | Route | Existing components | Backend/API | Classification | Functional gaps | V3A gaps | Test evidence | Treatment |
|---|---|---|---|---|---|---|---|---|
| P01 Homepage | `/` | HomePage, MissionCards, CommunityResources, FirstStepOriginals, CommunityInformation, NewLaws | `/api/home` | Partially Implemented | Discover card → stub (D-04); capped FAQ previews have no detail link; flyer enlargement absent (D-02). **[R1]** First Original shown is the OR-003 fixture (C-01) | Cityscape/fade, maintained What's New, event column, 2×4 categories, inclusive First Rate News and Program Updates previews absent | R0 index 0; R1 Connect→Find Help history | Structural redesign + functional completion |
| P02 Category | `/category/housing` | CategoryPage, CategoryBrowse, ContentCollection, ContentCard, context rail | `/api/category/housing` | Established | No primary-path failure demonstrated; provider/expert/law context can be absent | Existing Get Help block remains; approved Housing layout unavailable | R0 index 1; CategoryDestination/CategoryPage suites; R1 Updates→Housing history | Structural redesign |
| P03 Topic | `/category/housing/transitional-housing` | TopicPage, breadcrumbs, ContentCollection | `/api/category/housing/transitional-housing` | Established | Missing outbound URLs produce unlinked titles; external destination correctness not tested | Mobile cards remain full result cards; exact criteria unavailable | R0 index 2; TopicPage tests | Visual refinement, pending contract |
| P04 All Category Help | `/category/housing/help` | TopicPage reserved help mode, topic details, progressive collection | `/api/category/housing/help` | Established | No inventory loss: 36 Resources, all reachable via "32 more" | Must stay reachable after Get Help removal | R0 index 3; HelpBrowse tests; [journeys.json](evidence/journeys.json) | Verification only / visual refinement |
| P05 Notices overview | `/community-notices` | CommunityNoticesPage, previews, view links | `/api/community-notices` | Established | No flyer enlargement (D-02); loading text not announced (D-03) | Overview/event-preview arrangement needs contract | R0 index 4; R1 fixture overview shows "Events · See all (4)" preview | Visual refinement + functional completion |
| P05 Events | `/community-notices/events` | Shared notices page, ContentCard | `/api/community-notices/events` | **[R1 corrected]** Established, with controlled-fixture evidence only. **Live nonempty journey: Unverified** | Live catalog has 0 current events: every event date precedes the audit date (C-03) | V3A event presentation needs current dated data | R0 index 5 (empty); **R1** fixture: 4 events, soonest first, posters load, Events↔Flyers back/forward restored, both widths (`supp-events-fixture-*`) | Verification only, then visual refinement |
| P05 Meetings | `/community-notices/meetings` | Shared notices page, ContentCard | `/api/community-notices/meetings` | Established | External target not checked | Contract unavailable | R0 index 6; 1 meeting | Visual refinement |
| P05 Announcements | `/community-notices/announcements` | Shared notices page, ContentCard | `/api/community-notices/announcements` | Established | External target not checked | Contract unavailable | R0 index 7; 3 announcements | Visual refinement |
| P05 Flyers | `/community-notices/flyers` | NoticeGallery | `/api/community-notices/flyers` | Partially Implemented | No enlargement (D-02). **[R1]** 4 of 6 posters contradict their record (C-02) | Grid, no current React carousel | R0 index 8; image load and contain sizing | Functional completion + content correction + visual refinement |
| P05 invalid view **[R1 added]** | `/community-notices/<unknown>` | Shared notices page | `/api/community-notices/<unknown>` | Established (error path) | Shows the raw i18n key `notices.not-a-view.intro` and the raw server message (D-06) | — | `supp-route_community-notices_not-a-view-*` | Functional completion (copy) |
| P06 Program Updates | `/updates` | UpdatesPage, UpdateGroup | `/api/updates/government` | Established | No interactive filters; loading text not announced (D-03) | Homepage preview absent; current heading is "Latest Updates". **[R1 corrected]** Community scope is an open requirement (A-01), not a confirmed gap | R0 index 9; UpdatesPage tests; **R1** Home→Updates, Article→Updates, Org→Updates, Updates→Housing history all restored | Structural redesign; resolve A-01 |
| P07 Editorial detail | `/originals/OR-003` | ArticlePage, lede, dates, plain-text body | `/api/originals/OR-003` | Partially Implemented | **[R1 corrected]** Served body is a test fixture (C-01); 503 shown as "Article not found" (D-01); no image slot | Inclusive First Rate News presentation exceeds the producer-only homepage preview | R0 index 10; **R1** Home→Article and Article→Updates history restored; 404-vs-503 comparison | Content completion + defect fix + visual refinement |
| **P08 Find Help [R1 added]** | `/find-help` | FindHelpPage, ContentCard; reached from Home **Connect** card | `/api/find-help` (7 gateways, 6 open-to-everyone items, 64 organizations) | **Established** | "1 organizations" (D-07); no-match and empty states give no guidance (D-08); query lost on Back (D-09) | Not covered by any supplied V3A design; placement relative to the new homepage undecided | §4A.1; `supp-find-help-*` | Verification only + small functional fixes; package ownership pending |
| P08 Organization | `/organization/american-red-cross` | OrganizationPage, offering cards | `/api/organization/american-red-cross` | Established | Sparse registry data limits identity/contact/actions. Successful page has no internal link in `main`; exits via the Primary nav | Profile expansion not authorized | R0 index 11; **R1** Find Help→Org, Category→Org, Org→Updates history restored; invalid slug shows "Organization not found" | Visual refinement |
| P08 Resource cards | Category, Topic, Help, Find Help, Organization | ContentCard, ContentCollection | Public ContentItem projections | Established | Cards correctly omit missing URLs/provider details | Shared design change has broad impact | R0 indices 1–3, 11; R1 Find Help open-to-everyone cards | Visual refinement |
| P09 Shared visual system | Shared page frame | SiteHeader/Footer, tokens, typography roles, high-contrast theme, i18n | None | Partially Implemented | About/Community lead to stubs (D-04); placeholder footer contact (C-04); inconsistent announcements (D-03); stub pages lack an `h1` (D-05) | Reuse the token/font/component foundation; resolve the overloaded color token | All widths; contrast/focus evidence; Accessibility suite | Visual refinement + functional completion |
| Linked Discover | `/discover` | StubPage | None | Placeholder | No discovery journey | Do not count the homepage pathway as complete | R0 index 15 | Functional completion; ownership pending |
| **Discover facet [R1 added]** | `/discover/:facet` | StubPage | None | Placeholder | `/discover/seniors` and `/discover/not-a-facet` render identically ("Discovery pathway · Coming soon."); facet not validated | Seniors is also "Coming soon" on Find Help | `supp-route_discover_*` | Functional completion |
| Linked Community | `/community` | StubPage | None | Placeholder | Header/footer route does not open the real Notices overview | Navigation decision needed | R0 index 14 | Functional completion |
| Linked About | `/about` | StubPage | None | Placeholder | Shell only | Copy needed if retained | R0 index 13 | Functional completion |
| **Catch-all [R1 added]** | `*` (e.g. `/no-such-route`) | StubPage | None | Established (route resolves) | Says "Page not found" then "Coming soon."; no `h1` (D-05) | — | `supp-route_no-such-route-*` | Functional completion (copy, heading) |

## 4A. Supplemental Browser and Route Evidence [R1 added]

Setup:

- Verified jar on 8080 serving the live catalog.
- The same jar on 18081 serving the Events fixture only.
- Vite on 5173 with its unmodified proxy.

Headless Chromium via the project's Playwright install, at 1280×900 and 375×900. Results: [supplement-results.json](evidence/supplement-results.json). Hashes: [supplement-sha256.json](evidence/supplement-sha256.json). Index of R1 files: [README-R1.md](evidence/README-R1.md). Uncaught page errors recorded: **0**.

### 4A.1 Find Help (P08)

| Scenario | Result (both widths unless noted) |
|---|---|
| Home **Connect** card → `/find-help`, Back, Forward | Navigates; Back restores `/`; Forward restores `/find-help` |
| Direct load | `h1` "Find Help"; no overflow; count announced in `role=status` ("64 organizations") |
| Category gateways | 6 links (`/category/housing`, `employment`, `health`, `legal`, `furniture-household`, `food`); Seniors shown as `aria-disabled` "Coming soon" |
| Gateway → Category, Back/Forward | Opens `/category/housing?communityId=wilmington-de` (`h1` Housing); history restored |
| Open to everyone | 6 resource cards |
| Directory search (labelled `#org-search`) | "red cross" → 1 entry ("American Red Cross"), status "**1 organizations**" (D-07) |
| No-match search | "zzzz-no-match" → 0 entries; the only feedback is "0 organizations", with no guidance or reset (D-08) |
| Directory → Organization, Back/Forward | `/organization/american-red-cross`; history restored. The search query is **not** retained on Back (empty field, 64 shown) (D-09) |
| Loading (2.5 s synthetic latency) | "Loading…" in `role=status` — correct |
| Empty payload (synthetic) | Gateway heading renders with no items; directory shows "0 organizations"; no explanatory empty message (D-08) |
| Error (synthetic 503) | Server `errorMessage` shown in `role=alert` — correct pattern |

### 4A.2 Discover facet and catch-all routes

| Route | Result |
|---|---|
| `/discover/seniors`, `/discover/not-a-facet` | Identical stub: "Discovery pathway / Coming soon. / ← Back to home"; heading is `h2`, no `h1` |
| `/no-such-route` (catch-all) | "Page not found / **Coming soon.** / ← Back to home"; no `h1` (D-05) |
| `/originals/OR-999` | `h1` "Article not found" in `role=alert`, return link — correct for a genuine 404 |
| `/organization/not-an-org` | `h1` "Organization not found" in `role=alert`, return link — correct |
| `/community-notices/not-a-view` | Shows the literal text `notices.not-a-view.intro` and "Unknown notices view: not-a-view" (D-06) |

### 4A.3 Browser history: Article, Updates, Organization (1280)

Successful Article and Organization pages render **no internal link inside `main`**; their in-page "Back to First Step" link exists only in the not-found state. Exits are therefore through the Primary nav, which carries `?communityId=` when present. All rows below restored on both Back and Forward.

| Journey | From → To |
|---|---|
| Home Original → Article | `/` → `/originals/OR-003` |
| Article → Primary nav Updates | `/originals/OR-003` → `/updates` |
| Home → Primary nav Updates | `/` → `/updates` |
| Updates → Primary nav Housing | `/updates` → `/category/housing?communityId=wilmington-de` |
| Category Organizations → Organization | `/category/housing?…` → `/organization/american-red-cross?communityId=wilmington-de` |
| Organization → Primary nav Updates | → `/updates?communityId=wilmington-de`. The query reaches the URL, but UpdatesPage does not send it to the API (A-01) |
| Find Help → Organization / Category | See 4A.1 |

The links inside `main` on Updates and Organization are all external (`target=_blank`); they open new tabs and create no history entries. Whether their destinations are reachable and correct is still NOT TESTED.

### 4A.4 Nonempty Events journey (controlled fixture)

Every live event date precedes 2026-10-23; the latest is FL-008 on 2026-10-17. The server uses `Clock.systemUTC()` with no configuration override. A legitimate current-event journey therefore cannot occur against the live catalog.

The fixture ([manifest](evidence/supplement-events-fixture.json)):

- A session-scratchpad copy of `app/data` with every flyer `event_date` moved forward 90 days and no other field changed. This was verified by a sorted-key diff.
- Served by the same verified jar on 18081.
- Only `/api/community-notices*` requests were redirected to it, in browser memory.
- `git status app/data` was clean before and after.

| Check | Result |
|---|---|
| Server selection (live vs fixture, same moment) | Live `EVENTS=0`. Fixture `EVENTS=4`: FL-004 (11-24), FL-006 (12-01), FL-001 (12-06), FL-008 (2027-01-15), soonest first. FL-002, FL-003, FL-005 and FL-007 were excluded by the server's notice-kind/lifecycle rules |
| Events view render | 4 cards with title, organization, summary and date; 4/4 posters loaded with record-title `alt`; no overflow at either width |
| Events → Flyers view, Back/Forward | Restored |
| Overview with events | Counts 4/1/3/6; Events preview with "See all (4)" |
| Live catalog positive journey | **UNVERIFIED** — no current data exists |

The fixture screenshot ([supp-events-fixture-1280.png](evidence/supp-events-fixture-1280.png)) is also where C-02 surfaced. The poster images are the live catalog's own files; the fixture changed only dates.

### 4A.5 Fault and loading probes

| Probe | Result |
|---|---|
| `/originals/OR-999` (genuine 404) vs `/originals/OR-003` with synthetic 503 | **Identical**: `h1` "Article not found", "This article is not available. It may have been moved, or it may never have been published." (D-01) |
| Notices, 2.5 s latency | "Loading…" paragraph has no `role` and no `aria-live` ancestor (D-03) |
| Updates, 2.5 s latency | Same: no `role`, no live region (D-03) |
| Category, 2.5 s latency | "Loading…" in `role=status` — correct |
| Backend: `GET /api/does-not-exist`, `/favicon.ico`, `/nope` | HTTP **500** `INTERNAL_ERROR` with an ERROR stack trace (`NoResourceFoundException`) instead of 404 (D-10) |

## 5. V3A Acceptance Crosswalk [R1 revised]

**Official acceptance clauses: BLOCKED — awaiting controlled-document availability.** The work order cites H-01, C-02, T-04, N-02, R-05 and S-06 only as examples and supplies no definitions. The V3A Page Requirements & Acceptance Contract (P01–P09), the V3A-01–09 framework and the approved Homepage/Housing design decisions were searched for and are not present in the repository. Every official clause for P01–P09 remains BLOCKED. No invented numbering is presented as approved authority.

The table below covers only behaviors spelled out in the work order and the R1 instruction. It is not a substitute contract.

| Surface / supplied requirement | Status | Evidence and limit |
|---|---|---|
| P01 direct homepage, navigation, category links, API payload | PASS | R0 index 0; `/api/home` retained |
| P01 hero/fade, separately maintained What's New, Community Events column | NOT IMPLEMENTED | Homepage shows mission cards and a Laws placeholder instead |
| P01 2×4 categories, inclusive First Rate News, Program Updates preview, mixed editorial arrangement | NOT IMPLEMENTED | Current seven pathways, capped producer-only Originals sidebar, lower flyer row |
| P01 flyer preview | PASS | Three images from the six-item payload; no carousel controls |
| P01 Connect → Find Help pathway **[R1]** | PASS | 4A.1 |
| P01 loading/error | NOT TESTED | Homepage sections render static frames during latency; no full visual matrix |
| P02 header, four featured topics, item/topic links, Get Help, What to Know, rail, Organizations | PASS | R0 payload and index 1 |
| P02 Community in URL and All Help entry | PASS | `communityId=wilmington-de` explicit; help endpoint returns 36 |
| P02 approved Housing layout | BLOCKED | Approved design document missing |
| P03 direct real Topic, breadcrumb, mobile presentation, back/forward | PASS | R0 index 2 |
| P03 exhaustive item destinations and state combinations | NOT TESTED | Full matrix not claimed |
| P04 full inventory, Category access, topic narrowing, expansion | PASS | 36 resources; "32 more" exposes all; narrowing → Emergency Shelter (9) |
| P04 every large-set/empty/community combination | NOT TESTED | Finite fixture; not load testing |
| P04 invalid route understandable | PASS | `/category/invalid/help` → topic-not-found + Category link |
| P05 overview and four direct views | PASS | R0 indices 4–8; URL-addressed views |
| P05 nonempty Events journey **[R1 corrected]** | PASS (controlled fixture) / **UNVERIFIED (live catalog)** | 4A.4. R0's implicit Established rating lacked positive evidence |
| P05 flyer media sizing | PASS | Posters load with `object-fit: contain` |
| P05 flyer artwork matches its record **[R1 added]** | FAIL | C-02: 4 of 6 published flyers |
| P05 flyer enlargement | NOT IMPLEMENTED | D-02 |
| P05 current React carousel | NOT IMPLEMENTED | Carousel survives only in legacy `static/app.js` |
| P05 source/detail metadata | PASS | Producer labels and dates visible; not full provenance |
| P05 invalid view understandable **[R1 added]** | FAIL | D-06 |
| P06 direct Updates, government classification, grouping, producer/date/link | PASS | Six-item GOVERNMENT payload; R1 history scenarios |
| P06 interactive filtering | NOT IMPLEMENTED | Grouped feed, no filter control |
| P06 homepage Program Updates preview | NOT IMPLEMENTED | No `programUpdates` field in `/api/home`; the existing feed could supply one |
| P06 Community-scoped Program Updates **[R1 corrected]** | BLOCKED — open requirement A-01 | Not a confirmed defect; depends on the contract |
| P06 loading announcement **[R1 added]** | FAIL | D-03 |
| P07 homepage Original, Article route, publish date, lede | PASS | OR-003 renders (mechanics only) |
| P07 inclusive Article/FAQ/Expert collection | PASS | Housing First Rate News: OR-003, FAQ-002, EA-001, EA-005 |
| P07 resident-ready article body | FAIL | C-01 |
| P07 failure states distinguish unavailability from absence **[R1 added]** | FAIL | D-01 |
| P07 article image/fallback | NOT IMPLEMENTED | No image slot |
| P07 internal editorial fields excluded from public DTO | PASS | Projection and DTO regression suites |
| P07 browser history **[R1 added]** | PASS | 4A.3 |
| P08 result cards, provider relationships, Organization direct route | PASS | Red Cross offering; association tests |
| P08 Find Help: direct load, gateways, directory search, Organization links, mobile, loading, error **[R1 added]** | PASS | 4A.1 |
| P08 Find Help empty / no-match guidance **[R1 added]** | FAIL | D-08 |
| P08 Organization browser history **[R1 corrected from NOT TESTED]** | PASS | 4A.3 |
| P08 external sites reachable/correct | NOT TESTED | Rendering a link does not prove the target |
| P09 token/font reuse, shared header/footer, high contrast, keyboard focus | PASS | Montserrat/Open Sans bundled; contrast toggled; focus outline |
| P09 sampled responsive layout | PASS | No overflow at either width in R0 or R1 |
| P09 loading/error announcements everywhere | FAIL | D-03 |
| P09 stub/catch-all pages have a page heading **[R1 added]** | FAIL | D-05 |
| P09 exhaustive contrast/focus/screen-reader conformance | NOT TESTED | No certification, device or screen-reader session |
| Discover facet / catch-all routes resolve **[R1 added]** | PASS (resolve) / Placeholder content | 4A.2 |
| Combined Docker package deep-link serving **[R1 added]** | NOT TESTED | §2 runtime classes |

## 6. Functional Defects Register [R1 replaces R0 §6 list]

The R0 journey narrative is retained in [journeys.json](evidence/journeys.json). R0 numbered items 1–6 are redistributed below and in §6B, §6C and §6D. Severity is the auditor's estimate of resident impact, not an approved priority.

**Product defects** (current behavior is wrong, independent of V3A design):

| ID | Defect | Evidence | Severity | Notes |
|---|---|---|---|---|
| D-01 | ArticlePage shows "Article not found … may never have been published" for an HTTP 503, identical to a genuine 404 | 4A.5 | Medium | Deliberate in code ("One failure state, deliberately. The only error this endpoint produces is a 404", `ArticlePage.tsx`). The premise is false: the backend also returns 500 `INTERNAL_ERROR`, and network failures occur. A resident is told the content does not exist during an outage |
| D-03 | Loading states are not consistently announced: Notices and Updates "Loading…" have no `role=status` or live region; Category, Topic, Article, Organization and Find Help do | 4A.5; R0 static review | Medium (accessibility) | Existing error paragraphs do use `role=alert` |
| D-05 | Catch-all route says "Page not found" followed by "Coming soon."; every StubPage uses `h2` with no `h1` | 4A.2 | Low | "Coming soon" implies a 404 path will exist later |
| D-06 | Unknown Notices view shows the raw i18n key `notices.not-a-view.intro` and the raw server message | 4A.2 | Low | `CommunityNoticesPage.tsx` builds the key from the URL parameter |
| D-07 | Find Help count reads "1 organizations" | 4A.1 | Low | Matches the existing plural-handling tech debt |
| D-08 | Find Help no-match and empty-payload states give no guidance or reset; an empty gateway list leaves an orphan heading | 4A.1 | Low | |
| D-10 | Backend returns 500 `INTERNAL_ERROR` and logs an ERROR stack trace for any unknown `/api/*` path or missing static resource, instead of 404 | 4A.5; [supplement-runtime-8080.log](evidence/supplement-runtime-8080.log) | Low–Medium | `GlobalExceptionHandler`'s catch-all `Exception` handler absorbs Spring's `NoResourceFoundException`. A backend change; reported, not fixed |

**Missing capabilities and incomplete pathways** (absent behavior; whether it is required depends on the contract):

| ID | Gap | Evidence | Notes |
|---|---|---|---|
| D-02 | No flyer enlargement on the Flyers grid or homepage preview | `NoticeGallery.tsx` renders a bare `<img>` with no button, link or dialog | Needs no backend change. The carousel exists only in the legacy demo; comments saying the current homepage has one are stale |
| D-04 | Navigation leads to placeholder pages: Home **Discover** card → `/discover`; header and footer **About** → `/about`; header and footer **Community** → `/community` (not the live `/community-notices`); `/discover/:facet` accepts any facet | R0 indices 13–15; 4A.2 | Incomplete resident pathways, not crashes |

**Observations** (behavior to confirm against the contract; not asserted as defects):

| ID | Observation | Evidence |
|---|---|---|
| D-09 | The Find Help search query is not kept in the URL, so Back returns an unfiltered directory | 4A.1 |
| — | Successful Article and Organization pages offer no in-page way to return or continue; only the Primary nav | 4A.3 |
| — | Missing contact/source/provider URLs leave some cards non-actionable. Correct: do not manufacture provider identity or destinations | R0 |

## 6A. Open Architectural Requirements [R1 added]

| ID | Requirement question | Demonstrated facts | Why it is not a defect |
|---|---|---|---|
| A-01 | Must Program Updates (the `/updates` page and the future homepage preview) be Community-scoped? | `UpdatesPage` calls `/api/updates/government` with no query. The Primary nav nonetheless carries `?communityId=wilmington-de` into `/updates` (4A.3). `/api/updates/government?communityId=bear-de` returns the same six items. `UpdatesService.getForCategory` *does* filter by `communityId` for Category pages | Government programs are often state-wide, and the sector endpoint was designed to aggregate by producer (Decision 045). R0 treated the ignored parameter as a gap. Whether Program Updates should be partitioned by Community is a contract decision, to be reconciled before V3A homepage work |
| A-02 | Where does Find Help sit in the approved V3A navigation? | Live, functional and reached from the Connect mission card, which the V3A homepage replaces | No supplied V3A design mentions it. Removing the mission cards without a decision would orphan a working P08 surface |

## 6B. Content Readiness / Editorial Integrity Register [R1 added]

These are catalog and editorial findings. No catalog record was changed. Each needs an editorial or owner decision.

| ID | Finding | Evidence | Severity | Owner decision needed |
|---|---|---|---|---|
| C-01 | **OR-003, the only `approved` First Step Original, is a test fixture served publicly.** Its body reads "PLACEHOLDER — approved fixture. This record exists so the boundary test has an article that IS publicly servable…". It is the first item in `/api/home` `originals` and is bylined "Published by Admin" | `app/data/articles.json:147,167`; [api.json](evidence/api.json); 4A.3 page text | **High** | Replace with real approved guidance, or withdraw OR-003 and move the boundary fixture into test resources. The publication filter itself works: OR-001 and OR-002 are correctly withheld (`flagged`) |
| C-02 | **4 of 6 published flyers display poster artwork for a different event than their record**:<br>FL-004 "Back-to-School Supply Drive", Ministry of Caring, Aug 26 → American Legion Post #11 Black-Tie Gala, May 2<br>FL-006 "Free Community Health Fair", Westside Family Healthcare, Sep 2 → Caribbean Women's Health & Wellness Conference, May 9<br>FL-001 "Summer Youth Enrichment", West End Neighborhood House, Sep 7 → "The Monthly Focus", Production Crew, June 4<br>FL-003 "Volunteer Recruitment Drive", United Way, Sep 15 → Special Olympics Bocce Volunteers, May 16 | R0 [page-1280-8.png](evidence/page-1280-8.png) (live); R1 `supp-events-fixture-*`. Record→file mapping is consistent (`Fundraiser.jpg`, `Health Fair.jpg`, `Youth.jpg`, `Volunteer.jpg`); the mismatch is in the artwork | **High** | The page says "the flyer itself carries the details", so residents get the wrong event, organizer and date. FL-002 (eviction legal help) matches in substance; FL-008 matches. R0 noted only "can contain dates/titles inconsistent with captions" |
| C-03 | Every event date in the catalog has passed (latest 2026-10-17), so the Events view is empty. The Flyers view still shows posters whose events have concluded | 4A.4; live `EVENTS=0`, `FLYERS=6` | Medium | Refresh event content before V3A event-column work. Confirm whether Flyers should keep concluded events (lifecycle rule, not changed here) |
| C-04 | Site footer shows "PLACEHOLDER CONTACT hello@example.org" on every page | All screenshots | Medium | Provide a real contact, or remove it |
| C-05 | Homepage "New Laws in Delaware" section is a "Coming soon." placeholder | R0 index 0; journeys text | Low | Superseded by the V3A homepage if that design drops it |
| C-06 | The Red Cross Organization page links the organization's name to a shelter-finder page, not its home page | 4A.3 `orgLinks` | Low | Confirm the intended `website` value |
| C-07 | Stale comments and documentation: Find Help says "63 organizations" (64 served); App/Updates/NoticeGallery comments describe a homepage carousel and routes that no longer exist; the root README describes older UI/API scope | Source review | Low | Correct when those files are next touched (mirror-scope rule) |

## 6C. Tooling and Environment Register [R1 added]

| ID | Issue | Evidence | Disposition |
|---|---|---|---|
| T-01 | On Node 26, plain `npm test -- --run` fails (17 tests) unless `NODE_OPTIONS=--no-experimental-webstorage` is set. The project declares no `engines`, and Docker uses Node 24 | §3 logs | Future tooling change: pin `engines`/`.nvmrc` or set the flag in the test script. Not a product defect |
| T-02 | No Maven wrapper; the work order's `./mvnw test` is unavailable. A local Maven 3.9.16 + JDK 25 toolchain was used | §2 | Record only |
| T-03 | A Maven-built jar contains no SPA (documented in the runbook); only the Docker build bundles `static/app-next` | §2 | Expected behavior. The resulting 500s are D-10 |
| T-04 | Port-8080 resets in R0 | §2 | Resolved as an audit-environment artifact; not reproducible with the current jar |

## 6D. Unverified Behavior [R1 added]

| Item | Why unverified |
|---|---|
| Nonempty Events journey on the live catalog | No current dated data exists (C-03); demonstrated with a fixture only |
| External destination reachability and correctness (resources, updates, organizations) | Outbound links were not followed for truth; out of read-only browser scope |
| Combined Docker production package | Not run in V3A-00 |
| Full WCAG, screen-reader and physical-device conformance | No such session performed |
| Remote RSS ingestion | Deliberately disabled (`--news.rss.initial-delay`) to keep the catalog stable |
| Exhaustive empty/loading/error combinations on every page | Sampled only |

## 7. V3A Design Gaps

Unchanged from R0.

The current homepage runs mission cards → New Laws placeholder → Community Resources/Originals split → flyer row. It lacks the supplied cityscape, introduction/What's New, event column and paired lower Notices/Program Updates arrangement. The visible Housing Get Help block remains, and twelve featured cards create a very long mobile scroll before All Help and What to Know. The current sidebar shows First Step producer content, while inclusive First Rate News is already available through a controlled Housing collection. These are separate presentation and selection changes, not proof that destination pages are broken.

**[R1 added]** The Events view renders full-width cards with a large centered poster. Whether that matches the approved event presentation cannot be assessed without the design document. The V3A homepage removes the Connect mission card, which is currently the only homepage entry to Find Help (A-02).

Keep All Housing Help reachable if the visible preview is removed, through the independent top "Get help in Housing" link and the direct `/category/housing/help` route. Confirm full 36-ID membership in the next package's regression evidence.

## 8. Dependencies and Risks

- **Controlled documents are the primary sign-off dependency.** Example clause IDs cannot establish approved content or package mapping.
- `/api/home` exposes `aiConfig`, `communityResources`, `originals`, `delawareLaws` and `communityFlyers`. It lacks separately authored intro/What's New, an inclusive First Rate News selection and an explicit Program Updates preview. The Updates and Notices endpoints already provide grouped information, so a new backend entity is not justified. Decide between frontend composition and a bounded BFF extension once the contract specifies authoring and A-01.
- Housing APIs already support deterministic featured Topics/items, controlled collections, explicit Organization relationships, approved editorial admissions and 36-item help retrieval. Do not change classification or lifecycle rules to achieve a layout.
- **[R1 corrected]** Event counts depend on the clock, and the live catalog has none current (C-03). Poster artwork contradicts records for four flyers (C-02). Any V3A event column or flyer enlargement will make C-02 *more* visible, so fix it first.
- **[R1 added]** The homepage's only Original is the fixture (C-01). A redesigned First Rate News preview built on today's catalog would feature it prominently.
- Shared ContentCard/ContentCollection changes affect Category, Topic, Help, Organization, Notices and **[R1]** Find Help. Header, footer and token changes affect almost every page.
- `--primary-color` is overloaded between text and background uses in the high-contrast theme. Assess contrast when changing roles.
- **[R1 added]** Removing the mission cards without a navigation decision orphans Find Help (A-02).
- No backend implementation is authorized. Candidate backend requirements, if the contract requires them:
  - bounded homepage composition/authoring data;
  - Community-scoped Program Updates (A-01);
  - **[R1]** correct 404 mapping for unknown paths (D-10).

  Flyer enlargement alone needs no backend modeling.

## 9. Recommended Package Refinements

Keep the V3A-01 through V3A-09 identifiers unchanged. Their approved scope mapping was not supplied, so assigning titles or numbered ownership here would fabricate authority. Attach these refinements to the matching existing package once the framework is available:

- **Homepage package:** replace the mission composition; define the What's New source, event eligibility, inclusive editorial selection and A-01 before final layout; **[R1]** decide the Find Help entry point (A-02); do not feature OR-003 until C-01 is resolved.
- **Housing package:** reuse existing selection and collections; keep independent All Help access; test all resource IDs after removing the visible preview; budget mobile vertical density.
- **Topic/Help packages:** refine existing working destinations; verify narrowing, expansion, Community and invalid/empty states rather than rebuilding APIs.
- **Notices package:** keep the five shareable URLs and original media; **[R1]** resolve C-02 and C-03 before presentation work; add enlargement (D-02) if contracted; fix D-03 and D-06; settle carousel versus grid only from the approved design.
- **Updates package:** reuse government producer classification; resolve A-01 as a contract decision; fix D-03; add the homepage preview without recategorizing notices.
- **Editorial package:** separate publication mechanics (working), inclusive selection, imagery and content readiness; **[R1]** resolve C-01; fix D-01; do not expose editorial review metadata.
- **Resource/Organization package [R1 expanded]:** include Find Help as a P08 surface (D-07, D-08, D-09 decision); keep explicit provider relationships and truthful absent fields; check C-06; validate real outbound targets where feasible.
- **Shared visual package:** reuse tokens, fonts and components; resolve stub navigation (D-04), the catch-all copy and heading (D-05), placeholder footer contact (C-04) and status announcements (D-03); regression-check all consumers.
- **Outside V3A, reported only:** D-10 (backend 404 mapping) and T-01 (Node test tooling).

## 10. Final V3A-00 Disposition [R1 revised]

**Escalation Required.**

Exit criteria, assessed against the approved work order §13:

| Criterion | Status |
|---|---|
| Correct `copyofdev` baseline confirmed | Met (`a4ed4ae…`, reconfirmed) |
| Existing automated test/build results recorded | Met (§3) |
| Every contracted surface has an evidence-supported classification | **Met for every surface the work order names, plus Find Help, Discover facet, catch-all and invalid-view routes.** Cannot be confirmed against the contract itself, which is unavailable |
| Major journeys exercised where the environment permits | Met (R0 + §4A) |
| Missing or blocked verification explicitly recorded | Met (§5, §6D) |
| Functional gaps distinguished from design changes | Met (§6–§6C vs §7) |
| V3A-01–09 plannable without assuming unfinished pages are complete | Met for page readiness; package mapping is blocked on the framework |

**Why the disposition changed from R0's "Verification Incomplete":** R0's rating rested on two things. One was auditor work not yet done (Find Help, history, Events, catch-all); that is now complete. The other was missing documents. Everything that remains needs a decision or input only project authority can provide, which is the work order's definition of escalation rather than further verification. V3A-00 is **not Satisfied**.

**Remaining blockers, by owner:**

| # | Blocker | Owner | Unblocks |
|---|---|---|---|
| 1 | Provide the V3A Page Requirements & Acceptance Contract (P01–P09) | Project authority | Official per-clause crosswalk (§5) |
| 2 | Provide the V3A-01–09 implementation framework | Project authority | Mapping the §9 refinements to package IDs |
| 3 | Provide the approved Homepage and Housing design decisions | Project authority | P01/P02 design-gap confirmation |
| 4 | Decide C-01 (OR-003 fixture publicly served) and C-02 (flyer artwork mismatch) | Editorial owner | Content readiness for P01/P05/P07 |
| 5 | Decide A-01 (Program Updates Community scope) and A-02 (Find Help placement) | Project authority | Homepage and Updates package scope |

When blockers 1–3 arrive, the auditor can complete the official crosswalk from the existing evidence without re-running the application, except where a clause requires behavior not yet sampled.

No production code, tests, catalog JSON, dependencies, branches, commits or remote records were changed. R0 and all R0 evidence files are preserved unchanged.
