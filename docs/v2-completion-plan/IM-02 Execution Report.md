# IM-02 execution report

**Package: IM-02 — Expiry Diagnostics & Admin-Facing Attention Signal.**
**Disposition: SATISFIED within the issued package boundary.**
**Execution date:** October 15, 2026 (America/New_York).

## Starting state and authority

Branch `copyofdev`, full starting HEAD
`f66b3ceea346e53bab786d4022032342eece4c0d`. The working tree was clean, with no uncommitted IM-01 material
to preserve. IM-01 was committed and pushed after its report was written; the
new work order's historical “no commit” statement did not describe current state.
No IM-01 files were reset or unrelated edits overwritten. No staging, commit,
push, dependency upgrade or catalog mutation was performed during IM-02.

Authority: [issued IM-02 work order](IM-02%20Coding%20Agent%20Work%20Order.md), approved
Completion Contract/P4/P5, authoritative reconciled IM-01–IM-14 matrix, Architecture
Resolutions, [PC-01–PC-04](IM-01%20Readiness%20Decisions%20PC-01%20to%20PC-04%20-%20Approved.md)
and [IM-01 execution evidence](IM-01%20Execution%20Report.md).
No subsequent IM package or v2 release declaration is included.

## Inspection, existing capability and bounded design

Before production changes, inspected `CivicContent`, the seven content variants,
`EditorialLifecycle`, `EditorialExpiryConfiguration`, raw Resource/News/Flyer/Faq/
ExpertAnswer/Article repository contracts and JSON loaders, RSS retained/current
getters, Article review guards, existing lifecycle/API/projection tests, catalog
validators and authoring/completion-plan documentation.

Already satisfied by IM-01: authoritative lifecycle assessment, configured Clock,
Community timezone/window, missing-expiry rules, independent status/approval and
current resident filtering. A fresh untouched baseline run passed **417 tests**.
Raw repositories retain expired/withdrawn/unapproved material. LAW is represented
by NewsItem in the relevance-admitted RSS cache; it has no separate persisted law
repository. Both existing RSS getters filter current inventory. No shared stored
expiry diagnostic, human report or accessible Admin expiry guide was found.

The bounded integration rationale was recorded here before production edits:
read raw repositories and retained relevance-admitted cache; reuse IM-01 assessment;
add only a shared calendar-distance helper; return immutable finding/error lists;
use a non-web, non-scheduled CLI with a frozen instant and thin text output. A new
reporting framework, Admin endpoint, live-feed fetch and second expiry algorithm
were rejected. The original Admin Guide is absent from the repository and its
supplied Library link was previously inaccessible; IM-02 §7 explicitly permits a
narrow supplement while IM-14 retains final reconciliation ownership.

## Exact production changes

| File | Purpose |
|---|---|
| `backend/src/main/java/org/firststep/backend/shared/lifecycle/EditorialExpiryDiagnostics.java` | One internal collector over raw six repositories plus stored RSS records. Preserves concrete class/ContentType, id/title, Community, source id, date/status and Article review/approval. Returns immutable attention findings and separate metadata problems. Exact optional Community scope; deterministic total ordering. |
| `backend/src/main/java/org/firststep/backend/shared/lifecycle/EditorialExpiryReport.java` | Thin human-readable renderer with lifecycle, attention, expiry eligibility, signed days, status/approval and reasons; clear empty sections, no normal exception traces, one-line source metadata. No resident DTO or endpoint. |
| `backend/src/main/java/org/firststep/maintenance/EditorialExpiryReportCommand.java` | Standalone packaged CLI outside resident component scanning. Imports only repositories/collaborators; no scheduling, web controllers or remote fetch. Supports `--at`, `--community`, data/config overrides, `--help`; freezes one Clock per run. Checks raw-file/loaded-record counts because loaders can log and suppress failures; refuses false complete reports after partial loads. |
| `backend/src/main/java/org/firststep/backend/shared/lifecycle/EditorialLifecycle.java` | Additive `daysUntilExpiry` with OptionalLong for missing dates, using the same configured private Community-local date resolver as `assess`. Existing Assessment/API and lifecycle rules remain intact; no competing state calculation. |
| `backend/src/main/java/org/firststep/backend/news/service/RssFeedService.java` | Add `getStoredRssItems`: immutable list copy of the retained, relevance-admitted cache without expiry/status filtering. No fetch, classification change or promotion of ungated bills. Existing resident getters unchanged. |

No stored metadata is edited and no derived state is persisted. The report's
status and Article approval fields are explicitly separate from expiry eligibility;
passing those checks is not a claim that classification/other projection gates pass.
Malformed nonblank expiry raises the same resident assessment error as IM-01; only
internal diagnostics isolate DateTimeParseException into a problem with no assigned
lifecycle state. Configuration or incomplete catalog-loading failures return a
setup error rather than a misleading empty success.

## Tests and acceptance mapping

New tests:

- `backend/src/test/java/org/firststep/backend/shared/lifecycle/EditorialExpiryDiagnosticsTest.java`
  — **19 cases** (including parameterized boundaries/DST).
- `backend/src/test/java/org/firststep/maintenance/EditorialExpiryReportCommandTest.java`
  — **6 cases** using real catalog loaders and temporary copies for invalid metadata.

All passed in the final full run. Existing IM-01 tests also ran again, rather than
being treated as inherited verification.

| Acceptance | Named evidence |
|---|---|
| T01 ACTIVE outside window | `shouldReportOnlyAttentionAtExactCommunityCalendarBoundaries` (31 days remaining produces no finding); `shouldHonorTheExistingConfiguredApproachingWindow` |
| T02 First window date | Same boundary test at exact New York midnight, 30 calendar days remaining |
| T03 Inclusive expiration date | Same boundary test at final local second, 0 days; original IM-01 boundary/projection tests rerun |
| T04 Following local date | Same boundary test at following midnight, EXPIRED / -1 day |
| T05 Missing/blank expiry | `shouldNotInventFindingsOrExpiryFromMissingBlankAndUnrelatedDates` |
| T06 Repeatability | `shouldProduceIdenticalFindingsAndOrderingForTheSameClockAndReorderedStoredRows`; `shouldReportRealStoredExpiredPoliciesAndApproachingItemRepeatably`; two identical packaged CLI runs |
| T07 Community zones/DST | `shouldUseOwningCommunityZonesAndExactScope`; six `shouldCalculateCalendarDistanceAcrossDaylightSaving` cases; original IM-01 configuration tests rerun |
| T08 Seven content variants | `shouldReadAllSevenContentClassesWithOriginalIdentityTypeAndProvenance`: Resource, NEWS NewsItem, Flyer, FAQ, ExpertAnswer, Article/NEWS and cached NewsItem/LAW |
| T09 Stored versus current | `shouldRetainStoredRecordsAndResidentExpiryBehaviorWithoutMutation`: expired retained in diagnostics/cache, absent from resident inventory |
| T10 Independent restrictions | `shouldKeepInactiveWithdrawnAndUnapprovedContextIndependentOfExpiry`: approaching draft still unapproved; inactive/withdrawn remain distinguished |
| T11 Invalid dates | `shouldIsolateMalformedExpiryWithoutAssigningAStateOrHidingValidRecords`; command controlled-date test; original assess still throws |
| T12 Stable ties | Reordered stored rows yield identical sorted finding and error lists/text, including same-date A/B identities |
| T13 Community scope | Owning-zone test, real Wilmington CLI, empty-scope CLI; other Community excluded before assessment |
| T14 Empty report | `shouldRenderEmptyResultsAndMissingMetadataIntelligiblyWithoutTraces`; `shouldRenderAnIntelligibleEmptyCommunityReport`; packaged empty-scope run |
| T15 No mutation | Serialized source rows and dates/status remain byte-identical around collection; command temp source unchanged; live app/data Git diff empty |
| T16 Real NP fixtures | `shouldReportRealStoredExpiredPoliciesAndApproachingItemRepeatably` identifies NP-001/NP-002/NP-006; packaged real report preserves original identities/source ids/dates |
| T17 Resident regression | Shared stored/current test plus all IM-01 lifecycle, seven-projection and real API tests in the 442-test full run |
| T18 Scope regression | No public controller, dashboard, CRUD, scheduler, queue, notification, content enum/domain hierarchy, persisted eligibility or event/ranking change; dependency/catalog diffs empty |

Additional command tests cover help, invalid/duplicate/unknown arguments and partial
repository-load rejection. They verify the intended diagnostic/exit path, not just
an arbitrary failure.

## Reproducible commands and actual results

Maven was executed through the retained cloud wrapper
`bash /workspace/.firststep-tools/im01-mvn <goals>`. It runs from `backend/`, using
verified Temurin 25+36, Maven 3.9.9, Java 17 compile target, the existing proxy and
system Java CA store. Expanded Maven arguments are:

```sh
/workspace/.firststep-tools/apache-maven-3.9.9/bin/mvn -B -ntp \
  -s /workspace/.firststep-tools/settings.xml \
  -t /workspace/.firststep-tools/toolchains25.xml \
  -Dmaven.repo.local=/workspace/.firststep-tools/m2 verify
```

The wrapper activates `/workspace/.firststep-tools/jdk-25+36` and sets
`MAVEN_OPTS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts` in addition to
existing options. No manifest/lockfile or verification setting was weakened.
Outside this cloud machine, use the repository's configured JDK 25 toolchain and
standard `mvn verify` from `backend/`.

| Command | Actual result |
|---|---|
| Wrapper `test` before implementation | 417 passed; 0 failures/errors/skipped |
| Wrapper `-Dtest=EditorialExpiryDiagnosticsTest,EditorialExpiryReportCommandTest test` | 25 passed; 0 failures/errors/skipped |
| Wrapper `verify` after implementation | **442 passed; 0 failures/errors/skipped**; executable JAR packaged successfully |
| `npm test` in frontend | **19 files / 126 tests passed** |
| `npx tsc --noEmit` in frontend | Exit 0 |
| `npm run build` in frontend | Exit 0; Vite production build |
| Python catalog validators from repository root | All six exit 0: content_sources, homepage, navigation, news, organizations, schema |

Each validator command was `python3 data-cleaning/scripts/validate_<name>.py`.
No new failure was hidden or accepted. An intermediate package build and focused
19-case run also passed; they are not represented as additional distinct coverage.

The actual packaged command below was run twice from the repository root with
byte-identical stdout and exit 0 (JDK 25 binary used in this environment):

```sh
java -Dloader.main=org.firststep.maintenance.EditorialExpiryReportCommand \
  -cp backend/target/firststep-backend-0.1.0.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher \
  --at=2026-10-15T16:00:00Z --community=wilmington-de
```

A separate temporary catalog copy with one controlled invalid date produced exit 1
and preserved the valid findings. A separately run empty Community scope produced
exit 0 and intelligible empty output. These actual runs are distinct from unit tests.
No live catalog record was changed to obtain an example.

## Real and controlled diagnostic evidence

[Real Wilmington output](evidence/im02/real-wilmington-report.txt) inspects **86**
stored records in that Community, with **6** attention findings: five expired
(NP-001/002/006/009/010) and one approaching (NP-003). No malformed dates were found
in the live catalog. The report includes ids, titles, NewsItem/NEWS identity,
Community, original source ids, dates, status, eligibility, attention, signed days
and human reasons. At the fixed instant, NP-003 has 16 calendar days remaining.
NP-001/002/006 retain expiry dates 2026-06-30, 2026-07-14 and 2026-08-07.

[Controlled invalid-date sample](evidence/im02/controlled-invalid-date-report.txt)
is explicitly labeled synthetic and uses a temporary copy of the same catalog.
It demonstrates a separate error section, original invalid date/status, no fabricated
state and continued reporting of valid approaching/expired items. It is not a claim
that the runtime catalog contains that error.

A fresh offline CLI has **0** warmed RSS records. Both data and rendered output
state retained-cache coverage; no claim of complete remote-law coverage is made.
The all-variant test explicitly seeds the retained relevance-admitted LAW cache to
prove it is included even when expired. In a running application the same internal
collector can inspect its existing cache, but IM-02 adds no network-fetch command
or public Admin endpoint.

## Admin guidance, boundaries and downstream impact

[Admin expiry supplement](../authoring/editorial-expiry-supplement.md) documents
ACTIVE, APPROACHING_EXPIRY, EXPIRED, local inclusive boundaries, configuration,
commands/exit codes, no-expiry rules, status/approval context, invalid/empty results,
cache limitations and authorized human follow-up. It explicitly rejects automatic
ranking boosts, task/interview assignment, notifications and article generation.
The original guide remains unavailable; this is the authorized narrow supplement,
not a replacement for unrelated Admin guidance. IM-14 owns final reconciliation.

IM-01 remains valid: its 417 tests are part of the final 442-test pass; resident
filtering/approval/error decisions are unchanged. The only shared extension is
calendar distance and a read-only retained-cache getter. No schema, classification,
Community ownership, source attribution, catalog or dependency change occurred.
No new P5 design, V3 operations, database migration, event occurrence (IM-10),
featured ranking (IM-06/07), IM-03 or IM-04 work was begun.

Full annotated mirrors for the five touched production files and two new test
files are in `references/` and `references/tests/`, following the repository's
scoped documentation convention. The supplied work order is retained verbatim.
Evidence excerpts under `evidence/im02/` keep baseline, focused, final, runtime and
controlled outputs separate.

**Final disposition: SATISFIED.** All applicable IM-02 acceptance gates have executed
passing evidence. Live external-law coverage, original-guide reconciliation and
later-package functionality are explicitly limited as above, not silently claimed.
