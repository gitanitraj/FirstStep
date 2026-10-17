# First Step v2 — IM-11 Coding-Agent Work Order

**Package:** IM-11 — Real-catalog selector, collision & regression validation  
**Disposition:** **Verification Required — authorized next**  
**Authority:** Authoritative IM-01–IM-14 Matrix §3, §14B; approved v2 Completion Contract; approved 2B-P4/2B-P5 §§18–28 and R6a–R6d; Featured-Topic Architecture Resolution; Decisions 051/052; accepted IM-01–IM-10 execution and editorial decisions.  
**Journeys:** J1/J2/J7; **Epics:** E2/E5.  
**Start condition:** IM-06/IM-07/IM-10 accepted. IM-08–10 composition and collection changes are part of the then-current runtime. IM-12 may be independently scheduled but is not silently completed here.

## 1. Authorized objective and limits

**Verify, do not redesign**, the frozen seven-stage IM-06 featured-Topic selection and IM-07 maximum-distinct-role featured-Item selection on the **then-current real catalog** after IM-08, IM-09 and IM-10 changes. Produce reproducible, record-level actual selector/collision and downstream Category Destination evidence and regression fixtures. Repair only a demonstrated v2 contract failure within an approved rule. New data anomalies, ties, empty semantic roles, and post-event Flyers alone are **not** permission to reopen P5 or add catalog fields, scores, flags, editorial approvals, or V3 systems.

**Acceptance is not another implementation of IM-06/07.** Report unchanged successful verification as a valid completion outcome; do not pad the diff to create work. IM-11 is not a complete RSS inventory audit, independent source fact-check, provider graph audit, second-Community launch, release/WCAG certification, or final v2 closure.

## 2. Inspect-first and protected baseline

1. Read `claude.md`, the latest supplied matrix including §14B, this order, P5 §§18–28 / R6a–R6d, Architecture Resolutions, Decisions 051/052, IM-06 qualification decision, IM-06–10 work/evidence reports, and the accepted three-entry First Rate News decision. Do not substitute an older count-based summary for the approved full seven-stage hierarchy.
2. Capture full branch/HEAD, `git status --porcelain=v1`, current tracked/untracked/ignored relevant files and checksums for original catalogs, taxonomy, Resource/Flyer/News models, selectors, Category composition, `EventOccurrence`, `first-rate-news-membership.json`, fixtures, build manifests and tests. IM-10 reported uncommitted/unpushed files; inspect real state and protect it. No staging, reset, stash, commit, push or catalog rewrite without specific user authorization.
3. Inspect the actual IM-06 `FeaturedTopicSelector`, `FeaturedTopicInventory`, `AuthoredTopicEvidence`, `FeaturedRoleQualification`; IM-07 `FeaturedItemSelector` and acceptance tests; IM-08 `CategoryDestinationService`/API/UI; IM-09 authored LAW proof and admission manifest; IM-10 `EventOccurrence`/Notices/Flyer DTO. Trace Community, lifecycle, source/provenance, category/topic and public projection seams.
4. Execute untouched baseline: repository-specific JDK25 Maven verify/package with required Mockito setup; frontend tests/TypeScript/build; Python unit tests and six catalog validators. Document environment-only workarounds independently of genuine failures. Then capture a **read-only** fixed-clock and runtime-clock candidate audit **before** any change.
5. Write `evidence/im11/inspect-first.md`, `real-catalog-before.json`, `baseline-commands.md`, and an actual candidate table for all controlled Housing Topics, candidate IDs, origin/Community, currentness, authored proof, role evidence, eligibility exclusions, tie stages and selection. Distinguish stored rows from current eligible inventory and contextual from actionable.

## 3. Required unchanged selection contracts

**Featured Topics (IM-06):** only controlled subcategories with positive current **authored, actionable Help** IDs can be featured, at most four. Compare lexicographically: **(1)** distinct help inventory descending; **(2)** maximum feasible distinct-ID coverage of TIMELY/DURABLE/VISUAL; **(3)** distinct approved direct-action events; **(4)** recently published explicitly authored relevant LAW/`policy-update` context; **(5)** genuine catalog/Article approval newness; **(6)** stable canonical Topic slug; **(7)** nonblocking scoped Admin technical-tie diagnostic. Context/new laws cannot outrank help via weighted score. Do not pad zero-help Topics into a four-slot selection. No keyword-derived LAW/Topic proof.

**Featured Items (IM-07):** admit only current scoped authored classified content already eligible through the existing controlled projections, with canonical ID deduplication and no invented content/provider/source. Positive TIMELY: actionable Flyer carrying exact `ACTIONABLE_HELP_ACCESS` and explicit structured event date in `[Community-local today, today+30]`, or explicitly authored LAW/`policy-update` published in `[today−30, today]` as late contextual evidence. Positive DURABLE: current Resource with exact explicit `urgency=standard`; this is **not** proof of permanent service availability. Positive VISUAL: admitted actionable Flyer with genuine original media and safe source evidence. The approved IM-07 service's exact valid metadata requirements control. An item may qualify for multiple roles but may fill **only one**; maximize feasible distinct-role coverage first, then role priority TIMELY → DURABLE → VISUAL, within-role help/direct relevance → meaningful applicable time distinction → true catalog/approval newness → immutable ID → diagnostic. Follow exact feasible-assignment algorithm, not greedy ranking. Fill up to three with distinct admitted candidates as truthful `FALLBACK` positions, not invented roles. Do not let retrieval/publication/expiration or an event date masquerade as catalog newness.

**Occurrence separation (IM-10):** date-only events remain current Events through the owning Community-local date; explicit `endsAt` controls exact inclusion; UNKNOWN occurrence excludes Events only. These event-currentness rules **must not silently become a global filter for Flyer, Get Help, Featured Topic/Item, Policy Info, Article, or contextual Flyers**. Likewise, no past/concluded Flyer automatically becomes actionable Help or qualified TIMELY. If a test demonstrates an interaction, prove the existing authorized event-date TIMELY qualification and current Help eligibility separately; do not invent a second cutoff or infer timing from prose. IM-01 expired content is excluded from current projections; approaching expiry remains eligible without boosted role.

**Editorial/source/community:** preserve approved Article publication guard, IM-09 exact Wilmington Housing First Rate News admissions (OR-003 plus manifest FAQ-002/EA-001/EA-005), independent supporting collections, source versus Organization separation, precise Community ownership and controlled Category/Topic authored proof. No real Flyer has yet been assigned `helpAccess` (as of IM-10: 0/8). Do not modify those eight production records to improve coverage.

## 4. Real catalog audit and collisions

At minimum audit **every** controlled Wilmington Housing Topic and a non-Housing Category and second Community scope. Recompute all counts at an explicitly frozen, injected Clock **and** a clearly separated real-current snapshot; compare against accepted IM-06/07/08 fixed-clock baseline (`2026-10-16T16:00:00Z`) before asserting changes. Baseline expectation **for that frozen instant**, subject to verified same-catalog facts: Topics: Transitional Housing 11, Emergency Shelter 9, Homeownership 4, Rental Assistance 4; remaining Housing Topics audit includes Sober Living 3, Senior Housing 2, Youth Housing 2, Public Housing 1, Eviction Prevention 0. Distinguish Wilmington's 36 current help IDs from older unscoped 44-resource counts. Do not hard-code historic counts into production.

The accepted IM-07 fixed-clock **baseline item outcomes**, for comparison, are:

| Topic | Qualified winner | Truthful fallback 1 | Truthful fallback 2 |
|---|---|---|---|
| Transitional Housing | DURABLE HA-002 | HA-003 | HA-005 |
| Emergency Shelter | DURABLE HA-016 | HA-001 | HA-006 |
| Homeownership | DURABLE HA-011 | HA-013 | HA-021 |
| Rental Assistance | DURABLE HA-008 | HA-023 | HA-026 |

Record **why each selected identity wins** with exact comparator stage, underlying explicit role/temporal/newness evidence, next eligible rival and technical fallback diagnostics. Existing real Homeownership DURABLE tie HA-011/HA-013/HA-021/HA-022 requires stable-ID evidence, not a fabricated timestamp. Audit other Topic/item collisions and ties across/failing the top-four cutoff, empty/missing roles, a candidate eligible for more than one role, weak/missing source/media/urgency/createdDate, and approach-versus-expiry. Use actual production collisions for persisted test fixtures where possible, synthetic separately labeled when production lacks a relevant positive multi-role/TIMELY/VISUAL example.

Record how IM-09 Law authored-proof changes impact Topic/context selection and how IM-10 event conclusion affects **Events only**. `FL-008` (date-only October 17) may remain a contextual Flyer past local midnight; no missing `helpAccess` may be inferred. The original `FL-002` must remain contextual Housing Flyers, not Housing Help. Maintain lawful empty state and no remote RSS completeness claim.

## 5. Regression/acceptance matrix — IM11-T01–T20

| ID | Required executed proof |
|---|---|
| T01 | Audited true current repository HEAD, dirty/untracked state, actual authoritative versions, full protected hashes, untouched baseline and preservation. |
| T02 | Entire real Wilmington Housing controlled Topic inventory and current actionable IDs: exclude contextual records from stage-one count and explain deviations from frozen baseline. |
| T03 | Seven-stage Topic comparator ordering, full tie and cutoff diagnostics, stable Topic slug; shuffles/repeats give same winner/order/evidence. |
| T04 | All four real featured Topics selected (or evidence-grounded new winners), exactly ordered; positive-help and no zero-help padding. |
| T05 | For all four selected Topics, actual ordered up-to-three unique item IDs and **genuine** roles versus FALLBACK, plus provenance/navigation references. |
| T06 | Real same-role collision fixture (at least Homeownership four DURABLE candidates) proves accepted relevance/time/newness/ID fallback and internal diagnostic. |
| T07 | Additional real Topic/item technical ties, including top-four cutoff and multiple fallback candidates, remain stable across shuffled input. |
| T08 | Global distinct-role feasibility, not greedy assignment; synthetic multi-role Flyer moves to VISUAL when an independent TIMELY candidate maximizes role count; one ID in one slot. |
| T09 | Direct TIMELY explicit actionable Flyer and Community-local today/day+30 boundaries; missing token/eventDate, yesterday/day+31 have no positive TIMELY qualification. |
| T10 | Contextual TIMELY original authored LAW/`policy-update` with [today−30,today] publication, contextual lower precedence, no RSS keyword relevance; no new ContentType. |
| T11 | Exact explicit `urgency=standard` DURABLE; no missing/alternate urgency, undated Resource, retrieved date, or expired record promoted. |
| T12 | VISUAL requires authorized actionable Flyer, real original media and safe source facts; all 8 unchanged unactivated production Flyers remain contextual where eligible. |
| T13 | Fallback fills up to three distinct eligible items, honestly marked FALLBACK in public contract, with no false semantic role badges; fewer-than-three behavior and canonical-ID dedup. |
| T14 | Real Category aggregate passes through Topic+Item selectors without resorting; same 12 selected IDs/roles on actual HTTP/API/UI for frozen snapshot, unless documented catalog change. |
| T15 | IM-10 real FL-008 date-only midnight and synthetic explicit-end tests show Events conclusion without automatic Flyer/help/selector deletion or retroactive TIMELY promotion. |
| T16 | Editorial expiry state and independent Community zone/scope: expired excluded; approaching eligible; two Communities isolated; correct DST/calendar boundaries. |
| T17 | IM-09 authored LAW proof and approved Article/FAQ/Expert membership remain independent of featured Help counts, identity and supporting projections; News/Flyer/Article public DTO guards. |
| T18 | Missing dates, source, media, approval, Topic proof and unknown event occurrence fail conservatively with deterministic diagnostic; no inferred facts/newness. |
| T19 | Full backend `verify` and executable package, frontend tests/TypeScript/build, Python tests, six validators, focused packaged HTTP and built-app browser smoke where touched (375/1280 keyboard/scoped navigation); explicit results and known limitations. |
| T20 | Compare all protected files and complete annotated source/test mirrors where edits occur, no unsupported model/taxonomy/approval/Flyer mutation, and a complete evidence-to-acceptance map. |

## 6. Narrow correction and escalation rules

If tests show a real IM-11 breach, first isolate whether it is an incorrect **test assumption**, a data-quality problem, a previously accepted boundary not exercised on current records, or a real selector/projection defect. Keep a failing-before and passing-after test with minimally scoped production correction. Existing admissibility services and immutable selection evidence are authoritative. Do not change the approved P5 comparison order, add new entity/status/score, infer class membership, mutate original Flyers, or shift event occurrence into editorial expiry. An ordinary tie, missing role/date, optional current-feed gap, or limited selection is **not** a schema blocker. Escalate only a **genuinely necessary, unrepresentable v2 fact**, with exact record/contract evidence and smallest decision requested.

**Do not begin:** IM-12 complete provider/HOST/public-boundary audit, IM-13 full J1–J7 release/WCAG proof, IM-14 guide and deployment reconciliation. You may record cross-package findings for their handoff, without marking them satisfied. No Admin UI/notifications/queues, personalization, AI relevance score, publication automation, database change, second-community launch, new EVENT ContentType or generalized event system.

## 7. Required evidence and execution report

Create `docs/v2-completion-plan/evidence/im11/` with inspect-first analysis, baseline commands/full logs, actual stored/current audit tables, selection evidence with exact IDs and role comparators, collision fixtures/tests, fixed and runtime-clock results, before/after differences if changed, public integration/scope checks, final command exits, preserved hashes, and complete annotated mirrors only for touched source/test files. Name and run all IM11-T01–T20 scenarios; cross-reference J1/J2/J7 and E2/E5. Avoid screenshots as a substitute for machine-readable selection evidence.

**Mandatory report headings:** Starting state and authorities; untouched baseline and gap finding; exact real catalog/Community/Topic inventory; selector evidence and collisions; IM-09/IM-10 interaction regression; changed files or verified-no-change; acceptance mapping T01–T20; actual test/build/validator/HTTP/browser gates; preservation and provenance; limitations and IM-12/13/14 handoff; proposed final disposition. Record commit status, full HEAD, commands, expected vs actual values, failures and reruns, not simply “all passed.”

**Done when:** all approved selection and composition contracts are empirically verified against the then-current real catalog, representative genuine collision fixtures are locked as regressions, documented limitations remain honest, quality gates are green, and any needed bounded correction is separately evidenced. Submit the execution report for review **without self-editing the authoritative disposition**. Do not commit or push unless separately instructed. No v2 closure declaration.
