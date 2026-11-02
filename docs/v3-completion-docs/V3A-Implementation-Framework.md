# First Step V3A — Implementation Framework

**Version:** 1.0 — implementation-planning draft for product-owner review  
**Date:** October 24, 2026  
**Project:** First Step  
**Phase:** V3A — Resident Experience & Frontend  
**Basis:** `V3A-Page-Requirements-and-Acceptance-Contract.md`, Version 1.0 (approved P01–P09), V3A-00 Evidence Completion Report R1, completed C-02 flyer-integrity closure, and previously approved V3A package sequence.  
**Status:** Proposed execution framework. **Not yet product-owner approved.** No code execution or implementation acceptance is implied.

## 1. Purpose and authority

This framework translates the approved resident-page contract into finite coding-agent packages **V3A-00 through V3A-09**. It specifies dependencies, permitted work, deliverables, acceptance-test expectations and evidence requirements. The page contract is the authority for **what** must be delivered; this framework governs **sequence and proof**. If an implementation detail conflicts with the contract, stop, document the conflict and seek a product decision rather than silently alter the contract.

V3A implements a coherent, accessible, mobile-first resident experience on the existing **Java Spring Boot / React + Vite** foundation and the approved CivicContent, ContentSource, Community, editorial lifecycle and deterministic selection mechanisms. Work primarily refines existing features. No package is permission to rebuild working architecture without demonstrated necessity.

### Document precedence

1. Approved V3A Page Requirements & Acceptance Contract, v1.0.
2. Explicit later product-owner decisions, recorded with date and affected acceptance IDs.
3. Approved V3A Homepage/Housing design decision record (to be consolidated), including `First Step original visual reference.png` **subject to later approved changes**.
4. Existing v2-approved architectural and editorial contracts; V3A-00 and C-02 evidence establish **observed state**, not new product policy.
5. This implementation framework for package boundaries, sequencing and evidentiary completion.

**Reference availability:** The original visual file has been identified in the First Step Library. Its repository attachment/path is not yet verified. Do not claim the image is checked into Git until it is actually copied and verified.

### Fixed scope boundaries

- No new `ContentType` entries, generalized CivicContent restructuring, database migration or replacement of deterministic selection.
- No automatic article creation, machine source verification, full Admin UI, approval dashboard, media-review schema, queues, or workflow assignment (V3B/Later).
- Editorial approval and the accuracy of original journalism remain human responsibilities. Engineering enforces the outcome in resident-visible projections.
- Sparse actual content is legitimate: **never seed synthetic events, articles or flyers merely to fill a design.** Fixtures may exist only in tests and must be labeled in evidence.
- Preserve relevant Community scoping, producer identity distinctions, expiry/event-date rules, accessible URLs and direct item navigation.

## 2. Baseline and operating method

**Repository:** `gitanitraj/FirstStep` on `copyofdev`. **Verified reference commit:** `f4ae9e05f8e5687d9287dba82e5cf5f1a847ca59`, containing the C-02 publication-integrity correction. Before beginning any implementation package, the agent must record the **actual** branch, HEAD SHA, working-tree status and relevant changes since the reference commit; do not assume the branch remains unchanged.

**V3A-00 audit status:** R1 was *Escalation Required*, principally because its author could not access a consolidated approved contract, implementation framework or Homepage/Housing decisions. R1 must be preserved as a historical audit; issue a **closure addendum and per-criterion crosswalk**, not a rewritten audit with retroactively changed outcomes. The C-02 correction is committed. C-01's editorial acceptance requirements are approved, but OR-003 fixture remediation remains to be demonstrated in V3A-06.

**Execution rhythm for each package:** inspect current code and contract → enumerate exact accepted criterion IDs → submit a bounded implementation plan if ambiguous → implement minimal changes → run targeted tests and relevant full suites → collect browser/API evidence including mobile → reconcile each criterion → produce a package closure report and commit. Never substitute an assertion that a component exists for a demonstrated resident journey.

**Mandatory evidence classes:** (L) observed against live eligible catalog; (F) controlled fixture; (S) screenshot or screen recording of rendered behavior; (T) automated test/validator/build; (I) source-code inspection; (N) not verified, with reason. One criterion may have several classes. Distinguish functional correctness from external destination/content factual accuracy.

## 3. Dependency map and package ownership

| Package | Primary mission | Dependency | Lead contract areas |
|---|---|---|---|
| **V3A-00** | Evidence, authority reconciliation and readiness gate | Existing v2/C-02 baseline | All P01–P09 classification; C-01/C-02/A-01/A-02 ledger |
| **V3A-01** | Shared design system and reusable presentation primitives | 00 | S-01–S-05, S-09 foundation |
| **V3A-02** | Approved five-row Homepage | 01 | H-01–H-09 |
| **V3A-03** | Category Destination / Housing composition | 01; shared integration with 02 | C-01–C-09 |
| **V3A-04** | Topic and All Help browsing | 03 for navigation; existing backend preserved | T-01–T-06, AH-01–AH-05 |
| **V3A-05** | Community Notices / Events / Flyers | 01; coordinate Homepage previews | N-01–N-08; H-06/H-07 support |
| **V3A-06** | Program Updates and First Rate News / article detail | 01; coordinate Homepage and Housing previews | U-01–U-09; H-05/H-07 support |
| **V3A-07** | Resource cards, outbound links and Organizations | 04 plus existing routes | R-01–R-05 |
| **V3A-08** | Find Help, cross-route navigation and functional corrections | 02–07 available for integration | F-01–F-07; S-05/S-06 |
| **V3A-09** | Integrated acceptance and release evidence | 01–08 | S-01–S-09; final crosswalk for all criteria |

The sequence establishes responsibility, **not a mandate to redo components**. A package may reuse already-tested behavior, recording “retained/verified” instead of changing files. If cross-cutting bugs need an earlier shared fix, assign one owning package and list consuming packages; do not make conflicting copies.

### Gate rules

- **00 → 01:** Approved contract present; framework and design decision record sufficiently controlled; R1 crosswalk and unresolved implementation backlog recorded. Audit closure does not require finishing V3A implementation.
- **01 → 02–07:** Shared tokens, accessible primitives and regressions established. Do not block unrelated page work on optional polish.
- **02–07 → 08:** Main page experiences sufficiently integrated to verify navigation/back-forward behavior across them.
- **08 → 09:** No misleading navigable stubs or critical unresolved journey breaks; all feature-package evidence submitted.
- **09 → acceptance:** All contract criteria have a verdict and references; full gates pass; any critical defect blocks closure; deferred items are explicitly outside V3A rather than relabeled as passed.

## 4. Package specifications

### V3A-00 — Authority Reconciliation & Evidence Closure

**Goal:** Turn R1's evidence audit and approved product decisions into a traceable, implementable baseline, not another exploratory redesign.

**Deliverables:**
- Controlled copy of approved Page Requirements & Acceptance Contract v1.0 in `docs/v3-completion-docs/`.
- This Implementation Framework, after explicit product-owner approval, in the same directory.
- Consolidated Homepage/Housing Design Decisions record, referencing the **exact** original visual image if present in repository or recording its Library-only location.
- `V3A-00-closure-addendum.md`: R1 disposition reconciliation; C-02 commit and tests; C-01 approved rules and OR-003 open implementation; A-01 Community-scoped Updates requirement; A-02 Find Help role/navigation disposition; outstanding D/T issues.
- Criteria crosswalk covering **each** acceptance ID (H, C, T, AH, N, U, R, F, S) with evidence and V3A package owner.

**Acceptance:** all authority documents are identified and versioned; every requirement maps to one primary owner and a verification plan; unresolved implementation obligations remain visible. R0/R1 and their evidence remain intact. No application code changes required for this gate. If a supposed approved document cannot be recovered, do not invent approval—draft and seek it.

**Evidence:** repository paths/SHA, decision ledger with dates, crosswalk, links to R1/C-02. **Not a success claim:** OR-003 replacement, live event inventory, production Docker end-to-end, and other untested functions are not marked satisfied.

### V3A-01 — Shared Design System & Component Foundation

**Goal:** Implement coherent reusable tokens and components without breaking existing accessible behavior.

**Deliverables:**
- Existing typography retained: Montserrat for display, Open Sans for body/UI.
- Primary blue `#1B4383`, pale powder-blue outside/hero treatment, functional warm orange, mostly white content surface, restrained additional accents.
- Shared spacing, responsive layout, accessible link/card/button focus, headings, state presentation, image sizing and semantic patterns.
- Reusable styles applied to navigation, section headings, editorial previews, resource and flyer containers **without** a new monolithic component per content type.
- High-contrast mode preservation and CSS-token compliance; no unsupported fallback-literal token pattern.

**Acceptance plan:** S-01, S-03–S-05 initial regression; focused screenshot comparisons at 375/1280 and intermediate width; keyboard/tab order, focus visibility, contrast, loading/status tests. **Do not change** eligibility, data classification or URL structure for styling convenience.

**Evidence:** changed tokens/components, before/after screenshots, targeted accessibility/frontend test logs, build output.

### V3A-02 — Homepage Assembly

**Goal:** Build P01's **five approved rows** using eligible existing content and shared components.

**Deliverables:**
1. Global navigation in approved hierarchy (final route cleanup owned by 08).
2. **Compact** text statement on pale blue **transitioning into Wilmington cityscape image**, not image transitioning into text; intro paragraph stays in Row 3. Initial tagline: “Your guide to community resources, program updates and local information.” Copy is **editorially flexible**, not hard-coded as an immutable product invariant.
3. Left: one/two short introduction paragraphs and a **separately maintained** What's New administrator-selected note; right: vertical upcoming Community Events flyer preview with honest empty state.
4. Left: exactly eight approved category doors, **2 columns × 4 rows** on desktop; right: original reporting previews (title, summary, date, image/fallback, detail link), with appropriate related expert perspectives but no reclassification of every FAQ/Expert as journalism.
5. Left: Community Notices (nongovernment); right: Program Updates (primarily government), with clear linking and source-sector distinction.

**Acceptance plan:** H-01–H-09 via viewport captures, interactive clicks and fixture/live eligibility tests. Ensure FL-001–007 never recur; OR-003 fixture must not appear as genuine reporting (resolved with 06; interim 02 can suppress it). No visual filler when no future event exists. No invented ninth category or oversized hero.

**Evidence:** five-row annotated screenshots at desktop/mobile; traced preview links; event empty-state evidence; editorial-note content source/maintenance mechanism; test logs.

### V3A-03 — Category Destination / Housing

**Goal:** Deliver P02 while retaining approved backend category projection and deterministic selection.

**Deliverables:**
- Housing hero: concise heading/intro on pale blue, building illustration on right, responsive.
- Four featured topics selected **by backend approved deterministic logic**; each card: orange icon alongside heading in shaded band; up to three independently clickable content links; heading opens Topic page, each link opens its actual source/detail.
- Remove the **standalone visual Get Help block**, not the underlying help projection; prominent All Housing Help route.
- Left flyer preview (up to two eligible posters); right What to Know: Policy Info, Law, Flyers, FAQs and Expert Sessions, per existing collection contracts.
- Separate **in-place switching rail**: First Rate News ↔ Videos, not stacked, no Latest Laws third tab.
- Lower Organizations related to offerings. Graceful sparse states and generalization beyond Housing.

**Acceptance plan:** C-01–C-09; validate four-featured winners, item role/temporal eligibility, correct heading-vs-item destinations, Help inventory continuity, switcher focus, and generalization with at least one other category. Do not replace selector with frontend ranking.

**Evidence:** controlled and live projection/API captures, Housing at 375/1280, secondary-category example, individual item navigation, tests of deterministic winners and no withdrawn flyers.

### V3A-04 — Topic Destination & All Category Help

**Goal:** Refine already working `/category/:key/:topic` and `/category/:key/help` routes without duplicating classification.

**Deliverables:** accessible topic headings/context/breadcrumbs; resource-first results with valid destinations; understandable missing/invalid/empty states; complete eligible Help inventory with progressive navigation; correct Category ↔ Topic ↔ All Help paths.

**Acceptance plan:** T-01–T-06 and AH-01–AH-05. Audit inventory equality against backend eligible resource projection and prove all pages of progressive results are reachable. Show both a populated and an empty/invalid case. **All Help ≠ all CivicContent**.

**Evidence:** data/count reconciliation, Playwright/journey results, mobile and keyboard evidence, test suite.

### V3A-05 — Community Notices, Events & Flyers

**Goal:** Complete usable Community Notices with **any inventory size** while preserving C-02 holds.

**Deliverables:** overview and four shareable views; event-date separation of upcoming vs historical; a readable, accessible poster enlargement/inspection interaction; useful zero-content states; translated/not-raw invalid-view/error messaging; links and mobile navigation.

**Acceptance plan:** N-01–N-08. Test against live catalog (possibly no current events) and clearly marked controlled fixture proving actual future-event behavior. Confirm the seven withdrawn flyers absent from homepage/notices/updates/category APIs and 404 direct detail. FL-008 may be historical, never upcoming after its event. Producer attribution for withdrawn FL-005/006/007 remains unresolved; **do not silently reassign**.

**Evidence:** C-02 closure SHA and 12 regressions as inherited controls, new modal/inspection tests, current-clock and controlled-clock route checks, live/fixture screenshots.

### V3A-06 — Program Updates, First Rate News & Article Detail

**Goal:** Complete P06's original-journalism presentation and government-update discovery without introducing editorial automation.

**Deliverables:** Program Updates explicitly takes/applies Community scope; government producer criteria remain controlled. First Rate News displays actual approved original reporting and approved related expert previews under current rules. Article detail preserves applicable byline, summary/body, dates and source/disclosure text where supported by the existing public contract. Prior publication is optional metadata, not prerequisite to First Step original reporting. Implement correct not-found versus 503/error handling.

**C-01 remediation:** OR-003 is a test fixture; **remove it from resident-facing publication eligibility or replace with a genuinely approved article**, retaining test coverage separately. If no genuine article is approved, display truthful empty state. The supplied Wilmington Home Repair article is a possible candidate but is **not** automatically approved, licensed or imported by a developer. Human administrator controls approval. No automatic citation verification or interviews audit.

**Acceptance plan:** U-01–U-09, including no public fixture, nonapproved/withdrawn/404 parity, valid original with no external publication date, community scoping and 503 distinction. Preserve FAQ/Expert identities (no universal Article classification).

**Evidence:** per-community API test, public article DTO surface, approved-content/fixture controls, homepage/Housing links, browser journeys and error-state screenshots.

### V3A-07 — Resource, Link & Organization Presentation

**Goal:** Ensure offerings lead residents to actionable destinations; organizations stay secondary.

**Deliverables:** consistent resource cards across Category/Topic/Help/Find Help/Organization; truthful disabled/unlinked presentation for missing URL; accessible organization detail with existing scope and error states; preserved Community/category context. No organization registry redesign.

**Acceptance plan:** R-01–R-05, including representative linked/unlinked resources, in-app versus external destinations, missing org and server-error semantics, mobile/accessibility. External website truth/availability is not automatically proven by successful link rendering.

**Evidence:** test URL matrix, route journeys, error screenshots, targeted suites.

### V3A-08 — Find Help & Cross-Page Integration

**Goal:** Complete the P08 gateway and reconcile deliberately stubbed navigation with approved resident journeys.

**Deliverables:** preserve `/find-help`, category gateways, open-to-everyone offerings and organization search; correct “1 organizations,” provide actionable no-match/zero states, restore search state on Back/Forward. Finish route decisions:
- `/discover`: do not build competing discovery; remove or redirect links to an existing Category/Find Help destination with accessible communication.
- `/community`: resolve navigation to implemented `/community-notices` rather than leave a live stub.
- `/about`: if header nav retains it, implement a simple informative About page using approved copy, no backend service; alternatively change/remove nav by approved decision (do not invent lengthy editorial claims).
- `/discover/:facet`: defer specialized facets, e.g. Seniors; eliminate active misleading doors. Direct requests receive a truthful unavailable/not-found state under existing route policy.
- Catch-all, invalid-view, placeholder heading and raw error-message corrections; preserve deep links/history and Community context.

**Acceptance plan:** F-01–F-07; S-05/S-06. Test Home→Find Help→Category, Find Help→Organization→Back, Home→Updates, Category→Article, Notices view direct entry, invalid paths, and browser Back/Forward at desktop/mobile.

**Evidence:** approved route disposition table with old→new behavior, focused searches and history tests, end-to-end browser traces/screenshots, navigation regression suite.

### V3A-09 — Integrated Quality, Acceptance & Closure

**Goal:** Demonstrate V3A as a complete resident experience, not just independently passing pages.

**Deliverables:** all-IDs acceptance crosswalk; end-to-end journey matrix; visual comparison record to original reference and approved later refinements; accessibility review; responsive snapshots (375, 1280 and sensible intermediate width); full regressions/build/validators; documented production-package check where available; closure decision and deferred-work register.

**Acceptance plan:** S-01–S-09 and final confirmation of H/C/T/AH/N/U/R/F criteria. Verify 503-vs-404, empty events, no withdrawn flyer, no fixture-as-news, Community context, direct links, correct category count, no stub navigation. If local development jar does not serve `/app-next`, test combined production package separately rather than misclassify the development artifact as a production failure.

**Evidence:** immutable dated evidence index, commit SHAs, full commands and exit codes, browser captures, criterion verdicts (`Pass`, `Fail`, `Not verified`, `Deferred by approved scope`), final sign-off. **Never mark unverified as passed.**

## 5. Contract-to-package traceability (primary owner)

| Contract criteria | Owning package | Other required integration |
|---|---|---|
| H-01–H-09 | 02 | 01 visual; 05 events; 06 editorial/Updates; 08 nav; 09 proof |
| C-01–C-09 | 03 | 01 styles; 04 All Help; 05 flyers; 06 rail content; 09 proof |
| T-01–T-06 | 04 | 03 links; 07 resource/organization cards; 08 routing |
| AH-01–AH-05 | 04 | 03 All Help entry; 07 links |
| N-01–N-08 | 05 | 02 events preview; 03 flyer preview; 08 nav |
| U-01–U-09 | 06 | 02/03 previews; 08 nav |
| R-01–R-05 | 07 | 03/04/08 link use |
| F-01–F-07 | 08 | 02 global entry; 04/07 destination integrity |
| S-01–S-05 | 01 (foundation), 09 (final) | Each package responsible for its own page |
| S-06 | 08, 09 | 02 nav initial integration |
| S-07–S-09 | 09 | All packages supply evidence and retain invariants |

**IDs C-01 and C-02 are reused in the page contract as Category acceptance identifiers, and separately appear as historic content-integrity findings.** Always disambiguate **`P02/C-01`** versus **`audit C-01`**, and **`P02/C-02`** versus **`audit C-02`** in coding-agent tickets and evidence.

## 6. Cross-cutting test and evidence contract

### Required verification dimensions

1. **Data/publication:** lifecycle states, Community, producer sector, temporal eligibility, deterministic featured ordering and source/item link target.
2. **Functional:** direct deep link, in-app link, browser Back/Forward, error and empty state, progressive inventory and switcher interaction.
3. **Accessibility:** headings/landmarks, labels, keyboard traversal, visible focus, appropriate role/status and high-contrast mode; mobile legibility.
4. **Responsive/visual:** approved reference plus later decisions; 375 and 1280 widths at minimum; capture overflow or clipping evidence.
5. **Engineering:** targeted unit/integration/browser tests, full backend/frontend/Python/validator gates at final package, built production artifacts.

### Baseline commands (confirm against current repo before execution)

```bash
# Backend, using the project-prescribed JDK 25/Maven toolchain
mvn -B verify

# Frontend (existing Node 26 workaround T-01)
cd frontend
NODE_OPTIONS=--no-experimental-webstorage npm test -- --run
npm run build

# From repository root
python3 -m unittest discover -s data-cleaning/tests -v
python3 data-cleaning/scripts/validate_content_sources.py
python3 data-cleaning/scripts/validate_homepage.py
python3 data-cleaning/scripts/validate_navigation.py
python3 data-cleaning/scripts/validate_news.py
python3 data-cleaning/scripts/validate_organizations.py
python3 data-cleaning/scripts/validate_schema.py
python3 data-cleaning/scripts/validate_schema.py --input app/data/resources.communities.json
```

The last seven lines are the **six standard catalog validators plus a second schema invocation for `resources.communities.json`**. Do not conflate seven commands with seven validator programs. Tooling issue **T-01** may be addressed by a small explicit tooling change if approved; do not bury an unrelated Node upgrade in styling work. Full-suite counts are **not fixed contract requirements**; report actual counts from each run. C-02's recorded baseline was backend 643/643, frontend 162/162, Python 16/16, seven validator commands passing.

### Evidence template for every package

- Package ID, owner/agent, date, branch, initial/final commit SHA and working-tree status.
- Approved criterion IDs and scope boundaries; files changed, excluding generated build outputs.
- Screenshots/route/API evidence linked by criterion and viewport; label live versus fixture.
- Targeted and full commands, results and environment/workaround details.
- Before/after behavior summary, failures, open defects, no-regression notes and deferred items.
- Exact recommended disposition: **Accepted**, **Accepted with documented nonblocking exceptions**, **Blocked**, or **Not verified**. Only product-owner approval closes a substantive scope deviation.

## 7. Known audit findings and ownership

| Finding | Status entering framework | Owner / treatment |
|---|---|---|
| **Audit C-01** OR-003 original is a fixture | Editorial requirements approved; resident-facing resolution not yet demonstrated | 06; prevent misleading exposure earlier in 02 |
| **Audit C-02** seven flyer mismatches | Correction committed at `f4ae9e0` | Preserve exclusion across 02/03/05/06/09; no reopening history |
| **A-01** Updates lacks explicit Community request | Approved architectural requirement, not yet demonstrated fulfilled | 06 |
| **A-02** Find Help placement | Direction approved | 08 with 02 navigation |
| Flyer enlargement | Not demonstrated in R1 | 05 |
| Events populated journey | Controlled fixture only; live inventory lacked current event | 05/09; sparse case valid |
| Article 503 shown as 404 | Known functional defect | 06 |
| Stubbed `/discover`, `/community`, `/about` and facet | Deliberate legacy scaffolding; route decisions approved | 08 |
| Invalid Notices view raw i18n/server error | Known functional defect | 05/08 |
| Find Help singular/no-result/history issues | Known functional defects | 08 |
| Placeholder 404 heading/error copy | Known functional defect | 08 |
| Runtime packaging caveat (dev jar vs combined Docker image) | Production package not exercised in V3A-00 | 09 |
| **T-01** Node 26 webstorage test quirk | Existing documented workaround | 09 verification; optional narrow tooling task |
| `mediaReview` MR-1–MR-8, editorial dashboards and automated workflows | **Deferred** | V3B/Later; not closure blockers |
| Limited verified upcoming event/flyer supply | Editorial catalog stewardship need, not app defect by itself | 05 empty state; editorial replenishment outside framework |

## 8. Change control, sign-off and next work

### Change classification

- **Bug/functional completion:** Already governed by approved contract; may be implemented in its mapped package with tests.
- **Presentation implementation:** May follow accepted reference and specific decisions; maintain accessibility and truthful content.
- **New resident capability or model change:** Requires explicit product-owner approval and contract amendment before coding.
- **V3B/Later proposal:** Record in deferral register; cannot become a V3A dependency by convenience.

### Planned milestones

1. Product owner reviews and approves this proposed framework.
2. Consolidate and approve the Homepage/Housing Design Decisions companion document; attach the original visual reference in repo if accessible.
3. Agent finishes V3A-00 closure addendum and contract crosswalk, without asserting that V3A itself is implemented.
4. Execute V3A-01 through V3A-08 under these bounded specifications and package gates.
5. V3A-09 supplies integrated end-to-end evidence and an explicit final acceptance decision.

**Framework approval status:** **Proposed / awaiting product-owner approval**. The P01–P09 page requirements themselves are already approved. Approval of this framework authorizes its package ownership and gating method, **not unconditional permission for unreviewed new features**.
