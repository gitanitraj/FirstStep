# First Step v2 — Authoritative IM-01–IM-14 Execution Matrix

**Status:** Reconciled authoritative coding baseline — October 15, 2026  
**Change control:** Preserves every existing IM identifier, direct dependency, implementation class, evidence record requirement, and package closure gate. Incorporates approved Five Historical Questions and full P5 featured-selection alignment (including R6c/R6d). No change to Completion Contract.

## Frozen governing rules

1. Completion Contract is the sole authoritative v2 Definition of Done; new discoveries enter only if required to meet an existing obligation or by explicit amendment.
2. P4 is the v2 scope constraint and P5 is approved logical architecture; implement, validate, and correct narrowly rather than reopen design.
3. Identity, classification, and resident projections remain separate. Fixed ContentTypes: RESOURCE, NEWS, FLYER, LAW, EXPERT.
4. Every normalized CivicContent belongs to exactly one Community; all resident selection is scoped to a Community, but launching another city is deferred.
5. Implement help-first/topic selection and TIMELY/DURABLE/VISUAL item positions as approved in P5. No AI or engagement ranking.
6. Reuse where correct: inspect and verify before changing code. Never pull V3 operations, dashboards, notifications, personalization, database migration, or generalized graphs into v2.
7. Package `Satisfied` requires logged acceptance evidence; documented historical resolutions are not implementation evidence.

## Dependency spine (unchanged)

**IM-01 → IM-02 → IM-03 / IM-04 → IM-05 → IM-06 / IM-07 → IM-08 → IM-09 → IM-10 → IM-11 / IM-12 → IM-13 → IM-14**. Direct dependencies in the matrix control parallelism.

## Consolidated package matrix


| ID | Work package | Depends on | Execution class | Required deliverables | Acceptance tests / evidence | Authority references |
|---|---|---|---|---|---|---|
| IM-01 | Central expiry state & resident eligibility | Approved P4/P5 baseline | Implementation Required | Central lifecycle calculation for ACTIVE, APPROACHING_EXPIRY, EXPIRED; separate expiry state, current-availability eligibility, and editorial-attention determination; preserve temporal-event facts separately. | ACTIVE eligible where otherwise qualified; APPROACHING_EXPIRY remains eligible and reportable; EXPIRED removed only from applicable current-availability surfaces and retained in catalog; absent expiry follows approved type-specific rule. Date-boundary, frozen-clock, and per-projection tests. | P4-20–P4-23; P5 §§9–11, 29; J6/E4; closure backlog; NP-001/002/006 as candidates for data-backed tests, if still present. |
| IM-02 | Expiry diagnostics & Admin-facing attention signal | IM-01 | Implementation Required | Repeatable report of approaching-expiry and expired items; Admin-facing human guidance, no workflow infrastructure. | Report flags approaching expiry without removing the record from resident delivery; expired records identifiable; deterministic output; no dashboard, queue, assignment, email, or article creation. | P4 expiry stewardship; P5 §§10, 28; J6/E4; V3 exclusions. |
| IM-03 | Expert/FAQ category-integrity enforcement | IM-02 | Implementation/Data Correction Required | Inspect and correct real Expert/FAQ category classifications and enforce controlled validation. | Category-scoped Expert/FAQ must have explicit valid Category; Topic only required for Topic-scoped surface, and Topic parent must match Category. Missing category never inferred from title, body, producer, or placement. Positive/negative fixtures and corrected record tests. | P5 §§5,16,29; carried issue; J2/J4/J7; E5. |
| IM-04 | Flyer / Get Help dual-projection rule | IM-02 | Verification First — Bounded Correction If Needed | Verify or implement same-identity membership in Flyers and Get Help with explicit help/access semantics. | FLYER is sufficient for Flyers but not Get Help. A non-RESOURCE item enters Get Help only with explicit controlled actionable-help semantics; no headline/topic inference, duplicate CivicContent, or sixth type. Respect IM-01. | P5 §§12–15, R2; J1/J4/J7; E2/E3/E5. |
| IM-05 | Topic sub-destination & All-[Category]-Help browsing | IM-01, IM-03, IM-04 | Implementation Required | Topic sub-destination and broader eligible help inventory within approved Category → Topic classification. | Resident navigates Housing→controlled Topic→eligible help results and All Housing Help; no taxonomy explosion, organization dump, or fabricated membership; correct expiry, Community, and classification scope. | Decision 051; P5 §§2,5,12–13; J1/J2; E2. |
| IM-06 | Deterministic featured-topic selector | IM-01, IM-03, IM-05 | Implementation Required | Select up to four derived featured Topics within established Community and Category. Rank resident-actionable inventory ahead of contextual law/news; implement full approved tie hierarchy without persistent featured flags. | Reproducible ranking: (1) strength/count of qualifying resident-actionable inventory, (2) TIMELY/DURABLE/VISUAL role completeness, (3) strength of direct-action opportunities, (4) explicitly Topic-classified meaningful law/policy context, (5) catalog/approval newness, (6) stable Topic key, (7) Admin diagnostic for technical fallback. RSS headline alone confers no Topic membership; expired records excluded from applicable inventory. Tests include context-rich but help-poor Topic, complete tie, and Community scope. | P5 §§18–19, 28–29; J2; E2/E5; approved P5 featured-topic alignment. |
| IM-07 | Deterministic three-position featured-item selector | IM-01, IM-03, IM-04, IM-05 | Implementation Required | Fill TIMELY/DURABLE/VISUAL semantic positions using controlled metadata; missing-role fallback R6a, same-role R6b, timely hierarchy R6c, multi-role R6d; diagnostics. | Eligibility→role qualification→maximize distinct role coverage→within-role tie-break→presentation. Direct-help timely > contextual timely > stewardship timely; relevance→meaningful temporal distinction→catalog/approval newness→stable immutable fallback→diagnostic. No repeated item in a Topic column; fallback never impersonates missing role. Approaching expiry is stewardship signal, not automatically stronger than an actionable deadline. | P5 §§20–28, R6a–R6d; J2; E2/E4/E5. |
| IM-08 | Category Destination integration | IM-05, IM-06, IM-07 | Implementation Required | Connect featured Topics, Get Help, What to Know, Organizations, context rail, and direct item links at /category/:key. | Approved resident hierarchy renders on mobile and desktop; bounded previews; source navigation works; Organizations derive from PROVIDER rather than HOST or ContentSource; no retired three-pillar navigation. | Decisions 051/052; P5 §§12–17; J1–J4; E2/E3. |
| IM-09 | Controlled civic-information collections & projection contracts | IM-08 | Implementation / Verification Required | Verify Policy Info, Law, Flyers, First Rate News as controlled projections; preserve provenance and public DTO contract. | Correct scoped membership; inclusive Originals+Expert definition; no new ContentType; unknown metadata creates no new collection; attribution traceable, editorial/review internals not exposed in resident DTOs. No universal Citation entity required. | P5 §§2,6,12,17; historical Citation resolution; J4/J7; E3/E5. |
| IM-10 | Community Events architectural generalization test | IM-09 | Verification First — Bounded Correction If Needed | Exercise United Neighbors 2026 community-produced event with flyer media and controlled EVENT kind. | One CivicContent identity can appear in Events and Flyers; temporal availability follows endsAt or local-date rules, not arbitrary invented duration; independent editorial expiry; Community and ContentSource preserved; HOST not assumed PROVIDER; no sixth ContentType. | P4-17; P5 §§7,9,11,13; J4/J7; E3/E5. |
| IM-11 | Real-catalog selector, collision & regression validation | IM-06, IM-07, IM-10 | Verification Required | Run both selectors on current Housing inventory; capture actual collisions as regression fixtures. | Reproducible topic/item choices including P5 full topic ordering and R6a–R6d; stable technical fallback and Admin diagnostics; ordinary data issues do not reopen P5; real fixtures preserved. | P5 §§19–28,34; J1/J2/J7; E2/E5. |
| IM-12 | Identity, provenance, public-boundary & community audits | IM-09, IM-10; implementation packages sufficiently complete to audit | Verification Required | Audit Community cardinality, scoped retrieval, source attribution, identity, PROVIDER/HOST, and public DTOs; bounded corrections only. | Every normalized CivicContent belongs to exactly ONE Community; delivery establishes Community scope before selection; no new Wilmington-only assumptions; demonstrate same application architecture can scope another Community without launching one. Traceable source provenance required but universal Citation class not required. Offering/contact information not incorrectly promoted to Organization-wide information; no internal editorial fields leaked. | P5 §§3,29; historical Community/Citation resolutions; J3/J5/J7; E1/E5. |
| IM-13 | Seven-journey release regression & quality gates | IM-01–IM-12 | Verification Required | Re-run J1–J7 with backend/frontend tests, validators, accessible desktop/mobile behavior, provenance and Community partition checks. | Each journey supported by concrete passed evidence; multi-community-capability test not second-community launch; source traceability; practical usable performance at accepted form factors, no invented numerical budget; accessibility, high-contrast, keyboard, error/loading and existing release gates pass. | Completion Contract J1–J7/E1–E5; all five historical resolutions; P5 §§29–31. |
| IM-14 | Documentation reconciliation & v2 closure | IM-01–IM-13 | Final Closure Package | Reconcile all package records and authoritative contract, P4/P5, five-question resolutions, backlog, Admin expiry guidance, V3 exclusions. | All IM dispositions backed by evidence; J1–J7 and E1–E5 closed; record Community foundation satisfied/audit performed and second-community launch deferred; Citation class superseded while provenance verified; Seniors illustrative; no independent numerical performance budget; historical eight-point DoD superseded. No unresolved historical Verify blocker or parallel Definition of Done. | Completion Contract; five historical resolutions; P4/P5; IM evidence log; V3 park list. |

## Historical resolution closure crosswalk

| Resolved historical question | Disposition | Retained evidence / package |
|---|---|---|
| Community cardinality and non-Wilmington promise | First-class Community foundation satisfied; exactly-one Community invariant; no Wilmington-only regression; second launch and Community administration deferred | IM-05/06/09/10 community-scoped behavior; IM-12 identity/partition audit; IM-13 regression; IM-14 disposition |
| Literal Citation obligation | Traceability / attribution remains required; universal `Citation` class superseded, generalized infrastructure deferred | IM-09 public collections; IM-12 provenance audit; IM-13 regression; IM-14 record |
| Seniors derived discovery | Illustrative capability, not an eighth journey, taxonomy addition, or separate surface | IM-05/12 verify no erroneous invented taxonomy; IM-14 historical closure; no new coding package |
| Formal performance budget | No substantiated independent numeric gate; usable resident journeys and standard quality gates remain mandatory | IM-13 and IM-14 only; do not invent Lighthouse/latency thresholds |
| Eight-point historical DoD | Superseded by approved Completion Contract; no parallel checklist | IM-13 evidence; IM-14 reconciliation |

## Featured-topic and item P5 reconciliation

**IM-06:** Earlier text suggesting simple *eligible content counts* was incomplete. The authorized hierarchy is resident-actionable inventory → role completeness → direct-action opportunities → explicitly classified meaningful law/policy context → approval newness → stable Topic key → diagnostic. This is a clarified acceptance criterion, not a new dependency.

**IM-07:** Role coverage takes priority: maximize distinct TIMELY/DURABLE/VISUAL positions without duplicates, then apply R6b collisions. R6c distinguishes direct-help timely > contextual timely > stewardship timely. R6d resolves one item qualifying for multiple positions. If a role is absent, R6a fills deterministically without relabeling semantics.

**IM-11:** Real Housing test fixtures prove the complete sequence, including collision/role overlap and stable fallback.

## Preserved journey/epic crosswalk

| Journey | Required IM evidence |
|---|---|
| J1 — Resident finds help via Category Destination | IM-01, IM-04, IM-05, IM-08, IM-11, IM-13 |
| J2 — Resident explores featured topic | IM-01, IM-03, IM-05, IM-06, IM-07, IM-08, IM-11, IM-13 |
| J3 — Resident navigates offering/provider | IM-08, IM-12, IM-13 |
| J4 — Resident discovers civic information | IM-03, IM-04, IM-08, IM-09, IM-10, IM-13 |
| J5 — Editor updates offering without false identity | IM-03, IM-12, IM-13 |
| J6 — Editor handles approaching expiry | IM-01, IM-02, IM-07, IM-13 |
| J7 — Catalog integrity and classification | IM-03, IM-04, IM-06, IM-07, IM-09, IM-10, IM-11, IM-12, IM-13 |

| Epic | Principal packages |
|---|---|
| E1 — Civic Catalog Identity & Relationships | IM-04, IM-10, IM-12 |
| E2 — Resident Help & Topic Discovery | IM-04, IM-05, IM-06, IM-07, IM-08 |
| E3 — Civic Information Collections | IM-08, IM-09, IM-10 |
| E4 — Content Lifecycle & Editorial Stewardship | IM-01, IM-02, IM-07 |
| E5 — Catalog Integrity & Classification | IM-03, IM-04, IM-06, IM-07, IM-09, IM-10, IM-11, IM-12 |

## Mandatory package evidence contract (unchanged)

Every package agent report records: **package identifier; starting disposition; inspected repository files/classes/data/tests; demonstrated gap (or none); bounded code/data/document change (or none); named tests added/changed; exact commands and suite results; clause-level acceptance mapping; J/E crosswalk; architecture/V3-boundary assessment; ending disposition; and evidence references (diff, commit, test output, document section).** Compilation alone never satisfies an IM package.

Permitted planning/execution dispositions: Implementation Required; Implementation/Data Correction Required; Verification Required; Verification First — Bounded Correction If Needed; Satisfied; Superseded/Reinterpreted; Deferred; Final Closure Package.

Agent protocol: **inspect → verify → identify gap → minimum authorized change → regression tests → broad checks → evidence/disposition.** Material missing domain concepts are escalations; mundane ties, bad metadata, temporal boundary cases, or projection overlaps are not.

## Closure gate

After IM-01–IM-13 have evidenced terminal dispositions, IM-14 must reconcile Completion Contract, approved P4/P5, five closed historical questions, Open Required Work Closure Backlog, Admin expiry guide, journey/epic evidence and V3 deferrals. Only then may v2 be declared complete.
