# Editorial expiry — IM-02 Admin supplement

This is narrowly scoped operating guidance for the IM-02 expiry report. The
existing *First Step Admin Guide — Expiry Eligibility and Editorial Stewardship*
is not present in the repository and its supplied Library link was inaccessible.
This supplement does not replace that guide or define unrelated Admin procedures.
IM-14 owns final reconciliation. The approved PC-01–PC-04 decisions and P4/P5 remain
controlling.

## Run the report

Use the repository's JDK 25 Maven toolchain. From `backend/`, run `mvn verify`,
then return to the repository root. Run the packaged maintenance command:

```sh
java -Dloader.main=org.firststep.maintenance.EditorialExpiryReportCommand \
  -cp backend/target/firststep-backend-0.1.0.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher \
  --community=wilmington-de --at=2026-10-15T16:00:00Z
```

Omit `--at` to freeze the current instant for that run. Supply an ISO-8601 instant
with an offset/UTC marker for reproducible evidence. Omit `--community` to explicitly
inspect all stored Communities; every finding still identifies its owning Community.
Community ids are matched exactly. `--help` prints usage without loading the catalog.
The command starts no web server or scheduler, fetches no remote feeds, and exits
without creating a dashboard, workflow, task, notification or catalog edit.

`app.data.dir` defaults to `app/data` relative to the working directory; override it
with `--app.data.dir=/path/to/catalog`. The report checks that repository loading
covers the supplied catalog files before claiming a complete report. It requires
resources, news, flyers, FAQ, expert-answer and article JSON inputs, even when a
class has an empty array. The extra resources.communities.json is included when
present. Taxonomy/source configuration is loaded by the existing repository machinery.

The packaged app's ordinary main class and resident endpoints are unchanged.
Reports are internal stewardship output and may show article review context; do
not turn them into a public resident payload.

## Interpret editorial lifecycle

| State | Meaning | Human action |
|---|---|---|
| ACTIVE | Outside the approaching window, or no editorial expiration date. No expiry-attention finding is produced. | Continue normal stewardship. This is not proof of fresh verification or publication eligibility. |
| APPROACHING_EXPIRY | Within the configured calendar-day window, including the exact expiration date. Remains editorial-expiry eligible. | Verify accuracy and continuing usefulness. Contact the producer/provider if appropriate; check service availability or a deadline and determine whether editorial follow-up is warranted. |
| EXPIRED | The day after the inclusive expiration date has begun in the owning Community's zone. Excluded from applicable current resident inventory, retained in the catalog. | Investigate and decide whether to verify/update/renew through the authorized catalog-editing process or retain as history. Do not delete it merely to clear this report. |

The shared default window is **30 calendar days**. Wilmington uses
**America/New_York**, including daylight saving. An item expiring November 14 is
approaching from October 15 through November 14 and expires at the start of
November 15 locally. Signed days-to-expiry is 30 at that window's start, zero on
November 14 and -1 on November 15. These are calendar dates, not 24-hour intervals.

Configuration is shared with IM-01:

- `app.editorial-expiry.approaching-days`
- `app.editorial-expiry.default-zone`
- `app.editorial-expiry.community-zones.<community-id>`

Unlisted Community ids inherit the configured deployment zone, not the machine's
implicit timezone. A report invocation may use `--app.editorial-expiry.<setting>=value`
for an explicitly controlled check. Record overrides with any evidence; changing
report parameters does not edit catalog data or change a separately running app.

## Status, approval and provenance are separate

The report includes stored inactive/withdrawn and unapproved/draft records when
their editorial dates require attention. Their lifecycle state does **not** make
them publicly available. Read `Stored status`, `Status allows publication` and,
for Articles, `Article review` / `Approved` separately. The status Boolean only
answers IM-01's inactive/withdrawn restriction; classification, Community selection,
article approval and other projection gates still apply independently.

Missing metadata is shown as `(not recorded)`, not fabricated. Missing/blank expiry
on RESOURCE, NEWS/Policy Info, FLYER, LAW, FAQ, Expert Answer and First Step Article
does not itself cause expiry exclusion or attention. Approval still controls articles.
No publication, retrieved, audit, session or event date is converted into expiry.

Approaching expiry does not automatically increase featured ranking. Editorial
expiry is not event-occurrence eligibility: a past event can remain editorially
ACTIVE, while IM-10 will separately govern whether it is a current event. No event
occurrence rule, event duration or automatic renewal is implemented by this report.

## Invalid metadata, scope and empty results

`METADATA PROBLEMS` is an error section, **not** a fourth lifecycle state. A malformed
nonblank date is reported with its original identity/date and an explanation; no
ACTIVE state or replacement date is fabricated. Other valid rows are still reported.
Investigate the source record under existing editing/review procedures. Resident
assessment keeps IM-01's existing error behavior; reporting does not weaken it.

- Exit **0**: complete report, no invalid-expiry findings. Approaching/expired
  attention findings alone are normal and do not make the command fail.
- Exit **1**: report completed with isolated invalid-expiry metadata findings.
- Exit **2**: invocation/configuration/catalog-load problem; completeness is not claimed.

An empty scope prints a clear “No approaching-expiry or expired records” message.
Rows are ordered by Community, raw expiration date, concrete class, ContentType,
identity/title/source and stored status/review context. Fixed clock, configuration
and unchanged inputs produce the same findings and text.

The report reads raw repositories, never filtered resident service inventories.
LAW findings can come from the retained relevance-admitted RSS cache through the
internal diagnostic collector. A new offline CLI process has no warmed cache:
its output explicitly states how many cached records were included and makes no
claim of complete remote-law coverage. It does not fetch or promote ungated bills
to normalized CivicContent merely to fill the report.

A finding requests **human attention**, not an automated workflow state. No article
generation, interview assignment, ownership routing or notification occurs. Use the
existing approved catalog procedures; the report itself is read-only.
