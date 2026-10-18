# First Step v2 — IM-13 Coding-Agent Work Order

**Package:** IM-13 — Seven-Journey Release Regression & Quality Gates  
**Authorization:** Implementation packages IM-01–IM-12 accepted on submitted evidence; IM-13 **Verification Required — authorized next**  
**Controlling matrix:** `First Step v2 - Authoritative IM-01-IM-14 Execution Matrix - IM-12 Closed 2026-10-17.md` §14D together with its canonical package table, crosswalks and execution contract.  
**Date:** October 17, 2026  
**Outcome requested:** reproducible release-quality evidence for J1–J7 and E1–E5, with minimal correction of demonstrated v2 defects only. **Do not declare v2 complete**; IM-14 is the only documentation/final-closure gate.

## 1. Authority and inspection order

Read `claude.md`, the supplied updated authoritative matrix **first**, the approved v2 Completion Contract (J1–J7/E1–E5), P4/P5 and Architecture Resolutions, Decisions 048/051/052, IM-01–IM-12 accepted reports and the IM-12 deployment/association/public-field evidence. Inspect current source, compiled/package boundaries, actual catalogs/configuration, CSS/dictionary/routes, active tests and any existing release checklist before edits. Treat older historical matrices as subordinate to §14D. Do not invent source file names or assume exact repo paths; confirm them.

Before touching files: capture branch/full HEAD, tracked modifications/deletions, staged/unstaged/untracked/ignored states, baseline hashes of existing source/catalogs/config/authority, and baseline backend/frontend/Python/validator/build results. IM-12 reported an **already-deleted tracked prior matrix** and untracked user-supplied authority files: preserve those exactly, do not restore or rewrite them. Its runtime source changes were uncommitted at report time; verify your actual checkout rather than assume that still holds. No `git reset`, stash, destructive cleanup, staging, commit or push without explicit authorization.

Write an inspect-first gap map and a test/evidence plan **before** making changes. Prefer existing tests, services and browser journeys; do not create competing models, selectors, data copies or endpoints solely for testing. Explicitly distinguish true release failures from tests covering unavailable data, and from V3/Later ambitions.

## 2. Release scope and required evidence types

Use the real configured runtime, **packaged Spring Boot executable and built Vite frontend** (not development-only mocks) for representative resident journeys. Exercise actual Housing/Wilmington, a valid inhabited secondary Community such as Food/Bear, and empty/invalid requested scopes. Anchor time-based tests to fixed Clocks and owning-Community zones; identify separately any live-clock smoke. Audit public HTTP payloads with exact field allowlists, and verify displayed links/keyboard outcomes in the browser. Reuse the existing six data validators and full Maven, TypeScript, frontend and Python suites.

Verify accessible names and hierarchy, keyboard-only navigation/focus restoration, loading/empty/not-found/network errors, English/Spanish diacritics, high contrast, 375px mobile and 1280/1440px desktops; inspect screenshots plus DOM/console/viewport measurements. Run available automated accessibility checks (e.g. axe only if already present, or other available tool) and manually check critical WCAG-relevant interactions. **Do not claim full WCAG conformance or physical-device proof if not actually performed.** Record untested criteria explicitly, and resolve any required release-blocking defects.

Capture both controlled public Category Destination and legacy endpoint behavior; do not accidentally change intentionally aggregate API semantics under the guise of scoping. Test safe HTTP(S), telephone, original Flyer media, approved Article/internal routes and honest absent links. Inspect release configuration including `app.data.dir`, catalog volume, new admission manifest, running backend + frontend API base path, and required assets/fonts. Build/test Docker only if build support and environment are available; otherwise state the uncovered release gate and whether an equivalent packaged deployment test is sufficient under the Completion Contract.

## 3. Canonical journey contracts — prove each end to end

| Journey | Required complete scenario | Primary evidence |
|---|---|---|
| **J1 — Resident finds help through a Category Destination** | Open Housing Category; featured content present; direct **Get Help** action; four-item bounded preview and **complete 36 Wilmington Housing Help items**, controlled topic/All Help navigation and accurate counts; zero-help Eviction Prevention honest. | Built app/browser, real Category/Get Help/All Help HTTP, screenshots/keyboard. |
| **J2 — Resident explores a featured topic** | Four deterministic Housing Topics in IM-06 order; IM-07 winners (HA-002, HA-016, HA-011, HA-008) with two distinct truthful FALLBACK items each; Topic click, source/media/Article-safe navigation; role evidence never falsely claimed; real Homeownership tie survives input permutations. | Selector suites + real API equality + built UI/browser. |
| **J3 — Resident moves between an offering and provider** | Navigate HA-001 → explicit ORG-001 provider and back, retaining Community; other positive explicit links; missing provider ID remains absent, not inferred from source, parent text, physical location or same name. | API, provider detail/card browser, positive/negative identity tests. |
| **J4 — Resident discovers civic information rather than assistance** | Correct Policy Info NP-004/005; Flyers FL-002 with original media and no Help promotion; First Rate News exactly OR-003, FAQ-002, EA-001, EA-005 under independent admission, correct links/non-links; LAW empty locally acknowledged as cache limitation; Community Notices Events conclusion independent of Flyer visibility and editorial expiry. | Real collections and notices APIs, built switcher/media/Article navigation, fixed occurrence tests. |
| **J5 — Editor can add/update an offering without creating false identity** | Use **controlled fixture/in-memory or temporary external catalog**, without altering production catalogs, showing explicit owner/provider ID precedence, missing optional provider preserved, stored Topic evidence, source producer separate from Organization, validator outcomes and read-model classification. Verify edit/read/reload where actual supported authoring exists; otherwise demonstrate the approved JSON/manual authoring workflow and describe the limitation truthfully. | Actual loader/validator integration tests, isolated fixture, documented authoring steps and source hash protection. |
| **J6 — Editor can handle approaching expiry** | Fixed-Clock NP-003 remains eligible approaching, five expired News stored but absent from current views, Admin-readable CLI diagnostics reproducible with scope/zone, malformed-date handling; no automated dashboard/email/queue assumed; event occurrence not inferred from expiry. | Packaged expiry CLI, current/retained HTTP and time-boundary tests, authoring guidance evidence. |
| **J7 — System protects catalog identity and classification** | All six concrete kinds / five ContentTypes; canonical dedup, authored Topic/LAW proof, Article approval/404 parity, FAQ/Expert independent First Rate News admission, exact helpAccess and unchanged 8 production Flyers, Community partitions, Source vs Organization, private-field allowlists for every resident endpoint; selection determinism. | IM-12 full endpoint/262-row catalog audits rerun + negative fixtures + serialized API checks. |

**Release coverage:** Map each J1–J7 and E1–E5 to named actual tests, real HTTP/browser captures, relevant IM package(s), and gaps. No journey is PASS merely because an earlier report called it satisfied; rerun the relevant tests on the **then-current** checkout.

## 4. Specific inherited findings — carry accurately

- The user confirms **171 Resource rows without explicit provider IDs are inherited from an original demo dataset**; their structure is a **future library-management design input**, not authorization to backfill organizations. The catalog contains **262 unique normalized IDs**, but possible semantic duplication between demo and other records has **not been proved or enumerated**. Do not perform speculative deduplication or redesign in IM-13; log as future examination, preserving original metadata.
- IM-12 found **58 Resources with resolving explicit provider IDs**, **64 Organizations**, and no typed persisted HOST relation. Verify J3 through existing explicit IDs; do not claim full P5 `ContentOrganizationAssociation` physical implementation.
- All 229 stored Resource rows lack explicit `communityId` and use legacy normalization; verify existing owner assignments and explicit-owner precedence using temporary fixtures, without global catalog rewrite. Community ownership ≠ geographic location ≠ resident eligibility.
- `dscyf-directory` has **unknown governed producer sector**; it is not automatically an Organization/PROVIDER/HOST. Do not invent a registered producer.
- **Manifest runtime behavior is known:** valid external `app/data/first-rate-news-membership.json` admits three exact Expert/FAQ entries alongside OR-003; **missing manifest starts successfully but declines those three admissions**; malformed manifest fails before resident readiness. Do not incorrectly state absent manifest always prevents startup. Check the intended release's actual copied catalogs and configuration, and surface a missing-manifest deployment as an operational release failure if the four-item experience is expected.
- FL-008 is date-only on October 17, 2026; no inferred 4pm cutoff. NP-003 has unknown occurrence but approaching editorial expiry. Independent lifecycle and occurrence tests are mandatory.
- Current admitted LAW/RSS cache was empty under delayed refresh in previous fixed-catalog audits. Do not claim Delaware has no relevant laws, complete remote feed coverage, or a verified external-provider web page.
- IM-12 corrections include privacy allowlists across legacy resource/search/expert endpoints and AI scope/exception sanitation. Include those routes in regression, even if not visually emphasized on the Category page.
- Preserve workstream scope: Admin UI/dashboard, automated queues, emails, generalized Organization/Host graph, second-community launch, database migration, demographic recommendation and engagement/AI ranking are V3/Later.

## 5. Named acceptance matrix — IM13-T01–T20

| ID | Required acceptance proof |
|---|---|
| **T01** | Inspect actual authority/current HEAD, dirty/untracked/deleted state, protected hashes and clean untouched baseline; no user file rewritten. |
| **T02** | Full J1 Housing resident Help journey including 36 current scoped records, complete All Help, zero-help Topic, counts and direct navigation. |
| **T03** | J2 four exact Topic identities/order and twelve actual selected item identities/position types, role truth, direct source, repeatability and Homeownership tie. |
| **T04** | J3 offering→explicit provider→offering journey with preserved Community, plus absent-provider and source/name/parent/host noninference. |
| **T05** | J4 four controlled civic collections and real mixed First Rate News, bounded previews/expansion, original Flyer media, safe/absent links and honest empty LAW cache. |
| **T06** | J4 Event/Flyer split for FL-008 at Community-local next midnight, NP-003 unknown occurrence and independent editorial current/expired states; exact structured dates/DST. |
| **T07** | J5 isolated creation/change/reload/validator exercise (or accurately documented supported authoring boundary), preserving real canonical identity, Source, owner and optional provider; unchanged original catalogs. |
| **T08** | J6 current/approaching/expired boundary behavior and packaged read-only expiry command, diagnostics and interim Admin process; no automated Admin tooling invented. |
| **T09** | J7 canonical distinct IDs, approved five types/six kinds, cross-view dedup, authored vs inferred Category/Topic, Expert/FAQ/Article publication guards, Flyer exact token. |
| **T10** | Wilmington and inhabited second Community plus empty/unknown scopes across Category, Topic, All Help, provider, Search/News/Notices and by-ID where supported; intentional aggregates noted. |
| **T11** | Complete currently exposed public API allowlists and nested-source/privacy/unsafe URL/exception checks, no internal review/lifecycle/selection/ingestion leak. |
| **T12** | Actual deployment against external `app.data.dir`: valid/missing/malformed admissions manifest, complete catalog/media, production API+frontend configuration and reproducible packaged startup. |
| **T13** | 375px mobile and >=1280px desktop visual/responsive checks: no horizontal overflow, readable cards, right rail below primary content on mobile, no squashed laws; inspected captures. |
| **T14** | Keyboard-only major navigation, controlled switcher focus/pressed states, heading/landmark semantics, loading/not-found/error announcements and no keyboard trap. |
| **T15** | Spanish labels/diacritics and English parity, high-contrast readable/focus states, accessible names/alt/media, production fonts; clearly scoped automated/manual accessibility evidence. |
| **T16** | Broken link/missing source/media/unsafe scheme and inaccessible provider/no-contact fallbacks are truthful, safe and do not fabricate Article URLs or duplicate content. |
| **T17** | All five Epic crosswalks and seven journeys have explicit traceability to implemented capability, named rerun tests and observed resident/editor outcomes; unresolved blockers enumerated. |
| **T18** | Re-run original IM-01–IM-12 selection, lifecycle, temporal, membership, privacy, Community and catalog regressions under same final revision; catalog snapshots and hashes preserved. |
| **T19** | Full backend Maven `verify`/package, frontend test/typecheck/build, Python tests and six validators plus packaged HTTP and actual built-browser tests; exact commands, timestamps, exits, failures and reruns. |
| **T20** | Exact file/diff/manifest preservation, annotated mirrors for touched source/tests, no silent v2 scope expansion; acceptance report separates PASS, FAIL, NOT TESTED and legitimate V3/IM-14 deferrals. |

## 6. Change control and release blockers

**Verification first.** Before production edits, establish the defect against an existing approved v2 contract with a failing test or real reproducible browser/API observation. Apply the smallest bounded correction; retain failure+success evidence. Do not alter user-managed approved authority, source-of-truth catalog, editorial admission or Flyer helpAccess without separate record-level authorization. Do not invent structured event times, provider relationships, Topic classifications, creation dates or URLs.

Escalate an **actual required-v2 blocker** with record/test ID, failing contract, why existing P5 seams cannot satisfy it, minimal options, and effects on the Completion Contract. Missing optional demo provider IDs, offline RSS completeness or unlaunched second Community are **not** automatically blockers. Do not treat a future library system as an IM-13 implementation goal.

A release gate may be considered passing only if its test was actually run, and any not-run device/automated-a11y/production image gate must be labelled **NOT TESTED** with implications. Do not resolve a gate by weakening a test, changing snapshot expectations to hide behavior, disabling a validator or silently shrinking public journeys.

## 7. Evidence deliverables and execution report

Create `docs/v2-completion-plan/evidence/im13/` with: protected starting/ending-state hash inventories; inspect-first design and journey/epic traceability matrix; isolated J5 fixture/authoring reproduction; fixed-clock J6/occurrence and current-catalog audits; clean command ledger and failed/final logs; full public endpoint/privacy responses; packaged runtime configuration including manifest three-state results; browser screenshots/results at mobile/desktop plus Spanish/high contrast and keyboard path notes; compiled quality-gate results; exact changed files/annotated mirrors; and a final PASS/FAIL/NOT TESTED issue register with v2-blocking classification. Avoid copying confidential user data into artifacts.

**Required report sections:** Starting state and authority; untouched baseline; seven-journey test matrix J1–J7; five-epic mapping; packaged/runtime/config proof; real catalog snapshot; cross-community/identity/DTO proof; accessibility/responsive/keyboard/Spanish/high-contrast evidence; event/expiry boundary proof; defects and bounded fixes; named T01–T20 acceptance mapping; full quality-gate results; preservation hashes; actual unresolved release blockers, untested gates and IM-14/V3 handoff; proposed reviewer disposition. Link to exact local evidence and clearly distinguish real from synthetic.

**Exit condition:** Submit an evidence-based recommendation for IM-13 Satisfied **only if all mandatory v2 journeys and quality gates genuinely pass**. If any required release gate remains untested or fails, report `Blocked` or `Implementation Required` with the exact gap, not a blanket Satisfied. **Do not self-close IM-13, self-edit the authoritative matrix, start IM-14 or declare First Step v2 complete.**
