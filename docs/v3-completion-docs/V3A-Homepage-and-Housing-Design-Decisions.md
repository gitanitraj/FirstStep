# First Step V3A — Homepage and Housing Design Decisions

**Version:** 1.0 — consolidated decision record  
**Date:** October 24, 2026  
**Project:** First Step  
**Phase:** V3A — Resident Experience & Frontend  
**Status:** Consolidated from product-owner-approved design decisions; this document is the requested written decision record. It does not certify implementation.

## 1. Purpose, authority, and limits

This record preserves the **approved visual composition, content organization, and interactions** for the First Step Homepage (P01) and Category Destination, using Housing as the design reference (P02). It prevents drift during V3A coding without reopening approved decisions or creating requirements beyond the V3A scope.

**Governing companion documents** (approved October 24, 2026):

1. `V3A-Page-Requirements-and-Acceptance-Contract.md`, v1.0 — the controlling page-level requirements and acceptance criteria H-01–H-09 and C-01–C-09, together with cross-cutting P03–P09 requirements.
2. `V3A-Implementation-Framework.md`, Draft 1, **approved by the product owner** — bounded packages V3A-00–V3A-09, dependency gates, evidence obligations, and non-goals. Its embedded “awaiting approval” header is superseded by that subsequent approval; this record does not edit that file.
3. **`First Step original visual reference.png`** — primary visual-composition reference, identified in the user's First Step Library. **The image is not embedded here, and a repository copy/path is not yet verified.** Attach a controlled copy or link in the repository during V3A-00 documentation reconciliation, recording its path and provenance. Do not substitute subsequent exploratory mockups.

**Precedence for visual decisions:** explicit later product-owner approvals override conflicting details in the original visual image, including the hero text-to-cityscape direction, flexible tagline, distinct What's New note, and the refined Housing organization. Written acceptance criteria control behavior, navigation, editorial eligibility, and accessibility; the image provides composition and visual hierarchy. Do not treat placeholder headlines, counts, dates, or imagery in a mockup as actual catalog facts or mandatory copy.

**Implementation status:** “Approved” below means the product owner approved the **design decision**, not that it exists or passes tests in the application. Technical completion must be demonstrated under the acceptance contract and relevant V3A package.

## 2. Shared visual principles

- The site is **resident-first**, civic, restrained, legible, and editorial—not a crowded dashboard or a marketing landing page.
- Primary blue: **`#1B4383`**; pale powder-blue exterior and backgrounds; predominantly white reading surfaces; **warm orange** used for purposeful iconography/emphasis; brick red and yellow only as restrained supporting accents.
- **Montserrat** for approved display roles; **Open Sans** for body and interface roles. Retain existing typography tokens and accessibility/high-contrast architecture.
- Keep mixed **horizontal and vertical editorial composition**. Do not flatten distinct sections into a uniform grid of identical cards.
- Mobile-first behavior must preserve the hierarchy and actions of the desktop layout. Test at **375 px** and **1280 px**, with keyboard, focus, labels, high contrast, and truthful loading/empty/error states.
- Use **actual approved, resident-eligible** content. No synthetic article, flyer, event, category, or organization may be inserted to make a screenshot appear populated.

## 3. P01 — Homepage: approved five-row hierarchy

The Homepage is a curated front door, not a comprehensive data listing. Its rows appear in the order below. Desktop column directions are specified; mobile layouts may stack appropriately without removing content or actions.

### Row 1 — Global navigation

Keep a coherent site header and functional routes to approved resident destinations. Navigation integration and dispositions for legacy placeholders are governed by **P08**, not invented independently for this design. Do not add a second category-navigation system.

### Row 2 — Compact hero **[explicitly revised and approved]**

- A **concise text statement on a pale-blue background transitions smoothly into a Wilmington cityscape image**. The visual direction is **text/pale blue → cityscape**, **not cityscape fading into text**.
- On desktop the text and cityscape read as a continuous horizontal composition; on mobile, rearrange responsively without losing either element.
- Keep the hero **compact**. **No lengthy introductory paragraphs** belong here; the fuller explanation is in Row 3.
- Initial editorial statement: **“Your guide to community resources, program updates and local information.”**
- The tagline is **editorially flexible**, not immutable product-contract copy. An administrator may change it without reopening the acceptance contract if the hero's approved purpose, composition, and accessibility remain intact.
- Do **not** reinstate “trusted” as a required slogan: the product owner's explicit position is that **trust is earned**.

### Row 3 — Introduction + What's New (left); Community Events (right) **[approved]**

**Left column:**

- A small introduction of **one or two short paragraphs** explaining First Step's purpose and helping visitors orient themselves.
- Beneath the introduction, a **visually distinct What's New editorial note**, maintained separately from the stable introduction and from automated collection previews.
- What's New is selected or written by an administrator, can point to appropriate reporting or resources, and **need not change daily**. It should invite further reading without competing with First Rate News or becoming an automated “latest item” feed.

**Right column:**

- A **vertical Community Events flyer preview**, emphasizing real **upcoming** opportunities and offering a clear destination for event details or Community Notices.
- If no suitable upcoming flyer is eligible, present a useful, truthful empty state. Do not revive concluded events or require a minimum number of posters.

**Responsive:** Stack sections as needed on narrow screens while preserving introduction, What's New, and event access.

### Row 4 — Browse by Category (left); First Rate News (right) **[approved]**

**Left column:**

- **Exactly eight approved category cards**, **two columns × four rows** on desktop. They lead directly to Category Destinations.
- Use established category labels and purposeful iconography; never fabricate category records.

**Right column:**

- Curated **First Rate News** editorial previews with clear headlines, concise summaries, dates, suitable photograph or fallback, and direct article links.
- The right side should carry meaningful **editorial weight**, balancing the taller category-card grouping without requiring equal item counts or identical card design.
- First Rate News is **First Step's original reporting and explanatory journalism for the community**. Related expert perspectives may be surfaced according to the approved collection rules; an FAQ or Expert Session does **not** automatically become a journalistic article.
- Public previews must exclude misleading test fixtures (especially **OR-003** pending its V3A remediation) and any unapproved or otherwise ineligible item.

### Row 5 — Community Notices (left); Program Updates (right) **[approved]**

- **Left: Community Notices** — concise previews of nongovernment community organizations' announcements, programs, and opportunities; direct links to the full Community Notices experience.
- **Right: Program Updates** — government-produced program changes, policy information, and relevant civic updates; links to full Program Updates.
- The two sections are coordinated visually but **remain distinct by producer and purpose**. Apply explicit Community scope, existing producer-sector classification, and publication eligibility. Do not collapse them into a generic updates feed.

### Homepage acceptance mapping

| Decision area | Contract criteria | Primary implementation package |
|---|---|---|
| Five-row hierarchy, responsive balance | H-01, H-08, S-02–S-04 | V3A-02; shared foundation V3A-01 |
| Hero direction, tagline flexibility | H-02 | V3A-02 |
| Introduction and separate What's New | H-03 | V3A-02 |
| Eight categories, 2×4 | H-04 | V3A-02 |
| Original reporting previews | H-05, H-09; U-03–U-09 | V3A-02 + V3A-06 |
| Upcoming-event column / sparse state | H-06, H-09; N-03–N-06 | V3A-02 + V3A-05 |
| Producer-distinct Row 5 | H-07; U-01–U-02 | V3A-02 + V3A-05/06 |

## 4. P02 — Housing Category Destination: approved hierarchy

**Category is the scope; Destination is the experience.** Housing is the reference implementation, not permission to hard-code Housing behavior into other categories. Apply the approved controlled projections and classification to other categories without inventing content.

### 4.1 Housing hero **[approved]**

- Restrained **pale-blue** hero; heading and **brief introduction**; **building illustration on the right**.
- Follow the original visual reference's visual hierarchy and proportions as appropriate for a responsive page.
- Do not introduce an oversized promotional banner or overlong introductory copy.

### 4.2 Four Featured Housing Topics **[approved]**

- Present **four** featured-topic cards where four eligible topics exist, using the **existing approved deterministic backend selection**, initially driven by the strongest eligible topic inventories; do not hard-code the illustrative topics as permanent winners.
- Each card has an **orange icon beside the topic title inside a shaded horizontal heading band**.
- Each card contains **up to three individually selectable content items**. Entries must be true independent links: select the **topic heading** to open its Topic Destination; select a **specific item** to go to that item's appropriate source or detail.
- Preserve existing backend eligibility and role-based item selection, event/expiry distinctions, controlled intended-audience metadata where available, and approved deterministic tie-breaking. Do **not** introduce client-side AI ranking, engagement ranking, or a new classification system.
- The earlier examples **Eviction Prevention, Emergency Shelter, Homeownership, and Rental Assistance** are illustrative of inventory-driven selection, not a new hard-coded four-topic list.
- Preserve the original reference's recognizable topic-card treatment; do not substitute generic uniform resource cards.

### 4.3 All Housing Help, without a separate Get Help block **[approved]**

- **Remove the standalone Get Help visual presentation block** from the Housing Category Destination.
- **Keep the underlying Get Help backend projection and resident journey** intact. No deletion or redesign of the established help eligibility logic.
- Provide a **prominent “All Housing Help”** path to `/category/housing/help`, which reaches the **complete eligible Housing help inventory**, not merely featured items.
- All Help is for eligible programs, resources, and services; do not confuse it with the entire Housing corpus of laws, journalism, and policy notices.

### 4.4 Housing flyers (left) and What to Know (right) **[approved]**

- In the main content arrangement, **left**: preview of **one or two eligible Housing-related posters**, when available.
- **Right**: **What to Know** provides distinct pathways for **Policy Info, Law, Flyers, FAQs, and Expert Sessions** under existing collection/projection rules.
- The Flyers pathway goes to relevant **Community Notices flyer content**, rather than building a competing standalone flyer system.
- No need to manufacture flyers when the live Housing catalog contains none. Preserve empty/partial layout quality.
- Community opportunities that legitimately provide access to help may appear in the appropriate help projection as approved; the presence of a flyer alone does not turn an announcement into a service.

### 4.5 Separate switching context rail **[approved]**

- A separate **right-side context rail** switches **in place** between **First Rate News** and **Videos**.
- These are **two switchable views**, **not two permanently stacked lists**. Do **not** add a third “Latest Laws” tab.
- Related First Rate News content is an editorial discovery surface. Videos are relevant links/content when available; do not invent unsupported media catalog infrastructure.
- Maintain readable width and responsive access when the right rail stacks or changes placement on mobile.

### 4.6 Organizations section **[approved]**

- Provide a lower **Organizations** section identifying organizations connected to category resources and services.
- Organizations are **secondary to the help offering** and are presented through existing supported relationships and public routes. Do not generalize this into new organization-profile architecture.

### Housing acceptance mapping

| Decision area | Contract criteria | Primary implementation package |
|---|---|---|
| Overall hierarchy/visual fidelity | C-01, C-07, C-09; S-02 | V3A-03 |
| Four deterministic topics | C-02, C-03 | V3A-03, existing backend preserved |
| Independent topic/item navigation | C-04 | V3A-03/04 |
| Remove block; keep projection; All Help | C-05, C-06; AH-01–AH-05 | V3A-03/04 |
| Flyer/What to Know and eligibility | C-07, C-08; N-05 | V3A-03/05 |
| Two-mode right rail | C-07, C-09 | V3A-03 |
| Organizations and resource-first approach | C-07; R-01–R-05 | V3A-03/07 |

## 5. Editorial, identity, and lifecycle boundaries

These decisions apply to the Homepage and Housing and **do not introduce additional V3A content-model work**.

- **First Rate News:** First Step's original journalism and explanatory reporting. Original First Step articles require no prior outside publication; previously published reporting is a separate admissible pathway with appropriate approval and attribution. Expert Sessions and FAQs have their own identities and may be authored by organizations or First Step staff. Human administrators make editorial decisions. Do not build automated fact-checking, generation, review queues, or Admin UI for V3A.
- **C-01 fixture:** OR-003 is test content. Its public misrepresentation remains a V3A-06 implementation defect until exclusion/replacement is shown; approved editorial requirements alone are not evidence of remediation.
- **C-02 flyer correction:** FL-001–FL-007 are withdrawn because catalog records contradicted their posters; records and images remain retained. FL-008 is a genuine matched historical flyer, but its October 17, 2026 event must not appear as upcoming after that date. The C-02 change is recorded at `f4ae9e05f8e5687d9287dba82e5cf5f1a847ca59` with the closure report under `docs/v3-completion-docs/`. Deferred `mediaReview` MR-1–MR-8 are **not** V3A requirements.
- **Producer integrity:** Government-versus-community sector is based on actual producer identity, not merely whether a government agency cohosts an event. FL-005/FL-007 have documented unresolved government-attribution discrepancies; FL-006 has another producer/host discrepancy. These withdrawn records must not be treated as verified sector data or silently rewritten.
- **Community:** Keep Community scope first-class in resident retrieval; Program Updates scope is an approved requirement, not a proven preexisting implementation.
- **Sparse catalog:** A coherent page with an informative empty state is preferable to presenting false, withdrawn, expired, or old content as current.

## 6. Navigation interfaces and non-goals

The Homepage and Housing do not independently dictate new site-wide routes. Their links must integrate with approved P03–P08 destinations. Already approved route dispositions are:

- `/discover`: no second competing category-discovery experience; resolve live links through the existing Category/Find Help pathways.
- `/community`: connect residents to the implemented Community Notices experience rather than to a placeholder.
- `/about`: if kept in global navigation, provide a simple, accurate About First Step page without a new backend.
- `/discover/:facet`: specialized facets such as Seniors remain deferred unless an already approved resident journey specifically requires them.

Do **not** add a second-community launch, advanced profiles, a general-purpose video model, engagement-based ranking, new content types, a new admin/editorial system, or hard-coded content fabricated to fit the visual.

## 7. Visual-reference handling and implementation evidence

**Identified Library asset:** `First Step original visual reference.png` (First Step Library). At consolidation, its specific visual file has **not** been bundled into this Markdown artifact or verified on `copyofdev`.

**V3A-00 documentation action:** place or link a controlled copy inside the project repository, record its final path and hash, and cross-reference it from the authoritative contract, framework and this record. Do not assert a final repo path until verified. No replacement or new mockup is requested.

**Visual acceptance is not pixel matching.** Implementers must use the original composition plus later explicit approvals, inspect source screenshots and page behavior, and document deviations if mobile/accessibility constraints require adaptation. Evidence must include rendered desktop and mobile screenshots, functional navigation, content-eligibility behavior, and test results.

**Required evidence distinctions:** live eligible catalog vs controlled fixtures; code inspection vs browser observation; editor-approved content vs test fixtures; approval of design vs technical implementation. Where the catalog is sparse, show the real sparse state and label controlled examples separately.

## 8. Decision and approval ledger

| Decision | Authority/status | Implementation implication |
|---|---|---|
| Homepage five-row hierarchy | Product owner approved rows 2–5; Row 1 retained with P08 integration | V3A-02 + V3A-08 |
| Hero statement on pale-blue transitioning into cityscape | Explicitly revised and approved October 24, 2026 | V3A-02, H-02 |
| Initial tagline without “trusted”; flexible editor copy | Explicitly approved October 24, 2026 | V3A-02; no contract amendment for copy change |
| Separate What's New below 1–2 paragraph intro; right-side event flyers | Row 3 composition approved | V3A-02/05 |
| Eight-category left, original-reporting right | Row 4 approved | V3A-02/06 |
| Community Notices left, Program Updates right | Row 5 approved | V3A-02/05/06 |
| Housing hero and four topic cards follow reference and existing deterministic selector | Explicitly approved | V3A-03 |
| Remove standalone Get Help block; retain backend; prominent All Housing Help | Explicitly approved | V3A-03/04 |
| Housing flyer/What to Know, two-tab context rail, Organizations | Remaining P02 decisions approved together | V3A-03 |
| Original visual file identified | Filename/location provided by product owner; Library entry located | Controlled attachment remains V3A-00 task |
| P01–P09 requirements acceptance contract | Product owner approved | Governs acceptance IDs |
| V3A Implementation Framework package structure | Product owner approved Draft 1 | Governs assignments/dependencies |

## 9. Closure use

This record is the **third V3A planning document**. After the product owner accepts its consolidated wording, include it with the approved page contract and approved implementation framework in the V3A-00 authority crosswalk. Closing V3A-00 requires reliable document/evidence references and an implementation backlog—not pretending the Homepage, Housing, or C-01 fixture are already implemented.

**No code was changed by creation of this document.**
