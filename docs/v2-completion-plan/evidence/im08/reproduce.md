# IM-08 reproduction and actual command ledger

Starting branch copyofdev, HEAD6336166 (full hash in starting/verification state). Only user-added v2.8 matrix and IM-08 work order untracked. All880 starting tracked/untracked files protected before editing. No reset/stash/stage/commit/push.

From root (existing local Maven/JDK25/Mockito caches, Java17 compilation release):

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home /Users/composedmedia/tools/apache-maven-3.9.16/bin/mvn -o -f backend/pom.xml '-DargLine=-javaagent:/Users/composedmedia/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar' -Dim08.evidence.dir=../docs/v2-completion-plan/evidence/im08 verify
```

baseline-backend.txt:529 tests, exit0/full packaging. final-backend.txt:537 tests,0 failures/errors/skips, exit0/full executable packaging. Agent and Node settings are the already-proven environment settings from IM-07; no dependency/config changes.

Focused same Maven/agent options with `-Dtest=CategoryDestinationServiceTest,CategoryDestinationCatalogTest,ScopedOrganizationPreviewTest,CategoryPageServiceTest,CategoryControllerTest test`:29 passed (focused-backend-second.txt). Earlier existing-only composition/controller/Organization checks:30 passed (focused-backend-first.txt). Catalog test uses actual JSON, real services/selection and fixed Clock2026-10-16T16:00Z; IM08 property emits real-category-api.json through actual Spring MockMvc HTTP response, not a synthetic count fixture.

```sh
NODE_OPTIONS=--no-experimental-webstorage npm --prefix frontend test
frontend/node_modules/.bin/tsc --noEmit -p frontend/tsconfig.json
npm --prefix frontend run build
```

Baseline133 tests/typecheck/build pass. Final **143 tests /21 files**, typecheck and Vite production build pass; final-frontend-accepted.txt, final-typecheck.txt, final-build.txt. Existing IM-05 browse and all other frontend assertions are included. Initial two Category tests expected the previously deferred Organizations and hidden empty-help section; replaced by new authorized behavior checks. New fixture tests initially used ambiguous breadcrumb lookup/wrong existing UI label case and expand-control wording; corrected selectors, not production behavior. Typecheck caught invalid test-only `exact` option on getByRole; removed. CSS build caught `composes` on descendant selectors; changed to local topicTitle/railTitle classes. Immediate header help link initially duplicated existing accessible All Help label; final visible label is Get help in [Category], preserving predecessor journey and adding its own assertion. Some intermediate build attempts used the wrong working-directory prefix; failed commands retained distinctly. Final logs supersede these failures.

```sh
python3 -m unittest discover -s data-cleaning/tests -v
python3 data-cleaning/scripts/validate_content_sources.py
python3 data-cleaning/scripts/validate_homepage.py
python3 data-cleaning/scripts/validate_navigation.py
python3 data-cleaning/scripts/validate_news.py
python3 data-cleaning/scripts/validate_organizations.py
python3 data-cleaning/scripts/validate_schema.py
git diff --check
```

Baseline/final Python14 tests and six validators exit0. Exact commands/output/exits in baseline-python-{0..6}.txt, final-python-{0..6}.txt and python-gates.json.

Built app and packaged API (two separate localhost processes, no tracked static copy-back):

```sh
/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/java -jar backend/target/firststep-backend-0.1.0.jar --server.port=8108 --app.data.dir=app/data --news.rss.initial-delay=86400000
python3 docs/v2-completion-plan/evidence/im08/serve-built.py
node docs/v2-completion-plan/evidence/im08/browser-check.mjs
```

Evidence server4178 serves frontend/dist at /app-next and proxies /api and /images to packaged backend8108. It introduces no app routes/dependencies. Browser uses existing cached Playwright at ~/.npm/_npx/e41f203b7505f1fb/node_modules/playwright and Chromium151.0.7922.34. No installation or lockfile change. Sandbox loopback binding and Chromium Mach-port launch were refused; identical tools rerun with explicit sandbox approval. Raw failed logs plus approved logs retained. Browser script exits0 when every assertion passes, writes browser-results.json and screenshots, and always closes its browser. Browser runtime clock is the actual system date; packaged-category-api.json is distinct from fixed-clock real-category-api.json. Selection IDs remained equal in both snapshots. Provider navigation is recorded but remote website accuracy/reachability is not certified. Browser server processes stopped after evidence capture.

Browser checks: actual four Topic/twelve source-link order; keyboard Enter opens Be Ready CDC original source; Topic route→refresh→browser back; All Help→expand36; contextual FL-002 image loads; approved OR-003 reading; scoped Organization; switcher keyboard/focus;375/1280/1440 overflow and rail order; high contrast and Spanish; zero-help Eviction Prevention; bear-de Food; unknown Category alert; no app page errors. Screenshots housing-{375,1280,1440}.png, housing-375-high-contrast.png and housing-375-spanish.png. Browser-results-first.json retains the initial successful run before the immediate header action; latest browser-results.json/screenshots use final build. Not an exhaustive WCAG/mobile-device/provider audit.

preservation-before/after/result.json distinguish authorized source/mirror edits from protected data/selector/instruction files. annotated-mirrors.json covers23 complete source/test/style mirrors. Verification also checks untracked new-file whitespace and mirror byte equality; git diff --check alone would omit untracked additions.
