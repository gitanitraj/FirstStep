# IM-13 — Final Evidence-Integrity Supplement

October 17, 2026. **Overall determination: FAIL for unconditional evidence-integrity acceptance**, limited to the two verification gaps below. This is not a newly demonstrated product defect. Existing functional results and NOT TESTED findings remain intact. No implementation was restarted.

Artifact paths below are relative to `docs/v2-completion-plan/evidence/im13/`. The newly executed offline checks are recorded in [integrity-check.json](evidence/im13/integrity-check.json).

## 1. Final source consistency — PASS

Recomputed all hashes in `preservation-after.json`: zero mismatches. Current `git diff -- frontend/src` equals `production.patch` byte for byte. All six source hashes in `annotated-mirrors.json` match current bytes, including the untracked `frontend/src/Im13ReleaseBoundary.test.tsx`; all six mirrors contain the full corresponding source unchanged. Current HEAD, index and tracked diff match `ending-state.json`. The new test and mirrors appear in that inventory; its untracked evidence directory is intentionally directory-level, with individual contents covered by the hash inventory. Only this supplement and `integrity-check.json` were added during this confirmation; original inventories were not rewritten.

`preservation-comparison.json` records the nine expected existing source/reference changes against the initial baseline. Supplied authorities, manifest and catalogs remain protected. No staging, commit or push occurred.

## 2. Verification chronology — saved successes confirmed; historical source binding incomplete

The execution report and `command-ledger.json` identify these as **before interruption**: final Maven (631 tests, BUILD SUCCESS), frontend (162 tests), TypeScript/build, Python (16 tests)/six validators, Docker build/runtime, editor workflow, expiry CLI, packaged three-manifest checks and final browser PASS. Exact interruption time was not captured; the boundary follows the recovered session history, not an invented timestamp.

These artifacts record the original results: `final-backend.txt`, `final-frontend.txt`, `final-typecheck.txt`, `final-build.txt`, `final-python.json`, `final-python-0.txt`–`final-python-6.txt`, `docker-build-final.txt`, `docker-runtime-final.txt`, `editor-workflow.json`, `expiry-packaged-all.txt`, `expiry-packaged-bear.txt`, `packaged-http.json`, `browser-result.json`.

**After interruption:** the local Docker HTTP audit completed successfully at `2026-10-17T23:15:00Z` (`http-release.json`, `http-release-final.txt`). Mirrors, report and ending inventories were then produced. This confirmation adds offline hash, allowlist and nested-private-key mutation checks; it does not claim a new runtime suite run.

The five changed production sources have mtimes around `22:08:30Z`; the new regression test has mtime `22:05:08Z`. They precede saved final frontend completion (`22:08:36Z`), build (`22:08:42Z`), Docker build (`22:12:59Z`), backend completion (`22:14:52Z`) and later browser/package checks. Recovery introduced only documentation/mirrors, not executable source. These facts support no subsequent relevant source edit, and current bytes match the ending inventory. **However, no verification-time source hash manifest binds those earlier runs to their exact inputs; mtimes and later hashes cannot independently prove that historical binding.** Earlier gate PASS claims remain saved execution evidence, not independently rerun verification in this supplement.

Narrow closure if a proven run-to-source binding is required: capture current executable-source/config/catalog hashes immediately before and after the applicable frontend test/build and built-browser checks; retain commands, exits and distribution asset hashes. For independent recertification of backend/Python gates, rerun only their existing suites/validators against the same protected inputs; no implementation change is needed. This is an evidence provenance limitation, not a reason to add tests or requirements.

## 3. Docker HTTP allowlists — qualified PASS for 64 responses; one harness overclaim

Offline replay of the actual `http-release.py` checks against `http-release.json` reproduces exact allowlist checks for **64 of 65** responses. Non-Search routes require exact field-path equality to `http-baseline.json`. Search requires exact envelope keys, exact hit/content-kind keys and exact non-null source keys (`id,name,type,url`), with nested field paths restricted to the independently captured public DTO paths. Search permits optional nested shapes via a path subset check; it does not require every possible optional field to occur. The envelope `timestamp` is justified by the preexisting baseline resource envelope, not by adding an arbitrary unexpected property to a wildcard. Real Search responses are nonempty, unlike eight baseline MockMvc multiword probes.

The recursive `fields` function rejects all 12 named private keys at every depth. This confirmation executed that saved function with each prohibited key injected beneath `data[].content.contentSource`; every injection failed. The saved observed paths were also recomputed. These checks do not certify unobserved content kinds or empty nested-array element schemas.

**Gap:** baseline `POST /api/decide` lacks `fieldPaths`. The script consequently checks its HTTP status and recursive private-key exclusion but skips exact allowlist comparison, yet writes `exactObservedAllowlistMatch: true` and `schemaMode: exact concrete field paths`. Those labels overstate what ran. Offline checking here confirms the saved response has the expected envelope and summary keys (`answerTitle,steps,citations,notes`); both nested arrays are empty, so their element shapes are unexercised.

**Narrow required verification:** correct only the evidence harness to supply an explicit `/api/decide` envelope/summary allowlist and fail rather than claim an exact match when an expected schema is missing. Rerun that read-only request, preserving the previous artifact, and retain the existing nonempty AI citation/step and exception-sanitation test results (or rerun those specific tests if independent execution is required). No production code correction is indicated. This supplement does not silently rewrite the prior audit/report.

## 4. T01–T20 reconciliation

“Saved” means the runtime result is supported by preserved logs/JSON/screenshots whose hashes were checked, but was not re-executed here. It must not be described as independently rerun during this confirmation.

| Case | Final evidence | Integrity disposition |
|---|---|---|
| T01 | `starting-state.json`, `inspect-first.md`, `preservation-before.json` | PASS for current consistency; historical baseline capture is saved evidence |
| T02 | `browser-result.json` → T02-J1-help; `final-backend.txt` | Saved PASS; no independent runtime replay |
| T03 | `browser-result.json` → T03-J2-topics; `catalog-final.json`; `final-backend.txt` | Saved PASS; no independent selector/browser replay |
| T04 | `browser-result.json` → T04-J3-provider; `catalog-final.json` | Saved PASS for existing association navigation, not an in-card provider link |
| T05 | `browser-result.json` → T05-J4-collections; `http-release.json` | Saved browser PASS plus offline-revalidated HTTP shapes; empty Law limitation retained |
| T06 | `final-backend.txt` → EventOccurrenceTest/EventOccurrenceCatalogTest | Saved boundary-test PASS; no new clock execution |
| T07 | `editor-workflow.json`, `editor-workflow.txt`, `editor-invalid-provider.txt`, `editor-invalid-topic.txt` | Saved manual authoring/negative-diagnostic PASS |
| T08 | `expiry-packaged-all.txt`, `expiry-packaged-bear.txt`, `final-backend.txt` | Saved CLI/lifecycle PASS |
| T09 | `catalog-final.json`, `endpoints-final.json`, `final-backend.txt` | Saved classification/identity PASS |
| T10 | `scoped-endpoints-final.json`, `browser-result.json` → T10-bear-empty-invalid; `final-frontend.txt` | Saved scope PASS; source/mirror hashes independently matched |
| T11 | `http-release.py`, `http-baseline.json`, `http-release.json`, `final-backend.txt` | FAIL for blanket 65-response exact-schema claim; 64 offline revalidated, decide gap above |
| T12 | `docker-build-final.txt`, `docker-runtime-final.txt`, `packaged-http.json`, `http-release.json` | Saved build/manifest PASS; distribution-to-source input hashes not captured at execution |
| T13 | `browser-result.json` → T13-responsive/T13-synthetic-law-layout; `housing-375.png`, `housing-1280.png`, `housing-1440.png` | Saved viewport PASS; synthetic Law scope retained |
| T14 | `browser-result.json` → T14-loading-and-network; `final-frontend.txt`; `frontend-boundary-before.txt` | Saved keyboard/error PASS; no screen-reader certification |
| T15 | `browser-result.json` → T15-spanish-high-contrast-fonts; `browser-probe-after.json`; `final-spanish-ax-tree.json` | Saved scoped PASS; empty probe fact arrays are not standalone contrast proof |
| T16 | `browser-result.json`, `packaged-http.json`, `final-backend.txt` | Saved supported-path PASS; remote reachability not verified |
| T17 | `../IM-13 Execution Report.md` J/E tables and named T mapping | Traceability present; acceptance qualified by T11/T19 provenance gaps |
| T18 | `final-backend.txt`, `catalog-final.json`, `scoped-endpoints-final.json`, `preservation-comparison.json` | Saved regressions PASS; preserved inputs independently hash-checked |
| T19 | `command-ledger.json` and final suite/build/Python logs | Saved successes; historical exact flags/exits partly unknown and run-time input hashes missing |
| T20 | `production.patch`, `annotated-mirrors.json`, `ending-state.json`, `preservation-after.json`, `integrity-check.json` | PASS for artifact/source consistency; prior HTTP overclaim disclosed above |

Thus the runtime portions of T02–T10, T12–T16 and T18–T19 still rest on saved execution artifacts rather than independent re-execution here. Hash integrity is verified; a saved PASS status alone is not promoted into fresh execution proof.

## 5. Findings preserved

**NOT TESTED:** full WCAG conformance, physical-device testing, screen-reader execution and exhaustive external-content/provider/feed verification. No new conformance claim is made. No authoritative matrix or catalog changes, commit/push, or First Step v2 completion declaration accompanies this supplement.
