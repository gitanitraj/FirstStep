# IM-12 reproducibility / command ledger

Run from repository root. Baseline logs predate all production edits; starting-state.txt and preservation-before.json are authoritative for this session's starting state, including the preexisting user deletion and three untracked supplied docs.

Installed offline toolchain:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home /Users/composedmedia/tools/apache-maven-3.9.16/bin/mvn -o -f backend/pom.xml '-DargLine=-javaagent:/Users/composedmedia/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar' -Dim12.evidence.dir=../docs/v2-completion-plan/evidence/im12 -Dim12.phase=after clean verify
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
python3 docs/v2-completion-plan/evidence/im12/summarize-audit.py
python3 docs/v2-completion-plan/evidence/im12/packaged-check.py
java -Dloader.main=org.firststep.maintenance.EditorialExpiryReportCommand -cp backend/target/firststep-backend-0.1.0.jar org.springframework.boot.loader.launch.PropertiesLauncher --at=2026-10-17T16:00:00Z --app.data.dir=app/data
```

Use the installed JDK25 java for the last command. Maven toolchain and Mockito javaagent avoid the IDE/default-JDK and dynamic-attach mismatch. NODE_OPTIONS avoids the existing Node experimental-webstorage test issue. No dependencies installed. Browser harness imports the existing cached Playwright installation explicitly; adjust that local path on another workstation. The local built SPA uses4182 and proxies jar8112; no remote URL is visited. Script copies full catalogs into an automatically cleaned temporary directory and stops every process in finally blocks.

- Untouched: backend602, frontend146/21 files, Python14, six validators, typecheck/build all pass (baseline-*).
- Raw before: Maven prefix + `-Dtest=Im12CatalogAuditTest -Dim12.evidence.dir=../docs/v2-completion-plan/evidence/im12 -Dim12.phase=before test`:2 pass, audit-before.txt. Never rerun a before phase on changed source and claim it is baseline.
- Before regression: `-Dtest=Im12BoundaryRegressionTest,Im12OwnershipAndErrorTest test`:21 runs,20 failures,0 errors (demonstrated-gaps.txt).
- Focused first after: compile syntax error in replacement string (focused-after.txt), corrected;21 pass (focused-after-rerun.txt).
- Additional targeted before changes: `-Dtest=Im12AdditionalBoundaryTest test`:3/3 fail (additional-before.txt); Python new optional-ID tests2 runs/1 fail (additional-python-before.txt). These were captured after the first correction batch, before their respective citation/scope/confidential/contact/validator corrections. Original catalog remains untouched throughout.
- Focused complete26 pass (focused-complete.txt), before adding full scope and relationship cases.
- Organization before: `-Dtest=Im12RelationshipTest test`:2 runs/1 failure for unsafe organization website; ID/name/source/parent case passes (organization-navigation-before.txt).
- First full build final-backend.txt: source qualification typo prevented compilation; fixed import. Full final-backend-rerun.txt:631,0 failures,1 mock error after optional service overload. Controller now calls the original overload for omitted scope, preserving old collaborator contract. Accepted clean rerun **631/0/0/0** + executable jar (final-backend-accepted.txt).
- Final initial frontend146/21 files; after adding12 explicit public-navigation security/contract cases, final-frontend-navigation.txt passes158/22 files and final-typecheck-navigation.txt passes; build production inputs remain identical. Python16 and all six validators pass (final-*).
- Runtime default sandbox denied loopback bind: packaged-bind-denied.log / packaged-check-default.txt. Explicit escalation permitted local servers/Chromium. First browser assertion erroneously counted an organization heading and identical Resource title; browser-check-first-failed.txt retains failure. Scoped assertion to the actual main list items; rerun packaged-check-approved.txt, packaged-http.json and browser-result.json PASS. No product change was needed for that harness correction.
- CLI packaged command exit0, expiry-packaged.txt; no web server/feed fetch/scheduler starts. All262 rows considered,5 expired+1 approaching attention, no metadata errors.

`catalog-before.json == catalog-after.json` is asserted by summarize-audit.py. `endpoint-field-audit.json` records exact observed field paths and actual before leaks; `scoped-endpoints-after.json` asserts every observed owner across every Category/Topic/Help and all64 provider pages (1,160 requests). `packaged-http.json` is a separate runtime snapshot; no frozen clock injection into production jar.

Docker evidence is static inspection of backend/Dockerfile's COPY app/data/ → /data and APP_DATA_DIR=/data. Actual executable-jar paths are exercised with explicit absolute external data directories; no Docker image build, external-provider verification or remote feed completeness is claimed.
