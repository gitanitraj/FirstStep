# First Step v2 — IM-01 Readiness Decisions (PC-01–PC-04)

**Status:** APPROVED — CONSOLIDATED DECISION RECORD  
**Decision date:** October 15, 2026  
**Purpose:** Close the four bounded readiness questions identified by the coding agent in its October 15, 2026 IM-00 Readiness Report and establish an executable handoff for IM-01.  
**Applies to:** IM-01 and related downstream IM packages as specified below.  
**Authority and precedence:** The approved v2 Completion Contract, 2B-P4 scope, 2B-P5 Logical ER + Classification Schema, approved Architecture Resolutions, and authoritative IM-01–IM-14 Execution Matrix remain controlling. These decisions supply implementation parameters, division of work, and audit ownership; they do not reopen P5 or amend the v2 scope.

## Decision register

| ID | Decision | Status | Principal implementation ownership |
|---|---|---|---|
| **IM-01-PC-01** | Editorial expiry policy constants | **Approved** | IM-01; IM-02, IM-10 and downstream resident projections |
| **IM-01-PC-02** | Editorial expiry versus event occurrence | **Approved, with clarification** | IM-01 and IM-10; verification in IM-13; documentation in IM-14 |
| **IM-01-PC-03** | Red backend baseline reconciliation | **Confirmed / authorized for execution** | Pre-implementation baseline repair within IM-01 |
| **IM-01-PC-04** | Ownership of 99-rule stewardship mapping | **Approved** | IM-12 audit; IM-13 verification; IM-14 final reconciliation |

---

## PC-01 — Editorial Expiry Policy Constants

**Status: APPROVED**

### Lifecycle rules

- **ACTIVE:** The record passes the editorial-expiry condition and remains eligible for applicable resident collections if other independent eligibility rules also pass.
- **APPROACHING_EXPIRY:** Begins **30 calendar days before the stated expiration date**, inclusive of the start date. The record **remains resident-eligible**, including for featured selection where otherwise appropriate, and becomes reportable for editorial attention.
- **EXPIRED:** Begins at **00:00 local time on the day after** the stated editorial expiration date. The record is excluded from applicable current-availability resident projections but remains in the catalog for historical and editorial use.
- The stated date is **inclusive**: a record expiring November 14, 2026 remains within its editorial validity period through November 14 in the applicable local timezone and becomes expired November 15. With a 30-day threshold, it is approaching expiry beginning October 15.

### Timezone and implementation

- The Wilmington Community uses the IANA timezone **`America/New_York`**, including daylight saving transitions.
- Interpret date-only editorial expiry in the owning Community's configured timezone. Support a different Community timezone without changing the lifecycle algorithm or launching a second Community.
- Do not use the application server's implicit timezone as the business rule.
- Use an injectable **Java `Clock`** for deterministic boundary tests.
- Keep the approaching-expiry threshold configurable rather than independently hard-coding it in multiple services.
- Implement one reusable editorial-lifecycle/eligibility decision used by applicable downstream projections.

### Missing editorial expiry by content class

**A missing editorial expiration date does not by itself invalidate or exclude an item. No other date may be silently converted into an editorial expiration date.**

| Content class | Missing editorial-expiry rule | Independent conditions preserved |
|---|---|---|
| RESOURCE | No editorial-expiry exclusion solely due to a missing date | Explicit inactive/withdrawn state or other applicable restrictions |
| NEWS / Policy Info | No automatic expiry inferred from publication age | Explicit status and applicable presentation rules |
| FLYER | No editorial-expiry exclusion solely due to a missing date | Event occurrence and opportunity availability are separate |
| LAW | No expiry inferred from publication date or RSS age | Other verified legal/contextual validity rules where applicable; do not infer repeal |
| EXPERT / FAQ | No editorial-expiry exclusion solely due to a missing date | Classification, editorial and other independent publication rules |
| EXPERT / Expert Answer | No expiry inferred from a prior session date | Classification and other independent publication rules |
| First Step Article (NEWS subtype) | No automatic expiry solely due to a missing date | Existing approval/publication guard remains controlling |

The missing-expiry rule does **not** represent a statement that an item has been freshly verified. Last-checked/recertification questions remain conceptually separate. No automatic age-based recertification exclusion is authorized within IM-01.

### Applicable current-availability resident surfaces

The shared editorial-expiry exclusion must be applied consistently wherever content is presented as current or available, including:

- Category Destination Get Help and What to Know collections;
- Topic sub-destinations and All-[Category]-Help inventories;
- Featured Topics and featured items;
- Homepage previews, Find Help, currently discoverable Search results;
- Latest Updates and Community Notices;
- current associated offerings on Organization views;
- navigation and collection counts intended to represent currently discoverable inventory.

**Catalog/history/Admin records are retained.** Direct historical content URLs are governed by their applicable publication rules rather than being indiscriminately deleted or returned as current opportunities. Existing explicit inactive/withdrawn states and article editorial approval restrictions must not be overridden by missing expiry.

**Downstream ownership:** IM-02 supplies approaching-expiry and expired-item diagnostics; IM-10 handles occurrence-based event eligibility; downstream resident packages adopt the shared editorial-expiry rule.

---

## PC-02 — Event-Date Implementation Boundary

**Status: APPROVED — INCLUDING EXPLICIT CLARIFICATION**

### Controlling principle

**Editorial expiry and event occurrence are separate eligibility dimensions. A concluded event loses current-event eligibility without automatically changing its underlying CivicContent record's editorial expiry state.**

### Division of responsibility

**IM-01 — editorial lifecycle:**

- Calculates ACTIVE, APPROACHING_EXPIRY and EXPIRED from editorial-expiry metadata under PC-01.
- Supplies the shared editorial-expiry eligibility mechanism and deterministic date-boundary tests.
- Does **not** infer editorial expiration from `eventDate`, `startsAt`, `endsAt`, a session date, publication date or other occurrence metadata.

**IM-10 — event occurrence:**

- Verifies or implements event-occurrence eligibility, following approved P5-R5b, in Events and event-based access-to-help projections.
- Verifies that the same canonical civic item may appear in Events and Flyers without adding an EVENT ContentType or generalized Event entity.
- Retains historical records and flyer media without incorrectly treating HOST as PROVIDER.

### Binding event-date behavior from P5-R5b

| Occurrence metadata | Current-event eligibility boundary |
|---|---|
| Explicit `endsAt` | Through the stated end instant |
| Date-only `eventDate` | Through the end of that **local calendar date** |
| Start timestamp without an end | Through the end of the event's local calendar date; do not invent a duration |
| No usable occurrence date | Must **not** be presented as a *confirmed current/upcoming event* without suitable occurrence metadata or verification |

The last row is the approved PC-02 implementation refinement, not a claim that the prior P5 document expressly specified an undated-event fallback.

### Combined resident behavior

- A future event subject to editorial expiry must satisfy **both** editorial and occurrence eligibility to appear as current.
- A concluded event is excluded from current Events and **event-based** Get Help even if editorially ACTIVE or missing an editorial expiration date.
- Concluding an event does **not** set editorial state to EXPIRED; the underlying CivicContent record is retained.
- A past event flyer must not be advertised as an upcoming opportunity in current-event presentations. Its treatment in other collections depends on that projection's semantics and independent eligibility rules.
- FLYER alone does not qualify for Get Help. Actual access-to-help semantics must be explicit, consistent with P5-R2.
- Do not mass-mark FL-001–FL-007 editorially expired merely because their event dates are past. Use the real event fixture FL-008 where relevant, with date-sensitive assertions controlled by deterministic time.

**IM-13** verifies combined behavior; **IM-14** reconciles the human-facing Admin Guide with these approved rules. The existing *First Step Admin Guide — Expiry Eligibility and Editorial Stewardship* remains the human-facing operational document; do not invent a rival guide.

---

## PC-03 — Red Backend Test Baseline

**Status: USER CONFIRMED; BOUNDED REPAIR AUTHORIZED, NOT YET VERIFIED AS EXECUTED**

### IM-00 observed baseline (October 15, 2026)

- `mvn test`: **371 tests, 2 failures**, both in `EditorialStabilityTest`.
- Frontend Vitest: **126 passed** across 19 files; TypeScript type check passed; six catalog validators passed.
- The intentional addition of United Neighbors 2026 flyer **FL-008** increased the **Community Support** catalog count from **61 to 62** and the **overall catalog** count from **238 to 239**.
- FL-008 and related additions were uncommitted at the time of IM-00; unrelated working-tree changes were also present.

### Prior confirmations and authorization

The user confirmed that the coding LLM had already asked for and received confirmation of the relevant FL-008 facts. **Do not request those approvals again.** Treat FL-008 as an intentional accepted catalog addition; preserve its valid classification and source/organization relationships.

The coding agent is authorized, as a narrowly scoped **pre-expiry baseline reconciliation inside IM-01**, to update the two affected `EditorialStabilityTest` count expectations:

| Assertion | Old expectation | Authorized new expectation |
|---|---:|---:|
| Community Support catalog count | 61 | **62** |
| Total catalog count | 238 | **239** |

The agent shall:

1. Capture the branch/commit, preexisting failures, and working-tree diff in the evidence record.
2. Make only the demonstrated assertion changes; do not delete, duplicate, or reclassify valid records to satisfy assertions.
3. Run `mvn test` and report the actual result as the **pre-expiry baseline**.
4. Implement IM-01 expiry changes only after the baseline reconciliation, then separately capture the post-implementation test results.
5. Keep **canonical catalog inventory counts** distinct from **resident-facing currently eligible counts**; expiry must not silently change what is stored.
6. Use deterministic `Clock`-based test fixtures for date-dependent eligibility/count behavior.
7. Preserve unrelated uncommitted material; do not disable or weaken tests or rewrite unrelated data.

**Important:** User confirmation and work authorization do not mean the repository changes or green test run have already happened. Satisfaction requires coding-agent execution evidence.

---

## PC-04 — Stewardship 99-Rule Mapping Ownership

**Status: APPROVED**

### Scope and assignment

The 99 rules of the *Civic Catalog Stewardship & Security Model* are a **mandatory rule-coverage audit**, **not 99 new implementation tasks**.

- **IM-12 owns the complete 99-rule audit** and the Integrated Architecture Reconciliation's **R5 stewardship-mapping gate**.
- **IM-13 verifies applicable bounded corrections** through release and journey regression.
- **IM-14 owns final evidence reconciliation and closure verification**, including ensuring all rules have terminal dispositions and no applicable v2-required obligation has been silently deferred.
- **IM-01 is not blocked** on completing the complete 99-rule register; it must still obey any applicable approved stewardship requirements.

### Required IM-12 artifact: Stewardship Rule Coverage Register

Produce **one row for each of the 99 original rules**, preserving its original ID and requirement text, with:

1. Original rule ID and exact requirement.
2. Source section and governing approved clarifications.
3. V2 applicability: required, enabling foundation, V3/Later, or superseded/reinterpreted.
4. Implementation status: satisfied, partially satisfied, missing, or not applicable.
5. Evidence reference (code, test, validator, catalog record, document).
6. Responsible IM package.
7. Final disposition and, where necessary, bounded corrective action.

**Acceptance:** All 99 source rules are accounted for with evidence-backed dispositions; existing satisfied requirements do not generate work; V3/Later rules do not become v2 blockers; all v2-required gaps are resolved through an existing IM package or escalated under the Completion Contract's change control. No new IM-15 or generalized architecture is authorized solely because a rule needs auditing.

### Preserved architecture decisions

- The approved singular **Community 1 → many CivicContent** model is closed; IM-12 verifies community-scoped behavior and partition integrity rather than reopening cardinality.
- The full seven-step featured-topic selection hierarchy is closed; IM-06 implements it and IM-12 may verify coverage rather than create another ranking system.
- Existing CAT-02 and ACD-01 interpretations and other approved reconciliation gates remain effective except where expressly superseded by later approved architecture resolutions.

---

## Implementation handoff and evidence contract

These four decisions **resolve the four IM-00 readiness questions**. They do **not** certify that IM-01 has been coded, that backend tests now pass, or that the 99-rule audit is complete.

**Next executable artifact:** An IM-01 coding-agent work order incorporating PC-01 through PC-04, the approved P4/P5 requirements, the existing execution matrix, and the coding agent's inspect-first/evidence-gated procedure.

For IM-01, require an evidence record including: repository files inspected; starting branch/commit and diff; verified baseline repair and `mvn test` output; exact expiry implementation changes; focused tests for thresholds, missing expiry and timezone boundaries; surface-level regression checks; tests/commands with pass/fail counts; preserved scope boundaries; and final package disposition.

**Final boundary:** No redesign of P5, no V3 pull-forward, no invented event-duration rules, and no claim of package satisfaction without implementation/test evidence.

## Source references for Library / repository reconciliation

- **IM-00 Readiness Report (Revised)**, October 15, 2026, especially §§4–7 (two backend failures, four readiness questions and proposed IM-12 stewardship mapping).
- **First Step v2 — Authoritative IM-01–IM-14 Execution Matrix**, especially IM-01, IM-02, IM-10, IM-12, IM-13, IM-14, evidence contract and execution rule.
- **First Step v2 — 2B-P4 Scope**, especially P4-17, P4-20–P4-26 and its v2/v3 boundary.
- **First Step v2 — 2B-P5 Logical ER + Classification Schema — Approved**, especially §§10–11, §§13–15, §§18–25 and core invariants.
- **First Step v2 — Approved Architecture Resolutions: Community Cardinality & Featured-Topic P5 Alignment**.
- **First Step Admin Guide — Expiry Eligibility and Editorial Stewardship** (human-facing stewardship guidance; reconcile at IM-14).
- **Civic Catalog Stewardship & Security Model** (99-rule source for IM-12; not reprinted here).
- **Integrated Architecture Reconciliation**, including the R5 coverage gate (IM-12 ownership).

**Final decision disposition:** **PC-01 APPROVED · PC-02 APPROVED · PC-03 CONFIRMED/AUTHORIZED · PC-04 APPROVED**. IM-01 may proceed under a separately issued coding-agent work order.
