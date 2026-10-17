# IM-11 execution report — real catalog, collisions and selector regression

Executed October 17, 2026. **Proposed disposition: Satisfied, submitted for review.** Existing real winners are unchanged. One independently demonstrated VISUAL source-safety defect received a bounded predicate correction; the Topic hierarchy and Item assignment/ranking/fallback algorithms were preserved. No authoritative matrix/disposition was edited. No staging, commit or push; no v2 closure declaration.

## Starting state and authorities

Starting disposition: **Verification Required**, authorized by matrix §14B after IM-06/07/10 acceptance. Branch `copyofdev`; full starting/ending HEAD **`140bfe53161f87729545a785d597c90d87e63737`**. Tracked checkout clean; only the newly supplied IM-11 work order and v2.11 matrix untracked. IM-10 changes were committed in this actual checkout, superseding its report-time uncommitted state. [Starting state, including relevant ignored build outputs](evidence/im11/starting-state.txt), [all original hashes](evidence/im11/preservation-before.json).

Read `claude.md`, the IM-11 order and v2.11 matrix §14B, approved P5 §§18–28/R6a–R6d, Featured-Topic Architecture Resolutions, Decisions051/052, IM-06 qualification and IM-06–10 reports/evidence, including the three-entry First Rate News decision. Inspected FeaturedTopicSelector/Inventory, AuthoredTopicEvidence, FeaturedRoleQualification, FeaturedItemSelector, current services/lifecycle/Community zones, Category composition/API/UI, Article approval, IM-09 NewsSummary/admissions, IM-10 EventOccurrence/Notices/FlyerSummary and validators. [Inspect-first analysis](evidence/im11/inspect-first.md) records contracts and the bounded uncertainty before the production correction.

## Untouched baseline and gap finding

Untouched baseline: **587 backend tests and executable package;146 frontend tests; TypeScript/build;14 Python tests and six validators — all pass, exit0**. Exact environment/commands/logs: [baseline-commands.md](evidence/im11/baseline-commands.md). Existing JDK25 toolchain/Java17 release, installed Mockito agent, offline Maven and Node webstorage workaround were reused; no dependency install or product workaround.

Before existing-code changes, a read-only actual-repository test captured both the approved frozen Clock **2026-10-16T16:00:00Z** and a separate real-current Instant frozen at capture. [Fixed before](evidence/im11/real-catalog-before.json), [runtime before](evidence/im11/real-catalog-runtime-before.json). Both initial audit runs passed2 tests. The original catalog audit found **no changed real Topic/Item winner, collision instability or event-currentness coupling**.

One isolated synthetic negative exposed a real contract defect: an otherwise current, authored, explicitly actionable Flyer with original media and source ID gained **VISUAL from `source.url=javascript:alert(1)`**. The test first establishes valid actionable and TIMELY evidence, so this is the specific VISUAL source-safety gate, not rejection by unrelated metadata. [Failing-before log](evidence/im11/demonstrated-source-gap.txt):1 test,1 failure,0 errors, with the exact unsafe-source assertion. IM11-T12 requires safe source facts; nonblank executable text cannot supply that evidence.

The smallest correction changes only the Item VISUAL URL guard to require a parseable absolute **HTTP(S) URL with a host**. Relative, executable, data and malformed values remove positive VISUAL evidence, retaining other legitimate roles/fallback admission and original provenance unchanged. Missing source/media still uses existing deterministic limitation codes. HTTP(S) positives pass. No URL or production record is rewritten; no remote source certification is implied.

## Exact real catalog/Community/Topic inventory

**229 stored Resources across Communities;44 Housing Resources across Communities;36 current Wilmington Housing Help IDs.** The other8 Housing Resources belong to Newark3, New Castle2, Newport1, Middletown1 and Bear1. The old unscoped44 is not Wilmington inventory. The actual Housing audit contains **56 stored normalized rows**:44 Resources,6 NewsItems,2 Articles,1 Flyer,1 FAQ and2 ExpertAnswers. Raw stored facts are distinguished from current inventory, authoring proof and admitted projections.

The [complete candidate table](evidence/im11/real-housing-candidates.md) lists every stored Housing row with origin file, Community, Topic/authored proof, lifecycle/public-status gates, explicit urgency, Help/item admission, genuine item roles, producer and exclusions/limitations. [Original stored fields](evidence/im11/stored-housing-facts.json) preserve source classification and dates; [machine audit](evidence/im11/real-catalog-before.json) includes every admitted candidate's card, provenance, role evidence, missing facts and diagnostics. Existing controlled directory source mappings count as authored normalization; runtime keyword classification does not.

|Controlled Topic|Current authored Help IDs|Count|Coverage / direct / recent context|Featured|
|---|---|---|---|---|
|Transitional Housing|HA-002, HA-003, HA-005, HA-010, HA-012, HA-015, HA-019, HA-020, HA-028, HA-030, HA-037|11|1 / 0 / 0|yes|
|Emergency Shelter|HA-001, HA-006, HA-009, HA-016, HA-017, HA-018, HA-029, HA-031, HA-033|9|1 / 0 / 0|yes|
|Homeownership|HA-011, HA-013, HA-021, HA-022|4|1 / 0 / 0|yes|
|Rental Assistance|HA-008, HA-023, HA-026, HA-034|4|1 / 0 / 0|yes|
|Sober Living|HA-014, HA-024, HA-025|3|1 / 0 / 0|no|
|Senior Housing|HA-027, HA-036|2|1 / 0 / 0|no|
|Youth Housing|HA-007, HA-032|2|1 / 0 / 0|no|
|Public Housing|HA-035|1|1 / 0 / 0|no|
|Eviction Prevention|none|0|1 / 0 / 0|no|

All nine Topic rows match the accepted IM-06/07 fixed-clock baseline. The selected four are **Transitional Housing → Emergency Shelter → Homeownership → Rental Assistance**. No zero-help Topic is padded into selection.

Eviction Prevention's lower-level IM-06 image proxy reports coverage1 from contextual FL-002; this is **not** an IM-07 admission or editorial help assignment. It has0 Help IDs and cannot be featured. Item selection rejects FL-002 with `FLYER_HELP_ACCESS_REQUIRED`, and admits only approved contextual OR-003 as a truthful FALLBACK. All selected real Topics have only a supported DURABLE proxy, with no real positive TIMELY/VISUAL Item winner.

Independent real scopes: Food/Wilmington has no positive Help/featured Topic; Food/Bear has Food Pantry with 2 Help IDs; Housing/Bear has only Sober Living, ID **SD-048**. Actual per-scope evidence is captured without launching a second Community or importing Wilmington candidates.

The runtime before/after captures were **2026-10-17T20:26:26.168224Z / 2026-10-17T20:30:23.441011Z**. Both use the owning configured New York calendar date for Wilmington and reproduce the same36 Help IDs, nine counts and12 featured IDs. [Runtime after](evidence/im11/real-catalog-runtime-after.json), [comparison](evidence/im11/runtime-comparison.json). Runtime audit JSON is identical except capture Instant; fixed-clock before/after JSON is **entirely equal**, including raw/current inventory, diagnostics and Category DTO: [fixed comparison](evidence/im11/fixed-comparison.json), [fixed after](evidence/im11/real-catalog-after.json).

## Selector evidence and collisions

Every selected column contains one **DURABLE** position and two honest **FALLBACK** positions. These exact identities and role labels match IM-07/08; all 12 remain distinct.

|Topic|Position|Exact winning ID|Next feasible rival|Actual comparator stage|
|---|---|---|---|---|
|Transitional Housing|DURABLE|HA-002|HA-003|STABLE_CANONICAL_ID|
|Transitional Housing|FALLBACK|HA-003|HA-005|STABLE_CANONICAL_ID|
|Transitional Housing|FALLBACK|HA-005|HA-010|STABLE_CANONICAL_ID|
|Emergency Shelter|DURABLE|HA-016|none|SOLE_FEASIBLE_CANDIDATE|
|Emergency Shelter|FALLBACK|HA-001|HA-006|STABLE_CANONICAL_ID|
|Emergency Shelter|FALLBACK|HA-006|HA-009|STABLE_CANONICAL_ID|
|Homeownership|DURABLE|HA-011|HA-013|STABLE_CANONICAL_ID|
|Homeownership|FALLBACK|HA-013|HA-021|STABLE_CANONICAL_ID|
|Homeownership|FALLBACK|HA-021|HA-022|STABLE_CANONICAL_ID|
|Rental Assistance|DURABLE|HA-008|HA-034|STABLE_CANONICAL_ID|
|Rental Assistance|FALLBACK|HA-023|HA-026|STABLE_CANONICAL_ID|
|Rental Assistance|FALLBACK|HA-026|HA-034|STABLE_CANONICAL_ID|

For these real candidates, same-position peers tie on Help relevance and unknown genuine catalog newness. DURABLE has no applicable temporal discriminator; Resource source retrieval/created dates never become catalog insertion evidence. Source producer ID for the12 winners is `dscyf-directory`; original directory/file provenance, Resource display organization, authentic nulls and original website navigation remain separate. [Exact winner/rival/card/source references](evidence/im11/selection-explanations.json), [all nine Topics'21 positions](evidence/im11/real-housing-candidates.md).

**Homeownership:** HA-011/HA-013/HA-021/HA-022 all qualify DURABLE through exact `urgency=standard`, are actionable, lack meaningful DURABLE time and genuine catalog/approval newness. HA-011 wins by canonical ID; the scoped `STABLE_ITEM_ID_FALLBACK` explicitly names all four. Remaining HA-013 then HA-021 fill FALLBACK; that position label does not fabricate another role or deny their underlying DURABLE qualification.

**Additional collisions:** Transitional has10 DURABLE-qualified IDs among11 Help candidates; HA-002 precedes HA-003. Emergency has only HA-016 DURABLE among9, so its role winner is `SOLE_FEASIBLE_CANDIDATE`; fallback HA-001 precedes HA-006/HA-009 by ID without invented semantic roles. Rental's DURABLE rivals are HA-008 and HA-034. After HA-008, fallback compares remaining Help/newness/ID, so HA-023 and HA-026 precede HA-034 without promising a second DURABLE slot. Sober/Senior and other nonfeatured collisions remain captured.

**Topic tie versus cutoff:** Homeownership/Rental tie at stages1–5:4 Help, coverage1,0 direct actions,0 recent context, unknown newness. Canonical slug resolves Homeownership first and emits `STABLE_TOPIC_KEY_FALLBACK`. Senior/Youth also tie below the cutoff. The actual fourth/fifth boundary is **Rental4 versus Sober3**, resolved at **stage1**, not an invented technical tie. The existing synthetic five-way equal-rank fixture separately proves deterministic tied-cutoff behavior.

[Persisted real collision fixture](../../backend/src/test/resources/im11/real-housing-collisions.json) was extracted once from the actual before audit, with captured HEAD/Clock provenance; tests never rewrite it. New tests compare its four featured candidate lists against live catalog services, shuffle all 9 real Topic evidence100 times and all 9 real Item candidate sets100 times each, preserving full selections and diagnostics. Canonical duplicate projection, fewer-than-three and real contextual FALLBACK cases are included. The retained independent exhaustive oracle verifies global maximum matching and the full role tuple on200 seeded role graphs; independent TIMELY preserves a multi-role Flyer's VISUAL slot instead of greedy double use.

## IM-09/IM-10 interaction regression

IM-09 exact Housing First Rate News remains **OR-003, FAQ-002, EA-001, EA-005**. FAQ-002 remains independently supporting FAQ; EA-001/005 supporting Expert. Their Category-only classification does not invent a Topic or inflate36 Help IDs. Flagged Articles stay excluded with public404 parity. Authored LAW/Topic proof remains required; keyword/RSS freshness cannot confer relevance or catalog newness. The local admitted RSS cache is empty with refresh delayed; no complete legal inventory claim.

All **8 production Flyers remain byte-identical with 0 helpAccess assignments**. FL-002 remains contextual Housing Flyers. Real FL-008 stores only `event_date=2026-10-17`, no startsAt/endsAt; no4pm summary cutoff is inferred. Existing IM-10 actual-record tests prove local-midnight Events conclusion with retained Flyer/media/history. It is not a Housing candidate and is never promoted into Help or VISUAL.

The new synthetic interaction test proves **explicit endsAt conclusion removes Events only**. On the same local date, an explicitly authorized synthetic Help Flyer remains in Help/selector inventory and independently satisfies the approved date-only0–30 TIMELY window, even immediately after its scheduled end. At next local midnight its event_date is yesterday, so TIMELY disappears while other independently qualified admission/roles remain; removing its token excludes it from Items without deleting contextual Flyers. This is intentional separation, not a second occurrence cutoff.

New parameterized selector tests cover date-only today/day30/yesterday/day31 across both spring23-hour and fall25-hour days. New York versus Tokyo uses independent local dates with exact Community isolation. Existing lifecycle tests separately exclude editorial EXPIRED, retain APPROACHING without TIMELY boost and preserve current Article/source/DTO guards. Unknown occurrence gets `missing_occurrence`; it cannot derive a date from publication/prose or automatically delete Help/fallback content.

## Changed files or verified-no-change

**Production change:** only `FeaturedItemSelector.java` and its full annotated mirror: one source URL predicate plus a private URI guard. The entire assignment/comparator/matching/fallback/diagnostic implementation tail is byte-identical to starting HEAD, with matching SHA256 proof in [selector-preservation.json](evidence/im11/selector-preservation.json). FeaturedTopicSelector, FeaturedTopicInventory, AuthoredTopicEvidence, FeaturedRoleQualification, composition, models, lifecycle/EventOccurrence, controllers/DTOs and frontend are untouched.

Added3 test classes (`Im11RealCatalogAuditTest`, `Im11RealCollisionRegressionTest`, `Im11InteractionRegressionTest`), one captured real JSON test fixture and complete annotated test mirrors. **15 new executed tests**:3 audit/live/API cases,4 real-collision cases,8 interaction cases including four DST rows. No existing test assertion was weakened or disabled. New evidence/report/scripts are read-only verification artifacts, not a new product capability. [Four complete source/test mirrors](evidence/im11/annotated-mirrors.json), [exact final files](evidence/im11/ending-state.txt).

## Acceptance mapping T01–T20

Each IM11 scenario below has named executed evidence; prior tests are rerun in the final full suite rather than cited as inherited acceptance.

|ID|Named executed proof|
|---|---|
|T01|Starting state, baseline commands/full logs, inspect-first and SHA inventory; actual HEAD/ignored build outputs inspected.|
|T02|Im11RealCatalogAuditTest.im11T02T04T05T17ReadOnlyAllHousingCandidatesFixedOrRuntimeAudit;56 stored Housing rows, nine Topics and36 scoped Help IDs; stored raw facts and origin table.|
|T03|Im11RealCollisionRegressionTest.im11T03T07RealTopicTiesStrictTopFourCutoffAndAllEvidenceSurviveOneHundredShuffles; FeaturedTopicOrderingTest's nine seven-stage fixtures.|
|T04|AuditTest.im11T02T04T05T17; FeaturedTopicOrderingTest.fewerThanFourNonzeroHelpTopicsAreNeverPaddedWithContextOnlyTopics. Exact real4/9 and Food scopes.|
|T05|CollisionTest.im11T05T07AllNineRealItemSelectionsAndFallbackCollisionsSurviveOneHundredShuffles; exact12 winners/roles/rivals/provenance; actual API/browser proof.|
|T06|CollisionTest.im11T06RealFourWayHomeownershipDurableCollisionReachesCanonicalIdAndScopedDiagnostic; AuditTest.im11T06PersistedRealCollisionCandidatesMatchTheThenCurrentCatalog.|
|T07|CollisionTest.im11T03T07 and im11T05T07; actual Rental4/Sober3 strict cutoff and Senior/Youth tie; FeaturedTopicOrderingTest.shuffledInputsProduceExactlyEqualEvidenceAndCutoffDiagnostics for synthetic equal-rank cutoff.|
|T08|FeaturedItemAssignmentTest.t11GlobalMaximumAndLexicographicTupleMatchExhaustiveOracleAcrossRoleGraphs; FeaturedItemSelectorTest.t10IndependentTimelyPreservesFlyerVisualCoverage and t11CoveragePrecedesHigherRankedDirectTimely.|
|T09|FeaturedItemSelectorTest.t04ExactFlyerHelpAccessControlsAdmissionWithoutMutation and t05DirectLocalWindowBoundariesDoNotExcludeOrdinaryInventory; Im11InteractionRegressionTest.im11T09T16IndependentCommunityDaysControlDayThirtyWithoutChangingHelpOrOccurrence and four DST rows.|
|T10|FeaturedItemSelectorTest.t06AuthoredContextLocalWindowAndPolicyType, t12DirectHelpPrecedesNewerContextAndExpiryCannotQualify; FeaturedTopicSelectorIntegrationTest.rawRssFreshnessWithoutExplicitTopicNeverCreatesRelevanceOrNewness; IM-09 authored-law counterexamples rerun.|
|T11|FeaturedItemSelectorTest.t07DurableRequiresAffirmativeExactStandardNotUndated, t03LifecycleApprovalAndApproachingWithoutTimelyBoost, t14GenuineVisualNewnessButCopiedResourceDatesCannotDiscriminate; all real urgency/newness facts captured.|
|T12|InteractionTest.im11T12UnsafeSourceCannotSupplyPositiveVisualEvidence failing-before/passing-after; existing t08VisualRequiresMediaSourceAndRetainsOriginalPresentation and t04 exact Help token; real0/8 preserved.|
|T13|CollisionTest.im11T13RepeatedProjectionOfRealCanonicalIdCannotCreateExtraSlotsOrFalseRoles; FeaturedItemSelectorTest.t16FallbackTruthfulAndHelpPrecedesNewerContext and t17EmptyOneTwoAndLargeInventoryWithoutCap; Public Housing1/Youth2 real outputs.|
|T14|AuditTest.im11T14ActualMockMvcCategoryPreservesExactSelectorOrderAndRolesWithoutPrivateEvidence; CategoryDestinationServiceTest.t02t03PassesThroughInjectedTopicAndItemOrderWithoutHousingRanking;15 packaged API requests and built-app12-card comparison.|
|T15|InteractionTest.im11T15ExplicitEndConclusionDoesNotGloballyDeleteHelpOrOverrideIndependentDateTimelyRule; EventProjectionTest.t17RealFl008HasNoStructuredEndAndUsesItsActualCalendarBoundary; EventOccurrenceCatalogTest real next-midnight/independent Flyers; all IM-10 end/DST tests rerun.|
|T16|InteractionTest.im11T09T16SelectorWindowUsesCalendarDaysAcrossBothDstTransitions (four rows), independent Community test; retained IM-06/07 lifecycle/zone fixtures and IM-10 DST scenarios.|
|T17|AuditTest.im11T02T04T05T17 and actual public API memberships; CivicCollectionCatalogTest exact approved/denied admissions and publication/privacy; CivicCollectionContractTest authored LAW/Topic counterexamples; Article/News/Flyer DTO tests rerun.|
|T18|InteractionTest.im11T18UnknownOccurrenceAndMissingVisualFactsProduceDeterministicSeparateDiagnostics; existing missing urgency/source/media/dates/Article approval/authored proof/newness diagnostics; no inferred facts.|
|T19|Final602 backend verify/package,146 frontend/type/build,14 Python/six validators; fixed/runtime before/after;15 HTTP requests;375/1280 keyboard/scoped browser checks.|
|T20|All original file hashes compared; only the independently demonstrated guard/mirror change; four complete mirrors; unchanged authoritative matrix, original Flyers, models, taxonomy, selectors' ranking implementation and package boundary.|

## Actual test/build/validator/HTTP/browser gates

|Gate|Untouched baseline|Final executed result|
|---|---|---|
|Maven JDK25/Java17 verify + package|587 pass|**602 pass;0 failures/errors/skips; executable jar built**|
|Frontend tests|146 /21 files pass|**146 /21 files pass**|
|TypeScript / production Vite build|Pass / pass|Pass / pass|
|Python unittest / six catalog validators|14 pass / all exit0|14 pass / all exit0|
|Focused selector/collision tests|Before audits2+2 pass; unsafe URL case1 intended failure|44 focused-after pass; full final includes four additional DST cases|
|Actual repository audit|Frozen and runtime before captured|Fixed JSON exactly equal after; runtime JSON equal except capture Instant|
|Packaged HTTP|No baseline network claim|15 read-only requests pass; exact12 identities/positions, Help/scope, IM-09/10/Article/DTO guards|
|Built-app Chromium|No baseline browser claim|12 IDs/order/hrefs, honest role presentation,375/1280 layout, keyboard Topic navigation, refresh/back, scoped URLs and no page errors pass|

[Final backend](evidence/im11/final-backend-accepted.txt), [frontend](evidence/im11/final-frontend.txt), [typecheck](evidence/im11/final-typecheck.txt), [build](evidence/im11/final-build.txt), [Python commands/exits](evidence/im11/final-python.json), [all command/failure/rerun details](evidence/im11/reproduce.md). The deliberate before failure is retained; no environment problem was mistaken for a selector defect. Earlier full final-backend.txt passed598 before adding four explicit selector DST-window cases; final-backend-accepted.txt passes602 after them.

[Packaged HTTP bodies](evidence/im11/packaged-http.json) use actual runtime against unchanged catalogs, with RSS refresh delayed, and compare exact fixed-baseline winners where current facts remain equal. [Browser machine evidence](evidence/im11/browser-result.json) is primary; [375px](evidence/im11/housing-375.png) / [1280px](evidence/im11/housing-1280.png) screenshots supplement it. Public API positions retain honest FALLBACK labels; UI does not turn these into semantic role badges. Backend-private diagnostics, tie stages, source-proof/roles/approval details remain absent from resident DTOs. Original Article editorial revision remains public by its existing contract.

Local bind/loopback and Chromium required sandbox escalation; default denials and approved results are retained. Both local servers were stopped after capture. No external provider page was certified or contacted to establish source facts.

## Preservation and provenance

**1,126 starting files hashed;1,124 unchanged;2 authorized existing-file changes**, plus new tests/fixture/mirrors/report/evidence. [Final SHA comparison](evidence/im11/preservation-result.json), [after hashes](evidence/im11/preservation-after.json), [unchanged algorithm proof](evidence/im11/selector-preservation.json). Source catalogs, original media, all 8 Flyers, models, Topic selector/inventory/qualification/authored evidence, lifecycle/occurrence, Category composition, Article guards, admission manifest, source registry/taxonomy, frontend, dependencies/lockfiles and both supplied authority documents retain starting hashes. Only FeaturedItemSelector's demonstrated URL guard and its annotated mirror differ among original files.

The real fixture is test evidence with captured identity/Community/source/card facts, not new catalog approvals or a duplicate normalized product record. Original source/source mappings remain distinct from Resource/provider/host identity; no relationship was inferred. HEAD and index are unchanged; no staging/reset/stash/commit/push.

## Limitations and IM-12/13/14 handoff

Real catalog supplies no admitted actionable Flyer and hence no real positive TIMELY/VISUAL/multi-role Flyer example; these are clearly labeled synthetic tests, while all actual collisions remain genuine source snapshots. Exact standard urgency supports only the accepted DURABLE proxy, not permanent service availability. Resource/News copied source dates do not establish genuine catalog insertion; unknowns and internal technical ties remain explicit.

Local empty RSS/admitted LAW context is a coverage limitation, not proof that no relevant legislation exists. Original URLs/media are preserved and safely projected, without independent remote-source/media-content certification. The runtime epoch is separately captured, not represented as the historical Clock or an exhaustive future-calendar guarantee.

This supports **J1/J2/J7 and E2/E5**. IM-12 retains complete provider/HOST/identity/provenance/Community/public-boundary and deployment-config audits; IM-13 full J1–J7/WCAG/physical-device/release proof; IM-14 original-guide/deployment/documentation reconciliation and final closure. No new schema/status/ContentType/event platform, scores, editorial approvals, Flyer activation, second-Community launch, database, Admin/workflow/notification/personalization system was introduced.

## Proposed final disposition

**Satisfied — proposed for reviewer acceptance on the executed IM11-T01–T20 evidence.** Existing real selectors' outcomes and approved ranking/matching algorithms were preserved. The only production correction is separately supported by a failing-before/passing-after unsafe-source test. The authoritative matrix remains unchanged, later packages are not marked complete, and First Step v2 is not declared closed.
