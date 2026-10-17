# IM-06 Execution Report

**Package:** Deterministic Featured-Topic Selector.  
**Starting disposition:** Implementation Required — authorized next.  
**Ending disposition:** **Satisfied**, within the approved IM-06 boundary.  
**Date:** October 16, 2026 (America/New_York).  
**Journeys/Epics:** J2/J7; E2/E5.

## 1. Accepted predecessor commit/push and actual baseline

At the user's explicit request, committed the accepted IM-04/05 implementation, tests, mirrors, decisions and reports as **`8e94be42d4f65f7dfb7bd7ae4c5d05bea1abfa92`**. The repository's global log-ignore rule excluded referenced execution logs from ordinary directory staging, so explicitly retained those logs in a second commit, **`6f7a9554ba3d01e86a3028f4d50fe8b774197cba`**. Both were pushed to **copyofdev**; `git ls-remote origin refs/heads/copyofdev` confirmed the second full HEAD. No accepted code was reset or overwritten. These commits predate and exclude IM-06 implementation.

Initial checkout before these accepted-work commits: branch copyofdev at `26ad4cd6b3e339a58b010dbfd1dbfdb8bf4bd3ca`, with the accepted IM-04/05 implementation and evidence still dirty. IM-06 inspection started with a clean tree after the above commits, at `6f7a9554ba3d01e86a3028f4d50fe8b774197cba`. Ending branch and HEAD are unchanged. IM-06 files remain uncommitted/unpushed. No stash, reset, IM-06 staging or catalog authoring occurred.

Copied both attachments verbatim under their supplied names into docs/v2-completion-plan. Read **v2 — Updated Authoritative IM-01–IM-14 Execution Matrix.md** first, including IM-05 closure addendum, then **v2 — IM-06 Coding-Agent Work Order.md**. Reviewed approved P5 §§18–28, Featured-Topic P5 Alignment in the approved Architecture Resolutions, Decision051, claude.md and predecessor reports. The complete seven-stage resolution controls over older abbreviated classified-count language.

Protected 520 existing source/test/catalog/reference/manifest files before IM-06 changes. All existing production source, catalog and dependency files remain unchanged. Two existing frontend test timing defects were corrected; their assertions and expected behavior were preserved, with the affected mirror synchronized. See [preservation check](evidence/im06/preservation-check.json) and [baseline hashes](evidence/im06/protected-baseline-sha256.json). Starting/ending dirty-state inventories and full HEAD are retained in evidence/im06.

## 2. Inspect-first findings and smallest design

Inspected actual CivicContent/Resource/Flyer/News/FAQ/Expert/Article models, explicit review facts, JSON loader normalization and classifier behavior, CategoryContentService.helpFor/forTopic, Category/Topic services/controllers/DTOs, TaxonomyService/NavigationService, EditorialLifecycle/Community zone configuration, current content services, raw repository boundaries, relevance-admitted RSS, temporal fields, validators and existing tests.

No featured-topic selector existed. Generic navigation counts or broad Topic inventory did not implement the required hierarchy. Runtime Category/Topic fields alone also did not prove authored classification: existing classifiers can fill absent fields from prose, including RSS Topics. Treating a classifier-generated Topic as editorial law/policy relevance would violate this package.

[Inspect-first gap map and design](evidence/im06/inspect-first-design.md) records reuse, metadata limitations and proposed comparator/role coverage before production edits, followed by the explicitly approved qualification interpretation and authored-evidence refinement. No P5 redesign, UI redesign or new public endpoint was necessary.

## 3. Approved qualification decision

A narrow existing-field question was raised before positive role qualification was implemented. The user approved Option1 with boundaries; retained in [IM-06 Qualification Decision](IM-06%20Qualification%20Decision.md).

- **DURABLE:** current RESOURCE with exact explicit `urgency=standard`. This is a conservative selection proxy, not permanent availability or fresh verification. Missing/other urgency and undated alone do not qualify.
- **Direct TIMELY/action opportunity:** current exact-token actionable Flyer with a valid structured eventDate in **[Community-local today, today+30 calendar days]**, inclusive.
- **Contextual TIMELY/late law-policy evidence:** current LAW or exact `policy-update` with authored valid Category/Topic and publication in **[today−30 calendar days, today]**, inclusive.

Dates affect selection evidence only. They never introduce event-occurrence exclusion, editorial-expiry interpretation or an IM-07 role-composition rule. Missing/malformed facts contribute no positive evidence. Editorial approaching expiry alone is not TIMELY qualification or priority. No new catalog fields or live Flyer flags were needed.

## 4. Implemented callable contract and hierarchy

`FeaturedTopicSelector.select(communityId, categoryKey)` returns immutable internal `Selection` evidence: selection date, up to four ordered Topics, all controlled Topic candidate evidence, and diagnostics. A nonblank explicit Community is required; null/blank throws a specific IllegalArgumentException rather than extending historical unscoped browsing into a user-selection policy. Unknown Category returns no fabricated candidates. The existing canonical taxonomy/route slug is the available stable Topic key, compared using natural String ordering; no new Topic UUID/entity or locale-sensitive collation is introduced. Topic renaming would be a deliberate future taxonomy/routing change, not input-order mutation.

All seven stages are implemented in lexicographic order, without a weighted score:

| Stage | Actual evidence/comparison |
|---|---|
| 1 | Descending count of distinct actionable IDs from existing helpFor, intersected with current authored Category/Topic proof |
| 2 | Descending maximum feasible distinct-ID TIMELY/DURABLE/VISUAL coverage, not the sum of role labels |
| 3 | Descending count of distinct approved direct-action event IDs |
| 4 | Descending count of distinct eligible authored recent LAW/policy-update context IDs |
| 5 | Latest genuine known catalog/approval date, descending; missing is null/no positive date evidence, never a manufactured timestamp |
| 6 | Stable canonical Topic slug, ascending, for otherwise indistinguishable evidence |
| 7 | Internal STABLE_TOPIC_KEY_FALLBACK diagnostic when technical key resolves a substantive tie; delivery proceeds |

Known provenance dates are ordered before absent evidence; no actual insertion chronology is asserted for undated content. All-null dates fall through to the stable key. Event date, expiry, presentation history, retrieved and loader-copied publication dates never stand in for catalog newness. Actual Flyer/Article/FAQ/Expert createdDate may qualify; an approved Article's actual reviewedDate can also supply approval newness. Future/invalid date values contribute no date. Resource createdDate copied from retrieval, and News/RSS createdDate copied from publication, are deliberately excluded.

Topics with zero actionable help are retained in the audit but **not** padded into the featured selection. Context can supply qualifying role/late contextual evidence but never increases stage-one help inventory. Every eligible Topic may be audited; fallback diagnostics also cover ties crossing the four-Topic cutoff, not only the final displayed pair.

### Eligibility, authored proof and nonmutation

`FeaturedTopicInventory` reads only existing current service inventories, approved ArticleService.getPublishable and relevance-admitted RSS. It establishes explicit Community, valid Category, controlled Topic, lifecycle/status and Expert classification guards and deduplicates by canonical ID. Curated News precedes duplicate RSS IDs as in the existing projection.

`AuthoredTopicEvidence` reads **classification metadata only** from the same configured source files/classpath fallback, keyed by concrete kind and ID. Its proof is joined to already-current service candidates; raw records do not become an alternative eligibility path. Resource source-category normalization reuses the exact existing SourceMappingService, with an explicitly stored Topic. Stored canonical Category/Topic is required elsewhere. Conflicting duplicate authored proof fails closed. Missing/unreadable proof is diagnosed. RSS currently has no authored Topic evidence channel; a keyword-populated runtime field or fresh headline cannot qualify by itself. This is a coverage limitation, not a claim that the full remote feed was audited.

Stage-one IDs remain sourced from unchanged helpFor, with the additional explicit-classification proof required for selection. No classifier, loader, existing browse contract or public DTO was modified. No provider/HOST relationship is inferred from producer identity, organization text, image or event wording.

### Distinct role feasibility and diagnostics

`FeaturedRoleQualification.maximumDistinctCoverage` computes maximum matching over three roles and unique content IDs. One multi-role Flyer can contribute only one simultaneous position. Another independent TIMELY candidate can permit that Flyer to supply VISUAL while a Resource supplies DURABLE. The helper returns feasibility/cardinality only; it returns no final item winners and implements no IM-07 collision/fill algorithm.

Topic evidence records actionable/direct/context IDs, per-ID available roles, genuine newness and deterministic metadata-limit flags. Diagnostics reference Community/Category/Topic and equal substantive signals. Classification-proof coverage diagnostics are internal as well. No controller exposes these records; existing resident APIs are unchanged and cannot leak these new internal fields. There is no dashboard, queue, assignment, email or override flow.

## 5. Then-current real catalog and actual selection

Baseline packaged APIs were audited before selector production edits with RSS refresh delayed. [baseline-candidate-audit.json](evidence/im06/baseline-candidate-audit.json) distinguishes raw file counts, current explicit runtime memberships, actionable IDs and contextual/visual IDs. This initial runtime audit is not itself accepted authored-proof evidence; the final stored-catalog test uses the stronger authored join.

The fixed-clock final [real-selector-audit.json](evidence/im06/real-selector-audit.json) comes from the actual Spring service, real JSON repositories and authored evidence, frozen at **2026-10-16T16:00:00Z**. Raw Resource count is229; current Help for Wilmington Housing is36. All nine controlled Housing Topics were audited:

| Topic | Actionable | Distinct role coverage | Direct / recent context | Featured result |
|---|---:|---:|---|---|
| Transitional Housing | 11 | 1 | 0 / 0 | 1st: strongest help count |
| Emergency Shelter | 9 | 1 | 0 / 0 | 2nd: next help count |
| Homeownership | 4 | 1 | 0 / 0 | 3rd: stable key resolves complete tie with Rental Assistance |
| Rental Assistance | 4 | 1 | 0 / 0 | 4th: same substantive signals, stable key after Homeownership |
| Sober Living | 3 | 1 | 0 / 0 | Not in top four |
| Senior Housing | 2 | 1 | 0 / 0 | Not in top four; technical tie with Youth Housing diagnosed |
| Youth Housing | 2 | 1 | 0 / 0 | Not in top four; same tie diagnosed |
| Public Housing | 1 | 1 | 0 / 0 | Not in top four |
| Eviction Prevention | 0 | 1 | 0 / 0 | Excluded from selection despite an eligible visual Flyer |

The real role coverage for help Topics is DURABLE proxy evidence, not three complete roles. No authentic catalog/approval newness distinguishes the selected help Topics: source-retrieval-derived Resource dates were not used. The real catalog does not exercise a positive direct-event signal, because **0/8 live Flyers has helpAccess**. Recent explicit law-policy context is absent in this snapshot. These limitations were not filled by inference.

Both technical ties emit STABLE_TOPIC_KEY_FALLBACK; full evidence includes each pair and scope. Eviction Prevention still has0 help and cannot be promoted by its Flyer or other context/newness. The current source files remain byte-identical.

Additional Category: Food has0 Wilmington help in the explicit scope; a positive real **bear-de Food** selection returns Food Pantry with2 help IDs using the identical selector. Existing unscoped browse totals (Housing44/Food12) are audited separately and are not used by the selector.

Synthetic second Community uses the same clock/config/service chain: at **2026-10-16T23:30Z**, New York's today is October16 and Tokyo's is October17. A November16 event is day31 for New York (no direct TIMELY evidence) and day30 for Tokyo (qualifies). A September16 law publication is day−30 for New York (qualifies context) and day−31 for Tokyo (does not). Each scope retains its own canonical IDs; there is no physical service-area inference.

## 6. Exact changed files

New production files in `backend/.../category/selection/`:

- AuthoredTopicEvidence.java — stored classification provenance proof joined to current inventory.
- FeaturedTopicInventory.java — current, scoped, distinct metadata adapter and coverage limitations.
- FeaturedRoleQualification.java — approved existing-field proxies, genuine provenance dates and distinct-ID role matching.
- FeaturedTopicSelector.java — internal seven-stage selector, immutable evidence and nonblocking diagnostics.

New tests in the corresponding test package:

- AuthoredTopicEvidenceTest (3 cases).
- FeaturedRoleQualificationTest (8 cases, with window/value subcases).
- FeaturedTopicOrderingTest (9 cases, including full hierarchy and30 shuffles).
- FeaturedTopicSelectorIntegrationTest (11 cases, real services/lifecycle/taxonomy/helpFor/adapter, synthetic raw repositories/authored-fact boundary,20 shuffles).
- FeaturedTopicCatalogTest (1 actual stored-catalog Spring integration/audit).

Two frontend timing corrections: App.test.tsx now awaits the asynchronously loaded Seniors link rather than the already-present Housing header link; HelpBrowse.test.tsx waits for required heading focus after its effect rather than asserting synchronously on heading insertion. No assertion, expected route, focus requirement, test or production behavior was removed/disabled. Complete annotated mirrors cover all11 new/touched source/test files; [manifest](evidence/im06/annotated-mirrors.json).

Docs: attached updated matrix/work order retained verbatim; approved qualification decision; inspect-first design, baseline/final audit and evidence logs; this final report and reproduction instructions. No catalog, taxonomy, dependency or lockfile changes. Existing production source is unchanged.

## 7. Acceptance mapping IM06-T01–T18

| Case | Actual named evidence |
|---|---|
| T01 up to four | OrderingTest.fewerThanFourNonzeroHelpTopicsAreNeverPaddedWithContextOnlyTopics and shuffled cutoff fixture; CatalogTest verifies controlled children and <=4. |
| T02 help first | OrderingTest.actionableCountPrecedesEveryLaterSignalIncludingContextVolume; IntegrationTest.helpHeavyBeatsContextHeavyEvenWithStrongerRoleCoverageAndMoreRecentLaws uses20 contextual laws against stronger help. |
| T03 current only | IntegrationTest.realLifecycleRejectsExpiredInactiveWithdrawnButApproachingIsNotPriority; approvedNewnessBreaksLateTieButDraftOrCopiedSourceDatesCannot excludes draft; unchanged IM-01 lifecycle regression suite. |
| T04 identity dedup | IntegrationTest.duplicateProjectionMembershipAndInputShufflesDoNotChangeSelectionOrDiagnostics; roleCoverageMatchesDistinctIdsAndDoesNotCountTheFlyerTwice joins one Flyer ID through both projections. |
| T05 explicit relationships | AuthoredTopicEvidenceTest.runtimeKeywordPlacementIsNotAuthoredNewsOrRssClassification and resourceSourceMappingCountsAsExplicitCategoryButTopicMustBeStored; IntegrationTest.explicitParentsTopicsAndExpertGuardBlockNarrativeAndInvalidMembership. |
| T06 full hierarchy | OrderingTest.oneMultiTopicFixtureExercisesTheEntireHierarchyAndTechnicalDiagnostic plus dedicated precedence tests for each stage. Adapter integration exercises actual field-derived role, action, context and approval evidence. |
| T07 roles | QualificationTest.aSingleMultiRoleFlyerCannotFillTwoSlotsButIndependentTimelyAllowsBoth; IntegrationTest.roleCoverageMatchesDistinctIdsAndDoesNotCountTheFlyerTwice. |
| T08 direct action | QualificationTest.directWindowIncludesTodayAndDay30ButNotYesterdayOrDay31; directNeedsExactSignalAndStructuredDateNotTitlePublishOrExpiry; IntegrationTest.directActionDiscriminatesBeforeContextWithoutChangingEventEligibility. Day31 Flyer remains Help-eligible. |
| T09 late context | QualificationTest.contextualWindowIncludesTodayAndDayMinus30OnlyForLawOrPolicyType; IntegrationTest.explicitRecentContextBreaksTieOnlyAfterEqualActionAndRoleEvidence; rawRssFreshnessWithoutExplicitTopicNeverCreatesRelevanceOrNewness requires authored proof even after runtime Topic is populated. |
| T10 late newness | OrderingTest.authenticNewnessIsConsideredOnlyAfterFourStagesTieAndMissingFallsThrough; QualificationTest.loaderDerivedSourceDatesAndEditorialExpiryNeverBecomeCatalogNewness and actualFlyerCreationAndApprovedReviewSupportNewnessWithoutFutureInvention; IntegrationTest.approvedNewnessBreaksLateTieButDraftOrCopiedSourceDatesCannot. |
| T11 stable keys | OrderingTest.shuffledInputsProduceExactlyEqualEvidenceAndCutoffDiagnostics (30 seeds); IntegrationTest duplicate/shuffle case (20 seeds); String natural key order and existing Locale.ROOT taxonomy slug have no locale/machine-clock dependence. All dates use injected Clock/configured ZoneId. |
| T12 diagnostics | OrderingTest.stableKeyTieEmitsScopedDiagnosticAndDoesNotBlockDelivery; cutoff tie contains5 Topic keys while4 selected. RSS proof limitation assertion and real Housing diagnostics are captured. Internal service only; no public endpoint changes. |
| T13 real Housing | CatalogTest.actualHousingSelectionIsScopedRepeatableNonmutatingAndReconcilesEveryHelpCount; fixed-clock real-selector-audit.json and above table, including0-help Eviction Prevention. |
| T14 other Category | IntegrationTest.nonHousingAndEmptyCategoryUseSameContractAndRequireExplicitCommunity; real Food/bear-de audit positive and Wilmington empty. |
| T15 Communities | IntegrationTest.twoCommunitiesHaveIndependentLocalCalendarWindowsAndNoInventoryLeakage proves both temporal windows with scoped IDs/zone dates; null/blank Community rejected explicitly. |
| T16 weak metadata | Qualification tests for missing urgency, malformed dates, absent signal and non-genuine dates; AuthoredTopicEvidenceTest.conflictingDuplicateAuthoredClassificationDoesNotChooseAnArbitraryWinner; limitations/fallback evidence in real snapshot. |
| T17 nonmutation | CatalogTest before/after raw object equality and repeated selector equality;520 protected-file hash check with disclosed test-only exceptions; no source catalog/manifest changes. |
| T18 regressions/boundaries | Full502 backend/133 frontend/14 Python and six validators; unchanged existing production source and APIs. No final item winners, featured UI, event cutoff, new model or workflow. |

## 8. Actual quality gates and reproducibility

| Command/check | Actual outcome |
|---|---|
| Baseline JDK25 Maven verify | 470 tests,0 failures/errors/skips; success |
| Initial frontend baseline |132 passed/1 failed: preexisting async Seniors test race |
| Subsequent corrected run | Exposed a separate existing HelpBrowse focus-effect race; fixed synchronization without weakening its assertion |
| Final full frontend `npm test` | **133 passed across20 files**,0 skipped; both timing corrections included |
| `npx tsc --noEmit`; `npm run build` | both exit0 |
| Focused final selector suite | **32 passed**,0 failures/errors/skips |
| Final `bash /workspace/.firststep-tools/im01-mvn -Dim06.evidence.dir=/workspace/FirstStep/docs/v2-completion-plan/evidence/im06 verify` | **502 tests,0 failures,0 errors,0 skips; BUILD SUCCESS**, repository JDK25 toolchain/Java17 release |
| `python3 -m unittest discover -s data-cleaning/tests -v` | **14 passed** |
| content_sources/homepage/navigation/news/organizations/schema validators | all six exit0; Expert/FAQ and Flyer helpAccess gates preserved |
| `git diff --check`, source mirrors and protected-file comparison | pass |

Exact reproduction: [reproduce.md](evidence/im06/reproduce.md). Baseline, initial failures, corrected runs, final gates and audit output are retained under evidence/im06. Baseline packaged API audit used real HTTP; final selector uses actual Spring wiring plus stored-catalog integration, with no new public API. No featured-page/browser proof is claimed: IM-08/13 own that UI evidence. Frontend production is untouched and its regression suite/build passed.

## 9. Limits, preservation and downstream handoff

All existing production files, catalogs and dependency declarations remain unchanged. The three protected-file differences are the two test timing corrections and the affected existing HelpBrowse mirror. New selector source/tests/mirrors and package docs are the remaining IM-06 diff. No live Flyer assignment was requested or written; **0/8 activated**. Source normalization's existing Community behavior remains unchanged; explicit catalog partition is not physical service eligibility.

Only the currently admitted RSS cache is observable; raw RSS classification currently lacks authored Topic proof, so it cannot positively influence this selector from keywords. No complete-feed or complete-law-inventory claim. Missing deadlines/availability windows, explicit evergreen data and genuine Resource/News catalog insertion dates are disclosed, not manufactured. The approved standard-urgency proxy is not fresh verification. Internal result evidence must be deliberately projected before any future public endpoint; no internal diagnostics are added to resident APIs here.

IM-07 owns actual TIMELY/DURABLE/VISUAL item winners, same-role collision ordering and missing-role fill; these IM-06 proxies are not its comprehensive role definitions. IM-08 owns featured columns/Category composition/context rail. IM-10 owns event occurrence/cutoffs. IM-11 owns later real-catalog selector/collision release validation. IM-12 owns complete provider/HOST/source/ownership audit. IM-13 final release regression and IM-14 documentation/v2 closure remain open.

**IM-06 is Satisfied:** complete seven-stage callable selection, approved explicit qualification, maximum distinct-role coverage, meaningful provenance safeguards, deterministic fallback diagnostics, real/synthetic generalization and full gates are evidenced. No First Step v2 closure declaration. Accepted IM-04/05 work is pushed; IM-06 remains reviewable, uncommitted and unpushed.
