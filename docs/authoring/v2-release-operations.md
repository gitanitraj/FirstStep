# v2 release operations — IM-14 reconciled runbook

October 18, 2026. Use with the [Admin/editor handbook](v2-admin-editor-handbook.md) and [current architecture reconciliation](../architecture/v2-implemented-reconciliation.md). Commands are from the repository root unless a subshell explicitly changes directory. They describe operator actions; this document does not authorize remote publication, catalog changes, Git commits or destructive recovery.

## Build and environment

Inspected sources: `backend/pom.xml`, `backend/Dockerfile`, `docker-compose.yml`, `backend/src/main/resources/application.properties`, `frontend/package.json`, `frontend/vite.config.ts`, current validators and IM-13 `closure/commands.json`.

Backend compiles Java 17 bytecode using the required **JDK 25 Maven toolchain**. Configure your own `~/.m2/toolchains.xml` as documented in the repository README, with the real JDK25 home. `mvn` must be installed/on PATH; IM-13's exact machine executable was `/Users/composedmedia/tools/apache-maven-3.9.16/bin/mvn`, recorded as evidence, not a portable install location. No build wrapper is included. Frontend is React18/TypeScript/Vite; Docker builds with Node24. The accepted local Node26/jsdom test invocation disables experimental web storage so jsdom supplies storage. This is a test environment setting, not a production shim.

```sh
(cd backend && mvn -B verify)
(cd frontend && NODE_OPTIONS=--no-experimental-webstorage npm test)
(cd frontend && ./node_modules/.bin/tsc --noEmit)
(cd frontend && npm run build)
python3 -m unittest discover -s data-cleaning/tests -v
```

Use `npm ci` only when dependencies need installing, retaining the lockfile. Do not assume a local Maven package includes the separately built frontend: `frontend/dist` is a separate output. The existing Dockerfile explicitly copies that Vite output into `src/main/resources/static/app-next` **before** Maven packaging, then bakes the executable jar plus full catalogs/media into its runtime image. That combined distribution is what passed IM-13 browser verification. The root legacy static demo is retained; the current SPA is `/app-next/`.

## Combined packaged startup and reload

```sh
docker compose up --build -d app
docker compose logs app
curl --fail --silent http://127.0.0.1:8080/api/categories
curl --fail --silent http://127.0.0.1:8080/app-next/
```

`app` has no `depends_on` Ollama requirement. The optional old `ollama` service remains in Compose; its presence does not wire a Spring AI provider. No model-provider starter is currently configured. `/api/decide` returns the sanitized unavailable-provider result; do not promise a successful remote AI response. Startup alone is not an editorial completeness check.

For an API-only local jar process, use the repository root as working directory and inspect startup logs:

```sh
java -jar backend/target/firststep-backend-0.1.0.jar \
  --app.data.dir=app/data \
  --app.seasonal.images.dir=backend/src/main/resources/static/images/seasonal
```

Vite's `server.proxy` forwards `/api` to local backend 8080 in development; production is same-origin, not a development-proxy assumption. Built SPA base and Router basename are `/app-next/`; production JS/CSS/fonts must load below `/app-next/assets/`. `SpaWebConfig` handles deep-route refresh. Preserve original seasonal media and server-encoded `/images/seasonal/...` URLs. `APP_SEASONAL_IMAGES_DIR` selects the media directory; Docker uses `/seasonal`.

JSON repositories load at process startup. For baked data, an authorized change requires rebuild/recreate (`docker compose up --build -d app`). For external mounted data, deploy a complete validated snapshot and recreate/restart the process; there is no hot reload guarantee. Readiness logs plus scoped payload/asset checks must confirm the new snapshot rather than assuming restart implies correct data.

## External catalog configuration

`app.data.dir` / `APP_DATA_DIR` defaults to `app/data` relative to working directory. Docker sets `APP_DATA_DIR=/data` and copies **all** `app/data/`, including `resources.communities.json`, taxonomy, navigation, source mappings, producer registry, homepage configuration and the editorial manifest. A volume replacing `/data` must supply the complete directory; it hides the image's baked files. Ordinary resource loaders can tolerate missing files, so a healthy process does not prove the full catalog was loaded.

For an isolated read-only local catalog mount (using the existing build target):

```sh
docker build -f backend/Dockerfile -t firststep-v2-candidate .
docker run --rm --publish 127.0.0.1:8080:8080 \
  --mount "type=bind,src=$PWD/app/data,dst=/data,readonly" \
  firststep-v2-candidate
```

Use a nonconflicting port if another instance runs. The mount command is an operational recipe grounded in the Dockerfile; IM-13 actually tested baked external `/data` plus separate explicit absolute `app.data.dir` catalog copies, not this exact operator invocation. Do not infer TLS, backups or production-host configuration from a local test fixture.

| Configuration | Meaning |
|---|---|
| `server.port` | Default 8080 |
| `app.default-community-id` | Loader/feed fallback, default Wilmington; not a resident-eligibility decision |
| `app.data.dir` / `APP_DATA_DIR` | External complete catalog directory |
| `app.seasonal.images.dir` / `APP_SEASONAL_IMAGES_DIR` | Preserved original Flyer media |
| `app.editorial-expiry.approaching-days` | Default 30 calendar days |
| `app.editorial-expiry.default-zone` | Default America/New_York; explicit deployment fallback |
| `app.editorial-expiry.community-zones.<id>` | Owning-Community zone override; Wilmington explicitly configured |
| `news.rss.initial-delay` / `news.rss.refresh-interval` | Milliseconds; defaults 500/3600000. Offline acceptance used 86400000 initial delay, not a claim that feeds were exercised |
| `ai.enabled` / `AI_ENABLED` | Existing feature flag; does not install/configure a provider |

## Manifest deployment

The current `app/data/first-rate-news-membership.json` has exactly three admissions: ExpertAnswer EA-001, ExpertAnswer EA-005 and independently FAQ FAQ-002, all `wilmington-de`/`housing`. Article OR-003 enters separately through approval/category rules. Record any authorized manifest decision separately from generation/session dates; copying a record or changing producer does not authorize admission.

| Input state | Actual packaged behavior | Operator consequence |
|---|---|---|
| Valid present manifest | Four Housing First Rate News IDs: OR-003, FAQ-002, EA-001, EA-005 | Verify all four if this is the intended distribution |
| Missing manifest | **Application starts**; no FAQ/Expert admissions, only OR-003 | Operational completeness failure for the intended four-item experience; restore the authorized manifest through deployment procedure |
| Malformed/invalid admission manifest | Startup fails before resident readiness | Inspect startup failure and authorized candidate; do not bypass the loader or broaden admissions |

After startup fetch `/api/category/housing?communityId=wilmington-de`, inspect its `destination` First Rate News collection and compare the ID set, not just a200 response. `CategoryDestinationService` owns the controlled projection. The three-state proof is retained in `docs/v2-completion-plan/evidence/im13/closure/packaged-http.json`; malformed startup evidence is separately logged. There is no standalone Python manifest validator: backend `FirstRateNewsMembership` validates it and its eligibility references at runtime, with unit/packaged tests. Do not describe missing manifest as startup-fatal.

## Validation coverage

These are the **six** existing entry points; each must exit zero for its intended coverage:

```sh
python3 data-cleaning/scripts/validate_content_sources.py
python3 data-cleaning/scripts/validate_homepage.py
python3 data-cleaning/scripts/validate_navigation.py
python3 data-cleaning/scripts/validate_news.py
python3 data-cleaning/scripts/validate_organizations.py
python3 data-cleaning/scripts/validate_schema.py
```

| Gate | Actual coverage and limits |
|---|---|
| content_sources | Registry/sectors, producer references in News/Flyer/Expert/FAQ, governed notice kinds; not proof of substantive claim support or a registered directory producer sector |
| homepage | Authored homepage pathway/config references; not all runtime catalogs |
| navigation | Authored grouped taxonomy/navigation and both Resource inputs/Flyers; not complete schema validation of every supplementary Resource field |
| news | Curated News shape/vocabulary/date/source constraints; not actual remote feed truth |
| organizations | Registry uniqueness/shape, relevant references including supplementary explicit provider IDs, display-only attribution warnings; not inferred producer/provider equality |
| schema | Curated `resources.json` by default, explicit Expert/FAQ classification and Flyer helpAccess; does not assert all 171 supplementary Resources meet the curated shape |

For a candidate supplementary Resource file, the **existing** targeted command is:

```sh
python3 data-cleaning/scripts/validate_schema.py --input app/data/resources.communities.json
```

This targeted invocation was executed in IM-14: **171 passed, 0 failed, exit 0**, with the summary reporting warnings for171 records. The default six-gate invocation does not itself run the Resource schema over this supplementary file; this explicit option closes that command-coverage question without claiming every optional fact is complete or substantively verified. Preserve and review warnings instead of globally filling unknowns. All 171 supplementary records already carry stored Topics; missing provider IDs/owners and optional cost/other fields must not be fabricated. Default warnings are not failures; `--strict` is a different policy, not the accepted baseline. New authorized records should be complete and validated in isolation or as a reviewed candidate.

There is no general standalone Article/editorial correctness validator and no universal Community/service-area/semantic-duplicate validator. Article approval/DTO/404 rules are exercised by backend tests; manifest loading validates its own structure/refs; source/organization/classification tests and IM-12/13 audits cover relevant gaps. Validation does not create truthful provider IDs, verify URLs substantively or establish semantic nonduplication. Default warnings and `--strict` behavior differ; do not silently make a warning-failing strict invocation the claimed accepted baseline.

## Expiry diagnostics

Build the jar with the configured JDK25 toolchain, then invoke the existing non-web maintenance entry point:

```sh
java -Dloader.main=org.firststep.maintenance.EditorialExpiryReportCommand \
  -cp backend/target/firststep-backend-0.1.0.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher \
  --community=wilmington-de --at=2026-10-17T16:00:00Z --app.data.dir=app/data
```

Omit `--at` for a frozen live instant per run; use an offset/UTC ISO instant for reproduction. Omit `--community` for all stored owners; use exact IDs. `--help` gives usage. The 17 October example is an historical fixed clock, not a claim of today's source freshness. NP-003 was approaching and five expired News were retained at that clock. Community-local inclusive date and independent occurrence rules are in the handbook. The CLI does not run a web server/scheduler, fetch feeds, mutate records or warm the RSS cache. It states its cache limitation and returns0/1/2 as documented. Diagnostic output is internal and may contain review context.

## Release checks and evidence

Before any authorized release, compare candidate source/config/catalog hashes, retain exact commands/exits, and verify required scope, counts, sanitized errors, public field shapes, original media/fonts, and direct/deep navigation. Required compiled/runtime behavior is the finite IM-13 scope, not every speculative pipeline feature. At the accepted fixed catalog, Housing Help is36, four featured topics have honest DURABLE/FALLBACK positions, and0/8 Flyers have Help approval. Event counts are time-sensitive; FL-008 is concluded after New York midnight October 18, without editorial expiry or media deletion.

Final accepted verification is `docs/v2-completion-plan/evidence/im13/closure/`:402 unchanged inputs,631 backend tests,162 frontend tests,16 Python tests, six validators,22 zero-exit commands,65 corrected public HTTP requests, four SPA/media paths, combined Docker assets and real browser/editor journeys. The original interrupted history and failed intermediate integrity supplement remain historical, superseded only for their two corrected evidence gaps. No full release suite is rerun by IM-14.

The retained browser scripts use an explicitly machine-local cached Playwright import; the packaged scripts name a machine-local JDK25 path. Inspect those paths before reuse elsewhere; they are reproducible evidence for that machine, not a portable installer. `closure/run-verification.py` records its exact executable paths and outputs. Browser checks use local synthetic fault injection and synthetic Law layout where live Law cache was empty. Comprehensive WCAG certification, physical devices, screen readers, exhaustive external source/provider/RSS correctness and successful remote AI-provider responses remain **NOT TESTED**.

## Recovery and retention

Repeat this isolated recovery validation periodically under the deployment owner’s backup cadence and after relevant catalog/configuration changes; v2 does not schedule it automatically.

1. Identify the intended source revision/catalog snapshot and authorized actor/change rationale. Keep original records/media, internal editorial evidence, logs and deployment backups as separate retention classes. Git is practical source history, not a complete backup/audit service.
2. Restore into an **isolated candidate directory** first. Copy the entire catalog/config set, including manifest and supplementary Resources, plus the associated media version. Do not partially restore one relationship file into an incompatible registry snapshot.
3. Compare hashes/IDs, run applicable six validators with paths/working directory directed to the candidate, document supplementary coverage limits, and check Article approval/manifest/lifecycle/Community relationships. Renewed visibility is a new decision; a historical valid snapshot cannot bypass current gates.
4. Verify scoped packaged output using the intended clock/configuration before an authorized distribution swap. Retained expired/unpublished/withdrawn content does not automatically regain public currentness. Do not use `git reset`, blanket deletion or a direct production overwrite as a recovery recipe.
5. Record results, source revision and any unresolved metadata. Permanent disposal requires a separate authorized retention decision; automatic RSS collection need not create permanent catalog records. V3 owns structured change-history APIs, Admin reports, automated assignment and notifications.

IM-14's narrow recovery smoke copies the complete current catalog into a temporary directory, compares hashes, and reruns the six validators against that copy. It demonstrates candidate reconstruction/validation only, not production backup restore, disaster recovery or production security certification. Host TLS/CORS/access restrictions, secrets, backups, resource limits and operational incident response must be checked by the deployment owner for the actual environment; local acceptance does not certify a remote deployment that was not inspected.
