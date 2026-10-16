# First Step v2 — IM-02 Coding-Agent Work Order

## Expiry Diagnostics & Admin-Facing Attention Signal

**Status:** AUTHORIZED FOR IMPLEMENTATION
**Execution package:** IM-02
**Execution class:** Implementation Required
**Dependency:** IM-01 — Satisfied within its authorized scope
**Date issued:** October 15, 2026
**Governing authority:** First Step v2 Completion Contract, authoritative IM-01–IM-14 Execution Matrix, approved 2B-P4 and 2B-P5, IM-01 Readiness Decisions PC-01–PC-04, approved Architecture Resolutions, and the IM-01 execution report.

---

## 1. Mission

Implement the minimum diagnostic and reporting capability necessary for First Step editors and administrators to identify CivicContent records that:

1. Are approaching editorial expiry and need attention.
2. Have passed their editorial expiry date.
3. Require investigation because lifecycle metadata cannot be interpreted safely.

Deliver a repeatable, deterministic, Admin-facing report suitable for human catalog stewardship.

**IM-02 must consume the existing IM-01 lifecycle implementation. It must not establish a competing expiry calculation or create a full Admin operations platform.**

The central acceptance principle is:

> Approaching-expiry content remains available to residents while becoming visible to editors for attention. Expired records remain in the catalog and are identifiable through stewardship reporting even though they are excluded from applicable current resident-facing inventories.

---

## 2. Proven Starting State: IM-01

The October 15, 2026 IM-01 execution report establishes the following implementation baseline.

### 2.1 Existing lifecycle implementation

**`shared/lifecycle/EditorialLifecycle.java`**

Reported capabilities:

- Immutable lifecycle assessment.
- `ACTIVE`, `APPROACHING_EXPIRY`, and `EXPIRED` states.
- Editorial-expiry eligibility.
- Editorial-attention requirement.
- Parsing of canonical `expirationDate`.
- Independent inactive/withdrawn status handling.
- Rejection of malformed nonblank expiration dates.

**`shared/lifecycle/EditorialExpiryConfiguration.java`**

Reported capabilities:

- Injectable UTC `Clock` as instant source.
- Configurable approaching-expiry window, currently 30 calendar days.
- Default deployment zone: `America/New_York`.
- Per-Community timezone overrides.
- Explicit Wilmington configuration.

### 2.2 Existing integration

IM-01 integrated lifecycle filtering at current-inventory boundaries for:

- Resources.
- News.
- Flyers.
- FAQs.
- Expert Answers.
- Approved Articles.
- RSS discovery and signed-bill presentation.

Repositories retain stored and historical content.

### 2.3 Existing verification

The IM-01 report records:

- 417 passing backend tests.
- 126 passing frontend tests.
- Successful TypeScript checks and production build.
- Six passing catalog validators.
- Ten successful local runtime endpoint checks.
- No changed catalog records, dependency declarations, or frontend lockfiles.

### 2.4 Source-control condition

IM-01 was implemented on `copyofdev`, starting from HEAD:

`2209f65f496f4e205b515e4ddf788e2fb035e35f`

No commit, staging, or push was performed.

**The coding agent must inspect the actual current checkout and working tree. It must not assume IM-01 changes have already been committed or are available on another branch.**

If the lifecycle implementation or required tests are missing from the accessible checkout, stop before making unrelated changes and report the discrepancy.

---

## 3. Mandatory Pre-Implementation Inspection

Before modifying code:

1. Confirm branch, HEAD, and working-tree status.
2. Identify all uncommitted IM-01 changes and preserve them.
3. Inspect `EditorialLifecycle`, `EditorialExpiryConfiguration`, and their actual public methods and result types.
4. Inspect the canonical lifecycle metadata exposed by `CivicContent` and relevant concrete classes.
5. Identify how all seven concrete content classes are stored and retrieved.
6. Inspect existing administrative utilities, validators, reports, and catalog-maintenance commands.
7. Inspect existing tests and documentation associated with expiry and catalog stewardship.
8. Confirm whether any existing functionality already satisfies part or all of IM-02.

**Inspect first. Reuse before extending. Implement only demonstrated gaps.**

Do not replace the established lifecycle API merely to simplify report generation.

---

## 4. Required Deliverable A — Authoritative Diagnostic Collection

Implement or verify one shared diagnostic collection path that can inspect stored CivicContent independently of resident-facing current-inventory filtering.

### Requirements

The diagnostic must:

- Use the shared `EditorialLifecycle.assess` contract or its existing authoritative equivalent.
- Use the configured clock, Community timezone, and approaching-expiry window.
- Include otherwise qualifying APPROACHING_EXPIRY and EXPIRED records.
- Operate against stored catalog records rather than already expiry-filtered resident inventories.
- Retain the record's original identity and ContentType.
- Preserve Community boundaries and content-source provenance.
- Avoid changing record classifications, statuses, or expiration dates.
- Avoid persisting derived lifecycle states.

Inspection must cover all seven concrete content classes represented in IM-01, including content whose presentation ContentType is shared with another class.

**Important:** A record that is excluded from resident delivery because it is inactive, withdrawn, draft, or unapproved must not be misrepresented as currently resident-visible merely because its expiration date indicates ACTIVE or APPROACHING_EXPIRY.

Where these distinctions matter, diagnostic output must keep editorial-expiry state separate from other publication/status constraints.

---

## 5. Required Deliverable B — Repeatable Admin-Facing Report

Provide a deterministic, human-consumable stewardship report.

For v2, a bounded command-line report, repository-native maintenance command, or existing administrative report mechanism is acceptable.

A new web dashboard, general Admin API, scheduled notification service, or administrative UI is not required.

### Minimum report information

Each applicable diagnostic record must make the following information available:

| Field                | Required meaning                                                  |
| -------------------- | ----------------------------------------------------------------- |
| Content ID           | Stable CivicContent identifier                                    |
| Content title/name   | Human-readable identification                                     |
| Content class/type   | Actual content identity and existing ContentType                  |
| Community            | Owning Community identifier                                       |
| Content source       | Producer/provenance identifier, where available                   |
| Expiration date      | Canonical editorial expiration date                               |
| Lifecycle state      | APPROACHING_EXPIRY or EXPIRED                                     |
| Attention indication | Whether editorial attention is required                           |
| Days to/from expiry  | Calendar-day distance calculated in the applicable Community zone |
| Status context       | Relevant active/withdrawn/approval status, where available        |
| Diagnostic reason    | Human-understandable explanation of why the record appears        |

Do not invent missing metadata to populate optional information.

### Reporting behavior

The report must:

- Be reproducible against a fixed clock and unchanged catalog.
- Order results deterministically.
- Permit Community-scoped reporting.
- Clearly distinguish approaching-expiry and expired records.
- Support identification of records that need a human follow-up.
- Avoid displaying internal Java exception traces as normal report descriptions.
- Produce an intelligible empty result when no records qualify.
- Be usable without launching a new Admin frontend.

The preferred implementation is a structured internal diagnostic representation with a thin human-readable output layer, if existing architecture supports this without disproportionate change.

**Do not introduce a general reporting framework solely for IM-02.**

---

## 6. Required Deliverable C — Invalid Metadata Handling

IM-01 established that malformed nonblank expiration dates must not be silently interpreted as ACTIVE.

IM-02 must preserve that invariant.

When diagnostic collection encounters malformed editorial-expiry metadata:

1. Identify the affected record where possible.
2. Report the metadata problem deterministically.
3. Preserve the underlying catalog record.
4. Do not silently repair its date.
5. Do not assign a fabricated expiration date or lifecycle state.
6. Do not allow one invalid record to make the entire stewardship report unusable if the issue can be isolated safely.

Invalid-metadata findings may be reported in a separate error/validation section rather than treated as a fourth lifecycle state.

**Do not introduce a new persisted `INVALID` lifecycle enum value without an approved requirement.**

Any diagnostic error handling must preserve existing resident-facing error and eligibility behavior rather than weakening IM-01's safety decisions.

---

## 7. Required Deliverable D — Admin Expiry Guidance

Create or update the existing Admin-facing documentation covering the editorial-expiry lifecycle.

The guide must explain:

### ACTIVE

- The record has not entered its approaching-expiry window, or has no editorial expiry date.
- Lifecycle state alone does not guarantee publication or other resident eligibility.
- No approaching-expiry attention is required on this basis.

### APPROACHING_EXPIRY

- The record is within the configured review window.
- It remains resident-eligible where all other rules permit.
- The editor should verify whether its information remains accurate and useful.
- The editor may contact the producing or providing organization, verify the service, review a deadline, or determine whether an editorial follow-up is warranted.

### EXPIRED

- The editorial expiration date has passed under the configured Community-local calendar rules.
- The record is excluded from applicable current-availability resident views.
- It remains retained in the catalog.
- The editor must determine whether to verify, update, renew through an authorized catalog process, or leave it historical.

The guide must also explain:

- Editorial expiry is not an event occurrence date.
- APPROACHING_EXPIRY does not automatically increase a featured item's ranking.
- No automatic article generation, interview assignment, or notification occurs.
- A diagnostic represents a request for human attention, not an automated workflow state.
- Editing catalog records remains subject to the existing approved procedures.

Use the existing Admin Guide where accessible. If it is unavailable, document the access limitation and provide a narrowly scoped supplemental guide without inventing replacement guidance for unrelated Admin functions.

---

## 8. Required Acceptance Tests

The following test groups must be implemented or demonstrated through existing named tests.

| ID       | Test requirement                                             | Expected result                                                                                                      |
| -------- | ------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------- |
| IM02-T01 | ACTIVE with future expiry outside approaching window         | Not reported as an expiry-attention finding                                                                          |
| IM02-T02 | First calendar day inside the approaching window             | Reported as APPROACHING_EXPIRY                                                                                       |
| IM02-T03 | Exact expiration date                                        | Still APPROACHING_EXPIRY and resident-eligible where otherwise qualified                                             |
| IM02-T04 | First local calendar day after expiration                    | Reported as EXPIRED                                                                                                  |
| IM02-T05 | Missing/blank expiration date                                | Not invented or falsely flagged                                                                                      |
| IM02-T06 | Fixed clock and unchanged data                               | Identical diagnostic findings and ordering                                                                           |
| IM02-T07 | Different Community timezones / DST                          | Correct local-date behavior using existing IM-01 configuration                                                       |
| IM02-T08 | All seven concrete content classes                           | Applicable findings can be retrieved without excluding an entire class                                               |
| IM02-T09 | Stored expired record absent from current resident inventory | Present in diagnostic report                                                                                         |
| IM02-T10 | Withdrawn/inactive/unapproved content with expiry            | Expiry and independent status remain distinguishable                                                                 |
| IM02-T11 | Malformed nonblank expiration date                           | Visible diagnostic error; never silently ACTIVE                                                                      |
| IM02-T12 | Multiple records with matching date/state                    | Stable deterministic ordering                                                                                        |
| IM02-T13 | Community-scoped reporting                                   | Records from another Community are excluded                                                                          |
| IM02-T14 | No matching records                                          | Valid, intelligible empty report                                                                                     |
| IM02-T15 | Diagnostic collection                                        | Does not mutate source records, statuses, or dates                                                                   |
| IM02-T16 | Real expired Policy Info fixtures                            | NP-001, NP-002, NP-006 are identifiable if present in the current stored catalog                                     |
| IM02-T17 | Resident regression                                          | Approaching-expiry remains visible; expired remains excluded from applicable current projections                     |
| IM02-T18 | Scope regression                                             | No new Admin dashboard, workflow engine, persisted eligibility flag, new ContentType, or notification infrastructure |

For real-data tests, distinguish controlled fixtures from the runtime catalog. Where actual records have changed, describe that change instead of manufacturing a match.

Existing IM-01 tests may supply part of the lifecycle boundary evidence. New IM-02 tests should focus on diagnostics, report behavior, stored-catalog access, and separation of stewardship from resident eligibility.

---

## 9. Implementation Boundaries

### Authorized

- A narrowly scoped diagnostic service or equivalent existing implementation.
- Small repository access adaptations needed to inspect stored records.
- A human-readable report mechanism.
- Focused automated tests and validators.
- Admin-facing expiry documentation.
- Minimal configuration or invocation instructions.

### Not authorized

- New Admin dashboard.
- Full Admin UI or catalog CRUD.
- Automated editorial queues.
- Workflow assignment or task ownership system.
- Email alerts or notification schedules.
- Automatic article generation.
- AI-generated recommendations for editorial decisions.
- New persisted lifecycle states.
- Generalized content lifecycle framework.
- Database migration.
- New Community administration capability.
- Event occurrence eligibility implementation (IM-10).
- Featured-topic or featured-item ranking changes (IM-06/07).
- Broad catalog cleanup unrelated to demonstrated IM-02 requirements.

Any proposed change outside these boundaries must be reported as a scope conflict rather than implemented.

---

## 10. Required Execution Sequence

The coding agent must follow this sequence.

**Step 1 — Preserve baseline**

Confirm source-control state, current IM-01 changes, and applicable reference files.

**Step 2 — Inspect**

Read the existing lifecycle implementation, stored repositories, admin tooling, tests, and documentation.

**Step 3 — Verify existing capability**

Determine which IM-02 requirements, if any, are already satisfied. Document evidence.

**Step 4 — Design the bounded integration**

Choose the smallest repository-consistent mechanism for collecting and reporting diagnostics. Record the rationale before changing production code.

**Step 5 — Implement**

Reuse IM-01 lifecycle assessment, add only necessary diagnostic/report functionality, and preserve stored records and existing resident-delivery rules.

**Step 6 — Test**

Add targeted tests using fixed clocks, multiple Communities, all applicable content classes, invalid metadata, and real catalog fixtures.

**Step 7 — Run quality gates**

Execute the focused tests, then the available broader backend tests, frontend tests, type checking, build, and catalog validators according to the established repository procedures.

**Step 8 — Produce evidence**

Provide sample sanitized or catalog-safe report output, verification commands, results, changed-file inventory, and required disposition mapping.

**Step 9 — Stop at package boundary**

Do not begin IM-03, IM-04, or other packages in the same run.

No commit, staging, push, dependency upgrade, or catalog mutation is authorized unless separately requested.

---

## 11. Evidence Required for Acceptance

The final agent report must include:

| Evidence field               | Required response                                                                       |
| ---------------------------- | --------------------------------------------------------------------------------------- |
| Package                      | IM-02                                                                                   |
| Initial source-control state | Branch, HEAD, clean/dirty condition, protected IM-01 changes                            |
| Repository inspection        | Relevant implementation classes, repositories, tests, and Admin documentation           |
| Existing capability          | What was already satisfied and how it was verified                                      |
| Gap demonstrated             | Specific missing diagnostic/reporting behavior                                          |
| Production modifications     | Exact files and purpose                                                                 |
| Test modifications           | Named tests and covered acceptance criteria                                             |
| Commands executed            | Reproducible commands, not only summary statements                                      |
| Test results                 | Pass/fail counts and any limitations                                                    |
| Real catalog evidence        | Findings for applicable existing expired records                                        |
| Diagnostic example           | Sample output showing APPROACHING_EXPIRY, EXPIRED, and error distinctions as applicable |
| Admin guide evidence         | Updated file and location                                                               |
| Architecture protection      | Explicit confirmation of no unauthorized v3 scope or P5 changes                         |
| Dependency impact            | Confirm IM-01 remains valid and identify only legitimate downstream interfaces          |
| Final disposition            | Satisfied, still open, or escalation required                                           |

The report must distinguish actual executed verification from assumed or inherited IM-01 results.

---

## 12. Acceptance and Completion Gate

IM-02 may be marked **SATISFIED** only when:

1. An Admin or editor can deterministically identify approaching-expiry and expired stored CivicContent.
2. Diagnostic evaluation reuses IM-01 rather than duplicating expiry rules.
3. Approaching-expiry content remains resident-eligible under the IM-01 contract.
4. Expired content remains retained and discoverable for stewardship.
5. Malformed expiry metadata cannot silently become valid.
6. Community ownership and timezone behavior are preserved.
7. The Admin-facing guide accurately explains the lifecycle states and expected human actions.
8. Relevant automated tests and quality gates pass, with evidence recorded.
9. No unapproved domain expansion or V3 operations tooling is introduced.

If a requirement cannot be demonstrated, retain an open disposition with the exact blocker and evidence.

**Passing compilation alone does not satisfy IM-02.**

---

## 13. Authorized Final Instruction to Coding Agent

Implement IM-02 under the preceding constraints.

Treat the reported IM-01 implementation as the established baseline, but verify its actual source code and working-tree availability before proceeding.

Reuse `EditorialLifecycle.assess` and existing stored catalog access. Build the smallest practical diagnostic/reporting capability that allows an editor to identify and investigate editorial expiry without creating an Admin dashboard or workflow system.

Preserve all approved P4/P5 classification, Community, provenance, publication, and lifecycle boundaries.

Complete the acceptance tests, produce the Admin expiry guidance, and return a reproducible execution report.

**Do not begin the next IM package. Do not declare First Step v2 complete.**
