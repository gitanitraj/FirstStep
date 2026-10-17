# IM-09 actual command ledger and reproduction

All commands from repository root; existing JDK25 toolchain with Java17 release, existing Mockito agent, existing Node webstorage workaround. No dependencies installed or lockfiles changed.

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home /Users/composedmedia/tools/apache-maven-3.9.16/bin/mvn -o -f backend/pom.xml '-DargLine=-javaagent:/Users/composedmedia/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar' verify
NODE_OPTIONS=--no-experimental-webstorage npm --prefix frontend test
frontend/node_modules/.bin/tsc --noEmit -p frontend/tsconfig.json
npm --prefix frontend run build
python3 -m unittest discover -s data-cleaning/tests -v
python3 data-cleaning/scripts/validate_content_sources.py
python3 data-cleaning/scripts/validate_homepage.py
python3 data-cleaning/scripts/validate_navigation.py
python3 data-cleaning/scripts/validate_news.py
python3 data-cleaning/scripts/validate_organizations.py
python3 data-cleaning/scripts/validate_schema.py
```

Baseline logs baseline-{backend,frontend,typecheck,build}.txt and baseline-python-{0..6}.txt. Baseline537 backend,143 frontend,14 Python, six validators, typecheck/build all pass.

Before edits: new CivicCollectionContractTest failed3 of4 tests, specifically inferred RSS Law admitted to Category, equal-publication-date ordering following input order, duplicate Article ID counted twice (demonstrated-gaps.txt). CivicCollectionCatalogTest before edits passed3 cases and emitted collection-audit-before.json at fixed2026-10-16T16:00Z. This snapshot records observed First Rate News exclusion of Expert/FAQ; it does not approve Originals-only membership.

Focused commands use the same Maven/agent prefix, `test` and `-Dtest=...`; real audit adds `-Dim09.evidence.dir=../docs/v2-completion-plan/evidence/im09`, baseline phase also `-Dim09.audit.phase=before`.

Initial corrected code run (focused-first.txt) exposed6 failed/1 errored predecessor fixtures that claimed authored Category law without recording its provenance. Existing positive law fixtures now explicitly supply original authoredCategoryTags; no real RSS metadata/catalog was edited. New negative fixture separately proves inferred law cannot enter collection, legacy Category updates or counts while broad sector/relevance feed remains intact. Focused-second/third attempts exposed missing assertion import/temp-directory parameter in the new undated-feed test; corrected with existing JUnit5 TempDir pattern. All failed attempts retained; final logs govern acceptance. No test was skipped or disabled.

Final backend command is the same prefix plus `-Dim09.evidence.dir=../docs/v2-completion-plan/evidence/im09 clean verify`, forcing a clean compile after test scaffolding corrections. Final counts/results are in execution report and verification-summary.json. The audit property only writes test evidence, never runtime state. Full scopes include Housing/Wilmington, Food/Wilmington, Food/bear-de and Legal/Wilmington, plus synthetic wrong Community/Category fixtures. Tests use MockMvc HTTP dispatch with actual Spring/repositories and fixed Clock; not a remote RSS/complete-feed claim.

Public API samples use `/api/category/housing?communityId=wilmington-de`, `/api/news?communityId=wilmington-de`, `/api/news/rss?communityId=other`, `/api/originals/OR-003`, and flagged/nonexistent Article404 paths. No provider or legal-content accuracy audit is claimed; source facts are preserved, not endorsed.

The editorial question was resolved by explicit user approval of EA-001/EA-005/FAQ-002 and denial of EA-002/003/004/006. The minimal scoped manifest was documented in first-rate-news-decision.md before implementation. Original records/catalogs were untouched. Final acceptance logs are final-backend-admissions.txt (558), final-frontend-admissions-accepted.txt (144), final-typecheck-admissions.txt and final-build-admissions.txt. The first new frontend test required expansion of the existing preview; corrected without changing product disclosure behavior.

Packaged API and focused built-app checks (local servers, then stop with Ctrl-C):
```sh
/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/java -jar backend/target/firststep-backend-0.1.0.jar --server.port=8109 --news.rss.initial-delay=86400000
python3 docs/v2-completion-plan/evidence/im09/serve-built.py
python3 docs/v2-completion-plan/evidence/im09/http-smoke.py
node docs/v2-completion-plan/evidence/im09/browser-check.mjs
```
Binding/loopback and Chromium launch required explicit sandbox escalation. The browser's first approved assertion counted the containing region, including supporting FAQ/Expert projections (7 entries), instead of the four selected collection cards. It was corrected to scope assertions to #active-collection; independent supporting memberships are deliberate. Failed logs remain. Browser smoke is limited to local built app, keyboard collection selection, four exact memberships, safe Article/source links,375/1280 layout and page errors. No external provider/network content certification.
