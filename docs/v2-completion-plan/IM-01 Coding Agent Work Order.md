# First Step v2 — IM-01 Coding-Agent Work Order

**Status:** ISSUED FOR EXECUTION (not evidence of completion)  
**Date:** October 15, 2026  
**Package:** IM-01 — Central Expiry State & Resident Eligibility  
**Primary scope:** approved editorial expiry only.  
**Required references:** v2 Completion Contract; approved 2B-P4 scope; approved 2B-P5 Logical ER + Classification Schema; authoritative IM-01–IM-14 Execution Matrix; Approved Architecture Resolutions; **IM-01 Readiness Decisions PC-01–PC-04**; October 15 IM-00 Readiness Report; First Step Admin Guide — Expiry Eligibility and Editorial Stewardship.

## Copy/paste instruction to the coding LLM

You are executing **IM-01 only** for First Step v2. This is authorization to make the minimum code, focused test, and documentation changes that satisfy the approved IM-01 requirements. Do not perform a new planning pass or reopen P5. Inspect the repository first; file locations and baseline findings from IM-00 are leads to verify, not substitutes for inspecting the current checkout. Do not claim a passing result until you actually run the command.

### 1. Nonnegotiable decisions

**PC-01 — editorial expiry:**

- Compute ACTIVE, APPROACHING_EXPIRY, EXPIRED from **editorial `expirationDate`** only, and separately answer whether the item passes editorial expiry eligibility. Other independent eligibility gates remain intact.
- APPROACHING_EXPIRY starts **30 calendar days before the inclusive expiration date**, through the date itself; at the **start of the next local date** the item becomes EXPIRED. Earlier dates are ACTIVE.
- Interpret date-only expiry in the owning Community's timezone; Wilmington uses **`America/New_York`**, not the system-default zone. Support different Community configuration without a second Community rollout. Use injectable `java.time.Clock` and a configurable 30-day window.
- Missing editorial expiry **does not itself exclude** RESOURCE, NEWS/Policy Info, FLYER, LAW, EXPERT/FAQ, EXPERT/Expert Answer, or approved First Step Article (NEWS subtype). Do not infer editorial expiry from `eventDate`, `sessionDate`, `publishDate`, audit dates, `retrieved`, RSS publication age, or other fields. Missing expiry does not imply freshly verified information.
- Explicit inactive/withdrawn status, article review/approval gating, classification and other independent restrictions still apply. Expired records are **retained** in catalog/history, but excluded from applicable **current-availability** resident projections. Counts that mean currently discoverable inventory must reflect the filtered set. Do not equate stored catalog counts with current resident counts.
- Apply one shared editorial eligibility interpretation to existing resident delivery surfaces that advertise currently available content: Category Destination Get Help/What to Know; Topic and All-[Category]-Help; Featured Topic/item candidate inventories where already implemented; homepage previews; Find Help; current Search; Latest Updates; Community Notices; current Organization offerings; and current navigation/collection counts. Verify whether each exists and is wired today. If a surface is not yet implemented under a later IM package, provide a tested reusable contract and record its deferred integration owner; do not pre-build IM-05–IM-09.

**PC-02 — event occurrence is separate:**

- **IM-01 must not implement event-occurrence eligibility or mass-expire historical flyers**. Event occurrence is assigned to **IM-10**, following P5-R5b (explicit `endsAt`, date-only event, start without end, unusable event date). A concluded event loses current-event eligibility without changing the record's editorial lifecycle state. Do not mark FL-001–FL-007 editorially expired merely because the event date passed.
- Preserve the distinction between editorial state and future combined projection eligibility. Avoid asserting that current-event presentation is complete before IM-10.

**PC-03 — known failing backend baseline:**

- The user **already confirmed** FL-008 as intentional and the relevant catalog classification/association facts; do not seek confirmation again.
- IM-00 observed `mvn test`: **371 tests, 2 failures** in `EditorialStabilityTest`, due to FL-008 raising Community Support **61 → 62** and stored total **238 → 239**. These are authorized assertion updates, *not* authorization to change the catalog to fit the assertions.
- Capture initial branch/HEAD/working-tree status, and the original failing output if reproducible. Reconcile only the two demonstrated pinned stored-count assertions; run `mvn test` again and log its result **before expiry changes**. If more or different failures exist, report them accurately and distinguish them from the IM-00 baseline rather than broadly rewriting tests.
- Preserve FL-008 and all unrelated preexisting uncommitted edits. Do not reset, stage, delete, reclassify or commit unrelated work.

**PC-04 — stewardship ownership:**

- Do **not** perform the 99-rule audit during IM-01. The complete original-rule coverage register/R5 audit belongs to **IM-12**; release regression to IM-13; final reconciliation to IM-14. Observe applicable existing rules; do not invent 99 coding tasks or new IM packages.

### 2. Execute in this sequence

**A. Inspect and record the baseline.** Record branch, HEAD, uncommitted/tracked/untracked changes, data files, lifecycle fields, model/repository loading paths, services doing current resident delivery, existing status semantics, source/community scoping, and tests/validators. Pay particular attention to `CivicContent`, `JsonNewsRepository`, `CategoryContentService`, `UpdatesService`, `CommunityNoticesService`, `SearchService`, `HomeService`, `FlyerService`, `FindHelpService`, `NavigationService`, and `EditorialStabilityTest`; names are exploratory pointers, not a directive to edit every file. Determine whether type/status/date/Community contracts require a narrow adaptation rather than assuming all subtypes share fields.

**B. Repair the FL-008 stored-count baseline only.** Preserve evidence of the originally failing assertions, make the two authorized pinned-count corrections when confirmed against current checkout, then run the entire backend suite. Record baseline result distinctly from post-IM-01 result. Do not mask novel failures.

**C. Implement one shared editorial lifecycle calculation.** It must be deterministic, Community-zone-aware, Clock-driven, configurable for 30 calendar days, safe for null/missing editorial expiry, and free of event-occurrence inference. Keep the API narrow; don't add a persisted generic `residentEligible` Boolean or rebuild `CivicContent` hierarchies. Use existing metadata whenever possible. Document the convention for the date-only expiry boundary and how the relevant Community timezone is resolved.

**D. Integrate eligibility where IM-01 can safely do so.** Inspect the actual resident projection paths and API/count behavior; apply the shared decision at appropriate common chokepoints without introducing independently coded duplicate rules. Retain historical records and existing editorial approval guards. Make the smallest changes needed to prevent expired records from being offered as currently available. For downstream views not yet built, leave clear contract/tests and record the future package; do not implement featured selectors, redesigned Category UI, complete Community Events processing, or broader community-scoping refactors in this work order.

**E. Test focused behavior and real-data regressions.** Run unit and service/API tests with fixed times, then full backend suite, frontend suite/type check and relevant catalog validators where practical. Record command, actual pass/fail count, and failure causes. Do not amend fixture expectations just because expiry changes resident-visible counts unless the assertion is explicitly supposed to represent currently eligible inventory.

### 3. Mandatory acceptance tests

1. ACTIVE before the 30-day boundary; APPROACHING_EXPIRY at the exact start of the 30-day window; APPROACHING_EXPIRY throughout the inclusive editorial expiration date; EXPIRED starting at 00:00 on the following local date.
2. Deterministic `Clock` tests in `America/New_York` across relevant date and daylight-saving boundaries; at least one different configured Community timezone fixture, without launching a second Community.
3. Null/missing `expirationDate` on each relevant content class does not itself result in editorial-expiry exclusion, nor fabrication of a date.
4. Existing explicit inactive/withdrawn and approved-article publication restrictions remain enforced independently.
5. Expired items are excluded from current resident collections and current discoverability counts, without being removed from the underlying stored catalog; approaching-expiry items remain eligible.
6. At least regression tests using currently expired real Policy Info fixtures **NP-001, NP-002, NP-006**, if still present, for Housing Category and Updates where applicable. If absent, report the replacement evidence rather than asserting them without inspection.
7. Distinguish stored-count pins (239 overall and 62 Community Support if current repository still matches IM-00) from dynamic eligible-count assertions. The pre-IM-01 pinned-count fix and post-IM-01 current-count changes have separate evidence.
8. A prior `eventDate`/`sessionDate` is not automatically mapped to `expirationDate` or editorial state EXPIRED. Do not claim current-event occurrence tests completed; assign to IM-10.
9. No additional ContentType, generalized event model, workflow engine, Admin dashboard, global validity-by-age heuristic, or P5 architectural redesign.

### 4. Evidence return contract

Return a concise **IM-01 execution report** containing:

- Starting branch, HEAD, and working-tree state; preexisting changes protected.
- Files/classes/catalog data inspected and pre-implementation behavior found.
- Pre-expiry FL-008 baseline assertion changes, named tests, `mvn test` result, and whether any baseline failure persists.
- Exact production files and tests changed for expiry; explanation of shared calculation, Community zone, Clock, threshold configuration, and each updated resident projection/count.
- Acceptance-test mapping for items 1–9 with names and pass/fail evidence; commands run and full-suite counts; real NP fixture outcomes if available.
- Surfaces that remain unimplemented or await downstream package integration, with exact owner (IM-02, IM-05–IM-09, IM-10, IM-13, IM-14) and reason.
- Evidence of no P5 redesign, V3 pull-forward, flyer event-date mass expiry, or unintended stored-record deletion.
- Ending IM-01 disposition: **Satisfied**, **Partially satisfied / outstanding bounded items**, or **Escalation required**, with evidence. Never mark Satisfied based only on successful compilation.

### 5. Stop and escalation conditions

Stop only the affected portion and report the evidence if (a) the latest repository contradicts the confirmed FL-008 count attribution; (b) a v2 requirement truly cannot be represented without violating an approved P5 invariant; (c) an unrelated failing baseline cannot be distinguished from the work; or (d) fixing a service would require a V3 capability or another IM package's substantive functionality. Continue independent, within-scope IM-01 work where safe. **Do not request already-given approvals or silently resolve architecture conflicts.**

**Completion rule:** IM-01 is accepted only upon a supported terminal disposition, passing applicable evidence gates, and explicit cross-reference to the approved PC-01–PC-04 decision record. This work order does not itself establish implementation success.
