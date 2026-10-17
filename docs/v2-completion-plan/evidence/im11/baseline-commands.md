# Untouched baseline commands and outcomes

Repository root, branch copyofdev, HEAD140bfe53161f87729545a785d597c90d87e63737. Only the two supplied authority files untracked. Baseline precedes new tests and all production edits. No dependency installations.

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home /Users/composedmedia/tools/apache-maven-3.9.16/bin/mvn -o -f backend/pom.xml '-DargLine=-javaagent:/Users/composedmedia/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar' clean verify
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

All exit0: backend587 tests,0 failures/errors/skips, executable package built; frontend146 tests/21files; typecheck/build success; Python14; six validators. Full baseline-*.txt and baseline-python.json retain outputs. JDK25 toolchain compiles Java17; explicit existing Mockito agent and Node webstorage workaround are environment setup, not product failures. Maven runs offline with installed dependencies; no network access required.

Before existing-code changes: same Maven prefix plus `-Dtest=Im11RealCatalogAuditTest -Dim11.evidence.dir=../docs/v2-completion-plan/evidence/im11 -Dim11.audit.phase=before test`, then `-Dim11.clock=runtime -Dim11.audit.phase=runtime-before` for a separate runtime snapshot. Both baseline audits passed2 tests, captured all actual selector evidence and never wrote catalogs. The runtime Clock is frozen at its actual capture Instant for a coherent snapshot.
