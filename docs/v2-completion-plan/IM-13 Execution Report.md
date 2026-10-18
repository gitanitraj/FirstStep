# IM-13 Execution Report

Date: October 17, 2026 (America/New_York). Package: Seven-Journey Release Regression & Quality Gates.

**Disposition: execution complete; proposed Satisfied on the bounded v2 evidence below, pending reviewer acceptance.** No unresolved demonstrated v2 release blocker remains in the executed checks. This report does not amend the authoritative matrix, accept itself, or declare v2 complete. IM-14 remains the final documentation/closure gate.

## Starting state and authority

Controlling authority is the supplied v2.13 matrix §14D and the IM-13 Coding-Agent Work Order, with the Completion Contract, P4/P5, Architecture Resolutions and Decisions 048/051/052. See `evidence/im13/inspect-first.md` for the pre-edit gap map and test plan.

Branch `copyofdev`; starting and ending HEAD `c9ffb07fd8828c9e8f8ee9de7b401170364938b6` (IM-12 changes). Starting tracked tree and index were clean. The supplied work order and matrix were untracked; the evidence directory was created for this audit. The historical matrix deletion was already committed at this HEAD; it was not restored. Starting Git state, ignored build directories and baseline file hashes are recorded in `starting-state.json` and `preservation-before.json`.

The IDE interruption left completed tests and browser evidence, five bounded source edits and a new regression test, but no final report/mirrors/ending inventory. Completion reviewed those artifacts, executed the unfinished real-Docker HTTP allowlist audit successfully, synchronized mirrors and produced this handoff. Saved earlier execution results are distinguished from the resumed HTTP run; the full suites were not unnecessarily repeated after documentation-only additions.

## Untouched baseline and final quality gates

All paths below are relative to `docs/v2-completion-plan/evidence/im13/`. `command-ledger.json` records artifact timestamps, commands, evidence and outcome; historical shell exit values not separately captured are identified as such rather than reconstructed as fact.

| Gate | Untouched baseline | Final evidence and result |
|---|---|---|
| Backend Maven verify/package | `baseline-backend.txt` | `final-backend.txt`: 631 tests, zero failures/errors/skips, BUILD SUCCESS; executable jar produced |
| Frontend Vitest | `baseline-frontend.txt`: 158 tests | `final-frontend.txt`: 162 tests across 23 files passed |
| TypeScript | `baseline-typecheck.txt` | `final-typecheck.txt`; build also runs TypeScript successfully |
| Production Vite build | `baseline-build.txt` | `final-build.txt`: built successfully |
| Python | `baseline-python.json` and logs | `final-python.json`, `final-python-0.txt`: 16 tests passed, exit 0 |
| Six validators | baseline Python ledger/logs | `final-python-1.txt` through `-6.txt`: content sources, homepage, navigation, news, organizations, schema; all recorded exit 0 |
| Existing multi-stage Docker image | before/build logs retained | `docker-build-final.txt`: image `firststep-im13:local` built; `docker-runtime-final.txt`: external catalogs loaded and application ready |
| Packaged manifest deployment | isolated copies | `packaged-http.json`: valid/missing/malformed scenarios verified |
| Built-browser release journeys | failed probes retained | `browser-result.json`: PASS, no recorded browser errors; Chromium 151.0.7922.34 |
| Actual Docker public HTTP | earlier assertion failures retained | `http-release.json`, `http-release-final.txt`: PASS, 65 responses and four SPA/media paths |

## Seven journeys and five epics

| Journey | Observed outcome and named evidence | Result |
|---|---|---|
| J1 Category Help | Browser `T02-J1-help`: four preview items, all 36 Housing/Wilmington Help records, zero-help Eviction Prevention, correct direct navigation/counts; `HelpBrowseApiTest`, `CategoryDestinationCatalogTest` in final Maven suite | PASS |
| J2 Featured Topics | Browser `T03-J2-topics`, `FeaturedTopicCatalogTest`, `FeaturedItemCatalogTest`, `Im11RealCollisionRegressionTest`: four deterministic topics; durable winners HA-002/016/011/008, each with two distinct truthful FALLBACK items; Homeownership collision/order regressions retained | PASS |
| J3 Offering/provider | Browser `T04-J3-provider`: HA-001 inventory → existing Category Organizations association → ORG-001 provider offering card → back, scoped Community retained. This uses existing navigation, not an invented in-card provider link. `Im12RelationshipTest`, `Im12BoundaryRegressionTest` and catalog audit cover positive links and noninference | PASS within existing explicit-provider workflow |
| J4 Civic information | Browser `T05-J4-collections`: Policy NP-004/005, Flyer FL-002 original media, First Rate News OR-003/FAQ-002/EA-001/EA-005; local Law cache empty honestly. `EventOccurrenceTest`, `EventOccurrenceCatalogTest`, `FirstRateNewsMembershipTest`, `CivicCollectionCatalogTest` rerun in final suite | PASS; live remote Law coverage not certified |
| J5 Authoring | `editor-workflow.json`, script and fixture captures: isolated external catalog, create/read/update/reload same SD-913 identity; optional provider removal, explicit-owner precedence, authored Topic, separate source; validator rejects exact unknown provider ORG-999 and invalid Topic diagnostics | PASS for supported manual JSON workflow; no Admin UI/hot reload claimed |
| J6 Stewardship | `expiry-packaged-all.txt`, `expiry-packaged-bear.txt`; `EditorialExpiryDiagnosticsTest`, `EditorialExpiryReportCommandTest`, `EditorialLifecycleTest`, `EventOccurrenceTest`: fixed-clock lifecycle and independent occurrence, scoped/zone-aware CLI; NP-003 approaching remains current, five expired News retained outside current views | PASS for read-only CLI/manual stewardship |
| J7 Integrity | `Im12CatalogAuditTest`, `Im12OwnershipAndErrorTest`, `Im12AdditionalBoundaryTest`, `Im12BoundaryRegressionTest`; `catalog-final.json`, `endpoints-final.json`, `scoped-endpoints-final.json`, actual `http-release.json`: canonical identity, classifications, privacy and Community partitions preserved | PASS |

| Epic | Implementation packages and current proof |
|---|---|
| E1 Civic Catalog Identity & Relationships | IM-04/10/12; J3/J5/J7, explicit association tests, isolated authoring and catalog/provider traces |
| E2 Resident Help & Topic Discovery | IM-04–08; J1/J2, full Help browser path and selector/collision regressions |
| E3 Civic Information Collections | IM-08–10; J4/J7, controlled collections, manifest and occurrence/browser/API evidence |
| E4 Content Lifecycle & Editorial Stewardship | IM-01/02/07; J6, fixed-clock suites and packaged diagnostics; manual interim Admin process |
| E5 Catalog Integrity & Classification | IM-03/04/06/07/09–12; J2/J5/J7, real catalog audits, negative fixtures, serialized public allowlists |

## Catalog, identity, Community and public boundaries

Final audit clock is `2026-10-17T16:00:00Z`; 262 content rows with unique canonical IDs, six concrete kinds/five ContentTypes, eight owning Communities, 64 Organizations and 58 Resources with resolving explicit providers. The 171 inherited demo Resources without provider IDs remain unchanged future library-management input. Unique IDs do not establish semantic nonduplication. All 229 stored Resource rows retain legacy Community normalization; isolated fixtures verify explicit-owner precedence. No persisted typed HOST graph is claimed, and `dscyf-directory` is not promoted to an Organization or assigned an invented sector.

Final audit captures 26 endpoint mappings and 1,160 scoped responses. Browser Food/Bear verifies SD-032/042; empty/unknown and invalid scopes are distinguished. Controlled Category projections are scoped; intentional aggregate legacy semantics are retained. Approved Article detail now forwards its requested Community to the existing backend gate. Exact concrete field paths, nested source shapes and Search kind keys are checked by the actual HTTP audit, alongside existing AI sanitation/exception tests. Search uses an independent concrete DTO allowlist because the old MockMvc URI-string probe retains `+` in multiword searches while real HTTP decodes it. The final script includes the independently observed envelope timestamp; it does not waive a private content field.

Eight original production Flyers and exact helpAccess tokens are preserved; no production Help activation was added. Safe HTTP(S), telephone, approved internal Article routes, source-less unlinked content and original media are exercised by browser/API suites. Remote website factual accuracy/reachability is outside the executed proof.

## Packaged runtime and configuration

The actual existing multi-stage Dockerfile builds Vite and the executable backend together; final image manifest list is `sha256:af5e4162477c874106ec77ab8c5b7ecf8f53101479e91cecc04a6ca6d37e6b13`. Runtime logs load external `/data` catalogs, 229 Resources, eight Flyers, three Articles (one approved), six FAQs, six Experts, ten News and 64 Organizations. Built SPA `/app-next/`, deep routes, fonts and media are served same-origin with the actual backend at port 8113; no development proxy substitute is claimed.

`packaged-check.py` uses temporary full external catalog copies and explicit absolute `app.data.dir`: valid manifest yields all four First Rate News members; missing manifest starts successfully with only OR-003; malformed manifest exits 1 before resident readiness. A release intended to show all four must distribute the manifest. This is an operational requirement, not a claim that missing manifest prevents startup.

## Accessibility, responsive and failure evidence

Built Chromium browser measurements cover 375, 1280 and 1440 pixels, no horizontal overflow, primary content before the secondary rail on mobile, one main/level-one heading, names/alt text, keyboard navigation/refresh/back and controlled switcher behavior. Screenshots and accessibility-tree captures are retained. Completion visually inspected mobile Housing and Spanish/high-contrast captures; measured synthetic Law layouts are explicitly synthetic because the admitted live Law cache is empty. Browser delay and aborted-network cases are injected faults, not claimed production incidents.

Spanish document language and diacritics, production font requests, high-contrast visible yellow focus outlines and final theme probes pass. Spanish UI parity does not imply translation of authored catalog titles/body text. `browser-probe-after.json` includes observations with empty fact arrays; these alone are not independent contrast certification. Automated DOM/Chromium accessibility-tree checks and focused keyboard review support the bounded gate; no axe/pa11y was installed.

**NOT TESTED:** full WCAG conformance, physical mobile devices, assistive screen-reader execution and exhaustive external provider/RSS coverage. These limits remain explicit release-review information; no conformance or physical-device certification is asserted.

## Defects and bounded corrections

| Demonstrated issue | Minimal fix | Before/after proof |
|---|---|---|
| Article fetch dropped Community; route destinations lacked useful heading focus | Forward existing query; focus Article/provider heading; preserve Community on error return | `frontend-boundary-before.txt`, four new `Im13ReleaseBoundary.test.tsx` tests, final frontend and browser results |
| Missing Topic error used h2 and did not focus the error destination | Use/focus h1 | Same boundary test and browser network/error checks |
| Warm LAW/FLYER badges, urgency/focus/hover ink failed intended contrast behavior | Bounded existing card/theme CSS token corrections | Retained before-confirmed screenshots/AX probes, after probes/screenshots and built-browser high-contrast checks |
| HTTP harness rejected real Search envelope timestamp after earlier query/shape mismatch | Compare actual envelope against independently audited resource envelope, retaining exact kind/nested allowlists | `http-release-first-failed.txt`, `http-release-second-failed.txt`, `http-release.txt`; final script and PASS artifact |

Early browser failures `failure-T02-J1-help.png` and `failure-T13-responsive.png` and first-result logs remain diagnostic history, superseded by final browser PASS; they are not silently deleted or counted as current failures. No catalog, editorial admission, taxonomy, selector or backend behavior was changed in this package.

## Named T01–T20 acceptance mapping

| ID | Disposition | Evidence |
|---|---|---|
| T01 | PASS | starting state, inspect-first, baseline hashes |
| T02 | PASS | J1 browser check, HelpBrowseApiTest, HTTP captures |
| T03 | PASS | J2 browser IDs/positions, selector/catalog/collision suites |
| T04 | PASS, existing association navigation | J3 browser, explicit-provider/noninference tests |
| T05 | PASS, empty local Law acknowledged | J4 browser, membership/collection suites |
| T06 | PASS | EventOccurrenceTest/CatalogTest; FL-008 date-only local next-midnight and DST/structured boundaries; editorial expiry independent |
| T07 | PASS, manual workflow | isolated editor fixture/read/reload and exact validator diagnostics |
| T08 | PASS, CLI/manual boundary | packaged expiry logs and lifecycle/diagnostics tests |
| T09 | PASS | final 262-row audit, classification/Article/admission/Flyer tests |
| T10 | PASS | 1,160 scoped audit responses, Bear browser, Article scope regression |
| T11 | PASS | exact public HTTP allowlists plus privacy/AI/error suites |
| T12 | PASS | actual Docker build/runtime, executable three-manifest scenarios, SPA/assets |
| T13 | PASS for tested viewports | browser measured 375/1280/1440, screenshots, labeled synthetic Law |
| T14 | PASS for executed keyboard/DOM cases | browser navigation/fault checks, heading-focus regression tests |
| T15 | PASS for executed checks; broader accessibility NOT TESTED | Spanish/high-contrast/font/browser/AX artifacts and limitations above |
| T16 | PASS for supported local paths | source/media/absent-provider/unsafe scheme tests and browser captures; external reachability not certified |
| T17 | PASS | J/E tables in this report; no demonstrated unresolved v2 blocker |
| T18 | PASS | final Maven suites/catalog and endpoint reruns; protected original catalog hashes |
| T19 | PASS for executed gates | quality-gate table, command ledger and original/final logs |
| T20 | PASS | exact patch/file list, synchronized full mirrors, ending hash inventory and issue register |

## Preservation and exact change manifest

Five existing production files changed: `frontend/src/components/ContentCard/ContentCard.module.css`, `frontend/src/pages/ArticlePage/ArticlePage.tsx`, `frontend/src/pages/OrganizationPage/OrganizationPage.tsx`, `frontend/src/pages/TopicPage/TopicPage.tsx`, `frontend/src/styles/themes.css`. New regression source: `frontend/src/Im13ReleaseBoundary.test.tsx`. Complete annotated mirrors for these six files are listed and verified in `annotated-mirrors.json`; no unrelated mirror drift was repaired.

`production.patch` preserves the tracked source diff; `ending-state.json` records index/worktree/untracked/ignored state. `preservation-after.json` hashes baseline paths and new deliverables, excluding itself to avoid self-reference; `preservation-comparison.json` identifies every baseline change. Existing mirror changes are documentation updates only. Catalogs, manifest and both supplied authority documents match their original protected hashes. No staging, commit, push, stash/reset, catalog deduplication, provider backfill or speculative redesign was performed.

## Final issue register and handoff

| State | Item | Disposition |
|---|---|---|
| PASS | Demonstrated scope, focus, hierarchy and contrast defects | Corrected within existing v2 contracts; final tests green |
| PASS | Interrupted HTTP release audit and missing report/mirrors/preservation deliverables | Completed in this handoff |
| FAIL | Unresolved demonstrated required-v2 release failure | None found in executed evidence |
| NOT TESTED | Full WCAG, screen reader and physical-device testing | No certification claimed; reviewer may require further testing for a separate release policy |
| NOT TESTED | Exhaustive external provider/feed coverage | Local empty Law cache is a data limitation, not evidence of no Delaware laws |
| IM-14 | Original Admin Guide, complete historical/deployment documentation and final v2 reconciliation | Remains pending; include manifest operations and manual scoped expiry/authoring guidance |
| V3/Later | Admin dashboard/queues/email, generalized provider/HOST persistence, semantic duplicate audit, demo library-management design, second-community launch | Deferred; no new implementation pulled forward |

Proposed next action is reviewer acceptance of IM-13 on this evidence, followed by IM-14. The authoritative matrix and final v2 status remain unchanged.
