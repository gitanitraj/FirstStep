# IM-07 reproduction and actual command ledger

Run from repository root. JDK25 is installed at /Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home; Maven at /Users/composedmedia/tools/apache-maven-3.9.16/bin/mvn. Existing ~/.m2/toolchains.xml selects JDK25; compilation remains release17. These are environment paths, not new dependencies or committed toolchain settings.

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home /Users/composedmedia/tools/apache-maven-3.9.16/bin/mvn -o -f backend/pom.xml '-DargLine=-javaagent:/Users/composedmedia/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar' -Dim07.evidence.dir=../docs/v2-completion-plan/evidence/im07 verify
```

Actual final command above: exit0,529 tests/0 failures/0 errors/0 skips, executable jar packaged. Log final-backend.txt. Evidence paths resolve from Maven's backend working directory.

Focused command uses the same executable/agent/options, replaces verify with test and adds:

```text
-Dtest=FeaturedItemSelectorTest,FeaturedItemAssignmentTest,FeaturedItemCatalogTest,FeaturedTopicCatalogTest
```

focused-accepted.txt:28 passed (27 new IM-07 +1 retained IM-06 catalog regression), exit0. final-backend.txt subsequently reruns the complete IM-01–06 suite with final source.

Original baseline command used `-f backend/pom.xml verify`, initially without agent (dynamic attachment stalled; interrupted exit130). Baseline with explicit agent and `-Dim06.evidence.dir=../docs/v2-completion-plan/evidence/im07/baseline`:502 tests passed, packaging exit1 because the local plugin cache lacked its POM/dependencies. Online retry failed before execution because ~/.m2 was outside sandbox write permissions. Explicitly approved Maven jar-plugin:help downloaded the missing dependencies, exit0 (maven-cache-repair.txt). No repository dependency or lockfile changed.

To complete original-code baseline verify after cache repair, an unmodified `git archive HEAD` was extracted with Python tarfile into /tmp/im07-baseline. The final command executable/agent above was run with `-f /tmp/im07-baseline/backend/pom.xml verify` (no IM07 property): exit0,502 tests, full executable packaging. baseline-backend-repaired.txt. This is the same HEAD, catalogs and tests, independent of new untracked IM-07 files. baseline/real-selector-audit.json was already produced from original source before production edits.

From frontend/:

```sh
NODE_OPTIONS=--no-experimental-webstorage npm test
npx tsc --noEmit
npm run build
```

Final exits0 each;133 tests across20 files. final-frontend.txt, final-typecheck.txt, final-build.txt. Original plain npm test baseline on Node26.7.0:6 failed/127 passed, caused by global localStorage being undefined under this Node webstorage mode and cascading teardown failures. Baseline rerun with the flag:133 passed (baseline-frontend-node.txt). No frontend edits or assertion changes. Baseline typecheck/build also exit0. Environment flag restores jsdom-owned storage.

From root:

```sh
python3 -m unittest discover -s data-cleaning/tests -v
python3 data-cleaning/scripts/validate_content_sources.py
python3 data-cleaning/scripts/validate_homepage.py
python3 data-cleaning/scripts/validate_navigation.py
python3 data-cleaning/scripts/validate_news.py
python3 data-cleaning/scripts/validate_organizations.py
python3 data-cleaning/scripts/validate_schema.py
python3 docs/v2-completion-plan/evidence/im07/audit-stored.py
git diff --check
```

All exit0. Python14 tests. Baseline and final Python commands/output/exit codes in baseline-python-{0..6}.txt, final-python-{0..6}.txt and python-gate-results.json. Stored audit is read-only. Final catalog test generates real-item-audit.json using the actual Spring wiring/repositories at 2026-10-16T16:00Z. No remote RSS refresh, no external URL reachability assertions. Static Flyer media presence is recorded separately from semantic admission.

Non-gate development failures retained: focused-first.txt compilation encountered the temporarily unfinished new test file after a shell append used the wrong working directory; corrected by completing the file. focused-second.txt:20 tests pass. focused-final.txt:27 pass/1 fail because an initial catalog assertion incorrectly expected no newness/eligible context for every Topic; inspection established approved OR-003 in Eviction Prevention. Corrected test explicitly pins OR-003 as nonactionable FALLBACK and keeps zero-help assertion. Production behavior was already correct; no catalog or eligibility rule changed to satisfy the test.

Protection: preservation-before.json and preservation-after.json cover every824 preexisting tracked files, not just catalogs. preservation-result.json:zero changes; Flyer SHA256 identical. annotated-mirrors.json provides four new source/mirror mappings; each mirror contains the complete source after its explanatory header. No staging, commit, push, reset or stash.
