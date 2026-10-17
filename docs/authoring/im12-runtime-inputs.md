# v2 runtime catalog inputs (IM-12 verification)

Deploy the catalog directory and select it with `--app.data.dir=/absolute/catalog` or `APP_DATA_DIR`. The executable jar alone does not embed `app/data/*.json`. Existing backend/Dockerfile copies **the entire app/data directory** into `/data` and sets APP_DATA_DIR=/data, including first-rate-news-membership.json. An external volume replacing `/data` must include that manifest alongside the other catalogs, taxonomy and source registry.

The current manifest has exactly three scoped editorial admissions: ExpertAnswer EA-001, ExpertAnswer EA-005 and independently FAQ FAQ-002, all wilmington-de/housing. OR-003 qualifies through existing Article approval and category rules. Admissions never override kind, Community, Category, lifecycle or Article review; session dates, verified, producer identity and FAQ derivation confer no admission.

Missing manifest: startup succeeds with no FAQ/Expert editorial admissions; approved Originals remain eligible. This deliberately fails closed, and changes the current Housing First Rate News collection from four records to OR-003 alone. Malformed JSON or an unsupported/invalid admission aborts startup, rather than silently disregarding the editorial decision. Deployers must verify the positive four-ID response, not merely a healthy process.

Reproduction: `python3 docs/v2-completion-plan/evidence/im12/packaged-check.py` uses isolated complete catalog copies for positive/missing/malformed cases and terminates its temporary servers. Requires an already-built jar, built frontend, installed local JDK25 and cached Playwright; paths are explicitly declared in the script. No catalog edits or remote feed fetches occur.

The existing read-only CLI can be invoked from the packaged jar:

```sh
java -Dloader.main=org.firststep.maintenance.EditorialExpiryReportCommand -cp backend/target/firststep-backend-0.1.0.jar org.springframework.boot.loader.launch.PropertiesLauncher --at=2026-10-17T16:00:00Z --app.data.dir=app/data
```

Exit 0 means complete valid diagnostic; 1 identifies metadata problems; 2 is invocation/setup failure. It retains all ownership partitions, reports approaching/expired metadata and does not schedule refreshes, edit files, assign tasks or send notifications. Diagnostic output is internal stewardship evidence, not a resident DTO. Community editorial expiry uses owning-Community calendar zones; event occurrence is assessed separately. Missing event dates such as NP-003 are UNKNOWN, never inferred from publication or prose.

This operational note supports IM-12. Full Admin Guide and historical-decision reconciliation remain IM-14; dashboards, queues, notifications and generalized relationships remain V3.
