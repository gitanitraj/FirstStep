# First Step V3A-00 — Evidence Completion Report

Audit date: October 23, 2026. Authority: [approved work order](evidence_work_order.txt). Application, tests, dependencies and catalog inputs were not edited. New files are audit documentation, probes and evidence only.

## 1. Executive Summary

The current `copyofdev` baseline is `a4ed4aeb0ff70bcbb34e164cd9ec87df695245d3`. It descends from the previously inspected `bb4d55ee0074452223e517ede48a1ed30f5e4e1a`; the intervening commit adds the First Step Admin Guide, rather than changing application implementation.

The Category, Topic, All Help, Notices, Updates and Organization surfaces contain working resident functionality. The homepage requires structural redesign and its Discover entry leads to a placeholder. Article delivery works, but the approved article body is explicitly a public fixture, so editorial content readiness is incomplete. V3A should build on these capabilities rather than assume either universal completion or universal redevelopment.

**Disposition: Verification Incomplete.** Runtime and automated baseline evidence is available. The approved V3A Page Requirements & Acceptance Contract and V3A-01–09 framework/design documents were not found in the repository. Their exact acceptance clauses and package scopes cannot be reconstructed from example IDs. This report therefore cannot sign off every official acceptance clause. External-link destination correctness, full accessibility conformance and some state/history scenarios remain untested.

## 2. Environment & Baseline Verification

| Item | Recorded environment |
|---|---|
| Repository | `/Users/composedmedia/Documents/Projects/FirstStep` |
| Branch / SHA | `copyofdev` / `a4ed4aeb0ff70bcbb34e164cd9ec87df695245d3` |
| Initial tree | Only untracked `docs/v3-completion-docs/`; no tracked modifications |
| Java | Oracle JDK 25.0.2; Maven compiles Java 17 bytecode with required JDK 25 toolchain |
| Maven | 3.9.16 at `/Users/composedmedia/tools/apache-maven-3.9.16/bin/mvn`; no wrapper, `mvn` absent from PATH |
| Node / npm | 26.7.0 / 11.19.0; package has no engines constraint; Docker uses Node 24 |
| Frontend | React 18, TypeScript, Vite; installed dependencies used without installation |
| Configuration | Complete `app/data`, editorial manifest and original seasonal media; no DB required |
| App location | Current SPA `/app-next/`; `/` is retained legacy demo. Work-order routes are SPA-relative |

See [environment evidence](evidence/working-tree.txt), [Java](evidence/java-version.txt), [Maven](evidence/maven-version.txt), [Node](evidence/node-version.txt), [npm](evidence/npm-version.txt).

Documented commands are in `docs/authoring/v2-release-operations.md`: `docker compose up --build -d app` for combined packaging, or `java -jar backend/target/firststep-backend-0.1.0.jar --app.data.dir=app/data --app.seasonal.images.dir=backend/src/main/resources/static/images/seasonal` from the repository root. Frontend development: `cd frontend && npm run dev -- --host 127.0.0.1`; Vite forwards API requests to port 8080. Production build: `cd frontend && npm run build`.

The sandbox prevented server port binding and Mockito attachment. Approved execution outside the sandbox resolved build/test restrictions. Backend port 8080 listened but returned connection resets. A freshly verified jar with `-Djava.net.preferIPv4Stack=true --server.port=18080` worked. Audit startup also used `--news.rss.initial-delay=86400000` to avoid remote feed ingestion. Browser probes used Vite 5173 and forwarded only `/api/` and seasonal media requests to 18080, without editing Vite or application configuration. This is a development runtime audit, **not a new combined Docker production deployment verification**. The first port-8080 launch used a pre-existing jar; accepted browser payloads came from the jar rebuilt by the passing current Maven verification.

## 3. Automated Test Results

Commands below ran from their respective project directories. Counts are from this execution, not historical reports. All logs are under [evidence](evidence/).

| Exact command | Result | Executed / passed / failed / errors / skipped | Evidence |
|---|---|---|---|
| `npm test -- --run` | FAIL, Node environment | 162 / 145 / 17 / — / 0 | [raw frontend log](evidence/frontend-test.log) |
| `NODE_OPTIONS=--no-experimental-webstorage npm test -- --run` | PASS | 162 / 162 / 0 / 0 / 0; 23 files | [compatible frontend log](evidence/frontend-test-compatible.log) |
| `npm run build` | PASS | TypeScript compilation and Vite bundle; no test count | [build log](evidence/frontend-build.log) |
| `/Users/composedmedia/tools/apache-maven-3.9.16/bin/mvn -B verify` (sandbox) | FAIL, environment | 631 / 343 / 0 / 288 / 0 | [sandbox Maven log](evidence/backend-verify.log) |
| Same Maven command, approved outside sandbox | PASS | 631 / 631 / 0 / 0 / 0; build/package/verify successful | [accepted Maven log](evidence/backend-verify-approved.log) |
| `python3 -m unittest discover -s data-cleaning/tests -v` | PASS | 16 / 16 / 0 / 0 / 0 | [Python suite](evidence/python-0.log) |

Raw frontend failures include Accessibility controls, SiteHeader and CategoryDestination tests, with `TypeError: Cannot read properties of undefined (reading 'clear')` and downstream duplicate/missing DOM queries. The documented Node setting resolves them without code changes. Maven's sandbox run reports `Could not initialize plugin: interface org.mockito.plugins.MockMaker`; affected examples include EditorialProjectionTest and GlobalExceptionHandlerTest. Full names/messages remain in the raw logs.

All six established validator commands passed: `python3 data-cleaning/scripts/validate_content_sources.py`, `validate_homepage.py`, `validate_navigation.py`, `validate_news.py`, `validate_organizations.py`, and `validate_schema.py` (each filename under the same directory). The separate `python3 data-cleaning/scripts/validate_schema.py --input app/data/resources.communities.json` also passed. [Exact commands, exit codes and log names](evidence/python-commands.json). Warnings remain in logs; successful validation does not establish substantive truth of resources or external destinations. No stricter policy was substituted.

## 4. Resident Page Implementation Inventory

Routes below are relative to `/app-next`. Browser evidence: [probe](evidence/browser-audit.mjs), [route results](evidence/browser-results.json), [current payloads](evidence/api.json). `page-<width>-<index>.png` follows the route order recorded in results. Every sampled route was opened directly at 1280×900 and 375×900. No uncaught page errors or document horizontal overflow were recorded. Screenshots supplement executable DOM/navigation evidence; they are not mockups.

| Page | Route | Existing components | Backend/API | Classification | Functional gaps | V3A gaps | Test evidence | Treatment |
|---|---|---|---|---|---|---|---|---|
| P01 Homepage | `/` | HomePage, MissionCards, CommunityResources, FirstStepOriginals, CommunityInformation, NewLaws | `/api/home` | Partially Implemented | Discover leads to stub; capped FAQ previews have no detail link; flyer enlargement absent | Cityscape/fade, maintained What's New, event column, 2×4 category arrangement, inclusive First Rate News and Program Updates previews absent | Browser index 0; App/Home-related suites | Structural redesign + functional completion |
| P02 Category | `/category/housing` | CategoryPage, CategoryBrowse, ContentCollection, ContentCard, context rail | `/api/category/housing` | Established | No current primary-path failure demonstrated; provider/expert/law context can be absent | Existing Get Help block remains; approved final Housing layout unavailable | Index 1; CategoryDestination and CategoryPage suites | Structural redesign |
| P03 Topic | `/category/housing/transitional-housing` | TopicPage, breadcrumbs, ContentCollection | `/api/category/housing/transitional-housing` | Established | Missing outbound URLs produce nonlinked titles; external destination correctness not tested | Mobile cards remain full result cards; exact V3A criteria unavailable | Index 2; TopicPage tests | Visual refinement, pending contract |
| P04 All Category Help | `/category/housing/help` | TopicPage reserved help mode, topic details, progressive collection | `/api/category/housing/help` | Established | No demonstrated inventory loss: API has 36 Resources; further journey evidence recorded separately | Must retain access after future Get Help removal | Index 3; HelpBrowse tests; journey probe | Verification only / visual refinement |
| P05 Notices overview | `/community-notices` | CommunityNoticesPage, previews and view links | `/api/community-notices` | Established | No flyer enlargement; limited metadata | Overview/event-preview arrangement needs contract comparison | Index 4; CommunityNoticesPage tests | Visual refinement + functional completion |
| P05 Events | `/community-notices/events` | Shared notices page | `/api/community-notices/events` | Established | Zero live event fixtures; positive nonempty live journey not demonstrated | V3A event presentation needs dated data | Index 5; empty view rendered | Verification only, then visual refinement |
| P05 Meetings | `/community-notices/meetings` | Shared notices page, ContentCard | `/api/community-notices/meetings` | Established | Full external target check not performed | Contract unavailable | Index 6; one meeting in current counts | Visual refinement |
| P05 Announcements | `/community-notices/announcements` | Shared notices page, ContentCard | `/api/community-notices/announcements` | Established | Full external target check not performed | Contract unavailable | Index 7; three announcements | Visual refinement |
| P05 Flyers | `/community-notices/flyers` | NoticeGallery | `/api/community-notices/flyers` | Partially Implemented | Six posters displayed; images have no enlargement action | Current grid, no current React carousel | Index 8; image loads and contain sizing | Functional completion + visual refinement |
| P06 Program Updates | `/updates` | UpdatesPage, UpdateGroup | `/api/updates/government` | Established | No interactive filters; page request does not carry Community scope | Homepage preview absent; current heading Latest Updates | Index 9; UpdatesPage tests; government payload | Structural redesign; resolve scope requirement |
| P07 Editorial detail | `/originals/OR-003` | ArticlePage, lede, dates, plain-text body | `/api/originals/OR-003` | Partially Implemented | Public approved fixture body, not finished guidance; no article image presentation | Inclusive First Rate News presentation exceeds producer-only homepage preview | Index 10; ArticlePage and backend publication tests | Functional/content completion + visual refinement |
| P08 Organization | `/organization/american-red-cross` | OrganizationPage, offering cards | `/api/organization/american-red-cross` | Established | Sparse registry data means limited identity/contact/actions | Profile expansion not authorized | Index 11; OrganizationPage/identity/navigation tests | Visual refinement |
| P08 Resource cards | Category, Topic, Help and Organization routes | ContentCard, ContentCollection | Public ContentItem projections | Established | Many cards correctly omit missing URLs/provider details; do not invent links or infer provider from source | Shared design change has broad impact | Indices 1–3,11; ContentCard and Im12PublicNavigation tests | Visual refinement |
| P09 Shared visual system | Shared page frame | SiteHeader/Footer, tokens, typography roles, high-contrast hook/theme, i18n | None | Partially Implemented | Header/footer Community/About lead to stubs; placeholder footer email; loading announcements inconsistent | Reuse token/font/component foundation; resolve overloaded color token | All widths; contrast toggle/focus evidence; Accessibility suite | Visual refinement + functional completion |
| Linked Discover | `/discover` | StubPage | None | Placeholder | No discovery journey | Do not count homepage pathway as completed | Index 15 | Functional completion; package ownership pending |
| Linked Community | `/community` | StubPage | None | Placeholder | Header route does not open real Notices overview | Navigation decision needed | Index 14 | Functional completion |
| Linked About | `/about` | StubPage | None | Placeholder | Shell only | Copy/content needed if retained | Index 13 | Functional completion |

## 5. V3A Acceptance Crosswalk

**Official acceptance IDs: BLOCKED.** The work order cites H-01, C-02, T-04, N-02, R-05 and S-06 only as examples and supplies no clause definitions. None can honestly receive PASS/FAIL against an unknown definition. Every official clause for P01–P09 remains BLOCKED pending the actual contract. No invented numbering is presented as approved authority.

The following crosswalk covers the behaviors explicitly supplied in the work order; it is not a substitute contract. PASS applies only to the demonstrated behavior and sampled scope.

| Surface / supplied requirement | Status | Evidence and limit |
|---|---|---|
| P01 direct homepage, navigation, category links, current API payload | PASS | Index 0; `/api/home` retained; navigable Housing pathway |
| P01 hero/fade, separate maintained What's New, Community Events column | NOT IMPLEMENTED | Running homepage instead shows mission cards and Laws placeholder |
| P01 exact 2×4 categories, inclusive First Rate News, Program Updates lower preview, mixed editorial arrangement | NOT IMPLEMENTED | Current seven pathways, capped producer-only Originals sidebar, lower flyer row |
| P01 flyer preview | PASS | Three images from six-item payload; no carousel controls |
| P01 loading/error | NOT TESTED | Automated tests plus separate controlled fault probe do not establish every visual loading state |
| P02 header, four featured topics, item/topic links, Get Help, What to Know, rail and Organizations | PASS | Current payload and index 1; four topics × three positions |
| P02 Community in URL and All Help entry | PASS | Category makes `communityId=wilmington-de` explicit; direct help endpoint returns 36 |
| P02 approved Housing layout | BLOCKED | Approved design document missing; current Get Help removal is future work |
| P03 direct real Topic, Category breadcrumb, mobile presentation | PASS | Transitional Housing opened at both widths, back/forward recorded |
| P03 exhaustive item destinations and empty/loading/error combinations | NOT TESTED | Missing/invalid Help route sampled; full matrix not claimed |
| P04 direct full inventory API and page, Category access and topic choices | PASS | 36 resources; initial four visible plus 32-more action; journey probe covers further behavior |
| P04 every large-set/empty/community combination | NOT TESTED | Finite current fixture, not load testing; supplementary probe retained |
| P04 invalid route understandable | PASS | `/category/invalid/help` displays topic-not-found and Category return link |
| P05 overview and four direct views | PASS | Indices 4–8; URL-addressed views, live counts 0/1/3/6 |
| P05 flyer media sizing | PASS | Loaded posters and contain sizing; desktop/mobile screenshots |
| P05 flyer enlargement | NOT IMPLEMENTED | NoticeGallery renders images/text without button, link or dialog |
| P05 current React carousel | NOT IMPLEMENTED | Homepage limited row and destination grid; older carousel remains in legacy `static/app.js` |
| P05 source/detail metadata | PASS | Producer display labels/dates visible; not a complete provenance profile |
| P05 availability and event-boundary runtime transition | NOT TESTED | Current empty events observed; lifecycle tests pass, but no new time-travel browser transition executed |
| P06 direct Updates, government classification, content-type grouping, producer/date/link presentation | PASS | Six-item GOVERNMENT payload, index 9; producer-sector tests pass |
| P06 interactive filtering | NOT IMPLEMENTED | Grouped feed without filter control |
| P06 homepage Program Updates preview | NOT IMPLEMENTED | Home payload has no explicit programUpdates field; existing feed can supply a preview |
| P06 every loading/empty/error combination | NOT TESTED | No live empty government feed; fault probe limited |
| P07 homepage approved Original, Article route, publish date, lede | PASS | OR-003 renders; homepage also includes FAQ previews |
| P07 inclusive Article/FAQ/Expert collection | PASS | Housing First Rate News contains OR-003, FAQ-002, EA-001, EA-005; homepage does not use that selection |
| P07 resident-ready article body | FAIL | OR-003 body explicitly says `PLACEHOLDER — approved fixture.` |
| P07 article image/fallback behavior | NOT IMPLEMENTED | ArticlePage has no image slot; existing text-only rendering works |
| P07 internal editorial fields excluded | PASS | Current public projection and passing DTO/publication regression suites; no internal review UI in detail |
| P08 result cards, provider relationships, Organization direct route | PASS | Red Cross offering rendered; explicit association tests pass |
| P08 external sites substantively reachable/correct | NOT TESTED | Outbound link rendering does not prove target availability or correctness |
| P08 complete browser history coverage | NOT TESTED | Topic/Category/Notices links sampled; Article/Organization lacked in-main internal link for generic history probe |
| P09 token/font reuse, shared header/footer, high contrast and keyboard focus | PASS | Montserrat/Open Sans bundled; contrast class toggled; keyboard focus outline recorded |
| P09 sampled responsive layout | PASS | No horizontal overflow at either width; Housing mobile is a long stacked page |
| P09 exhaustive contrast/focus/screen-reader conformance | NOT TESTED | No full accessibility certification, physical-device or screen-reader session |
| P09 loading/error announcements everywhere | FAIL | Topic/Article have status roles; Notices/Updates loading paragraphs lack `role=status` or live region |

## 6. Functional Gaps

Additional executable [journey evidence](evidence/journeys.json), reproduced with [journey-audit.mjs](evidence/journey-audit.mjs), demonstrates Category → All Housing Help → `32 more` → Emergency Shelter → browser Back with the Wilmington query preserved. Expansion adds exactly 32 result rows (13 to 45 total list elements, including nine topic choices), exposing all 36 resources. Emergency Shelter reports nine listings. Bear Help independently returns one resource. Invalid Topic and Organization API requests return 404. Controlled 503 responses were injected in browser memory for Home, Category, Help, Notices, Updates and Article; these are synthetic fault checks, not genuine live outages. Article represents an outage with the same not-found message as missing content, so its diagnostic is misleading for a 503. The government Updates query with `communityId=bear-de` still returns six items, confirming that the endpoint does not apply the requested partition.

The generic Notices Events history probe clicked the already active Events link; its `restored:false` is not evidence of a product history defect because no new navigation entry was created. Other sampled Notices view transitions restored their source URL. Article, Updates and Organization still need dedicated complete resident-history scenarios.

1. Homepage Discover and shared About/Community links lead to real stubs. These are incomplete resident pathways, independently of visual design.
2. The served approved OR-003 article has a fixture body. Working publication plumbing is not finished resident guidance.
3. Flyer gallery/home previews offer no enlargement control. Carousel implementation survives in the legacy demo, not current React. Comments claiming the current homepage retains a carousel are stale.
4. Notices/Updates loading and empty messages lack consistent assistive announcements. Existing error paragraphs do have alert roles.
5. Updates uses `/api/updates/government` without Community query propagation; the service aggregates by sector. Determine the approved scope before treating it as a community-specific Program Updates preview.
6. Missing contact/source/provider URLs leave some cards nonactionable. Preserve unknown facts; do not manufacture provider identity or external destinations.

## 7. V3A Design Gaps

The current homepage is mission cards → New Laws placeholder → Community Resources/Originals split → flyer row. It lacks the supplied cityscape, introduction/What's New, event column and paired lower Notices/Program Updates arrangement. The visible Housing Get Help block remains, and twelve featured cards create a very long mobile scroll before All Help and What to Know. The current sidebar is First Step producer content, whereas inclusive First Rate News is already available through a controlled Housing collection. These are separate presentation/selection changes, not proof that existing destination pages are broken.

Keep All Housing Help reachable through the independent top `Get help in Housing` link and direct `/category/housing/help` API/page if the visible preview is removed. Move/retain the explicit All Housing Help link in the final arrangement rather than deleting it with its parent collection. Confirm full 36-ID membership in the next package's regression evidence.

## 8. Dependencies and Risks

- Missing official V3A contract/design/framework is the primary sign-off dependency. Examples of clause IDs cannot establish approved content or package mapping.
- `/api/home` exposes `aiConfig`, `communityResources`, `originals`, `delawareLaws`, `communityFlyers`. It lacks separately authored intro/What's New, inclusive First Rate News selection and explicit Program Updates preview fields. The separate Updates and Notices endpoints already provide grouped information; a new backend entity is not justified. Decide frontend composition versus a bounded BFF extension once the contract specifies authoring and Community scope.
- Current Housing APIs already support deterministic featured Topics/items, controlled collections, explicit Organization relationships, approved editorial admissions and 36-item help retrieval. Do not change classification or lifecycle rules to achieve a visual layout.
- Current event counts are clock dependent; original poster images can contain dates/titles inconsistent with authored captions. This audit observed rendered media, not substantive poster/catalog truth. Six available flyers do not imply six upcoming events.
- Shared ContentCard/ContentCollection changes affect Category, Topic, Help, Organization and Notices. Header/footer/token changes affect almost every page. Preserve approved DTO boundaries and test those consumers.
- `--primary-color` is overloaded between text/background uses in the existing high-contrast theme. Reuse existing typography roles, CSS Modules, palette/surface/spacing tokens and theme overrides; assess contrast when changing roles.
- Existing App/Updates/NoticeGallery comments contain stale architectural claims. Current routes use a dedicated CommunityNoticesPage; homepage no longer has the carousel some comments describe. The root README also describes older UI and API scope; the reconciled release runbook is more accurate.
- No new backend implementation is authorized. Demonstrated candidate backend requirements are bounded homepage composition/authoring data and Community-scoped Program Updates if required by the missing contract. Image enlargement alone needs no new backend modeling.

## 9. Recommended Package Refinements

Keep V3A-01 through V3A-09 identifiers unchanged. Their approved scope mapping was not supplied, so assigning new titles or numbered ownership would fabricate authority. Attach these refinements to the matching existing package once the framework is available:

- Homepage package: replace mission composition; define maintained What's New source, event eligibility, inclusive editorial selection and Program Updates Community scope before final layout.
- Housing package: reuse existing selection/collections; retain independent All Help access; test all resource IDs after removing the visible preview; budget mobile vertical density.
- Topic/Help packages: refine existing working destinations; verify narrowing, expansion, Community and invalid/empty states rather than rebuilding APIs.
- Notices package: preserve five shareable URLs and original media; add enlargement if contracted; settle carousel versus grid only from approved design.
- Updates package: reuse government producer classification; settle filtering/scope and homepage preview without recategorizing notices.
- Editorial package: distinguish publication mechanics, inclusive selection, imagery and actual content readiness; do not expose editorial review metadata.
- Resource/Organization package: retain explicit provider relationships and truthful absent fields; validate real outbound targets where feasible.
- Shared visual package: reuse tokens/fonts/components; resolve stub navigation, placeholder footer contact and status announcements; regression-check all page consumers.

## 10. Final V3A-00 Disposition

**Verification Incomplete.** Correct baseline, current automated results, all P01–P09 implementation findings and both-width runtime evidence are recorded. Major existing journeys have been exercised, and functional gaps are separated from V3A design work. Official per-clause acceptance and exact V3A-01–09 scope refinement require the missing approved documents. Remaining runtime limits are explicitly marked rather than promoted to PASS. No production code, tests, catalog JSON, dependencies, branches, commits or remote records were changed.
