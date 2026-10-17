# IM-11 final reproduction and execution ledger

Untouched commands/results are in baseline-commands.md. All commands run from repository root. Reuse this installed offline Maven/agent prefix:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home /Users/composedmedia/tools/apache-maven-3.9.16/bin/mvn -o -f backend/pom.xml '-DargLine=-javaagent:/Users/composedmedia/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar' -Dim11.evidence.dir=../docs/v2-completion-plan/evidence/im11 clean verify
```

Final full command above passes602 tests/0 failures/errors/skips and builds executable jar (final-backend-accepted.txt). final-backend.txt earlier passed598 before the four additional explicit spring/fall selector calendar-window scenarios. Final frontend146/21 files, typecheck/build and Python14/six validators all pass (final-*.txt/json). No test disabled or skipped; no dependencies installed.

After the before fixed/runtime read-only audits, the isolated failing-before command was the same Maven prefix with `-Dtest=Im11InteractionRegressionTest#im11T12UnsafeSourceCannotSupplyPositiveVisualEvidence test`. demonstrated-source-gap.txt records1 test/1 failure/0 errors, specifically javascript:alert(1) supplying VISUAL despite every other gate passing. Preserve this failure as actual defect evidence, not environment failure. Correction replaces only that source-URL predicate with parseable absolute HTTP(S) plus host. Rank/matching/fallback remain byte-identical.

Focused-after command adds `-Dtest=Im11RealCatalogAuditTest,Im11RealCollisionRegressionTest,Im11InteractionRegressionTest,FeaturedItemSelectorTest,FeaturedItemAssignmentTest,FeaturedTopicOrderingTest -Dim11.evidence.dir=../docs/v2-completion-plan/evidence/im11 test`;44 pass. Final suite additionally includes the four DST rows. Actual golden fixture is backend/src/test/resources/im11/real-housing-collisions.json, extracted once from real-catalog-before.json; tests never rewrite it. Runtime-after capture uses `-Dtest=Im11RealCatalogAuditTest -Dim11.clock=runtime -Dim11.evidence.dir=../docs/v2-completion-plan/evidence/im11 -Dim11.audit.phase=runtime-after test`;3 pass. Runtime before had2 audit tests before adding fixture-to-live comparison. Fixed audit JSON before/after is deeply equal; runtime audit JSON equal except capture Instant. No real result changed.

```sh
python3 docs/v2-completion-plan/evidence/im11/summarize-audit.py
/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/java -jar backend/target/firststep-backend-0.1.0.jar --server.port=8111 --news.rss.initial-delay=86400000
python3 docs/v2-completion-plan/evidence/im11/serve-built.py
python3 docs/v2-completion-plan/evidence/im11/http-smoke.py
node docs/v2-completion-plan/evidence/im11/browser-check.mjs
```

Local frontend4181 proxies packaged API8111. Default sandbox denied frontend bind/loopback/Chromium launch; explicit escalation reruns succeeded. http-smoke-approved.txt and packaged-http.json pass15 read-only API requests; browser-check-approved.txt/browser-result.json pass actual12 IDs/order/roles/links,375/1280px, keyboard Topic navigation/refresh/back and requested Community continuity. No external provider URL reachability or complete RSS inventory claim. Both local servers stopped with Ctrl-C after capture.
