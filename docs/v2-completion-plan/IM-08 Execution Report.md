# IM-08 execution report — Category Destination integration

**Executed:** October 17, 2026. **Starting disposition:** Implementation Required, authorized by the IM-08 work order and v2.8 matrix. **Ending recommendation:** Satisfied on executed integration evidence, submitted for review. No commit/push; IM-09–IM-14 remain open.

## Repository baseline, authority and protection

Started on `copyofdev`, HEAD `63361667a179ee139d6f410dedc222f2c81ab7c9` [Starting state](evidence/im08/starting-state.txt), [before hashes](evidence/im08/preservation-before.json).

Reviewed `claude.md`, the new work order and matrix/IM-07 closure, approved P5/R6a–d, Architecture Resolutions, Decision051/052 and accepted IM-05–07 reports, selectors and tests. Inspected Category/Topic/All Help API/DTO/services, publication/lifecycle/Expert boundaries, source/media cards, Organization registry/resource associations, React routes/components/collections, dictionary, typography and high-contrast tokens. [Inspect-first gap map and design](evidence/im08/inspect-first-design.md) preceded production edits.

Baseline passed **529 backend tests and Maven verify/package,133 frontend tests, TypeScript/build,14 Python tests and six validators** using the existing JDK25 toolchain, Mockito agent and Node webstorage setting documented in IM-07. No baseline application failure was concealed. Existing Category routing, help eligibility/Topic counts, controlled collection membership, triage navigation, generic cards/expansion and themes were reused.

Missing capabilities were the resident-safe selector projection, featured columns, context rail, bounded explicit-association Organization preview and scope continuity through these links. Existing Category outages shared the missing-route message; loading lacked an announced status. These were corrected narrowly. No selector, classification, catalog or domain model redesign.

## Implemented public integration contract

`GET /api/category/{key}` remains the single Category aggregate. It gains `destination`, an explicit public allowlist:

```json
{
  "destination": {
    "communityId": "wilmington-de",
    "featuredTopics": [{
      "name": "Transitional Housing",
      "slug": "transitional-housing",
      "items": [{
        "role": "DURABLE",
        "item": { "contentType": "RESOURCE", "id": "HA-002", "title": "Be Ready CDC - Esther's Place" },
        "href": "https://www.bereadycdc.org/"
      }]
    }],
    "headlines": [{
      "id": "OR-003",
      "title": "How to read a Delaware eviction notice",
      "href": "/originals/OR-003?communityId=wilmington-de",
      "date": "2026-08-10"
    }],
    "expertLinks": [],
    "laws": []
  }
}
```

Example abbreviates the existing ContentItem card fields and the remaining three Topics/positions; [complete fixed-clock HTTP payload](evidence/im08/real-category-api.json) and [packaged runtime payload](evidence/im08/packaged-category-api.json) contain exact values. No internal Selection/Candidate/Provenance object is serialized. Public role is the true selector position type, including FALLBACK; UI does not render technical role badges.

`CategoryDestinationService` calls **unchanged IM-06 and IM-07 selectors** in their returned order. It projects their existing immutable card snapshots; it does not rank Topics, rerank winners, assign a role or activate a Flyer. Same-role collisions and maximum coverage stay in IM-07. Real/full-suite regressions continue to exercise those contracts.

Source navigation uses an existing safe HTTP(S)/telephone URL; Flyer media retains the original encoded seasonal path; approved Articles use their canonical internal reading route. If a selected offering lacks a source URL, its controlled Topic Help destination is the permitted internal fallback, preserving access to IM-05's actual contact details. No source URL or provider is invented. Public href projection rejects executable schemes. Shared ContentCard now supports approved internal React links and displays original Flyer media; its original external/telephone/media navigation stays intact. Category First Rate News cards route to the approved Article collection's internal reading surface. Source-less contextual Flyers open their existing media.

At the public Category/Topic/All Help HTTP boundary, an omitted/empty Community query resolves the configured `app.default-community-id`; explicit supplied Community stays scoped. Selectors are never called with null Community. The returned Community is made explicit in the Category browser URL with history replacement, then carried through Topic/All Help/Organizations/internal cards and header links. Unknown keys retain the NOT_FOUND envelope and accessible not-found view. Generic network errors use a different announced error message. Historical internal null-scoped aggregate/directory overloads remain available for preexisting callers; the new resident path is scoped. Full wider-product scope audit remains IM-12.

### Page hierarchy and accessibility

The Category header retains existing taxonomy label/counts and adds a direct **Get help in [Category]** action, so mobile residents can reach complete Help before scrolling through feature cards. No evaluation time is called “last verified.” Up to four featured columns follow, each with up to three ordered cards and a controlled Topic heading link. Layout uses1 mobile,2 intermediate and4 wide tracks.

Get Help shows a bounded4-card preview with its true complete count and existing All [Category] Help route; complete results remain reachable via existing expansion. Triage Topic navigation, including valid zero-help Topics, remains in an accessible disclosure rather than a repeated full taxonomy panel above content. What to Know exposes the four controlled Policy info/Law/Flyers/First Rate News choices with labelled buttons, pressed state, controlled panel and announced empty state. Contextual preview is bounded2 with existing in-place expansion. Supporting FAQ/Expert material remains subordinate and accessible.

The secondary Organizations section is capped4; it joins current scoped Category Resources to registry entries by **explicit organizationId**, deduplicates resource IDs, then orders by associated count/name/id. No producer/source/host/display-name association is created. Scoped Organization links use the existing detail route and return only that Community's resources. A concise note explains that some registry names represent programs and this is a limited preview. Full PROVIDER/HOST semantics remain IM-12.

A bounded right rail carries approved editorial headlines, actual linked Expert/video material if available and authored scoped current law links if available. Mobile places it below all primary resident information; it never squeezes a laws column. No media/feed item is invented. Law rail uses existing authored Topic/Category proof conservatively, excluding keyword-only RSS relevance. Full lawful Category-only/collection semantics remain IM-09; absent rail evidence does not suppress legitimate existing contextual browse collections.

Decision051's existing membership is preserved: approved Originals form First Rate News today, with separately accessible eligible Expert/FAQ material. An interview worked into approved editorial content can qualify through its existing Article contract; an expert-session/source label alone is not a new editorial assignment. No current Expert record has such an approval/link. This preserves inclusive access without widening every Expert into First Rate News, which Decision051 explicitly rejected. IM-09 retains full semantic verification.

New chrome has English/Spanish dictionary entries with intact diacritics; focus states use existing tokens, headings/landmarks have semantic names, loading/empty/error states are distinguishable, collection controls work by keyboard, and high contrast inherits existing themes. No new icon/library/dependency or literal CSS fallback tokens.

## Actual catalog integration evidence

Fixed Clock **2026-10-16T16:00:00Z**, actual stored repositories/real services/HTTP JSON:

| IM-06 featured Topic order | Genuine role winner | Honest FALLBACK1 | Honest FALLBACK2 |
|---|---|---|---|
|Transitional Housing|DURABLE HA-002|HA-003|HA-005|
|Emergency Shelter|DURABLE HA-016|HA-001|HA-006|
|Homeownership|DURABLE HA-011|HA-013|HA-021|
|Rental Assistance|DURABLE HA-008|HA-023|HA-026|

All12 IDs, role types, existing card snapshots and selector order are asserted directly. No fake TIMELY/VISUAL classification or raw FALLBACK badge appears in the DOM. Actual emergency urgency fields can still display their authored value; that is distinct from assigning a TIMELY role.

Wilmington Housing has **36 unique current help Resources**, not total contextual counts. Get Help shows4; the complete journey expands all36. Eviction Prevention remains a valid zero-help Topic route and is excluded from the featured four; OR-003 contextual eligibility and FL-002 image do not inflate its help count.

**All eight production Flyers remain unchanged;0/8 have approved helpAccess.** FL-002 stays in Housing's contextual Flyers collection, loads its original image and can navigate to media. None appears in featured/help/fallback positions. Flyer catalog SHA256 before/after:

`5a9c17c1d64294cb5e0d144adfe5a7208763fa617654bb13b357fd6230cd24aa`

Rail: approved OR-003 is the real First Rate News headline and opens its reading surface. Flagged OR-001/OR-002 remain absent. There is no existing linked Expert/YouTube record or authored relevant current law in the local admitted cache; both rail groups state that no linked content is available. Remote RSS refresh was delayed; no complete remote-law-feed audit is claimed. Existing Policy info, Flyer and supporting Expert/FAQ collections remain available under their current gates.

The real scoped Organization preview contains American Red Cross, Be Ready CDC - Esther's Place, Be Ready CDC - Lazarus House and Catholic Charities - Bayard House, each linked through its actual resource organizationId/registry slug. These are associated records, not a claim of completed host/provider ontology or an arbitrary source-name directory.

Other Category/Community: **bear-de Food** produces Food Pantry through the same selectors/integration; the browser visits that real scoped page. A synthetic empty Community returns no Housing features, collections, headlines or Organizations. A synthetic second-Community service fixture injects Meal Delivery before Food Pantry and ordered Z/A winners, proving the projector does not hard-code Housing/count/ranking. Separate explicit-association fixtures place same-registry resources in Communities one/two and assert no cross-scope organization detail/preview leakage. Existing IM-07 timezone, multi-role and classification regressions remain in the full backend suite.

## Tests and acceptance mapping IM08-T01–T20

New backend tests:4 CategoryDestinationServiceTest cases,3 CategoryDestinationCatalogTest cases,1 ScopedOrganizationPreviewTest. New frontend:10 CategoryDestination.test.tsx cases. Existing CategoryController tests now expect the configured resident default Community and additive DTO; CategoryPageService fixture injects the new collaborator. Two existing CategoryPage UI assertions now verify the authorized Organization and empty-help behavior. No earlier eligibility/selector assertion was weakened, deleted or disabled; IM-05 browse regression itself is unchanged.

|Case|Executed evidence|
|---|---|
|T01|CatalogTest.t09t10ScopeDefaultsZeroHelpAndOtherCategoryAreIsolated; existing controller NOT_FOUND tests; browser unknown route alert and Housing hierarchy.|
|T02|ServiceTest.t02t03PassesThroughInjectedTopicAndItemOrderWithoutHousingRanking; actual fixed-clock selector order equality; browser bear-de Food.|
|T03|Same pass-through test, CatalogTest.t04RealHousingHttpHasExactTwelveWinnersAndNoPrivateFields, frontend t02t03t05 consumes original order.|
|T04|CatalogTest.t04 asserts all12 current IDs at fixed Clock, matching actual selector snapshots; complete real-category-api.json.|
|T05|Actual DURABLE/FALLBACK lists; frontend t02t03t05 asserts no technical labels; DOM browser check.|
|T06|ServiceTest.t06RoleCompleteFixtureRetainsTrueRolesAndDistinctIds; unchanged IM-07 multi-role/exhaustive assignment tests in full verify.|
|T07|ServiceTest.t07ExecutableUrlCannotBecomeNavigationAndExplicitCommunityIsRequired; frontend Topic/media/Article links; actual browser keyboard external-card activation and OR-003 read.|
|T08|Frontend t08 complete All Help journey,4-card preview/8 synthetic complete; packaged browser4→36 actual complete items; direct header action.|
|T09|Default Community canonicalization frontend test; t07t09 Topic navigation; browser refresh/back/All Help/scoped org; configured default HTTP assertion.|
|T10|Actual eviction-prevention API0 result assertion and browser empty-status route; unchanged IM-06 context exclusion and IM-07 truthfulness.|
|T11|Frontend t11t12 controlled collection switching, Article/internal navigation; existing collection tests and scoped real payload.|
|T12|CatalogTest.t04 FL-002 present in Flyers and no FL-* featured; exact0/8 assertion; frontend/browser original media loaded.|
|T13|CatalogTest.t13t14t15ApprovedHeadlinesAndScopedOrganizationLinksOnly; real flagged Articles absent; all retained lifecycle, Article approval, Expert classification and IM-01–07 suites.|
|T14|ScopedOrganizationPreviewTest.t14ExplicitAssociationIdsAndCommunityExcludeSourceAndNameOnlyInference; actual preview<=4, scoped detail browser journey and frontend API-query assertion.|
|T15|ServiceTest.t15RailRejectsUnprovenLawAndInventsNoExpertOrVideoLinks; catalog actual OR-003/empty links; frontend truthful absence and browser rail.|
|T16|Built-app browser375/1280/1440 exact scrollWidth measurements, readable card widths, mobile rail after primary content; screenshots visually inspected.|
|T17|Frontend loading/network/not-found, collection pressed controls, Spanish/high-contrast and route focus; browser keyboard Enter/focus outline, language/document lang, themes/screenshots.|
|T18|CatalogTest.t04 recursive public-key allowlist denial; frontend no private content; public DTO never references internal selection classes.|
|T19|Final537 backend/143 frontend/TypeScript/build/14 Python/six validators; actual packaged HTTP + built-app browser smoke and screenshots.|
|T20|Hash comparison,23 complete source/mirror pairs, unchanged selector/catalog/taxonomy/lock/instruction files; git/new-file whitespace checks; package boundary preserved.|

## Executed gates and browser evidence

[Exact command ledger/reproduction and intermediate failures](evidence/im08/reproduce.md).

|Gate|Baseline|Final|
|---|---|---|
|Maven verify, JDK25 toolchain /release17|529 pass/full package|**537 pass,0 failures/errors/skips; full executable package**|
|Focused composition/HTTP tests|existing composition/controller/Org30 pass|new/retained focused29 pass|
|Frontend Vitest|133 pass|**143 pass,21 files**|
|TypeScript /Vite production build|both pass|both pass|
|Python unittest|14 pass|14 pass|
|Six catalog validators|all pass|all pass|
|Browser built app + packaged API|n/a|PASS, Chromium151.0.7922.34; no app page errors|
|Preservation/mirrors/whitespace|880 starting hashes|27 authorized existing-file changes;853 unchanged;23 complete mirrors verified; no unexpected changes|

The full backend verify preceded final documentation-only Java comment changes; no backend behavior changed afterward. Final frontend tests/build/browser were rerun after the header action correction. Intermediate failures were test-selector wording/ambiguity, test-only TypeScript option, CSS Modules composition syntax and the duplicate accessible All Help name; each is retained and superseded by final passing logs. Sandbox refused loopback server binding/Chromium Mach-port launch; explicit approved reruns provided actual evidence. No checks were skipped to claim acceptance.

Latest [browser results](evidence/im08/browser-results.json) records actual journeys and metrics. Viewport375/1280/1440 each had scrollWidth equal to viewport width; featured card widths were339/about279/about316px. At375px the rail starts below the Organizations/primary content. Keyboard focus outline was solid3px. The original-source popup URL was exactly `https://www.bereadycdc.org/`. Browser Topic→refresh→back, complete Help36, contextual Flyer media, approved OR-003, scoped Organization, Food/bear-de, zero-help Eviction Prevention and invalid-category alert all passed.

Screenshots: [375px](evidence/im08/housing-375.png), [1280px](evidence/im08/housing-1280.png), [1440px](evidence/im08/housing-1440.png), [high contrast](evidence/im08/housing-375-high-contrast.png), [Spanish](evidence/im08/housing-375-spanish.png). Desktop and mobile screenshots were visually inspected, not merely created. The header immediate Help action addresses the long but bounded mobile feature stack. This is representative browser/keyboard/accessibility smoke, not an exhaustive WCAG audit or physical-device test. The source popup demonstrates correct original navigation, not provider website accuracy or guaranteed remote availability.

## Exact change inventory, preservation and handoff

New production: CategoryDestination.java, CategoryDestinationService.java. Modified backend production: CategoryController, CategoryPage, CategoryPageService, OrganizationController and OrganizationService. New tests: CategoryDestinationServiceTest, CategoryDestinationCatalogTest, ScopedOrganizationPreviewTest; modified CategoryControllerTest/CategoryPageServiceTest only for the scoped/additive integration contract.

Frontend modified: CategoryPage.tsx/module.css/test, ContentCard.tsx/module.css, ContentCollection.tsx, SiteHeader.tsx, OrganizationPage.tsx, types/api.ts and dictionary.ts. Added CategoryDestination.test.tsx. All23 touched source/test/style files have complete annotated reference copies; [manifest](evidence/im08/annotated-mirrors.json) names every source/mirror and hash. Supporting artifacts are this report and evidence/im08 logs, API/browser JSON, scripts, screenshots and protection manifests. No dependency, lockfile, taxonomy, source-of-truth JSON, media or selector change.

[Preservation comparison](evidence/im08/preservation-result.json) separates27 authorized modifications to preexisting source/tests/mirrors from853 byte-identical starting files. All64 explicitly protected catalog/selector/instruction/dependency paths were verified unchanged, including the two new authority documents. New untracked implementation/test/mirror/evidence files are additional reviewable work. HEAD remains6336166; no staging, commit, push, reset or stash. Local evidence servers/browser were stopped after capture.

**IM-08 is recommended Satisfied on the evidence above.** J1/J2/J3/J4/J7 and E1/E2/E3/E5 receive integration evidence without claiming completion of their later audits. IM-09 owns full civic collection semantics/inclusive editorial-membership verification and public-DTO audit; IM-10 event occurrence; IM-11 then-current post-integration catalog/collision regression; IM-12 complete PROVIDER/HOST/source/Community/identity/public-boundary audits; IM-13 full seven-journey release/accessibility/responsive/WCAG-quality evidence; IM-14 authoritative documentation reconciliation and v2 closure. None is declared satisfied or started as a separate package here. No V3 workflow/Admin/provider graph/AI ranking/new ContentType or First Step v2 completion declaration.
