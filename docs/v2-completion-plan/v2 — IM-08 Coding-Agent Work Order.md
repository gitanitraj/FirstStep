# First Step v2 — IM-08 Coding-Agent Work Order
## Category Destination Integration: Featured Topics, Get Help, What to Know, Organizations and Context Rail

**Status:** AUTHORIZED FOR IMPLEMENTATION — evidence-gated acceptance  
**Package:** IM-08  
**Execution class:** Implementation Required  
**Dependencies:** IM-05, IM-06 and IM-07 — Satisfied on reviewed execution evidence  
**Issued:** October 17, 2026  
**Controlling authority:** v2 Completion Contract, authoritative IM-01–IM-14 execution matrix including the IM-07 closure addendum, P4/P5, approved Architecture Resolutions (full seven-stage featured-topic hierarchy), Decision 051 Pass 1/Pass 2A, Decision 052, and accepted IM-01–IM-07 evidence and editorial decisions.

---

## 1. Mission and stop boundary

Integrate the **already implemented** IM-05 help/Topic browsing, IM-06 featured-Topic selection, and IM-07 three-item selection into the resident-facing Category Destination, preserving `/category/:key`. Deliver a coherent, accessible, mobile-first Category experience with:

- A header/category orientation, up to four **featured Topic columns** near the top, and up to three **distinct, directly navigable content items** per column.
- A bounded **Get Help** preview and accessible route to complete **All [Category] Help** and existing controlled Topic sub-destinations.
- **What to Know**, with controlled Policy Info, Law, Flyers, and inclusive First Rate News collection entry points/preview behavior consistent with currently implemented contracts.
- A secondary **Organizations** collection showing relevant organizations connected to offerings, without substituting source/host identities for providers.
- The approved **right-side context rail** for First Rate News headlines, available YouTube/Expert links and Delaware's latest laws, using only verified existing records/links, not fabricated feed items or media.

This is an **integration** package. IM-09 owns full civic-collection semantic enforcement/public-DTO audit, IM-10 event occurrence, IM-11 later real-catalog collision regression, IM-12 complete PROVIDER/HOST/provenance audit, IM-13 full accessibility/release acceptance. Keep those responsibilities explicit; do not mark them satisfied here.

**Core rule:** “Category is the scope. Destination is the experience.” Resident help is primary, civic information gives context, organizations are secondary. Do not restore the retired three-pillar navigational layout or build an overwhelming organization directory.

## 2. Required inspect-first baseline

1. Read `claude.md` and current repository-controlled matrix, IM-07 closure addendum, approved P5, Decision 051/052, IM-05/06/07 reports and their tests. Capture branch, full HEAD, `git status`, protected changed/untracked files and baseline hashes **before edits**. IM-07 reported new untracked work and no commit/push; verify reality rather than trusting stale state. Never overwrite, reset, stash, stage, commit or push predecessor work without explicit authorization.
2. Inspect `CategoryContentService.helpFor(categoryKey, topic, communityId)`, `TopicPageService`, `/api/category/{key}/help`, existing Category/Topic DTOs, Article and Flyer source/card contracts, controlled collection services, navigation and Organizations source, Category frontend components/styles/API/dictionary, high-contrast tokens, current tests and routing. Verify actual selectors (`FeaturedTopicSelector.select(communityId, categoryKey)`, `FeaturedItemSelector.select(communityId, categoryKey, topic)`) and their immutable results, true roles, fallback flags, card navigation, source/provenance and *internal-only* diagnostics. Do not assume signatures beyond inspected code.
3. Run the existing backend (JDK25 toolchain) verify and frontend/Python/type/build/validators as feasible before implementation; log baseline failures distinctly. Map already-satisfied Category behavior before changing it. Inspect current Housing and at least one other Category with real Community scope.
4. Produce an inspect-first gap map identifying which existing resident surfaces can be composed and which are missing. Record the minimum API/projection changes, including how to prevent internal selector diagnostics, article editorial/review fields and `helpAccess` from appearing in public responses. Test this mapping.
5. If existing Organization projection would require inventing provider associations, **do not** do so. Reuse evidenced current provider-related data as available, label limitations, and reserve the full association model/audit to IM-12. If rail YouTube links or relevant LAW entries do not exist, use a truthful absent/empty state rather than fictional media or keyword-promoted context.

## 3. Required deliverable A — Controlled Category composition

Preserve `/category/:key` and explicit Community query scope through API, cards, Topic links, All Help, collections and browser history. Validate category keys; unknown routes produce the existing accessible not-found state. The Category destination should keep a compact category introduction/count/last-checked treatment where supported; do not turn system evaluation time into “last verified.” The approved hierarchy is:

1. **Featured Topics** — up to four controlled Topic columns beneath the header/intro, using ordered IM-06 results, not hard-coded Housing labels or old broad-content count.
2. **Get Help** — bounded preview of current resident-actionable offerings; “See All / All [Category] Help” routes to IM-05's complete accessible collection. Retain helpful triage grouping/navigation where already approved without duplicating full taxonomy panels.
3. **What to Know** — contextual civic information collections with the approved labels Policy Info, Law, Flyers, First Rate News. Do not conflate NEWS and Policy Info, treat LAW as a new sixth ContentType, or create new classification to populate sparse collections. Reuse existing controlled collection membership pending IM-09 verification.
4. **Organizations** — secondary links/preview to relevant offering-connected organizations; don't dump every organization in the registry or infer a provider merely from ContentSource, an arbitrary mention or a Flyer host label.
5. **Context rail** — appropriately sized rail for First Rate News headlines, authentic Expert/YouTube links where available and current Delaware laws. Rail provides optional context, not a replacement for Get Help. Responsive small screens must not create a squeezed laws column.

Keep source cards useful at 375px and desktop widths; avoid previously rejected tags-above-headlines clutter and repeated taxonomy. When a section has zero verified items, represent its empty state concisely and accessibly; do not invent cards or silently draw from another Category.

## 4. Required deliverable B — Featured Topic columns and item cards

- Obtain up to four Topics in the exact IM-06 order for an **explicit** Community and Category. Do not reimplement Topic ranking in a controller or React. The fixed-clock October 16 Housing reference selects Transitional Housing, Emergency Shelter, Homeownership, Rental Assistance; this is **audit evidence**, not a hard-coded fixture or eternal result.
- For each selected Topic use the **existing IM-07 item selector** to obtain up to three distinct IDs, their current `ContentItem` presentation, genuine role `TIMELY`, `DURABLE`, `VISUAL`, or honest `FALLBACK`, and provenance/navigation. Do not rerank, manually swap, create role identity for a fallback, or fill vacant roles with unauthorized Flyers.
- **At the reported real Housing snapshot**, the four columns are:

| Topic | Genuine DURABLE role | Fallback 1 | Fallback 2 |
|---|---|---|---|
| Transitional Housing | HA-002 | HA-003 | HA-005 |
| Emergency Shelter | HA-016 | HA-001 | HA-006 |
| Homeownership | HA-011 | HA-013 | HA-021 |
| Rental Assistance | HA-008 | HA-023 | HA-026 |

Only one role is evidenced for each; the other two are presentation fallbacks. UI should prioritize clear resident-friendly titles and action, **not** display misleading “TIMELY”/“VISUAL” labels or raw technical `FALLBACK` badges. If semantic labels are presented, represent only actual proven roles, with no implied verification or deadline.
- Clicking content must open the intended original source or approved internal article/help destination. Use existing safe navigation/URL/encoded-media behavior; never fabricate a source URL or a provider identity. Card action must work by keyboard and be accessible. A Topic title leads to the controlled IM-05 sub-destination.
- Preserve zero-help Topics (such as the October Housing Eviction Prevention record) as navigable valid Topic routes without elevating them into the top four because contextual records or a Flyer image exist.
- IM-04 approval remains unchanged: **0/8 production Flyers** carry `helpAccess`; none may be activated or silently admitted to featured-help/fallback. They continue to appear in eligible contextual Flyers collections.

## 5. Required deliverable C — Get Help, collections and rail without scope leakage

- Continue to use `helpFor(...)` and IM-05 Topic/All Help routes; counts for Get Help use the actual eligible unique help inventory, not total NEWS/LAW/Expert counts. Preview is bounded; all eligible help remains reachable, even when there are many results.
- Preserve collection views/entry points for Policy Info, Law, Flyers, First Rate News as approved in Pass 1; preserve inclusive First Rate News meaning (eligible Originals/Expert material). Do not change underlying ContentTypes, approval restrictions, ContentSource sector or controlled classifications. Changes required to render the accepted layout may be made narrowly; full collection contract verification remains IM-09.
- Organization preview is grounded only in actual supported offering/organization links. If records lack true PROVIDER/HOST semantics, use existing trustworthy associated data with explicit limitations; do not fabricate associations for aesthetic completeness.
- Context rail may surface only real current eligible sourced headlines/media/law entries; source navigation must remain intact, no publication-time inference of event occurrence. Verify it does not hide Help, overwhelm mobile, repeat the entire contextual collection or render private editorial fields.
- Community scope established before selector/content projection. Unknown Category/Topic must not silently fall back to Wilmington or unscoped results. Lifecycle, status, article approval and explicit Expert Topic/Category rules remain with their current owners.

## 6. Interface, responsiveness and accessibility

- Mobile-first at **375px** and desktop at **1280/1440px**. Four featured Topics should reflow without tiny four-column mobile cards or horizontal overflow; rail should move below primary resident information rather than compress it.
- Preserve Montserrat/Open Sans typography roles and existing themes/high-contrast tokens. No literal-fallback CSS token anti-pattern, new icon library or unnecessary dependencies.
- Accessible heading hierarchy, grouped topic/section names, controlled collection switcher labels, meaningful source links, visible focus, keyboard use and stable browser navigation. Loading, empty, error and invalid-Category states must be distinguishable/announced. Spanish text where dictionary-driven labels are exposed; protect diacritics.
- Cards should show resident-relevant information rather than internal role diagnostics, debug values, review flags or `helpAccess`. Avoid badges implying source verification or an editorial “trusted sources” endorsement.

## 7. Mandatory acceptance matrix IM08-T01–T20

| Test | Required evidence |
|---|---|
| T01 | Existing `/category/:key` works and valid Community-scoped Housing page composes approved hierarchy; unknown Category is accessible not-found. |
| T02 | Up to four ordered featured Topics exactly match injected IM-06 output; input shuffled/no hard-coded Housing names; other Category positive. |
| T03 | Each featured Topic consumes corresponding IM-07 winners/positions in original order, <=3 distinct canonical IDs; no UI reranking. |
| T04 | Real Housing fixed-clock regression: four Topic headings and twelve expected current card IDs as audited; test should use a fixed clock and up-to-date source, not a global static content count. |
| T05 | Genuine DURABLE vs FALLBACK presentation truthful; missing TIMELY/VISUAL roles do not generate fake urgency/visual indicators. |
| T06 | Synthetic multi-role/role-complete fixture preserves IM-07 true assignments and one ID per column, no duplicate card. |
| T07 | Topic heading → valid Topic destination; item → actual external/source/internal destination; source-less Flyer respects existing media navigation. |
| T08 | Get Help preview bounded, true unique eligibility counts, All Help reaches complete inventory; no flat organization dump. |
| T09 | Topic/All Help/Category links, direct refresh/history retain explicit Community and never merge other Community catalog. |
| T10 | Valid zero-help Topic remains accessible without false actionable claims; contextual records cannot inflate Get Help/featured Topic count. |
| T11 | What to Know presents controlled Policy Info, Law, Flyers, inclusive First Rate News entries with real available data; no ad hoc collection discovery. |
| T12 | Contextual Flyers (including real unactivated examples) remain in Flyers view but cannot appear as actionable featured-help cards without `helpAccess`. |
| T13 | Article approval and unapproved/flagged/private boundaries, Expert Category integrity, Source/Community and lifecycle restrictions survive integration. |
| T14 | Organizations preview remains distinct/secondary, grounded in existing associated records and no invented source=provider or host=provider claim. |
| T15 | Context rail uses real linked headline/YouTube/law content if present, truthful empty/omission if absent; no fabricated or keyword-only relevance. |
| T16 | Responsive 375px and 1280/1440px: no horizontal overflow, readable topic cards, rail does not squeeze latest laws; bounded first-screen content. |
| T17 | Keyboard navigation/focus, screen-reader semantic names/roles, loading/empty/error states, collection switching, high contrast and Spanish labels are tested. |
| T18 | Public API/DOM has no internal `Selection`, diagnostics, approval/review internals, `helpAccess`, source debugging or other private fields. |
| T19 | Final backend, frontend, TypeScript, build, Python tests/six validators pass; browser/HTTP journey smoke tests and representative screenshot inspections recorded. |
| T20 | Existing IM-01–07 behavior/tests and actual working tree protected; no taxonomy/catalog/lockfile mutation, selector rewrite, IM-09/10/12 scope expansion or V3 UI. |

Synthetic tests must distinguish fixture behavior from verified real catalog. For UI acceptance demonstrate at least one actual route-click-through from Category to featured card and one complete Get Help journey, not only backend unit tests. Reconcile existing tests instead of weakening earlier assertions.

## 8. Required implementation procedure

**Inspect → baseline and protect → gap map → minimal integration design → implement narrow changes → focused tests → full suites/validators → built app + packaged API/browser/mobile checks → evidence report → stop.**

Do not create extra ContentTypes, persistent featured flags, AI or engagement ranking, admin dashboard/CRUD, workflow, provider graph, event-occurrence cutoff or second-community rollout. Do not amend the approved P5 model to fix a normal tie, absent data or sparse rail. Do not start IM-09 while implementing IM-08. No staging, commit or push without separate user authorization.

## 9. Mandatory execution-report contract

Return **`IM-08 Execution Report.md`**, including:

1. Package, starting/ending disposition, date, actual branch/HEAD and protected state, source hashes and change inventory.
2. Authority reviewed, existing Category behavior, inspect-first gap map, justification for exact production/API/frontend edits and what already worked.
3. Actual integration contracts and examples of public DTO shape; demonstration that selection diagnostics and private fields are withheld.
4. Real fixed-clock Housing featured Topic + item selection **IDs and true roles/fallback distinctions**, live Flyer activation count, help counts, factual organizations/rail coverage and gaps.
5. At least one additional Category and different Community fixture, including negative cross-scope cases.
6. Named acceptance mapping for **T01–T20**, actual executed commands and pass/fail test counts, separate initial failures/corrections.
7. Actual packaged API and built frontend/browser journey/keyboard/mobile/high-contrast/Spanish verification with screenshots or artifact paths, including limitations; do not claim an exhaustive WCAG audit if not performed.
8. Preservation of IM-01–07 contracts/files, catalog/taxonomy/source/identity restrictions and no V3 work.
9. Exact downstream responsibilities for IM-09–IM-14, including any genuine outstanding evidence, and final `Satisfied` / still-open / escalate recommendation.

**A successful compilation is not sufficient.** IM-08 is `Satisfied` only when the integrated resident experience and acceptance evidence exist. Do not mark the entire First Step v2 complete.

## 10. Direct coding-agent instruction

Implement only IM-08 under the approved source-of-truth contracts. Verify current checkout; preserve the existing IM-07 selector and live catalog. Project the already-approved featured Topics, their authentic IM-07 item positions and direct source links into the resident-facing Category Destination, alongside bounded Get Help, controlled What to Know, connected Organizations and the accepted context rail. Treat sparse metadata and absent links honestly. Deliver named tests, built-app/browser evidence and a complete execution report; stop at this package boundary.
