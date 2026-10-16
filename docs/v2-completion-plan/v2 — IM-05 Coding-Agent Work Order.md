# First Step v2 — IM-05 Coding-Agent Work Order
## Topic Sub-Destination & All-[Category]-Help Browsing

**Status:** AUTHORIZED FOR IMPLEMENTATION — evidence-gated acceptance  
**Execution package:** IM-05  
**Execution class:** Implementation Required  
**Direct dependencies:** IM-01, IM-03, IM-04 — Satisfied on reviewed execution-report evidence  
**Issued:** October 16, 2026  
**Authority:** First Step v2 Completion Contract; authoritative reconciled IM-01–IM-14 matrix and October 16 disposition ledger; approved 2B-P4 and P5; Decision 051 Pass 1/Pass 2A; approved Housing Topic/triage design; IM-01 through IM-04 execution reports, IM-03 editorial classification decision, and IM-04 semantic decision. If implementation evidence shows conflicting requirements, report the conflict rather than improvising a model change.

---

## 1. Mission and resident acceptance

Deliver the **usable browse-through-help journey** beneath each Category Destination: a resident may choose a controlled Topic/subcategory or choose **All [Category] Help**, and arrive at a useful, appropriately scoped collection of currently eligible programs, services, resources, and explicitly approved actionable help/access opportunities. These are help-oriented destination views, **not** an organization directory, a second taxonomy, or the featured-topic selector.

Housing is the first real-catalog acceptance case, but the implementation must support other Categories and Community partitions without Housing-specific logic. The design must tolerate growth beyond the historically cited Housing inventory size; do not freeze historical counts into UI or tests.

**Completion principle:** Category establishes scope; Topic narrows it; current actionable CivicContent supplies the results. Organizations are supporting context, not the primary search result identity.

## 2. Proven predecessor seams — verify their real signatures

- **IM-01:** shared `EditorialLifecycle` and resident-current inventory filtering. ACTIVE and APPROACHING_EXPIRY remain eligible where otherwise qualified; EXPIRED and independently excluded statuses do not. Never bypass this by querying raw stored content for resident results.
- **IM-02:** separate read-only expiry diagnostic/reporting. Do not insert Admin state or expiry-review priority into resident result order.
- **IM-03:** explicit Expert/FAQ `category_tags`, controlled Category/Topic parent rules, and prevention of inferred Category from prose. The 12 live Expert/FAQ records have Category-only classifications, **no Topics**. This does not make Expert/FAQ an actionable help offering.
- **IM-04:** `CategoryContentService.helpFor(categoryKey, topic, communityId)` is the reported reusable candidate seam, composing current Resources plus only Flyers with exact `helpAccess=ACTIONABLE_HELP_ACCESS`. Null Topic denotes Category scope; a supplied Topic must belong to that Category. Preserve one FLYER identity across projections, source/media and ownership. **All eight real Flyers currently lack the approved helpAccess token; none is live Get Help content.** No implicit activation, synthetic fixtures in production, or new help semantics.

These are reported implementation facts, **not a substitute for source inspection**. Check current branch/HEAD/working tree and actual method signatures before editing. Do not depend on the IM-04 work tree being uncommitted or committed: verify.

## 3. Mandatory repository inspection and protected baseline

1. Inspect actual git state (`git status --short`, branch, HEAD, remote) and identify committed and uncommitted predecessor work; preserve it exactly. No reset, stash, staging, commit, or push unless separately authorized.
2. Read relevant controlling source files: `CategoryContentService`, `CategoryPageService`, `TopicPageService`, existing Category/Topic controllers/DTOs; `ResourceService`, `FlyerService`, `FaqService`/`ExpertAnswerService`; taxonomy/Navigation services; Community filters; ContentItem and card/link navigation contracts; React Router routes, Category Destination components/CSS, existing Topic surfaces, loading/empty/error conventions; existing tests and validators.
3. Identify actual Category keys, Topic identifiers, existing topic route(s), and direct-link/navigation behavior **before choosing a new route**. Keep `/category/:key` stable. Reuse the established route and query-state conventions where possible; do not invent a second, conflicting Topic address scheme.
4. Audit current **resident-eligible** Housing help result set and all nine controlled Housing Topics; count by true content identity and current eligibility, not presentation collection memberships. Capture sample IDs and missing Topic membership where applicable. Compare at least one other Category and one synthetic second Community.
5. Run baseline backend/frontend/validator checks and record actual counts/failures. Historical IM-04 evidence reports 463 backend tests, 126 frontend tests, 14 Python tests, and six validators; these are comparison points only.
6. Produce a concise inspect-first gap map showing existing reuse and exact missing functionality; implement only demonstrated gaps.

## 4. Deliverable A — Shared, scoped help browse contract

Use or extend the proven `helpFor(categoryKey, topic, communityId)` seam rather than writing another rule for help admission.

- **All [Category] Help:** Category-wide current help inventory returned from the same central projection with `topic=null`; it does **not** mean all content, all Organizations, or all News/Law/Expert/Flyer records.
- **Topic collection:** Same eligible help inventory, narrowed by the *controlled* Topic that belongs to the selected Category. Reject/handle unknown Category, unknown Topic, and parent mismatch consistently with existing API conventions; do not broaden results or silently apply an unrelated Topic.
- RESOURCE remains eligible under existing Resource/current/category semantics; FLYER qualifies only through the approved exact-token controlled semantic *plus* all other applicable gates. No default EXPERT, NEWS, LAW, or Article inclusion into Get Help. Keep independent status, Community, category, source/provenance, identity, and IM-01 expiry rules.
- Deduplicate within a result set by canonical CivicContent ID; presentation membership across different views does not create a second underlying item. Do not count an organization record as an offering.
- Define deterministic browsing order using existing documented project convention or narrowly justify a stable order. **Do not** use IM-06 featured-topic ranking, IM-07 TIMELY/DURABLE/VISUAL selection, expiry-attention priority, personalized ranking, inferred semantic relevance, or popularity.
- If paging, filtering, or basic search is necessary for real catalog usability, reuse existing conventions and keep the scope narrowly bounded; do not create a generic faceted search engine or a parallel classification system.
- Preserve the full available eligible catalog behind browsing; a short preview/initial screen may be bounded but **must not silently drop the remainder** or impose an arbitrary unchangeable result cap.

## 5. Deliverable B — Resident navigable Topic sub-destination

A resident entering from `/category/:key` must be able to choose a **real controlled Topic** and reach a dedicated Topic-scoped help collection, by a stable address that survives refresh/back navigation and can be linked directly. Inspect and reuse existing topic routing/conventions before adding anything.

- Show the Category/Topic context with a plain understandable heading and a route back to the parent Category and/or its full help inventory.
- The primary result cards represent **programs/services/resources or explicitly approved access opportunities**. Organization/provider labels may accompany items when supported but must not replace the offering as the main item or imply that a HOST permanently provides the service.
- Preserve the item's real actionable destination: supported detail link or original/source link, according to existing item-navigation contract. Never silently send a resident to an incorrect unrelated source or fabricate a detail route.
- Topic counts and visible results should both be labeled/derived from consistent **help-only** eligibility, rather than combining a broad all-content Topic count with a narrowed help-only list without explanation.
- Keep Topic names and parent assignment controlled and avoid displaying a repeated taxonomy wall within each sub-destination.
- Appropriate empty state for a real Topic with no eligible help, distinct from unknown Topic and from loading/network error. Do not turn an empty result into fictitious programs.
- Keyboard access, semantic navigation, correct focus and loading/alert roles, responsive layout, high-contrast tokens and Spanish/diacritic safety should follow existing patterns; avoid introducing new CSS token fallbacks or icon libraries.

## 6. Deliverable C — All-[Category]-Help resident browsing

Add or verify a clearly discoverable path from the Category Destination to **All Housing Help** (and corresponding labels for other Categories). Its destination lists the complete current help inventory for that Category, with a way to narrow to one of its existing Topics. The resident may also enter from a Topic and return to all help without losing Category scope.

- Use the existing controlled Topics as narrowing controls or browsable links, not newly manufactured facets or audience classes. The historic Housing arrangement of nine Topics and its accepted resident triage groupings — **Need Help Right Away**, **Find a Place**, **Help Staying Housed** — may inform presentation, but **do not** create parallel taxonomy entities or hard-code nine Topics as a platform invariant.
- If the approved triage grouping already exists in controlled configuration, reuse it. If no stable machine-readable grouping exists, do not automatically infer it from prose; use a simple controlled Topic listing or submit the minimum presentation choice for a bounded decision. Do not turn optional group display into a blocker for the required browsing capability.
- Avoid the previously rejected page design: no giant flat organization directory, no duplicated Category hero taxonomy, no listing every record in an unreadable first screen. Provide a reasonable first-screen view and clear access to the rest (e.g. established pagination/expand/reachable list) using existing UI conventions.
- Category-wide result counts must reflect actual eligible help inventory. Counts must not be pinned to 44 or confuse stored category-placement counts with live discoverability.
- Do not implement featured columns, their four-Topic selection, three-role item selection, the Category context rail, or the final full Category Destination redesign here: **IM-06/07/08 own those.** IM-05 may add the minimum discoverable links needed for its resident journey without redesigning the parent.

## 7. Integration and negative boundaries

**IM-04 editorial boundary:** Production `helpAccess` remains absent from all eight Flyers. They remain in Flyers where eligible, not Get Help. Do not request or auto-assign real Flyer flags to pass a browsing test. Use synthetic, temporary catalog fixtures to demonstrate future dual-projection results if useful.

**Events:** Event occurrence/current-event cutoff belongs to **IM-10**. Do not mistake event dates for editorial expiration, create arbitrary durations, or activate an event Flyer from prose.

**Classification:** `ContentType` remains the fixed five; Topic is the existing Category→Subcategory classification; Category-only Expert/FAQ remain category-content candidates but not automatic Get Help offerings. Preserve singular Community ownership and explicit scoped retrieval; do not conflate Community catalog membership with geographic service eligibility.

**Provider/source:** Avoid manufacturing PROVIDER/HOST associations from `ContentSource`, `organization_id`, or host display text. Offering↔provider integrity is separately audited under **IM-12**. Source labels remain provenance.

**No V3:** No Admin dashboard, workflow/queue, email, AI ranking, recommendation engine, Organization registry overhaul, generalized eligibility engine, second-community production launch, DB migration, new ContentType, new durable Offering entity or mutable featured flag.

## 8. Required acceptance matrix

Each item must map to **named tests, API/browser checks and actual captured outcomes**, not just a statement of intention.

| ID | Acceptance scenario | Required result |
|---|---|---|
| IM05-T01 | Category → All-[Category]-Help | Discoverable navigation, stable/refreshable address and correct Category-wide help collection |
| IM05-T02 | Category → controlled Topic | Enter an existing Housing Topic and get its specifically scoped current help results |
| IM05-T03 | All help → Topic → all help | Correct back/forward/direct-link behavior; Category remains scoped |
| IM05-T04 | Category/Topic validity | Unknown Category/Topic and parent mismatch do not silently return other results |
| IM05-T05 | RESOURCE action semantics | Existing eligible Resources remain present; no `helpAccess` requirement for Resources |
| IM05-T06 | Flyer positive synthetic | Exact-token eligible FLYER enters the same scoped help view without type/ID change |
| IM05-T07 | Flyer negative production | All eight unactivated real Flyers excluded from current Get Help, still eligible for Flyers collection |
| IM05-T08 | Expiry/lifecycle | Expired/inactive/withdrawn excluded; approaching-expiry eligible without editorial attention re-ranking |
| IM05-T09 | Community isolation | Explicit Community scope; no cross-Community leakage or Wilmington-specific selector logic |
| IM05-T10 | Topic classification | No prose/topic inference; only approved controlled parent relationship; Category-only Expert stays non-help |
| IM05-T11 | Content identity & navigation | Cards open valid existing destinations, correct type/source/provenance, no fabricated PROVIDER |
| IM05-T12 | Complete collection and bounded view | No inaccessible eligible records behind preview; usable first screen plus reachable full inventory |
| IM05-T13 | Counts and dedup | Help-only displayed counts agree with eligible results and stable unique IDs; no projection double-counting |
| IM05-T14 | Empty/error/loading | Distinguish valid empty Topic, invalid route and network/loading states with accessible messages |
| IM05-T15 | Accessibility/mobile | Keyboard, screen-reader semantics, focus, 375px and desktop layout, high-contrast, diacritic regression evidence |
| IM05-T16 | Generality | At least one non-Housing Category and synthetic second-Community fixture covered |
| IM05-T17 | Predecessor regression | IM-01–IM-04 tests/validators and protected source behavior still passing |
| IM05-T18 | Boundary discipline | No featured-selector/context-rail/event cutoff/automated editor workflow or unrelated model redesign |

Where no usable browser automation exists, include reproducible manual interaction steps and concrete rendered/check evidence rather than claiming unexecuted visual or accessibility testing.

## 9. Verification gates

1. Run repository-specific baseline checks before modifications and record exact existing behavior/route inventory.
2. Add focused backend tests on the real `helpFor` path, HTTP service/DTO scope and synthetic positive/negative identity fixtures where appropriate.
3. Add frontend route/navigation, Topic and all-help collection tests, including empty/error/loading and direct-link/back behavior; do not limit evidence to Java projection tests for a resident-facing package.
4. Run full Maven `verify` with repository JDK 25 toolchain; report counts, errors, failures and skipped tests.
5. Run frontend `npm test`, `npx tsc --noEmit`, `npm run build` and all six current catalog validators, and the Python test discovery suite where available.
6. Run packaged backend + frontend/browser or equivalent end-to-end checks using real Housing data (including multiple Housing Topics), at least one other Category, and a **temporary synthetic** second Community. Inspect 375px and desktop composition if tooling exists; record actual results.
7. Confirm git diff scope, prior IM-01–IM-04 preservation, no unauthorized catalog flag edits, no new dependencies or lockfile edits without explicit need. Annotated source/test mirror convention applies if the repository requires it.
8. Capture output and links/paths in `evidence/im05/` or equivalent repository-approved evidence location.

**Do not equate passing unit tests with a usable resident experience.** IM-05 is not Satisfied until the browsing flows demonstrably work.

## 10. Work sequence

**Inspect source and current routes → preserve predecessor state → capture baseline / real Housing inventory → specify smallest navigation/read-model gap → implement shared scoped help browsing + Topic destination + All-[Category]-Help path → focused tests → full gates → runtime/user-journey and accessibility checks → execution report → STOP.**

Implementation should not begin with a taxonomy rewrite or a UI overhaul. Any genuine unresolved **architectural** contradiction must be stated with repository evidence; ordinary routing, fixture, classification or layout defects remain bounded implementation work.

No staging, commit, push, broad catalog correction or work in IM-06/07/08 is authorized without separate instruction.

## 11. Required execution report

Return **IM-05 Execution Report** with all of the following:

- Actual starting and ending branch/HEAD, working-tree state and predecessor preservation evidence.
- Exact files/classes/DTOs/routes/components inspected; baseline behavior and quantified current Housing/non-Housing inventories (distinguish stored and resident eligible).
- Demonstrated gaps, implementation choices and why existing `helpFor`/taxonomy/routing seams were reused.
- Exact changed code, tests, UI/CSS and documentation files; any catalog changes (normally none) itemized.
- Named evidence mapping for **IM05-T01–T18**, including direct-link/refresh/back behavior, all-result reachability, identity, counts, scoped Community, real and synthetic fixtures.
- Backend/full frontend/TypeScript/build/Python validators and unit-test commands and actual pass/fail/skip counts.
- Browser or equivalent resident-journey evidence with desktop and 375px results, keyboard/semantic/high-contrast findings, plus limitations if checks could not run.
- Confirmation **0/8 live Flyers activated** unless a separately documented editorial approval changed this after issue; do not silently adjust production records.
- Explicit list of deferred responsibilities: IM-06 Topic ranking, IM-07 item roles, IM-08 destination composition/context rail, IM-09 collection semantics, IM-10 occurrence, IM-12 relationship audit, IM-13 final release regression, IM-14 documentation closure.
- Ending disposition **Satisfied** only if fully proven; otherwise remaining gate(s), exact failures and bounded proposed next steps.

**Stop at the IM-05 package boundary.** IM-05 implementation cannot declare First Step v2 complete.
