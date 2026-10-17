# IM-10 actual commands and evidence ledger

All commands run from repository root; offline installed Maven, repository JDK25 toolchain/Java17 release, existing Mockito agent and Node webstorage workaround. No dependency install or lockfile edit.

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

Baseline backend558/package, frontend144, Python14/six validators/type/build pass (baseline-*.txt/json). Before production edits, the same Maven prefix plus `-Dtest=EventOccurrenceCatalogTest -Dim10.evidence.dir=../docs/v2-completion-plan/evidence/im10 -Dim10.audit.phase=before test` intentionally fails the new real next-midnight assertion,1 test/1 failure/0 errors. catalog-before.json captures Events5/Flyers6. No production catalog timestamp was changed.

Focused backend selection: same Maven prefix with `-Dtest=EventOccurrenceTest,EventProjectionTest,EventOccurrenceCatalogTest,CommunityNoticesServiceTest,CommunityNoticesControllerTest,FlyerControllerTest -Dim10.evidence.dir=../docs/v2-completion-plan/evidence/im10 clean test`. focused-backend.txt records46 tests/1 failure: privacy test incorrectly banned geographic location.state. Corrected to forbid internal diagnostic/status fields; location.state remains public. Existing positive source/kind fixture dates are synthetic future occurrences under the injected Clock; explicit test News startsAt is not runtime publication inference.

Full final command adds `-Dim10.evidence.dir=../docs/v2-completion-plan/evidence/im10 clean verify`. final-backend.txt passed586; final-backend-accepted.txt passed587 after the additional unsafe-source case and raw-news diagnostic audit. Both clean builds passed; no tests skipped. Final frontend146 tests/21files, typecheck/build pass. final-python.json records every Python command/exit. The evidence property writes only audit JSON, never application state. Repeating the focused catalog test emits all raw Flyer assessments and event-tagged raw News diagnostics at2026-10-18T04:00Z.

```sh
/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/java -jar backend/target/firststep-backend-0.1.0.jar --server.port=8110 --news.rss.initial-delay=86400000
python3 docs/v2-completion-plan/evidence/im10/serve-built.py
python3 docs/v2-completion-plan/evidence/im10/http-smoke.py
node docs/v2-completion-plan/evidence/im10/browser-check.mjs
```

Local frontend server4180 proxies packaged API8110. Binding, loopback and Chromium launch needed explicit sandbox escalation; denied default attempts remain in logs. The first approved HTTP smoke recursively banned ArticleDetail.updatedDate, which is an existing explicitly public editorial-revision fact. The script now permits that field only on Article endpoints, retaining strict internal audit omission on Flyers/Notices/News. http-smoke-approved.txt preserves this test-scaffold failure; http-smoke-final.txt passes11 API requests plus original JPEG. browser-check-approved.txt and browser-result.json pass. Chromium verifies375/1280px, keyboard view links, requested Community continuity, original loaded image, Spanish/high contrast and no page errors. Runtime real Events contains FL-008 on October17; fixed-clock tests separately prove its next-midnight conclusion. Both local servers stopped after capture. No remote-feed/source-content or exhaustive accessibility release audit claimed.
