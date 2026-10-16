# IM-05 Execution Report

**Package:** Topic Sub-Destination & All-[Category]-Help Browsing.  
**Starting disposition:** Implementation Required — authorized next.  
**Ending disposition:** **Satisfied**, for IM-05 only.  
**Date:** October 16, 2026, America/New_York.  
**Journey/Epic:** J1/J2; E2, with identity/classification safeguards supporting J7/E5.

## Repository, authority and predecessor protection

Initial branch `copyofdev`, full HEAD `3b8c3d6d4496c8321a4cb5f5c067661b1533c103`. The checkout contained 25 preexisting modified/untracked IM-04 files: its implementation, tests, validation, full mirrors, report/decision and evidence. They were not assumed committed. After the user pushed the missing documents, fetched `copyofdev`, inspected the remote diff, read **New v2 — Authoritative IM-01–IM-14 Execution Matrix.md** first (including controlling October16 ledger §10), then **v2 — IM-05 Coding-Agent Work Order.md**. Fast-forwarded documentation changes/renames only to `26ad4cd6b3e339a58b010dbfd1dbfdb8bf4bd3ca`. Ending branch and HEAD remain these values.

The matrix accepts IM-04 infrastructure with **0/8 live Flyer activations** and authorizes IM-05; its older historical register is not treated as reopening accepted predecessors. P4/P5, Decision051 Pass1/2A, the approved Housing triage configuration, Completion Contract, and IM-01–04 reports/decisions remain controlling. No P5 revision or new editorial semantic was required. Read applicable `claude.md` source/test mirror and scoped-change rules.

All 25 preexisting IM-04 files remain **byte-for-byte unchanged**. All 16 catalog JSON files plus three dependency manifests/lockfile remain unchanged (19 protected files). See [protected predecessor hashes](evidence/im05/protected-im04-sha256.json), [catalog/dependency hashes](evidence/im05/protected-catalog-sha256.json), [preservation check](evidence/im05/preservation-check.json), and final status inventory `evidence/im05/git-final.txt`. No reset, stash, staging, commit or push occurred. No production catalog correction or Flyer flag assignment occurred.

## Inspection, baseline and demonstrated gaps

Inspected actual CivicContent/ContentType/Flyer semantics; ResourceService/FlyerService lifecycle filters and raw repositories; FAQ/Expert services and explicit classification guard; TaxonomyService/NavigationService and controlled taxonomy/navigation configuration; CategoryContentService.helpFor/forTopic, CategoryPageService, TopicPageService, controllers and DTOs; ContentItem/source/media contracts; React App routes, CategoryBrowse, CategoryPage, TopicPage, ContentCard, ContentCollection, API client, dictionary, theme tokens and existing tests. Verified the original SPA deep-link handler `SpaWebConfig` rather than inventing another Topic route convention.

Baseline checks on the actual accepted predecessor work: **463 backend tests**, **126 frontend tests**, **14 Python tests**, six validators, TypeScript and frontend build all passed. Baseline packaged backend was also exercised against real catalog data. Captured logs and [baseline inventory](evidence/im05/baseline-inventory.json) preceded implementation.

Existing UI/API `/category/:key/:topic` used controlled taxonomy slugs, but Topic results were broad inventory, not help-only; Category Topic navigation advertised broad counts. There was no All Help destination. Topic pages rendered their entire list at once and dropped explicit Community scope in requests/links. Flyer cards with no source URL had no media action. Missing-route and network failures shared one error presentation. Detailed reuse/correction map: [inspect-first gap map](evidence/im05/inspect-first-gap-map.md).

## Actual real-catalog audit

Stored raw Resource records: **229**, comprising58 curated and171 community records. Of these,37 use the source Category `Housing Assistance`. The current Wilmington help collection contains **36 unique RESOURCE IDs**, all with controlled Housing Topics and no missing Topic assignment. HA-004 (Belvedere State Service Center) belongs to `newport-de`, explaining the difference from37 without claiming it expired. This is catalog Community partitioning, not a claim about geographic service eligibility.

| Controlled Housing Topic | Current Wilmington Help |
|---|---:|
| Emergency Shelter | 9 |
| Transitional Housing | 11 |
| Sober Living | 3 |
| Rental Assistance | 4 |
| Public Housing | 1 |
| Homeownership | 4 |
| Senior Housing | 2 |
| Youth Housing | 2 |
| Eviction Prevention | 0 |

These sum to36 in this actual catalog; neither code nor growth tests freeze historical inventory size. The Category Destination retains the authored triage groups from `navigation.json`; the all-help destination offers controlled narrowing inside a compact disclosure, rather than repeating the full taxonomy above results.

Sample real IDs include HA-001/002/003 (shelter), HA-012 (transitional housing) and HA-005 (public housing); full scoped IDs, all Topic inventories and destinations are retained in [final-inventory.json](evidence/im05/final-inventory.json). Previously Eviction Prevention advertised2 broad content items; its correct help-only count is0. Its valid Topic route now explains the empty help collection instead of implying those records are actionable.

Other Category evidence: Food has **0** current Wilmington help records and **12** in the existing unscoped inventory. Both real states were exercised through the API and browser. A temporary second Community has positive Housing and Food help fixtures, as well as negative scope/lifecycle/Flyer cases.

All eight live Flyers retain their original data, canonical IDs, type, producer, media and Community, with **no helpAccess field**. They remain available to applicable Flyers views and none enters Help. The 12 Category-only FAQ/Expert records remain supporting content; no Topic assignments or help eligibility were manufactured.

## Implemented contract and exact changes

1. **Topic/all-help HTTP contract.** `TopicPageService` now resolves existing Topic slugs and consumes unchanged `CategoryContentService.helpFor(key, canonicalTopic, communityId)`. New static GET `/api/category/{key}/help` uses the same seam with null Topic. Unknown Category/Topic or wrong parent yields the existing404 convention; a valid empty Topic yields200 with zero help results. Static `/help` takes precedence over variable Topic routing and is reserved for all-help browsing; no current controlled Topic collides.
2. **Shared response.** `TopicPage` adds controlled `topics` narrowing options with help-only counts; existing metadata/items remain. Both page size and type breakdown derive from actual returned help items. CategoryPageService remaps only its destination Topic counts through helpFor, preserving authored groups and leaving the generic NavigationService semantics unchanged. Category orientation metadata remains broad content metadata, not mislabeled as Help.
3. **Order/completeness.** Existing alphabetical browsing convention is retained; equal titles use canonical ID for a deterministic tie. No attention, popularity, TIMELY/DURABLE/VISUAL or featured ranking. Full unique help inventory is returned; no server cap. The established ContentCollection initially displays four and expands to every remaining result.
4. **Stable resident routing.** Reuses `/app-next/category/:key/:topic`; `topic=help` renders All [Category] Help. Parent `/category/:key` stays stable. Minimum All Help link added to parent; Topic/all-help breadcrumbs and narrowing preserve explicit `communityId` query state across requests, direct links and history. In-flight responses are ignored after route/scope changes so stale results cannot replace the new page.
5. **Action navigation.** Original external/source URLs remain primary. Flyer cards without one link to their existing encoded media, preserving FLYER identity. HA-012 has no website but a real stored telephone; help browse supplies `tel:3026587161` from that field, never an invented detail page or producer/provider association. ResourceService's existing public-status detail read provides this fallback only for already admitted help items. Other missing-contact data is not fabricated.
6. **States and accessibility.** Heading focus after successful navigation, announced loading/valid-empty status, distinct invalid-route/network alerts, native keyboard Topic disclosure, existing keyboard expansion controls, theme-aware text, responsive layout and English/Spanish labels. All Help uses collapsed controlled narrowing to keep useful results on the first mobile screen. No new CSS token fallback or icon dependency.

Production/UI files: CategoryController.java; TopicPage.java; CategoryPageService.java; TopicPageService.java; frontend api/client.ts, types/api.ts, dictionary.ts, CategoryBrowse.tsx, ContentCard.tsx, CategoryPage.tsx, TopicPage.tsx and its CSS module. Tests: new HelpBrowseApiTest.java (7 cases), updated TopicPageServiceTest.java for explicit actionable Flyer admission, new HelpBrowse.test.tsx (7 cases), updated TopicPage.test.tsx for the help-specific copy/counts/breadcrumb. Complete annotated mirrors for all16 touched/new source/test files: [manifest](evidence/im05/annotated-mirrors.json).

No change to the protected IM-04 source/seam, ContentType, taxonomy, lifecycle interpretation, catalog data, dependency declarations or lockfile. Evidence-only synthetic catalogs and harnesses are isolated under `evidence/im05` and `/tmp`.

## Acceptance evidence IM05-T01–T18

Java methods below are in `category/controller/HelpBrowseApiTest`; frontend methods are in `pages/TopicPage/HelpBrowse.test.tsx` unless specified. Browser checks are named steps/assertions in [browser-check.cjs](evidence/im05/browser-check.cjs), with actual [result](evidence/im05/browser-result.json).

| Case | Named tests and observed evidence |
|---|---|
| T01 Category→All Help | Frontend `Category exposes scoped All Help and Topic links with help counts`; browser follows real Category All Housing Help, refreshes stable address and receives36 current help items. |
| T02 Category→Topic | Existing TopicPageService resolution/scoping tests; browser opens Emergency Shelter and Rental Assistance using canonical slugs; final inventory audits all nine. |
| T03 All→Topic→All | Frontend `all help to Topic to all help supports history without losing Community`; browser refresh, Back/Forward and breadcrumb return all passed with query preserved. |
| T04 validity | Java `communityCategoryAndControlledTopicAreIndependentGates`: unknown Category, unknown Topic and Food Pantry under Housing all404; browser displays invalid-route alert. |
| T05 Resource semantics | Java `categoryWideRetainsTopiclessResourcesAndCompleteDeduplicatedStableInventory` and unchanged IM-04 Resource eligibility regression. No Resource helpAccess requirement. |
| T06 synthetic Flyer | Java `exactSignalPreservesFlyerIdentityMediaAndSourceWhileUnknownAndAbsentStayOut`; packaged SYN-F-yes appears as FLYER in same browse API; browser opens original media destination. |
| T07 live Flyers negative | All eight stored records remain unactivated and byte-identical; real help response contains none, retained Flyers catalog remains unchanged. Existing IM-04 tests retained and pass. |
| T08 lifecycle | Java `lifecycleExcludesExpiredInactiveWithdrawnAndKeepsApproachingWithoutPriority`; synthetic repository->service->HTTP test uses fixed time. Packaged synthetic R2/R3/R4 are absent, approaching R1 retained. |
| T09 Community | Java `communityCategoryAndControlledTopicAreIndependentGates`; frontend scoped URL checks; packaged/browser `synthetic-town-de` returns its own records, other Community Flyer excluded. No Housing/Wilmington-only selector. |
| T10 classification | Existing TopicPageService unclassified/dual-parent tests; Java `realCategoryOnlyExpertRecordsNeverBecomeHelpAndEmptyTopicRemainsValid`; no broad FAQ/Expert inclusion or prose activation. |
| T11 identity/navigation | Java exact-signal test asserts stable ID/type/source/media and no helpAccess/expiry leak; `missingWebsiteUsesOnlyExplicitStoredTelephoneWithoutInventingDetailRoute`; frontend `Flyer without source navigates to original media, never a fabricated detail`; browser checks HA-012 telephone and synthetic media. |
| T12 complete/bounded | Java growth fixture55 unique results with Topicless item retained; frontend `direct all-help preserves Community, focuses context and reaches every result` expands4→55; browser expands real4→36. No inaccessible remainder. |
| T13 counts/dedup | Java `categoryWideRetainsTopiclessResourcesAndCompleteDeduplicatedStableInventory` supplies repeated ID/reversed equal-title input and gets55 unique stable results; `categoryNavigationTopicCountMatchesItsHelpDestination`; real Topic counts sum to36. |
| T14 states | Frontend loading, valid-empty, network tests plus existing invalid-route test; browser injects delayed and aborted API responses, distinguishes all four states. No private backend message shown. |
| T15 accessibility/mobile | Browser keyboard summary and expansion with Enter, heading focus, named navigation/status/alert roles,375px no horizontal overflow,1440px desktop, high-contrast heading white/black, Spanish switch and Unicode content. Screenshots inspected; see below. |
| T16 generality | Real Food12 unscoped/0 Wilmington; synthetic Food positive R5 plus Housing second-Community fixtures; identical route/projection machinery. |
| T17 predecessors | Full470 backend +133 frontend +14 Python tests, six validators, and byte-for-byte preservation of all25 predecessor files. |
| T18 boundary | Diff/manifests/preservation review: no featured selectors, context rail, event cutoff, editor workflow, new entity/type or catalog activation. |

## Verification outcomes and reproducibility

| Check | Actual final outcome |
|---|---|
| JDK25 helper `bash /workspace/.firststep-tools/im01-mvn verify` | **470 tests,0 failures,0 errors,0 skips; BUILD SUCCESS**, Java17 release target |
| Focused backend (HelpBrowseApiTest/TopicPageServiceTest/CategoryPageServiceTest/CategoryControllerTest) | Initial42 passed before the added phone fallback case; final full suite includes all7 HelpBrowseApiTest cases |
| Frontend `npm test` | **133 tests passed,20 files**, no skipped tests |
| Frontend `npx tsc --noEmit` | exit0 |
| Frontend `npm run build` | exit0 |
| `python3 -m unittest discover -s data-cleaning/tests -v` | **14 passed** |
| Six content_sources/homepage/navigation/news/organizations/schema validators | all exit0, including12 Expert/FAQ classification checks and8 Flyer helpAccess checks |
| Built UI + packaged API browser harness | all journey assertions passed in system Chromium154; no page JavaScript errors |
| `git diff --check` and mirror/preservation checks | pass |

Logs and exact startup/verification steps: [reproduce.md](evidence/im05/reproduce.md). Runtime uses actual executable jar processes, actual built Vite assets, real HTTP, and real JSON loaders. A small verification server serves the build and proxies API/media; it does not claim frontend assets were bundled into the backend jar or that a deployment occurred. Initial managed Chromium download was forbidden by the network proxy; the installed system Chromium worked without dependencies/lockfile changes. A frontend regression query initially matched both Housing breadcrumbs after the new All Help link; made the original assertion specific to the parent link. Screenshot findings prompted the scoped high-contrast and compact-narrowing corrections; final suites/checks passed afterward.

Screenshots: [desktop](evidence/im05/desktop.png), [desktop high contrast](evidence/im05/desktop-high-contrast.png), [375px high contrast](evidence/im05/mobile-375.png), [synthetic Unicode](evidence/im05/synthetic-diacritics-375.png), [Spanish375px](evidence/im05/spanish-375.png). Visually inspected desktop and375px results; first help card is visible on the mobile first screen, with no sideways overflow. Browser verifies heading focus and native keyboard controls; measured high-contrast heading is white on black (21:1). This is bounded interaction/semantic/visual evidence, not a claim of a complete screen-reader/device/WCAG audit or a live external-site/call verification.

The temporary synthetic catalog includes an invalid metadata counterexample deliberately bypassing the catalog gate to demonstrate runtime defense; it is not publishable data. Existing Resource JSON normalization assigns catalog Community using its established city-derived behavior; this run scopes by the resulting singular Community field and does not redefine geographic service eligibility or fix the loader. Full relationship/ownership audit remains IM-12.

## Handoff and final disposition

**0/8 production Flyers activated.** No per-record editorial decision was requested or assumed; synthetic capability remains distinct from live eligibility. The helpFor contract and all approved prior boundaries remain intact.

Deferred owners: IM-06 Topic ranking; IM-07 item roles/ties; IM-08 full Category composition/context rail; IM-09 civic-information collection semantics; IM-10 event occurrence; IM-12 provider/HOST/source and ownership/public-boundary audit; IM-13 final release regression; IM-14 documentation reconciliation/closure. No work in those packages is declared complete here.

**IM-05 is Satisfied:** usable Category/Topic/all-help browsing, complete reachable inventory, scoped identity/eligibility, matching help counts and resident navigation are demonstrated by HTTP, frontend, browser and regression evidence. First Step v2 as a whole is not declared complete. Changes remain uncommitted and unpushed pending separate instruction.
