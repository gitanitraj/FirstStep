# First Step v2 — Integrated Architecture Reconciliation

Updated source-of-authority and code-audit package • 15 October 2026

## 1. Status and intent

Status: UPDATED RECONCILIATION BASELINE for source assembly, repository audit and integrated implementation planning. This is not a new coding authorization and does not supersede the already approved IM-01–IM-14 execution package. Coding paused after Decision 051 to prevent piecemeal redesign. Only demonstrated contract gaps become bounded work.

Current evidence limitation: this package combines approved discussion decisions, the available IM execution matrix, and the supplied P5 approval note. The full canonical Completion Contract, complete P4/P5 original source, original record-level catalog audit and repository are not all present as directly inspected files here. Treat missing originals as evidence gaps, not permission to invent requirements.

## 2. Authority and precedence

Approved v2 Completion Contract and explicit scope amendments govern what may be required for v2.

Seven canonical journeys (J1–J7) and five approved epics (E1–E5) define coverage; exact source text should be attached to agent handoff.

P4 limits model growth; P5 approved logical model defines invariants, role-based deterministic selectors and prohibitions against V3 pull-forward.

Decisions 048, 051 and 052 and subsequent approved resident discovery and stewardship amendments govern their respective topics.

Later approved CAT-02/ACD-01 supersede P5 or Decision 013 only on the specific Community/relevance semantics in conflict; preserve P5 other invariants.

Current repository and JSON records are evidence of implementation and factual coverage, not authority to contradict approved design.

In genuine conflicts, flag an architectural exception for review; do not silently choose the easiest interpretation.

## 3. Governing principles

Category is the center of resident discovery; the program/service is the primary help-seeking object, not the provider organization.

Community is first-class civic operating/discovery context, not a restriction based on municipality/provider address.

Location, service area, and provider-published eligibility remain separate; First Step does not decide who qualifies.

Identity, classification, publication/temporal eligibility, controlled projection and featured selection remain separate responsibilities.

Current five ContentTypes remain fixed; CivicContent may participate in multiple resident projections without duplicate identity.

Before any coding obligation, compare prior decisions, seven journeys, P5, prior FindHelp.org-informed discovery principles and the Completion Contract.

Inspect existing implementation and tests first; prove a gap before correcting it; do not implement during audit.

## 4. Canonical journey and epic coverage

| Journey | Coverage definition |

| --- | --- |

| J1 | Resident finds help through Category Destination |

| J2 | Resident explores a featured topic |

| J3 | Resident moves between an offering and its provider |

| J4 | Resident discovers civic information rather than assistance |

| J5 | Editor adds/updates an offering without false identity |

| J6 | Editor handles approaching-expiry content |

| J7 | System protects catalog identity/classification |

| Epic | Approved title |

| --- | --- |

| E1 | Civic Catalog Identity & Relationships |

| E2 | Resident Help & Topic Discovery |

| E3 | Civic Information Collections |

| E4 | Content Lifecycle & Editorial Stewardship |

| E5 | Catalog Integrity & Classification |

These names and meanings appear in the available IM execution reference. Attach the full canonical journey/epic acceptance text from the Completion Contract where available; do not treat these abbreviated summaries as replacements.

## 5. IM-01–IM-14 existing execution spine and reconciliation overlays

| Package | Existing package name | Reconciliation clarification |

| --- | --- | --- |

| IM-01 | Central expiry state & eligibility | LCM; common validity across projections |

| IM-02 | Expiry diagnostics & Admin-facing attention | LCM/MNT; no V3 queues |

| IM-03 | Expert/FAQ Category integrity | CAT-03/04 and explicit Category classification |

| IM-04 | Flyer/Get Help dual projection | CAT-06/11 and same CivicContent identity |

| IM-05 | Topic sub-destination and All-[Category]-Help | ACD-01; program-first discovery; GEO-01-LM |

| IM-06 | Featured-topic selector | P5 actionable inventory priority; not naive counts |

| IM-07 | Three-position featured-item selector | P5 TIMELY/DURABLE/VISUAL + R6a–d |

| IM-08 | Category Destination integration | Decision 051/052, provider secondary, context rail |

| IM-09 | Policy Info/Law/Flyers/First Rate News | controlled projections; public DTOs; selective Law admission |

| IM-10 | Community Events generalization | single identity, EVENT notice kind, time boundary |

| IM-11 | Real Housing collision regression | use current inventory and evidence; do not reopen P5 for ordinary ties |

| IM-12 | Identity/provenance/community audit | EXPAND audit overlay GEO-01-A/B/C/D and Community semantics |

| IM-13 | Seven-journey and release gates | J1–J7, mobile, accessibility, high contrast, tests |

| IM-14 | Documentation and v2 closure | all packages evidence-backed; update steward rules and deferrals |

Existing dependency spine: IM-01 → IM-02 → IM-03/IM-04 → IM-05 → IM-06/IM-07 → IM-08 → IM-09 → IM-10 → IM-11/IM-12 → IM-13 → IM-14. This remains the working baseline pending the integrated audit; adding stewardship overlays does not silently replace the execution matrix.

### Specific version-sensitive conflict checks

P5 stated each CivicContent belongs to exactly one Community and establishes Community scope before selection. Later approved CAT-02/ACD-01 reject geography-based eligibility inference and require GEO-01-C data-driven cardinality reconciliation. Treat as a specific semantic conflict, not blanket cancellation of Community.

Earlier IM-06 abbreviated the featured-topic rule as eligible classified-content counts. The approved detailed P5 logic prioritizes resident-actionable inventory, role completeness, help first/context second; audit selector specification accordingly.

RSS LAW collection is not automatic relevance, admission, publication or indefinite retention. Assess source checks, deduplication, selective Law admission and disposal.

Approaching expiry stays resident-eligible while requiring diagnostic attention; elapsed event dates are distinct from editorial expiry; direct-help timely outranks contextual and stewardship signals.

No source-code audit has yet established that all 99 stewardship requirements are enforced. Their inclusion is a coverage audit, not 99 coding tasks.

## 6. GEO-01 integrated domain/data audit

| ID | Required evidence |

| --- | --- |

| GEO-01-A | Inspect CommunitySlug.forCity, locations[0].city, default communityId, backend/frontend filter use and record assignments. |

| GEO-01-B | Use real offerings to test nearby, regional and remote-provider relevance without assuming eligibility. |

| GEO-01-C | Review prior record-level audit and current data to determine multi-community discovery across single CivicContent identities; do not infer cardinality from provider locations alone. |

| GEO-01-D | Review Category → Topic → CivicContent actual cardinalities and controlled projections against P5 and ACD-01. |

| GEO-01-LM | Program-first library-inspired discovery; distinguish service type, provider, physical location, area served, who is served, access and availability. |

Known historical implementation report from coding LLM: resources communityId derived from locations[0].city or default; granular APIs take optional community filter; homepage does not; frontend does not send it. Treat this as report requiring source verification, not accepted future semantics.

## 7. Six-area stewardship audit crosswalk

| Area | Rule range | Verification focal points |

| --- | --- | --- |

| Cataloging | CAT-01–16 | Stable IDs; taxonomy; source/organization; provider/host; flyer overlap; validator coverage; GEO-01 |

| Maintenance | MNT-01–15 | Routine vs material corrections; published verification; stale contacts; references; operating procedure |

| Lifecycle | LCM-01–17 | Active/approaching/expired; events; exclusion; operational withdrawal Option A; restore checks |

| Continuing Resources | CR-01–16 | Feed source identity; repeated ingestion; source changes Option A; selective Law admission/disposal |

| Security | SEC-01–18 | Public DTOs; unpublished records; safe inputs; no anonymous modification; community not authorization |

| Audit & Recovery | AR-01–17 | Version restoration; validate postrestore; retention/disposal; V3 structured history/reports |

## 8. Evidence standards and finding dispositions

Implemented and verified — current code and meaningful tests prove the requirement.

Implemented with verification gap — implementation exists but proof or deployment checks are incomplete.

Partial — enforcement does not cover all relevant inputs, endpoints or content families.

Missing — approved required behavior lacks demonstrated implementation.

Operationally satisfied — current manual/deployment procedure meets authorized v2 scope.

Deferred V3/Later — explicit future scope, not a v2 defect.

Architectural exception — an approved requirement cannot be represented consistently; escalate before coding.

For every finding record: requirement/decision and IM package, source revision/commit, exact file/method/JSON path, current behavior, negative and boundary tests, affected journey/epic, relevant catalog specimen, safety/regression impact, smallest remediation, execution disposition, evidence reference and reviewer decision.

## 9. Integrated reconciliation gates

R1 — Assemble exact authoritative texts and resolve source/version conflicts.

R2 — Establish current branch/commit, code/API/catalog/validator/test baseline.

R3 — Reconcile CivicContent, Category, Topic, Organization, ContentSource and GEO-01 using real records.

R4 — Trace seven journeys and five epics through real resident/editor scenarios.

R5 — Map all 99 stewardship rules to code, tests, operations or explicit V3 deferrals.

R6 — Consolidate confirmed gaps into existing IM dependencies; flag amendment requests separately.

R7 — Review evidence, approve finite modifications, and only then release coding work orders.

## 10. Deliverables for read-only coding LLM

Source/version/decision authority report and unresolved conflicts.

Repository baseline including commit and current suite counts.

Requirement-to-code evidence matrix and file/validator coverage.

Journey/epic regression crosswalk.

99-rule stewardship enforcement/disposition matrix.

GEO-01 and current CivicContent relationship/cardinality evidence, including prior audits.

Shared-service inconsistencies and publication/security boundary tests.

Consolidated package-level gaps mapped to IM-01–IM-14, dependencies, regressions and V3 deferrals.

Open architectural exceptions requiring user approval.

### Read-only agent stop rule

No code, tests, JSON, documentation, branches or commits may be altered during reconciliation. Do not begin IM-01 merely because the old execution matrix called it the next package. Stop with source-anchored findings and proposed bounded changes for human architectural review.

## 11. Required source bundle and coverage caveat

| Source | Handoff requirement |

| --- | --- |

| V2 Completion Contract | Attach exact approved version and amendments; scope controlling |

| P4 and full approved P5 | Attach exact models, R1–R6 refinements and acceptance |

| Decisions 048, 051, 052 | Attach relevant excerpts and any later amendments |

| Seven journeys/five epics | Attach exact canonical descriptions and acceptance details |

| IM-01–IM-14 matrix | Attach existing authoritative execution artifact; annotations in this document are overlays |

| Stewardship Model | Attach companion 99-rule model |

| Prior catalog/FindHelp.org review | Attach original audit where retrievable; otherwise identify the evidence gap |

| Coding repository | Require read-only full access for actual file/test verification |

## 12. Approval and closure semantics

The architecture—including all six stewardship areas, ACD-01, revised CAT-02, GEO-01 and S1–S3—is approved. This document is the UPDATED RECONCILIATION PACKAGE but is not evidence that implementation has been checked. The execution matrix remains the finite coding parent artifact and must receive a verified overlay before changes. V2 closure requires actual evidence across all terminal IM dispositions, seven journeys, five epics and release gates.