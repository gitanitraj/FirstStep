# First Step v2 — IM-07 Coding-Agent Work Order
## Deterministic Three-Position Featured-Item Selector

**Status:** AUTHORIZED FOR IMPLEMENTATION — evidence-gated acceptance  
**Package:** IM-07  
**Execution class:** Implementation Required  
**Dependencies:** IM-01, IM-03, IM-04, IM-05 — accepted; **IM-06** now also accepted and available for reuse  
**Issued:** October 17, 2026  
**Authority:** Approved Completion Contract; authoritative IM-01–IM-14 matrix and IM-06 closure addendum; approved 2B-P4/P5 (especially §§20–28; R6a, R6b, R6c, R6d); Architecture Resolutions on featured selection; Decision 051 Pass 2A; approved IM-06 bounded qualification decision; IM-01–IM-06 execution reports. The specific approved P5 rules control over abbreviated matrix language.

---

## 1. Mission and exact boundary

Implement a **reusable, deterministic, auditable selector returning up to three distinct CivicContent items for a controlled Topic in a specified Community and Category**, with semantic positions **TIMELY**, **DURABLE**, and **VISUAL**. Maximize feasible coverage of distinct semantic roles before ordinary within-role ranking; when roles cannot all be filled, use approved deterministic remaining-inventory fallback **without relabeling fallback items**. Reuse proven eligibility, authored classification, Community scoping, and IM-06 item/role evidence where appropriate.

**IM-07 selects the items, not the featured Topics.** IM-06 already chooses up to four Topics; IM-08 later projects the Topic and item selections into the full Category Destination and source navigation UI. No new page, permanent `featured` field, workflow, or generalized relevance engine is authorized here.

## 2. Confirm real starting state — mandatory

1. Read `claude.md`, current matrix/IM-06 closure, P5 §§20–28 and P5-R6a–R6d, Architecture Resolutions, the IM-06 Qualification Decision and IM-06 Execution Report. Inspect actual branch, HEAD, status and diffs **before changing anything**. The IM-06 report says its selector files and two corrected frontend timing tests were uncommitted/unpushed; verify instead of assuming. Hash/protect every preexisting change and do not overwrite any earlier work.
2. Inspect `FeaturedTopicSelector`, `FeaturedTopicInventory`, `FeaturedRoleQualification`, `AuthoredTopicEvidence`, result/diagnostic contracts, `CategoryContentService.helpFor`, `forTopic`, category/topic taxonomy, source repositories, all resident current-inventory services, lifecycle/status/article-approval restrictions, Flyer `helpAccess` and original media/source fields, Community timezone configuration, and existing public DTO boundaries.
3. Inspect actual structured timestamps/urgency/other metadata usable for **role relevance**, meaningful temporal distinctions, authentic catalog/approval newness and a stable immutable item ID. Identify evidence that is missing; distinguish editorial expiry from resident event/deadline facts. Confirm whether a real Item's Topic/Category proof is authored or merely runtime keyword inferred.
4. Run and record baseline backend verify, frontend tests/typecheck/build, Python tests and all six catalog validators. Audit real Housing candidates for all nine controlled Topics and the four IM-06 selected Topics, with at least one other Category and an additional Community fixture. Separate stored records, current eligible items, actionable help, contextual items, explicit role evidence, and catalog-only/context-only records by canonical ID.
5. Produce an inspect-first gap map and the smallest bounded design **before production changes**. Reuse IM-06 qualification and metadata adapters only where their semantics are fit for IM-07; do not assume those intentionally conservative IM-06 proxies form a complete definition of item roles.

**Stop and request a narrowly framed decision only if a genuinely necessary positive semantic cannot be represented or safely interpreted using approved metadata.** Do not invent calendar windows, service durability, provider relationships, approval facts, or publication timestamps. Missing positive evidence means a role is not established, not that the item should be reclassified or excluded from unrelated resident views.

## 3. Required selection inputs and output

Input must establish **nonblank explicit Community, controlled Category, controlled child Topic**. Existing Community and Category/Topic canonical semantics govern; wrong parent/unknown key yields a documented stable empty/invalid result or explicit domain error as consistent with existing internal APIs. No cross-Community aggregation from null Community is permitted for featured selection.

Return an immutable internal selection object with:

- up to three **distinct canonical content identities** and the original content presentation/provenance necessary for eventual IM-08 card navigation;
- **actual semantic role filled** for each qualified role, or `fallback` position with no fictitious semantic role;
- a deterministic explanation of role qualification and tie stage used;
- diagnostics for unfilled roles, metadata limitations, and technical fallback when materially indistinguishable candidates require an immutable key;
- no new public/private editorial fields in resident DTOs, and no persisted presentation state.

The service must be usable for the four IM-06 selected Topics *and* independently for any valid Category/Topic with a qualifying inventory. It must not hard-code Wilmington, Housing, the four Topic names, their fixed-clock counts, or the number of eligible live Flyers.

## 4. Strict candidate eligibility and proof

Selection starts with **current resident-eligible items** under IM-01 lifecycle/status and existing publication/approval contracts; APPROACHING_EXPIRY remains eligible, EXPIRED excluded. Respect controlled Category + child Topic classification, explicit owning Community, approved Get Help admission for Flyers, and provenance/source gates. Preserve Article approval and Expert/FAQ explicit classification protections. Do not generate Topic membership from prose, source, publisher, RSS keyword placement or a broad Category-only relation. Distinguish context-eligible from resident-help-eligible records.

Where contextual LAW/policy information can legitimately participate as a later-position fallback or relevant TIMELY evidence, require explicit authored Topic/Category proof and keep **direct help ahead of context**. An RSS entry classified only by headline keywords cannot become authored contextual relevance. Do not fabricate a provider relationship from a source or host label. Count/select the same CivicContent ID once even if it appears across projections or qualifies for multiple roles.

## 5. Role qualification — approved semantics

**TIMELY:** Meaningful near-term resident usefulness. Direct-help signals (actual structured access event, application/deadline, service availability window) outrank contextual recent law/policy, which outranks stewardship approaching-expiry. **Editorial approaching expiry alone is not a positive resident TIMELY event/deadline**; it remains separately diagnosable via IM-02. The IM-06 approved 30-day local event window (exact-token actionable Flyer) and 30-day recent authored LAW/`policy-update` window are bounded, known-positive evidence usable when applicable. They are **not** event-occurrence exclusion rules or exhaustive TIMELY definitions. Preserve direct-help priority; do not invent dates/durations or infer a deadline from text.

**DURABLE:** Stable, currently useful help content with explicit affirmative structured evidence. IM-06 accepted `urgency=standard` on a current Resource as a conservative positive qualification *proxy*, not proof of everlasting availability or newly verified accuracy. Merely missing a date does not qualify. Do not invent a service availability promise, metadata or evergreen designation.

**VISUAL:** Prefer a relevant, currently eligible FLYER with original accessible media/source context. A Flyer does not thereby enter Get Help: its separate explicit helpAccess gate still applies where used as actionable help. If the intended role considers a **contextual Flyer**, prove Topic/Category/Community/current-projection scope and keep the distinction explicit rather than automatically treating it as an actionable help item. Do not activate any real Flyer; **0/8 production Flyers** have approved `helpAccess` at the last reported catalog snapshot.

Where approved P5 semantics leave an implementation-specific ambiguity (e.g. exactly which contextual Flyers may be selected in a help-first three-item column), **do not silently adopt a novel inclusion rule**. Report concrete candidates and propose the minimal controlling interpretation for editorial authorization if needed. Do not modify the production catalog to make role tests pass.

## 6. Mandatory placement and collision algorithm (P5-R6a–R6d)

**Step A — maximize distinct role coverage.** Evaluate candidate-to-role assignments jointly, not greedily per slot. A single item may qualify for multiple roles, but may occupy only one position. First maximize the number of genuinely filled **TIMELY / DURABLE / VISUAL** positions using distinct IDs. Example: if a Flyer qualifies for both TIMELY and VISUAL and a second distinct TIMELY candidate exists, preserve the Flyer for VISUAL when doing so fills more separate roles. IM-06 `maximumDistinctCoverage` can inform feasibility but is **not** itself the final winner-selection algorithm.

**Step B — choose among feasible assignments.** Within matching coverage, apply the approved **same-role collision ordering** in sequence, not a weighted score:

1. Role relevance, with direct resident-help value and actual role meaning respected.
2. Meaningful temporal distinction among structurally qualified candidates, with **direct-help timely > contextual timely > stewardship timely**; use applicable Community-local dates and explicit evidence, not publication/expiry substitutions.
3. Genuine catalog/approval newness, where independently known and valid; **never** replace it with retrieved date, inferred insertion order, runtime publication-date copying, exposure history or event date.
4. Stable immutable technical fallback (canonical CivicContent identity with a documented invariant total order, never repository iteration order).
5. Internal Admin diagnostic when the choice materially depends on technical fallback instead of a meaningful discriminator; the resident presentation must continue.

For multiple global assignments with equal distinct-role coverage, define and test a deterministic, role-respecting global comparison which does not let selecting one higher-ranked item destroy available distinct role coverage. This is **not** authorization for generalized ranking/scoring. Record the exact comparator and evidence used.

**Step C — missing-role fallback (R6a).** If a role has no distinct qualifying candidate in any feasible assignment, fill up to three total presentation positions with remaining eligible Topic items using deterministic help-first ordering, excluding already chosen identities. **A fallback item must remain labeled fallback/unqualified**, never TIMELY, DURABLE, or VISUAL by virtue of occupying that slot. It is acceptable to return fewer than three when fewer distinct eligible items exist. A context-only item cannot displace available resident-actionable content simply because it is newer; use explicit allowed projection membership and documented ordering.

**Step D — diagnostics.** Emit nonblocking missing-role, metadata insufficiency, same-role technical tie, and collision explanations. Internal reports may contain stewardship metadata; public cards must not leak private review flags or diagnostic internals. No queue/dashboard/notification/override UI.

## 7. Required real-catalog acceptance

Execute the selector against the **then-current** real Housing inventory, with the same fixed clock used for comparability by IM-06 (2026-10-16T16:00:00Z) and a separate controlled date when time-boundary tests require it. Capture:

- all four IM-06 selected Topics: Transitional Housing, Emergency Shelter, Homeownership, Rental Assistance, and their exact winning record IDs/position types;
- all nine Housing Topics for collision/missing-role diagnostics (including zero-help Eviction Prevention without converting a context record to help);
- the distinction between selected authentic role winners and positions filled only by fallback; current selected Topics had **one supported DURABLE proxy role each** in the IM-06 snapshot, not proved TIMELY/VISUAL;
- real equal-role collisions, newness gaps, stable-ID tie resolution, source/provenance/Community continuity and any actual media/link availability;
- effect of the fact that **zero live Flyers have an approved `helpAccess` signal**; no live assignments without a separate editorial decision;
- one additional Category and a synthetic second Community with differing Community-local dates;
- current counts and differences from old IM-05/06 inventory snapshots, **without updating hard-coded count assertions to match an invented story**.

Turn representative **real** tie/collision findings into named automated regression tests or pinned representative catalog fixtures. IM-11 will rerun full real-catalog regression later; IM-07 must not defer all actual inventory evidence to IM-11.

## 8. Minimum acceptance scenarios (IM07-T01–T20)

| Test | Expected evidence |
|---|---|
| T01 | Valid Community/Category/Topic yields at most three distinct items; invalid or unrelated scope never leaks records. |
| T02 | Category-only FAQ/Expert items cannot gain Topic eligibility by inference. |
| T03 | EXPIRED/inactive/withdrawn/unapproved items excluded; APPROACHING_EXPIRY stays eligible without priority boost. |
| T04 | Exact controlled helpAccess required for Flyer participation in Get Help; all eight live Flyers remain unmodified. |
| T05 | TIMELY direct event: inclusive structured local today/day+30 boundaries; missing/invalid/older dates do not positively qualify. |
| T06 | Contextual LAW/policy TIMELY: authored Topic proof, local today/day−30, no RSS keyword promotion. |
| T07 | DURABLE positive explicit urgency=standard Resource; absent urgency/undated alone is not positive proof. |
| T08 | VISUAL positive qualifying Flyer; media/identity/source retained, no auto-help activation. |
| T09 | One multi-role Flyer cannot fill two distinct presentation positions. |
| T10 | Multi-role Flyer reserved for VISUAL when independent TIMELY candidate permits greater distinct-role coverage. |
| T11 | Role coverage maximized **before** choosing ordinary same-role higher-ranked candidate. |
| T12 | Same-role role relevance precedes dates; direct-help timely beats context/stewardship. |
| T13 | Meaningful temporal distinctions break proper ties; local time zones/DST handled consistently. |
| T14 | Genuine catalog/approval newness is a late tie-break; derived/retrieved/event/expiry timestamps are not substituted. |
| T15 | Stable immutable ID tie resolves regardless of input reorder and produces scoped nonblocking diagnostic. |
| T16 | Missing TIMELY, DURABLE or VISUAL falls back deterministically; fallback is not falsely assigned a role; never duplicate ID. |
| T17 | Empty/one/two/large Topic inventories terminate and return every permitted position without arbitrary server cap. |
| T18 | Same-ID duplicate projections and cross-Community fixtures cannot inflate candidates or create cross-scope selection. |
| T19 | Real Housing four-featured-Topic and all-nine-Topic audit captures actual IDs/roles/fallbacks and representative real collision regression. |
| T20 | Backend/frontend/Python/validators and source protection pass; no public diagnostic leak or V3/P5 scope change. |

Negative tests must assert the precise reason the candidate is rejected or the role is unqualified, not merely that a test run failed. Explicitly distinguish a selected contextual item from a help-qualified item.

## 9. Quality gates and source preservation

Run focused tests after implementation, then full Maven `verify` using the repository's verified JDK 25 toolchain; frontend `npm test`, `npx tsc --noEmit`, `npm run build`; Python unit tests and all six catalog validators; `git diff --check`; relevant packaged/integration proof where it adds meaningful confidence. **Baseline as last reported at IM-06: 502 backend, 133 frontend, 14 Python tests.** These are context, not imposed future exact counts. Record each actual command and result, with evidence locations.

Keep the source-of-truth JSON, taxonomy, manifests and lockfiles unchanged absent specifically authorized bounded necessity. Synchronize annotated source/test mirrors under the repository's `claude.md` practice. No reset, stash, staging, commit or push without separate authorization. Never silently rewrite IM-06 selector semantics; if a reusable helper requires a correction, demonstrate its need and rerun IM-06 acceptance regressions.

## 10. Required IM-07 execution report

Provide a complete report with:

1. Package ID, starting/ending dispositions, actual branch/HEAD/dirty-state and protected-file evidence.
2. Inspected repository files/decisions, baseline commands and real gap map.
3. Precise candidate contracts, role-qualification semantics, deterministic global role-assignment/tie/fallback algorithm and architecture impacts.
4. Changed production/test/data/doc files and any existing test adjustments, with justification.
5. Named IM07-T01–T20 test-to-evidence mapping, including **negative** and distinct-role assignment cases.
6. Fixed-clock real Housing selected four Topics and all-nine-Topic audit, exact winner IDs, actual role labels and fallback placements, plus one additional Category/Community evidence.
7. Sample internal diagnostics, real collision regression tests, source/provenance/URL/media integrity, no private public-DTO leakage.
8. Baseline/focused/full actual commands, counts, exit codes, frontend/accessibility scope limitations and preservation hashes.
9. Explicit preservation of IM-01–IM-06 contracts, declared unresolved metadata gaps, and owners of remaining IM-08/10/11/12/13/14 work.
10. Ending disposition **Satisfied only if supported by executed acceptance evidence**; otherwise keep the package open and describe the exact blocker.

## 11. Stop conditions and architectural boundaries

**Do not** implement: a new featured Topic selector (IM-06); full Category page/rail (IM-08); event-occurrence exclusion/duration (IM-10); full data/provider/host audit (IM-12); AI scoring, popularity/engagement ranking, personalization, exposure rotation, an Admin UI/queue, automated approvals, a new ContentType, new offering/event entity, or generalized eligibility. No production Flyer semantic assignments without explicit editorial approval. Do not conclude a blank/missing date means verified evergreen or infer Topic placement from law/news prose.

**Final instruction:** Implement only the approved IM-07 item-selector capability and demonstrate it against fixed-clock synthetic collisions **and the actual current Housing catalog**. Preserve the approved **help first, context second** precedence and **maximum distinct TIMELY/DURABLE/VISUAL coverage**, using truthful unqualified fallbacks for absent roles. Return the full evidence report. **Do not begin IM-08 or declare v2 complete.**
