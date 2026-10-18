# IM-13 — Final Verification Closure Supplement

October 17, 2026. **Disposition: PASS — submitted for reviewer acceptance.** This supersedes the two evidence gaps in the earlier Evidence-Integrity Supplement; it does not change the authoritative matrix or declare v2 complete.

All artifact paths below are relative to `docs/v2-completion-plan/evidence/im13/closure/`.

## Bound inputs and actual distribution

`inputs-before.json` and `inputs-after.json` record SHA-256 hashes of executable source/tests, backend/frontend configuration, catalogs, and the actual verification scripts. `binding-result.json` records identical before/after maps: **402 inputs, zero changes**. Canonical SHA-256 of the sorted input map before and after: `bd11c91d28965f639e8127f93b34af65317cc218d502049152da1b8b0241a511`. These include all five modified frontend files and the new regression test. The final hash check also confirms they still match the source state covered by the existing production patch and six annotated mirrors. Historical run provenance is no longer needed to establish the new gate results.

`commands.json` records exact argument arrays, working directories, explicit environment overrides, start/end UTC timestamps, process exit codes and output-log SHA-256 hashes. `distribution-hashes.json` records the local executable jar, actual Docker executable jar, local frontend build and each packaged SPA asset. Every packaged SPA asset was fetched from the fresh running distribution and its bytes matched the jar. `docker-image.txt` records the fresh image identity; `docker-start.txt` and `docker-runtime.txt` identify its local runtime. The Docker build used the existing multi-stage Dockerfile and unchanged catalogs at `/data`.

## Gate results

| Existing gate rerun | Result | Exact artifact |
|---|---|---|
| Maven verify/package, catalog and scope/privacy regressions | 631 tests; BUILD SUCCESS; exit 0 | `backend-verify.txt`, `catalog-closure.json`, `endpoints-closure.json`, `scoped-endpoints-closure.json` |
| Frontend tests | 162 tests/23 files passed; exit 0 | `frontend-tests.txt` |
| TypeScript and production build | Both exit 0 | `frontend-typecheck.txt`, `frontend-build.txt` |
| Python tests | 16 passed; exit 0 | `python-tests.txt` |
| Six validators | All exit 0 | `validator-content_sources.txt`, `validator-homepage.txt`, `validator-navigation.txt`, `validator-news.txt`, `validator-organizations.txt`, `validator-schema.txt` |
| Actual Docker build/runtime | Build/start/copy/readiness succeeded | `docker-build.txt`, `docker-image.txt`, `docker-start.txt`, `docker-copy-distribution.txt`, `docker-runtime.txt` |
| Public HTTP | All 65 requests and four SPA/media paths PASS | `http-release.txt`, `http-release.json` |
| Valid/missing/malformed external manifest | PASS | `packaged-manifests.txt`, `packaged-http.json`, `packaged-positive.log`, `packaged-missing.log`, `packaged-malformed.log` |
| J5 authoring/J6 expiry | PASS; isolated catalogs only | `editor-and-expiry.txt`, `editor-workflow.json`, `expiry-packaged-all.txt`, `expiry-packaged-bear.txt` |
| Built resident browser journeys and accessibility probes | PASS | `browser-release.txt`, `browser-result.json`, `browser-probe.txt`, `browser-probe-after.json` |

The first fresh attempt is preserved in `attempt-1/`. Backend passed, but Node 26's experimental web-storage global left `localStorage` undefined for jsdom; cleanup errors caused 17 frontend failures. The successful repeat uses `NODE_OPTIONS=--no-experimental-webstorage`, recorded explicitly in `commands.json`, allowing jsdom storage. No product/test expectation was weakened or production source changed. Both attempts have before/after input inventories; only the runner's explicit test environment option changed between them.

## Corrected HTTP audit

The original incomplete script and HTTP artifacts remain under `prior/`. The corrected `../http-release.py` retains the other 64 checks and recursive private-field exclusions. It fails before requests if a reference schema is missing, and fails rather than assigning an exact-match flag to an unchecked route.

`POST /api/decide` now explicitly requires the public envelope `success,data,errorCode,errorMessage,timestamp` and summary `answerTitle,steps,citations,notes`. Any present steps require `order,title,action,why`; citations require `sourceType,id,label,contentSource`, and a non-null source requires `id,name,type,url`. Recursive private-field exclusions apply throughout. The final response field paths are retained in the `/api/decide` entry of `http-release.json`.

`missing-schema-control.json` independently executes the actual harness guard against a deliberately removed expected schema and records its rejection. `http-negative-controls.json` proves deliberately unexpected properties are rejected at envelope, summary, step, citation and source depth, plus a nested private-field injection. The synthetic positive control includes nonempty steps/citations. The actual packaged AI response follows the existing sanitized unavailable-provider path with empty arrays; no external AI-provider success is claimed. Existing backend nonempty attribution/AI sanitation tests also ran in Maven verification.

## T01–T20 and seven-journey closure

| Case | Final disposition and evidence |
|---|---|
| T01 | PASS: original `../starting-state.json`, `../inspect-first.md`, protected baseline; fresh input inventories and final source-consistency check |
| T02 | PASS: `browser-result.json` T02-J1-help — four-preview/36 complete Housing Help, zero-help Topic; `backend-verify.txt` |
| T03 | PASS: browser T03-J2-topics, `catalog-closure.json`, rerun selector/collision tests — four topics, twelve positions, truthful fallback |
| T04 | PASS within existing explicit-association navigation: browser T04-J3-provider and backend relationship tests; no in-card provider link invented |
| T05 | PASS: browser T05-J4-collections, exact HTTP and membership tests; empty local Law cache acknowledged |
| T06 | PASS: rerun EventOccurrenceTest/EventOccurrenceCatalogTest and lifecycle tests in `backend-verify.txt`; Community-local date/DST boundaries independent of expiry |
| T07 | PASS: `editor-workflow.json`, isolated create/update/reload and exact negative-provider/Topic validator diagnostics |
| T08 | PASS: packaged all/Bear expiry logs, lifecycle/diagnostics suites; manual scoped stewardship boundary |
| T09 | PASS: `catalog-closure.json`, backend identity/classification/Article/Flyer guards; unchanged production catalog hashes |
| T10 | PASS: `scoped-endpoints-closure.json`, browser T10-bear-empty-invalid, rerun Article Community test |
| T11 | PASS: all 65 corrected exact HTTP schema checks, nested exclusions/negative controls and backend privacy/AI/error tests |
| T12 | PASS: freshly built Docker distribution, served-asset hash equality, external three-manifest scenarios |
| T13 | PASS for tested 375/1280/1440 widths: browser responsive/synthetic-Law checks and new screenshots; no physical-device claim |
| T14 | PASS for executed keyboard, focus, heading, loading/network cases: browser results, rerun frontend regression tests |
| T15 | PASS for executed Spanish/diacritics/high-contrast/font/DOM/AX checks; comprehensive accessibility remains NOT TESTED |
| T16 | PASS for supported local source/media/safe and absent-link paths: browser and backend/packaged checks; external coverage limitation retained |
| T17 | PASS: J/E mapping in [IM-13 Execution Report](IM-13%20Execution%20Report.md), refreshed J/T evidence here; no demonstrated unresolved v2 blocker |
| T18 | PASS: full fresh regression/audit rerun and identical before/after source/catalog/config hashes |
| T19 | PASS: all final commands exit 0, timestamps/log hashes recorded, fresh jar/image/SPA assets hashed |
| T20 | PASS: original incomplete and first failed attempts retained, final patch/test/mirrors consistent, fresh inventories and final artifact manifest |

Seven journeys are covered by the existing combined release workflow: **J1/J2/J3/J4** in the actual built browser and HTTP checks, **J5** in isolated manual authoring/read/reload, **J6** in packaged scoped expiry plus fixed-clock suites, and **J7** in rerun full catalog/Community/classification/privacy audits. Editor journeys are not falsely described as an Admin UI browser workflow. E1–E5 mappings in the execution report remain applicable to these fresh reruns.

`ending-state.json` records the final HEAD, index and worktree inventory. `final-consistency.json` rechecks current input bytes, all 22 zero exits and their log hashes, the original production patch/six mirrors, and original catalog/authority hashes. `tooling-mirrors.json` lists complete annotated mirrors of the touched evidence scripts. `artifact-manifest.json` hashes the final closure evidence and this supplement (excluding the manifest itself).

## Remaining findings and scope

**No unresolved FAIL remains in the final verification.** The first-attempt environment failure is retained and superseded by the successful bound rerun.

Fresh mobile/desktop and Spanish/high-contrast screenshots were visually inspected; automated/browser probes remain scoped checks.

**NOT TESTED:** comprehensive WCAG conformance, physical-device testing, screen-reader execution and exhaustive external-content/provider/feed verification. Local empty Law cache and lack of remote AI-provider success remain disclosed limitations. These results are scoped release evidence, not conformance certification.

Only audit tooling/output routing and verification documentation changed for this closure. Production functionality, selectors, editorial decisions, Community ownership, approved classifications and catalog records were not changed. No commit, push or authoritative-matrix update was performed. IM-14 and final v2 closure remain pending reviewer process.
