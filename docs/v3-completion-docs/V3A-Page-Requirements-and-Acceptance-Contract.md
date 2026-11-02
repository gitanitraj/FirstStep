# First Step V3A — Page Requirements & Acceptance Contract

**Version:** 1.0 — Consolidated approved requirements  
**Date:** October 24, 2026  
**Project:** First Step  
**Phase:** V3A — Resident Experience & Frontend  
**Approval basis:** Product-owner section-by-section approval of P01–P09 in the V3A planning conversation. This document records approved product requirements; it does **not** claim implementation acceptance or test completion.

## 1. Purpose, governing principles and scope

V3A delivers a coherent, accessible, mobile-first resident experience using First Step's existing civic catalog and approved backend architecture. First Step helps residents discover programs and services, community information, government program updates, and original reporting without advocacy or distortion of source facts.

The principles governing every page are:

1. **Residents first:** Information must be understandable, findable, and actionable.
2. **Category is the scope; destination is the experience:** Use controlled, resident-meaningful projections instead of exposing internal taxonomy.
3. **Resources and services are primary:** Organizations provide context, not a replacement for the actual help offered.
4. **Original journalism is distinct from curated information:** First Rate News is First Step's original reporting and explanatory journalism for its community.
5. **Editorial judgment is human:** Administrators and authors determine publication readiness; the application enforces the resulting publication lifecycle.
6. **Only eligible content is shown:** Honor approved, withdrawn, expiry, event-date and Community eligibility rules.
7. **Preserve the approved architecture:** No unnecessary rebuild of functional pages, CivicContent changes, or new ranking engine.
8. **Accessible, responsive design:** Core resident actions must work on desktop and mobile.

**In scope:** Homepage; Category, Topic and All Help destinations; Community Notices and flyers; Program Updates; First Rate News presentation and article detail; resources and organizations; Find Help and navigation; shared visual design and integrated quality gates.

**Out of scope:** Full Admin UI or catalog CRUD, automated editorial review or independent fact-checking, automated reporting generation, editorial queues and assignment, generalized eligibility/recommendation systems, new content types, sophisticated organization profiles, second-community launch, database migration, and other V3B/Later features.

### Visual authority

The primary composition reference is the user's Library image **`First Step original visual reference.png`**. The image should be attached or linked in the repository documentation when made available there. It governs the intended visual composition **subject to later explicitly approved refinements**, including the hero wording and placement, Homepage row arrangements, Housing category layout and other decisions recorded below. The reference does not govern outdated placeholder text, impose a pixel-perfect requirement, or override accessibility and content eligibility. At preparation time, the reference is identified in the Library, not embedded in this Markdown file.

## 2. P01 — Homepage

**Resident purpose:** Introduce First Step, invite navigation into community resources, and present curated original reporting and local information without overwhelming residents.

### Required five-row composition

**Row 1 — Global navigation.** Retain a coherent site header and working links to approved resident destinations. Final route behavior is governed by P08.

**Row 2 — Compact hero.** A concise text statement on a **pale-blue background transitions smoothly into an image of the Wilmington cityscape**, creating one continuous composition on desktop with a suitable mobile adaptation. The hero remains compact and contains no long introductory paragraphs; the fuller introduction belongs in Row 3.

Initial text: **“Your guide to community resources, program updates and local information.”** The statement is **editorially flexible**: an administrator may change the wording without amending the acceptance contract, provided the hero's purpose, composition and accessibility remain intact. Describing the service as “trusted” is not a tagline requirement; trust is earned.

**Row 3 — Introduction and Community Events.** The left column contains a small First Step introduction of **one or two short paragraphs** and, beneath it, a visually distinct, separately maintained **What's New** editorial note. What's New is administrator-selected copy, not an automatically generated newest-item feed; it may link onward, and it need not change on a fixed schedule. The right column presents a vertical Community Events flyer preview emphasizing eligible **upcoming** opportunities, with direct links to event information or Community Notices. If no events qualify, present a truthful empty state. On mobile the content stacks without losing any primary action.

**Row 4 — Browse by Category and First Rate News.** On desktop, the left side contains the **eight approved category cards in two columns and four rows**, each linking to its Category Destination. The right side presents editorial previews of approved First Rate News reporting with headline, short summary, date, suitable photograph or fallback, and direct article link. Related expert perspectives may appear under approved collection rules; FAQs and Expert Sessions do not automatically become journalistic articles. Preserve a balanced editorial layout rather than enforcing identical card styles or counts.

**Row 5 — Community Notices and Program Updates.** Community Notices previews appear **left**, representing nongovernment community organizations' program notices and opportunities. Program Updates previews appear **right**, representing primarily government-produced program and policy information. Preserve clear producer/purpose distinctions, Community scope and content eligibility, with direct links to the respective collections.

### P01 acceptance criteria

- **H-01:** All five rows appear in the approved order and adapt responsively without losing content or action paths.
- **H-02:** The compact hero uses the approved *text-on-pale-blue into Wilmington cityscape* treatment, not the reverse; the introduction remains outside it.
- **H-03:** What's New is separately editable editorial copy, distinct from the stable introduction and automated news listings.
- **H-04:** Exactly the eight approved categories appear in the intended desktop 2×4 layout; no invented category data.
- **H-05:** Eligible approved editorial previews lead to their correct destinations and have readable headline/summary/date treatment.
- **H-06:** Community Events shows eligible upcoming events, or a usable empty state when none exist.
- **H-07:** Community Notices and Program Updates remain distinct and link to their full collections.
- **H-08:** Homepage navigation, keyboard access, focus, high contrast and responsive behavior are demonstrated at 375px and 1280px.
- **H-09:** Withdrawn, expired, otherwise ineligible or misleading fixture content is not presented as current or genuine published information.

## 3. P02 — Category Destination (Housing acceptance reference)

**Resident purpose:** Provide practical access to help and related civic information within a category. Housing is the primary reference; approved structure must generalize without invented content.

**Housing hero.** Restrained pale-blue background, concise introduction and heading, with a building illustration at right. Follow the visual reference's hierarchy subject to responsive/accessibility requirements.

**Four Featured Housing Topics.** Display four featured-topic cards selected by the **existing approved deterministic backend rules** (when four eligible topics exist). Each has an **orange icon beside its topic heading in a shaded horizontal heading band**, and up to **three independently selectable content links**. Topic heading navigates to its Topic Destination; each individual content link navigates to its own appropriate source or detail. Preserve existing role-based selection, expiry, event-date and eligibility rules. No popularity or AI ranking.

**All Housing Help.** **Remove the separate Get Help presentation block** from the Category page, while preserving its underlying backend help projection. Provide a **prominent All Housing Help path** to the complete eligible help inventory.

**Housing flyers / What to Know.** Place a preview of one or two eligible Housing posters on the **left**, with **What to Know** on the **right**, including Policy Info, Law, Flyers, FAQs and Expert Sessions using approved collection rules. Flyers navigate to relevant Community Notices flyer content, not a competing system.

**Context rail.** A separate right-side **in-place switcher** for **First Rate News** and **Videos**. These are switchable views, not stacked columns, and no third Latest Laws tab is authorized.

**Organizations.** A lower section shows organizations connected to category resources and services; offerings remain primary.

### P02 acceptance criteria

- **C-01:** Housing follows approved hero, four-topic, All Help, flyer/What to Know, context rail and Organizations hierarchy.
- **C-02:** Four topics are selected deterministically where four eligible topics exist; selection does not rely on client-side AI or engagement ranking.
- **C-03:** Item slots honor existing content eligibility, event/expiry and role-based selection rules.
- **C-04:** Topic headings and individual item links navigate independently and correctly.
- **C-05:** All Housing Help remains prominent and provides access to full eligible help inventory.
- **C-06:** Standalone Get Help visual block is removed without deleting the backend projection or help journey.
- **C-07:** What to Know, flyer preview, First Rate News/Videos switching rail and Organizations function as distinct presentations.
- **C-08:** Sparse or absent content yields truthful empty/partial states; no invented filler.
- **C-09:** Mobile, accessibility and controlled generalization to other categories are demonstrated.

## 4. P03 — Topic Destination

**Resident purpose:** Let residents enter a specific topic (e.g., Eviction Prevention or Homeownership), browse eligible offerings and information, and reach useful destinations directly.

Retain `/category/:key/:topic`, category context and breadcrumbs. Reuse existing topic classification and eligibility; prioritize actionable programs/services, with organizations providing supporting context. Keep direct content/source links where real destinations exist. Support increasing inventory without repetitive internal taxonomy or a new filtering engine.

### P03 acceptance criteria

- **T-01:** Valid topic routes resolve to properly scoped topic content.
- **T-02:** Existing classification and eligibility rules govern results.
- **T-03:** Category context and breadcrumbs support return navigation.
- **T-04:** Content destinations work where source/detail information exists; missing links are not fabricated.
- **T-05:** Empty, unavailable and invalid-topic states are useful and accessible.
- **T-06:** Responsive browsing works without horizontal overflow or inaccessible controls.

## 5. P04 — All Category Help

**Resident purpose:** Provide the **full resident-eligible help inventory** within a category, not only a few featured selections. “All Help” means eligible help resources and services, **not every CivicContent item** (laws, reporting and policy collections have their own destinations).

Retain `/category/:key/help`, existing progressive listing and topic navigation, and resource-first emphasis. An organization is accessible when relevant but is secondary to its offerings. Never silently cap the eligible inventory to the number shown in a preview.

### P04 acceptance criteria

- **AH-01:** Every resident-eligible help resource within the category is reachable.
- **AH-02:** Progressive disclosure does not lose or silently omit eligible records.
- **AH-03:** Residents can navigate among Category, Topic, All Help and appropriate resource destinations.
- **AH-04:** Withdrawn and expired content is excluded according to approved eligibility rules.
- **AH-05:** The page remains accessible and usable as inventory grows.

## 6. P05 — Community Notices and Flyers

**Resident purpose:** Find community-originated events, meetings, announcements and flyers with reliable dates and readable posters, even when very few eligible items exist.

Retain the four shareable views: **Events, Meetings, Announcements and Flyers**, plus Community Notices overview. Preserve approved nongovernment producer classification. Past events must not masquerade as upcoming events; an ongoing program flyer need not be treated as a dated event. Provide accessible means to inspect/enlarge posters. Handle no-results, invalid views and loading/errors clearly.

**C-02 completed integrity correction:** FL-001 through FL-007 were withdrawn because the catalog descriptions did not match the actual posters. The underlying records and images remain retained for correction; withdrawal must be honored in all resident projections and public detail endpoints. FL-008 follows existing temporal eligibility and may remain a historical flyer, but its October 17 event is not upcoming. The C-02 correction is documented on `copyofdev` at commit `f4ae9e05f8e5687d9287dba82e5cf5f1a847ca59` and `docs/v3-completion-docs/C-02-flyer-integrity-closure.md`. Proposed `mediaReview` requirements MR-1–MR-8 are deferred and are **not** V3A implementation obligations.

### P05 acceptance criteria

- **N-01:** All four views and their direct/shared URLs work.
- **N-02:** Producer-sector distinctions follow existing controlled ContentSource classification.
- **N-03:** Upcoming versus concluded event treatment follows real dates and approved eligibility rules.
- **N-04:** Posters have an accessible enlarged/readable inspection experience.
- **N-05:** Withdrawn flyers are excluded from public previews, collections and details.
- **N-06:** Empty or sparse collections yield truthful and useful resident experiences, without synthetic filler.
- **N-07:** Invalid views and failures do not expose raw translation keys or technical errors.
- **N-08:** Mobile layout retains flyer legibility and navigation.

## 7. P06 — Program Updates and First Rate News

**Program Updates** presents appropriately scoped **government-produced** program changes, policy developments and related civic information. Community scope is explicit in retrieval/presentation; producer attribution and eligibility follow approved rules.

**First Rate News** is First Step's **original reporting and explanatory journalism** created for its communities. It is not simply an imported news aggregation page. Administrator-approved previously published work may also be admitted when appropriate, but **prior external publication is not required** for an article created exclusively for First Step. Related Expert Sessions may be surfaced in the resident-facing collection under approved rules; FAQs and Expert Sessions retain their distinct identities and may be produced by organizations, administrators using source material or interviews. They are not automatically journalistic articles.

**C-01 accepted editorial requirements:**

1. Original First Rate News reporting can enter the established approval path without a prior external publication.
2. Preserve applicable byline, First Step publication and update dates, text, source notes and disclosures.
3. Require prior-publication metadata **only when relevant** to republished/adapted work.
4. Administrators—not automated verification software—make editorial approval decisions; the existing lifecycle is enforced.
5. Only eligible/approved content is publicly shown.
6. Approved original articles are discoverable in First Rate News and relevant Category Destinations.
7. OR-003 remains a testing fixture and must not masquerade as actual resident-facing original reporting.

Reporting may be based on interviews and other original newsgathering; the application is **not** required to independently fact-check or reproduce that editorial work. The genuine Wilmington Home Repair article is a possible intake candidate but is not deemed ingested or approved by this contract.

### P06 acceptance criteria

- **U-01:** Program Updates presents properly scoped government-produced content.
- **U-02:** Community scope is explicitly honored in Program Updates retrieval/presentation.
- **U-03:** Original reporting and curated government updates are clearly distinguished.
- **U-04:** Approved original article detail renders required public-facing editorial information.
- **U-05:** Republishing-specific metadata is not imposed on First Step-original articles.
- **U-06:** Unapproved, withdrawn or otherwise ineligible articles remain publicly unavailable.
- **U-07:** OR-003 is not presented to residents as genuine reporting.
- **U-08:** Article detail pages distinguish a true missing article from server failures.
- **U-09:** Expert Sessions and FAQs retain distinct classification and approved discovery treatment.

## 8. P07 — Resource and Organization Navigation

**Resident purpose:** Lead people from discovering a resource or service to its actionable destination and supporting organization details, where available.

Resource/service offerings come before organization identity in help-oriented projections. Use only real, supported internal/external destinations. Preserve Community/category context and improve existing organization pages within presentation scope; no sophisticated organization-profile platform is required.

### P07 acceptance criteria

- **R-01:** Eligible offerings navigate to valid supported internal or external destinations where provided.
- **R-02:** Organization context is reachable without displacing the offering-first experience.
- **R-03:** Missing link targets are presented truthfully, not as functional links.
- **R-04:** Organization detail distinguishes available, unavailable and retrieval-failure states.
- **R-05:** Navigation and responsive treatment remain consistent across Category, Topic, Help and Organization surfaces.

## 9. P08 — Find Help and Integrated Navigation

**Resident purpose:** Provide a separate gateway for people seeking programs, services and organizations, complementing rather than copying Homepage category browsing.

Retain `/find-help`, existing category gateways, open-to-everyone resources and organization search. Correct singular/plural result counts, improve empty/no-match guidance, and preserve useful search/navigation state on return. Maintain accessible, functional browser Back/Forward behavior.

**Approved route dispositions:**

- **`/discover`:** Do not create a second competing Category/Find Help experience. Existing Category and Find Help pathways provide discovery. Remove, redirect, or otherwise resolve misleading live links consistently with approved navigation; exact implementation is determined in the package crosswalk.
- **`/community`:** Direct residents to the implemented Community Notices experience rather than a competing stub.
- **`/about`:** If retained in global navigation, implement a **simple, informative About First Step page**; no new backend is required.
- **`/discover/:facet`:** Specialized discovery (including Seniors) remains deferred unless an existing approved journey expressly requires it. Do not expose an unfinished pathway as working navigation.

The repository's `frontend/src/App.tsx` annotations describe these stubs as deliberate temporary scaffolding from the earlier homepage-composition slice; their existence does not independently authorize new resident destinations.

### P08 acceptance criteria

- **F-01:** Find Help is reachable through approved navigation.
- **F-02:** Category gateways open correct destinations.
- **F-03:** Organization search reports grammatically and numerically correct counts, with useful no-match feedback.
- **F-04:** Search/history state behaves predictably on return from details.
- **F-05:** Open-to-everyone offering cards navigate appropriately.
- **F-06:** Find Help does not recreate a competing Homepage Browse by Category system.
- **F-07:** Mobile navigation, labels, focus and accessibility meet shared requirements.

## 10. P09 — Shared Design, Accessibility and Integration

**Approved visual system:** Primary blue `#1B4383`; functional warm-orange accents; pale powder-blue exterior/background; predominantly white content surfaces; restrained brick-red/yellow accents; Montserrat display type and Open Sans body/interface text. Follow the identified original reference for composition as modified by later approvals; do not mistake temporary placeholder content for requirements.

Use consistent global frame, spacing and interactive patterns. Preserve high-contrast mode, semantic headings, keyboard focus, labeled controls and accessible loading/error/no-result states. Required journeys must be demonstrated at desktop and mobile sizes (including 1280px and 375px). Eliminate misleading active navigation to unfinished placeholders. Preserve Community scope, publication lifecycle, controlled resident projections and regression coverage; do not reimplement already working services simply for redesign.

### P09 acceptance criteria

- **S-01:** Shared design tokens, visual treatment and typography are applied consistently.
- **S-02:** Homepage/Housing match the approved visual hierarchy and subsequently approved modifications.
- **S-03:** All contracted experiences work on desktop/mobile without meaningful content loss.
- **S-04:** Headings, keyboard flow/focus, labels, announcements and high-contrast accessibility meet existing standards.
- **S-05:** Loading, empty, unavailable and error states are meaningful and resident-facing.
- **S-06:** Global navigation does not advertise unresolved placeholders as functional destinations.
- **S-07:** Relevant frontend/backend tests, validators and production builds pass, with environment limitations noted.
- **S-08:** End-to-end journeys are evidenced using eligible live records or expressly identified controlled fixtures; these two evidence classes are not confused.
- **S-09:** Content eligibility, event/expiry lifecycle and Community rules survive integration.

## 11. Cross-cutting acceptance and evidence rules

- **Content integrity:** Do not show fixtures as genuine news; do not show withdrawn/ineligible content.
- **Fixture versus live evidence:** A fixture-based passing test proves behavior, not availability of equivalent authentic live content.
- **Inventory honesty:** A sparse collection or empty event preview is acceptable and requires a usable truthful state, not filler content.
- **Existing capabilities:** A passing legacy function should be retained; rewriting it is not a deliverable in itself.
- **Editorial ownership:** Administrators are responsible for source evaluation, reporting accuracy, reviewing interviews, publication decisions and future content replenishment. V3A enforces public eligibility and approved display, not an automated editorial workflow.
- **Reference precedence:** Approved later decisions supersede contradictory elements of the original design image.
- **Acceptance evidence:** Distinguish implementation complete, demonstrated against controlled fixtures, observed in the live catalog, and not yet verified.

## 12. V3A final acceptance conditions

V3A may be accepted only when:

1. Every approved P01–P09 acceptance criterion has an evidence-supported disposition.
2. Integrated, accessible resident journeys work across contracted pages.
3. The approved composition and shared visual system are implemented.
4. Public content presentation honors human editorial approval and eligibility rules.
5. Required tests, validators and applicable builds pass; environment constraints are documented.
6. No unresolved critical resident-facing defect remains.
7. Deferred V3B/Later matters are clearly separated from unmet V3A obligations.
8. Final acceptance, evidence and decisions are recorded in the repository.

## 13. Approval and change record

| Date | Decision | Status |
|---|---|---|
| October 2026 | P01 Homepage five-row composition, with Row 2 text-on-pale-blue fading into cityscape; flexible initial tagline; separate What's New note | Approved |
| October 2026 | P02 Housing Category Destination composition and deterministic featured content, All Help, rail and Organizations | Approved |
| October 2026 | P03 Topic Destination, T-01–T-06 | Approved |
| October 2026 | P04 All Category Help, AH-01–AH-05 | Approved |
| October 2026 | P05 Community Notices/Flyers, N-01–N-08 | Approved |
| October 2026 | P06 Program Updates/First Rate News, U-01–U-09 and accepted C-01 requirements | Approved |
| October 2026 | P07 Resource/Organization Navigation, R-01–R-05 | Approved |
| October 2026 | P08 Find Help/Navigation, F-01–F-07 and route dispositions | Approved |
| October 2026 | P09 Shared Design/Accessibility/Integration, S-01–S-09 | Approved |
| October 2026 | C-02 seven-flyer withdrawal remediation | Implemented and committed; retained as dependency/evidence, not new V3A work |

**Document status:** Consolidated contract of approved product requirements. Acceptance identifiers preserve the Draft 1 P01–P09 structure reviewed and approved in conversation. **Implementation compliance remains to be audited**; product approval of this contract is not a claim that its software acceptance criteria have been met.
