# Report reconciliation excerpts

Original reports are preserved. These copied acceptance/limitation sections rebase relative evidence links for this evidence-directory location; report paths in headings identify provenance.

# IM-01 Execution Report.md
## Acceptance evidence

All tests named below passed in the final full backend run.

| Work-order item | Evidence |
|---|---|
| 1 — exact lifecycle boundaries | `EditorialLifecycleTest.shouldUseThirtyCalendarDaysAndInclusiveNewYorkExpiry`: 5 instants spanning the exact window start and inclusive expiration-day end. `shouldHonorConfiguredWindow` verifies a 7-day override. |
| 2 — Clock, DST, other zone | `shouldPreserveLocalExpiryAcrossSpringDaylightSaving` (4), `shouldPreserveLocalExpiryAcrossAutumnDaylightSaving` (4), `shouldResolveEachCommunityZoneWithoutChangingTheAlgorithm`, `shouldUseConfiguredDeploymentZoneForUnlistedCommunities`; `EditorialExpiryApiTest.shouldApplyBoundCommunityTimezoneConfiguration` verifies actual Spring property binding with a test-only Tokyo Community id. |
| 3 — missing expiry by class | `shouldAllowMissingExpiryForEveryContentClassWithoutInventingDates`: RESOURCE, NEWS, FLYER, LAW, FAQ, Expert Answer, Article (7 cases); old publication/audit/event/session dates do not create expiry or verification. |
| 4 — independent guards | `shouldKeepStatusIndependentOfExpiryState` (7 cases), `EditorialProjectionTest.shouldRetainHistoricalRecordsAndKeepPublicationStatusIndependent`, existing ArticleService/controller tests, and two new JsonNewsRepository normalization tests. Drafts remain private; withdrawn items remain excluded despite missing expiry. |
| 5 — projections, counts, retention | Seven `EditorialProjectionTest` cases cover all seven collections, Topic/navigation counts, Updates/notices, Search/category previews, Find Help/Organization, homepage Originals/laws/flyers and stored/historical retention, using active/approaching/expired/inactive/withdrawn fixtures. Approaching items remain included. |
| 6 — real expired NP fixtures | `EditorialExpiryApiTest.shouldExcludeRealExpiredHousingPolicyFixturesFromCategoryAndGovernmentUpdates`: NP-001 (2026-06-30), NP-002 (2026-07-14), NP-006 (2026-08-07) retained in repository, EXPIRED at fixed October 15 and absent from Housing/government Updates. |
| 7 — stored versus current | Reconciled `EditorialStabilityTest` remains 239/62. Real API tests retain 10 news records but deliver 5; Housing Policy Info has 2 current items and Housing NEWS count is 3 including one approved Article. Current Housing total is 51 in the runtime check. |
| 8 — event independence | Missing-expiry seven-class tests and `shouldPreservePastEventFlyersAndTheirHistoricalDetailLinks` retain FL-001–FL-008, with no fabricated editorial date. Community meetings retain FL-002 while excluding editorially expired NP-009/010. This is not a claim of occurrence eligibility completion. |
| 9 — scope protection | No ContentType/model hierarchy, generic persisted residentEligible, event model, workflow, Admin dashboard, selector, ranking, catalog removal or new package was introduced. `app/data` and dependency/lockfile diffs are empty. |

Focused additions: 31 lifecycle cases, 7 synthetic projection cases, 6 real-data
Spring/MockMvc API cases and 2 news normalization cases = **46 new cases**.
The final suite contains **417 tests**. Intermediate new-test iterations corrected
test compilation/DTO-path assumptions and preserved the existing approved-Article
contribution to NEWS counts and the past-event FL-002 meeting; they did not weaken
production rules or existing assertions. Final results supersede those iterations.


## Deferred integration and limits (not reopened scope)

| Owner | Remaining responsibility |
|---|---|
| IM-02 | Consume `EditorialLifecycle.assess` for approaching/expired diagnostics and human attention signals; no dashboard or workflow here. |
| IM-03 / IM-04 | Expert classification integrity and explicit non-RESOURCE Get Help semantics remain unchanged. |
| IM-05 | Integrate the same tested `isCurrent/assess` contract into dedicated All-[Category]-Help and expanded Topic inventories when built. Existing resource/flyer Topic browsing is filtered now. Existing Topic navigation counts cover all classified types, while the Topic list covers RESOURCE/FLYER; this preexisting breadth difference was not redesigned. |
| IM-06 / IM-07 | Featured Topic/item selectors do not exist yet. Use current inventories and the shared assessment; approaching-expiry is attention, not automatic ranking priority. |
| IM-08 / IM-09 | Carry the rule into redesigned Category composition and controlled collections; existing implemented collections are integrated now. |
| IM-10 | Occurrence eligibility, date-only events, endsAt and unavailable occurrence dates; past flyers are not mass-expired. Current-event presentation is not complete under IM-01 alone. |
| IM-12 | Full 99-rule/R5 audit, Community partition/scoping and provenance/association audits. Existing blank-Community aggregation and source ownership were not refactored. |
| IM-13 | Combined event/editorial and J1–J7 release regression, manual responsive/accessibility quality gates beyond the existing automated frontend suite. |
| IM-14 | Final evidence/documentation reconciliation, including the existing Admin Guide. That guide's Library link was inaccessible and its text was not supplied; approved PC-01/02 fully supplied the implementation parameters. No rival guide was invented. |

**Final finding:** PC-01 editorial eligibility and current-inventory enforcement are
implemented and evidenced; PC-02 event separation, PC-03 baseline sequencing and
PC-04 audit ownership are preserved. IM-01 is satisfied within its approved
boundary. This is not acceptance of the downstream packages or a v2 release declaration.


# IM-02 Execution Report.md
## Tests and acceptance mapping

New tests:

- `backend/src/test/java/org/firststep/backend/shared/lifecycle/EditorialExpiryDiagnosticsTest.java`
  — **19 cases** (including parameterized boundaries/DST).
- `backend/src/test/java/org/firststep/maintenance/EditorialExpiryReportCommandTest.java`
  — **6 cases** using real catalog loaders and temporary copies for invalid metadata.

All passed in the final full run. Existing IM-01 tests also ran again, rather than
being treated as inherited verification.

| Acceptance | Named evidence |
|---|---|
| T01 ACTIVE outside window | `shouldReportOnlyAttentionAtExactCommunityCalendarBoundaries` (31 days remaining produces no finding); `shouldHonorTheExistingConfiguredApproachingWindow` |
| T02 First window date | Same boundary test at exact New York midnight, 30 calendar days remaining |
| T03 Inclusive expiration date | Same boundary test at final local second, 0 days; original IM-01 boundary/projection tests rerun |
| T04 Following local date | Same boundary test at following midnight, EXPIRED / -1 day |
| T05 Missing/blank expiry | `shouldNotInventFindingsOrExpiryFromMissingBlankAndUnrelatedDates` |
| T06 Repeatability | `shouldProduceIdenticalFindingsAndOrderingForTheSameClockAndReorderedStoredRows`; `shouldReportRealStoredExpiredPoliciesAndApproachingItemRepeatably`; two identical packaged CLI runs |
| T07 Community zones/DST | `shouldUseOwningCommunityZonesAndExactScope`; six `shouldCalculateCalendarDistanceAcrossDaylightSaving` cases; original IM-01 configuration tests rerun |
| T08 Seven content variants | `shouldReadAllSevenContentClassesWithOriginalIdentityTypeAndProvenance`: Resource, NEWS NewsItem, Flyer, FAQ, ExpertAnswer, Article/NEWS and cached NewsItem/LAW |
| T09 Stored versus current | `shouldRetainStoredRecordsAndResidentExpiryBehaviorWithoutMutation`: expired retained in diagnostics/cache, absent from resident inventory |
| T10 Independent restrictions | `shouldKeepInactiveWithdrawnAndUnapprovedContextIndependentOfExpiry`: approaching draft still unapproved; inactive/withdrawn remain distinguished |
| T11 Invalid dates | `shouldIsolateMalformedExpiryWithoutAssigningAStateOrHidingValidRecords`; command controlled-date test; original assess still throws |
| T12 Stable ties | Reordered stored rows yield identical sorted finding and error lists/text, including same-date A/B identities |
| T13 Community scope | Owning-zone test, real Wilmington CLI, empty-scope CLI; other Community excluded before assessment |
| T14 Empty report | `shouldRenderEmptyResultsAndMissingMetadataIntelligiblyWithoutTraces`; `shouldRenderAnIntelligibleEmptyCommunityReport`; packaged empty-scope run |
| T15 No mutation | Serialized source rows and dates/status remain byte-identical around collection; command temp source unchanged; live app/data Git diff empty |
| T16 Real NP fixtures | `shouldReportRealStoredExpiredPoliciesAndApproachingItemRepeatably` identifies NP-001/NP-002/NP-006; packaged real report preserves original identities/source ids/dates |
| T17 Resident regression | Shared stored/current test plus all IM-01 lifecycle, seven-projection and real API tests in the 442-test full run |
| T18 Scope regression | No public controller, dashboard, CRUD, scheduler, queue, notification, content enum/domain hierarchy, persisted eligibility or event/ranking change; dependency/catalog diffs empty |

Additional command tests cover help, invalid/duplicate/unknown arguments and partial
repository-load rejection. They verify the intended diagnostic/exit path, not just
an arbitrary failure.



# IM-03 Execution Report.md
## 5. Acceptance mapping (all final evidence passed)

| ID | Named test / executed evidence |
|---|---|
| T01 Full stored audit | Twelve-row inventories above and audit-before/after JSON; `ExpertCategoryApiTest.shouldRetainAllTwelveApprovedCategoryOnlyRecordsAsExpertWithoutDerivedTopics`; Python real-catalog test |
| T02 Category-only FAQ | `ExpertCategoryIntegrityTest.shouldDeliverExplicitCategoryOnlyFaqAndExpertWithoutFabricatingTopic`; real Category API for all corrected FAQ IDs |
| T03 Category-only Expert Answer | Same service test and actual six-category supporting inventories; all remain EXPERT |
| T04 Explicit valid Topic/parent | `shouldDeliverExplicitTopicsAndValidMultiCategoryIdentityOnlyUnderMatchingParents`; updated IM-01 Topic projection test lists all eight eligible fixture IDs |
| T05 Topic without Category | `ExpertIngestionTest.shouldNotManufactureExpertCategoryFromProseOrTopic` (both classes); invalid-relationship service test; Python `test_topic_without_parent_cannot_manufacture_category` |
| T06 Missing/empty Category | Python `test_missing_empty_and_malformed_category_are_identified`; old runtime reproduction and twelve real validator errors before correction; actual repository loader negative test preserves null classification |
| T07 Unknown/mismatched values | `shouldRejectMissingUnknownAndMalformedRelationshipsBeforeCategoryCountsOrUpdates`, `shouldExplainExactValidationDefectsWhilePreservingCaseInsensitiveControlledMatching`; Python specific unknown/mismatch/invalid-topic tests |
| T08 Legitimate multi-Category | Java explicit Housing+Legal fixture retains one identity across matching projections; Python supports distinct multiple Categories and a Topic with at least one assigned parent |
| T09 Expired but sound classification | `shouldKeepClassificationIndependentOfExpiryStatusAndCommunityPartition`: raw expired record remains stored/valid, absent from current Category results |
| T10 Approaching/inactive/withdrawn | Same fixed-clock service test; approaching retained, inactive/withdrawn omitted independently of valid Category; existing IM-01/02 tests rerun |
| T11 Category service/API | `ExpertCategoryApiTest.shouldServeCorrectedRealFaqAndExpertSupportingCollectionsThroughCategoryApi`; packaged app checks six real Category destinations |
| T12 Real validator injection | `test_real_cli_rejects_injected_missing_category_and_restored_input_passes`: temporary second-Community fixture exits 1 with exact ID, missing_category and parent_mismatch; explicit valid restoration exits 0 |
| T13 Corrected real records | All twelve approved values pinned in Java/Python real-catalog regressions; zero defects after correction; decision source and substantive justification logged for every ID |
| T14 Second Community | Synthetic test-community/another-community scoped results; temporary unclassified loader records preserve test-community; Python Community-independent classification fixture |
| T15 Deterministic repeat | Python CLI restored-positive fixture runs twice with identical stdout; validator-after output repeated identically on unchanged live files; authored order/Category guards contain no random choice |
| T16 Scope/preservation | All 23 IM-02 file hashes unchanged; only two data files changed; all non-Category fields equal; taxonomy, ContentType and dependency/lockfile diffs empty; no new destination/workflow/type |

Negative tests assert the precise intended failure path, not merely any nonzero
exit. Raw loader tests show that a missing relationship cannot be silently repaired
by runtime keyword inference even when the title, answer and existing Topic contain
strong subject cues. Category-only records never receive invented Topics.


## 8. Architecture, limits and downstream ownership

- Fixed ContentTypes and concrete FAQ/Expert distinctions are preserved.
  First Rate News remains a controlled presentation projection name, not a new
  ContentType or Category; its composition was not changed by IM-03.
- Existing Category → Topic taxonomy and all nodes are unchanged. Topic validity
  and required explicit parent are enforced without inferring either field.
- Original IDs, one Community owner, source/provenance, organization fields, dates,
  status, Article review guards and IM-01 eligibility are preserved. No “freshly
  verified” claim follows from adding a Category. Stored rows are retained.
- IM-02's diagnostics, CLI, guidance, samples and all other preexisting changes
  are byte-for-byte preserved, and its tests are included in the 454-test pass.
- J2 gains trustworthy classified inputs for Topic discovery; J4 gains reachable
  Expert/FAQ supporting content; J7/E5 gain intake/gate and projection protection.
  J5 benefits from explicit authoring/validation instructions, without an Admin UI.
- IM-05 can reuse the validated Category/Topic contract for its broader help/Topic
  redesign; current resource/flyer/Expert Topic support is bounded to IM03-T04.
  Broader NEWS/LAW/Article Topic integration is not pulled forward.
- IM-06 uses sound classified candidate inventory; featured selection/ranking was
  not implemented. IM-08/09 retain ownership of redesign and collection composition.
  IM-12 retains the full identity/provenance/Community and 99-rule audit.
- No IM-04 Flyer overlap, IM-10 occurrence logic, new taxonomic nodes, public DTO
  fields, schema/domain rewrite, database migration or V3 operations were introduced.

**Unresolved editorial records: none.** All twelve conditional assignments were
supported and stored only after content verification. **Final disposition: Satisfied**
for IM-03, supported by the complete audit and executed acceptance evidence above.
Stop at this package boundary; later packages and final v2 closure remain outstanding.


## Acceptance record — October 16, 2026

The user recorded IM-03 as **Satisfied** and authorized attaching this execution
report and the editorial classification decision as evidence, followed by commit
and push. The authoritative execution matrix now links both documents in its
IM-03 closure entry. The earlier no-staging/commit statements describe package
execution before this separate acceptance authorization. The preserved IM-02
predecessor is committed separately so the pushed branch is self-contained.


# IM-04 Execution Report.md
## Named acceptance evidence

All Java cases below are actual methods in `FlyerHelpProjectionTest` unless stated otherwise.

| Case | Actual evidence |
|---|---|
| T01 | `exactSemanticEnablesDualProjectionWithIdentityMediaAndProducerIntact`: same FLYER ID in Flyers; baseline API returns all eight live IDs. |
| T02 | Same test; packaged SYN-access and SYN-near in both collections, one raw record each. |
| T03 | `absenceAndUnknownValuesCannotUseProseImageCategoryOrTopicToQualify`: ordinary Flyer stays Flyers-only; packaged SYN-ordinary. |
| T04 | Same test includes help-like title/body/image/organization; no signal means no help. |
| T05 | Same test retains Housing/Eviction Prevention while denying admission. |
| T06 | `exactSemanticEnablesDualProjectionWithIdentityMediaAndProducerIntact`: ID/type, source ID/URL, Community, encoded media and same raw object retained; packaged image GET succeeds. |
| T07 | `expiredIsRetainedInRawCatalogAndHistoricalDetailButExcludedFromBoth`; packaged SYN-expired detail survives, inventory excludes. |
| T08 | `approachingAndInclusiveExpiryRemainEligibleWithoutAttentionRanking`: inclusive expiry/attention eligible, alphabetical ordering rather than attention priority; packaged SYN-near. |
| T09 | `inactiveAndWithdrawnAreExcludedIndependentlyOfExpiry`; packaged SYN-withdrawn excluded. |
| T10 | `categoryTopicAndCommunityScopeAreRequired`: valid Topic positive, wrong record Category, different valid Topic and undeclared Topic negative; no inferred classification. |
| T11 | Same test uses second-community positive, Wilmington negative; packaged synthetic-community positive, unrelated-community empty. |
| T12 | `exactSemanticEnablesDualProjectionWithIdentityMediaAndProducerIntact`: independent producer vs host label preserved, no PROVIDER field fabricated. Existing model has no explicit stored role; full HOST/PROVIDER association implementation/audit remains IM-12. No test falsely claims a relationship exists. |
| T13 | `duplicateMembershipsDoNotDuplicateIdentityWithinACollection`: repeated source ID returned once per collection; positive test confirms one raw identity. Membership counts and unique inventory are explicitly distinguished above. |
| T14 | All eight real records audited in real-flyer-audit.json; none guessed or reclassified. |
| T15 | Synthetic Java fixtures and packaged SYN-* temporary catalog prove both directions. Production hashes unchanged; no fake live correction. |
| T16 | Full 463-test backend suite includes IM-01 lifecycle/API, IM-02 diagnostics/CLI and IM-03 Expert classification; 14 Python tests plus six catalog gates pass. |
| T17 | `jacksonRetainsExactSignalAndNeverPromotesMalformedStrings` plus positive test verify JSON input, exact matching, object rejection and no helpAccess leakage in raw/public cards. Packaged checks assert type/media/Community/producer and hidden signal. |
| T18 | Final diff/preservation review; only bounded code, tests, mirrors and evidence. `resourceEligibilityDoesNotRequireOrUseTheNewField` protects Resource semantics. |



# IM-05 Execution Report.md
## Acceptance evidence IM05-T01–T18

Java methods below are in `category/controller/HelpBrowseApiTest`; frontend methods are in `pages/TopicPage/HelpBrowse.test.tsx` unless specified. Browser checks are named steps/assertions in [browser-check.cjs](../im05/browser-check.cjs), with actual [result](../im05/browser-result.json).

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


## Handoff and final disposition

**0/8 production Flyers activated.** No per-record editorial decision was requested or assumed; synthetic capability remains distinct from live eligibility. The helpFor contract and all approved prior boundaries remain intact.

Deferred owners: IM-06 Topic ranking; IM-07 item roles/ties; IM-08 full Category composition/context rail; IM-09 civic-information collection semantics; IM-10 event occurrence; IM-12 provider/HOST/source and ownership/public-boundary audit; IM-13 final release regression; IM-14 documentation reconciliation/closure. No work in those packages is declared complete here.

**IM-05 is Satisfied:** usable Category/Topic/all-help browsing, complete reachable inventory, scoped identity/eligibility, matching help counts and resident navigation are demonstrated by HTTP, frontend, browser and regression evidence. First Step v2 as a whole is not declared complete. Changes remain uncommitted and unpushed pending separate instruction.


# IM-06 Execution Report.md
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


## 9. Limits, preservation and downstream handoff

All existing production files, catalogs and dependency declarations remain unchanged. The three protected-file differences are the two test timing corrections and the affected existing HelpBrowse mirror. New selector source/tests/mirrors and package docs are the remaining IM-06 diff. No live Flyer assignment was requested or written; **0/8 activated**. Source normalization's existing Community behavior remains unchanged; explicit catalog partition is not physical service eligibility.

Only the currently admitted RSS cache is observable; raw RSS classification currently lacks authored Topic proof, so it cannot positively influence this selector from keywords. No complete-feed or complete-law-inventory claim. Missing deadlines/availability windows, explicit evergreen data and genuine Resource/News catalog insertion dates are disclosed, not manufactured. The approved standard-urgency proxy is not fresh verification. Internal result evidence must be deliberately projected before any future public endpoint; no internal diagnostics are added to resident APIs here.

IM-07 owns actual TIMELY/DURABLE/VISUAL item winners, same-role collision ordering and missing-role fill; these IM-06 proxies are not its comprehensive role definitions. IM-08 owns featured columns/Category composition/context rail. IM-10 owns event occurrence/cutoffs. IM-11 owns later real-catalog selector/collision release validation. IM-12 owns complete provider/HOST/source/ownership audit. IM-13 final release regression and IM-14 documentation/v2 closure remain open.

**IM-06 is Satisfied:** complete seven-stage callable selection, approved explicit qualification, maximum distinct-role coverage, meaningful provenance safeguards, deterministic fallback diagnostics, real/synthetic generalization and full gates are evidenced. No First Step v2 closure declaration. Accepted IM-04/05 work is pushed; IM-06 remains reviewable, uncommitted and unpushed.


# IM-07 Execution Report.md
## Actual fixed-clock catalog acceptance

[Actual Spring/repository audit](../im07/real-item-audit.json) uses **2026-10-16T16:00:00Z** and real authored proof. [Stored-row audit](../im07/stored-catalog-audit.json) separately records canonical IDs/metadata/file hashes; stored records are never assumed current or actionable. [Original IM-06 baseline](../im07/baseline/real-selector-audit.json) is retained for comparison.

There are229 stored/current Resources across Communities and36 Wilmington Housing help items, unchanged from IM-06. IM-06 still selects **Transitional Housing, Emergency Shelter, Homeownership, Rental Assistance**, in that order. Every selected Topic has a supported DURABLE proxy winner and two truthful fallbacks, with no supported TIMELY/VISUAL winner. Counts are audit observations; selector production contains no catalog names/counts/IDs or fixed clock.

| Housing Topic | Help / admitted candidates | Authentic role winner | FALLBACK positions |
|---|---:|---|---|
| Transitional Housing (featured1) |11 /11|DURABLE HA-002|HA-003, HA-005|
| Emergency Shelter (featured2) |9 /9|DURABLE HA-016|HA-001, HA-006|
| Homeownership (featured3) |4 /4|DURABLE HA-011|HA-013, HA-021|
| Rental Assistance (featured4) |4 /4|DURABLE HA-008|HA-023, HA-026|
| Sober Living |3 /3|DURABLE HA-014|HA-024, HA-025|
| Public Housing |1 /1|DURABLE HA-035|none|
| Senior Housing |2 /2|DURABLE HA-027|HA-036|
| Youth Housing |2 /2|DURABLE HA-032|HA-007|
| Eviction Prevention |0 /1|none|OR-003, **context only**|

Eviction Prevention's approved, authored Article OR-003 is current contextual inventory, with authentic creation/approval newness, but has no positive role proxy and is never counted as help. Flagged OR-001/OR-002 remain unpublished. Category-only FAQs/Experts cannot gain Topic placement. FL-002 is an authored current contextual Flyer with original local media, but lacks approved helpAccess: `FLYER_HELP_ACCESS_REQUIRED`, excluded from IM-07 roles/fallback. This differs deliberately from IM-06's image-only coverage proxy; no IM-06 code was changed.

All eight production Flyers FL-001–FL-008 remain unchanged; **0/8 approved/activated**. Their eight original media files exist under backend static seasonal images (stored audit records exact paths). This local media fact does not confer semantic eligibility. Flyer catalog SHA256 before/after:

`5a9c17c1d64294cb5e0d144adfe5a7208763fa617654bb13b357fd6230cd24aa`

Real Homeownership collision: HA-011/HA-013/HA-021/HA-022 each provide the same DURABLE proxy, have no genuine Resource catalog timestamps or meaningful DURABLE temporal distinction, and reach stable-ID selection. HA-011 wins; the internal scoped `STABLE_ITEM_ID_FALLBACK` lists all four. Transitional/Rental/Sober/Senior also exercise actual role ties; full IDs/evidence are captured. Emergency's sole DURABLE is HA-016; its fallback Resources have no fabricated durable qualification. Fallback Resources retain original card URLs and source provenance, including authentic nulls; no links/provider relationships are synthesized to conceal catalog gaps.

Sample diagnostics: Homeownership emits `MISSING_ROLE: TIMELY`, `MISSING_ROLE: VISUAL`, the four-ID DURABLE technical tie, and per-ID `no_authentic_catalog_date`. Eviction emits rejected FL-002's helpAccess diagnostic, three missing roles and OR-003's positive-role limitations. Its contextual fallback does not change the zero-help Topic selection contract.

Other Category/Community: real **bear-de / Food Pantry** returns DURABLE SD-032 then FALLBACK SD-042; Wilmington Food Pantry is empty. Synthetic Tokyo uses the same selector/services and `Asia/Tokyo` versus New York. At2026-10-16T23:30Z, local dates differ (Oct17/Oct16): November16 event qualifies only in Tokyo; September16 law qualifies only in New York. Distinct IDs and Community ownership never leak. At Nov1 05:30Z and06:30Z (the repeated New York DST hour), today stays Nov1: Dec1 event and Oct2 law are inclusive boundaries; Dec2 event does not qualify.


## Changed files and acceptance mapping

All changes are new untracked files. Existing source, tests and data were preserved.

- Production: `backend/src/main/java/org/firststep/backend/category/selection/FeaturedItemSelector.java`.
- Tests in the matching package: `FeaturedItemSelectorTest.java` (23 service-fixture cases), `FeaturedItemAssignmentTest.java` (1 exhaustive-oracle case), `FeaturedItemCatalogTest.java` (3 actual-catalog/public-card cases).
- Complete annotated mirrors: `references/FeaturedItemSelector_annotated.java` and `references/tests/FeaturedItem{SelectorTest,AssignmentTest,CatalogTest}_annotated.java`. [Mirror manifest](../im07/annotated-mirrors.json).
- This report and `evidence/im07/`: inspect-first design, command ledger/logs, stored/real audits, read-only audit script, baseline snapshot, preservation manifests and verification results. No existing test adjustment, production catalog change, dependency or lockfile change.

| Acceptance | Named executed evidence |
|---|---|
|T01|SelectorTest.t01ControlledScopeDistinctImmutableResults: <=3 distinct, immutable nested output, valid labels/slugs; invalid parent/category diagnostic and explicit Community error.|
|T02|t02CategoryOnlyExpertFaqAndRuntimeInferredRssCannotGainTopicProof; exact unproven-authored-RSS diagnostic; retained IM-03/AuthoredTopicEvidence regressions.|
|T03|t03LifecycleApprovalAndApproachingWithoutTimelyBoost and t03RejectionReasonsAreIndependentlyLifecycleStatusAndApproval isolate expired/public-status/approval reasons, plus exact missing-TIMELY evidence for approaching item.|
|T04|t04ExactFlyerHelpAccessControlsAdmissionWithoutMutation tests null, empty, wrong case/space and exact token; CatalogTest and hashes preserve all eight actual Flyers.|
|T05|t05DirectLocalWindowBoundariesDoNotExcludeOrdinaryInventory tests today/day30, yesterday/day31/missing/malformed; exact unqualified reason and truthful fallback.|
|T06|t06AuthoredContextLocalWindowAndPolicyType, t02 authored proof, t13ContextWindowDiffersAcrossCommunitiesAtSameInstant; no keyword promotion.|
|T07|t07DurableRequiresAffirmativeExactStandardNotUndated: positive proxy and precise no-explicit-standard-urgency limitations.|
|T08|t08VisualRequiresMediaSourceAndRetainsOriginalPresentation: original image, encoded URL/source identity retained; missing source/media unqualified.|
|T09|t09MultiRoleFlyerAppearsOnce and assignment-oracle identity checks.|
|T10|t10IndependentTimelyPreservesFlyerVisualCoverage: three distinct roles and collision diagnostic.|
|T11|t11CoveragePrecedesHigherRankedDirectTimely and AssignmentTest.t11GlobalMaximumAndLexicographicTupleMatchExhaustiveOracleAcrossRoleGraphs.|
|T12|t12DirectHelpPrecedesNewerContextAndExpiryCannotQualify: direct help before contextual date/newness; expiry gives no TIMELY qualification.|
|T13|t13MeaningfulDatesPrecedeNewnessAndCommunityLocalDayChanges; t13DstFallBackRepeatedHourKeepsLocalCalendarBoundaries; otherCategoryAndContextDateDirection.|
|T14|t14GenuineVisualNewnessButCopiedResourceDatesCannotDiscriminate; approvalNewnessFallbackAndFutureDatesUnknown; all retained IM-06 role/newness regressions.|
|T15|t15StableIdDiagnosticsSurviveRepositoryReorder:20 shuffles, scoped diagnostic; exhaustive oracle shuffled role graphs; real Homeownership collision.|
|T16|t16FallbackTruthfulAndHelpPrecedesNewerContext; actual real four Topics each DURABLE plus two FALLBACK; no duplicate identities.|
|T17|t17EmptyOneTwoAndLargeInventoryWithoutCap:0/1/2 and1,002 candidates, full result count and stable winners.|
|T18|t18DuplicateProjectionsAndCommunityCannotInflate, real Community assertions and Tokyo fixtures.|
|T19|CatalogTest.t19ActualAllNineHousingTopicsAndFourFeaturedWinnersRemainTruthfulAndNonmutating, t19RealHomeownershipDurableCollisionUsesStableIdentityAndMissingNewnessDiagnostic; real-item-audit.json.|
|T20|CatalogTest.t20ResidentCardSerializationContainsNoInternalDiagnosticsOrReviewFields; full529 backend/133 frontend/14 Python, six validators, source hashes and mirror/diff checks.|

Tests require precise unqualified/rejected reasons where exposed: invalid scope, explicit Community, authored evidence, missing exact urgency, missing positive temporal/media/source evidence, and helpAccess. Lifecycle/status/approval rejection gates are independently asserted without relying on an overlapping rejection. Initial erroneous audit expectations were corrected against actual OR-003 evidence, without changing production eligibility or catalog data; failed runs are retained.


## Ending disposition and handoff

**IM-07 implementation is Satisfied on the executed acceptance evidence above, pending reviewer acceptance.** HEAD remains d3375b3; all existing tracked files remain unchanged. New implementation/tests/mirrors/report/evidence are reviewable and uncommitted. No staging, reset, stash, commit or push. The authoritative matrix was preserved rather than self-editing reviewer acceptance.

IM-01–06 eligibility, classification, scope, Article approval, helpAccess and topic-selection contracts are preserved. Missing structured service windows, authentic Resource/News catalog dates and unapproved Flyer semantics remain metadata gaps with editorial/catalog stewards; they do not justify inferred roles or data changes. IM-08 owns Category integration/card navigation; IM-10 occurrence rules; IM-11 later then-current real-catalog regression; IM-12 provider/host/source/Community audit; IM-13 release/browser/accessibility gates; IM-14 documentation and v2 closure. None was started or declared complete here.


# IM-08 Execution Report.md
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


## Exact change inventory, preservation and handoff

New production: CategoryDestination.java, CategoryDestinationService.java. Modified backend production: CategoryController, CategoryPage, CategoryPageService, OrganizationController and OrganizationService. New tests: CategoryDestinationServiceTest, CategoryDestinationCatalogTest, ScopedOrganizationPreviewTest; modified CategoryControllerTest/CategoryPageServiceTest only for the scoped/additive integration contract.

Frontend modified: CategoryPage.tsx/module.css/test, ContentCard.tsx/module.css, ContentCollection.tsx, SiteHeader.tsx, OrganizationPage.tsx, types/api.ts and dictionary.ts. Added CategoryDestination.test.tsx. All23 touched source/test/style files have complete annotated reference copies; [manifest](../im08/annotated-mirrors.json) names every source/mirror and hash. Supporting artifacts are this report and evidence/im08 logs, API/browser JSON, scripts, screenshots and protection manifests. No dependency, lockfile, taxonomy, source-of-truth JSON, media or selector change.

[Preservation comparison](../im08/preservation-result.json) separates27 authorized modifications to preexisting source/tests/mirrors from853 byte-identical starting files. All64 explicitly protected catalog/selector/instruction/dependency paths were verified unchanged, including the two new authority documents. New untracked implementation/test/mirror/evidence files are additional reviewable work. HEAD remains6336166; no staging, commit, push, reset or stash. Local evidence servers/browser were stopped after capture.

**IM-08 is recommended Satisfied on the evidence above.** J1/J2/J3/J4/J7 and E1/E2/E3/E5 receive integration evidence without claiming completion of their later audits. IM-09 owns full civic collection semantics/inclusive editorial-membership verification and public-DTO audit; IM-10 event occurrence; IM-11 then-current post-integration catalog/collision regression; IM-12 complete PROVIDER/HOST/source/Community/identity/public-boundary audits; IM-13 full seven-journey release/accessibility/responsive/WCAG-quality evidence; IM-14 authoritative documentation reconciliation and v2 closure. None is declared satisfied or started as a separate package here. No V3 workflow/Admin/provider graph/AI ranking/new ContentType or First Step v2 completion declaration.


# IM-09 Execution Report.md
## Named acceptance evidence IM09-T01–T20

New backend tests: CivicCollectionContractTest8, CivicCollectionCatalogTest4, FirstRateNewsMembershipTest5, NewsSummaryTest2, RssFeedServiceTest1 additional missing-publication case, NewsControllerTest1 additional scoped/privacy case — **21 new cases**. One new frontend case tests mixed NEWS/EXPERT membership and safe destinations. Existing positive Law fixtures now explicitly represent original authored metadata; negative tests independently prove runtime fields are insufficient. No earlier eligibility/approval/selector assertion was disabled or weakened. Record-level editorial authority is documented separately from metadata; positive and negative tests exercise its exact implementation.

|Case|Named executed evidence / disposition|
|---|---|
|T01|CatalogTest.t01t08t11t12RealFourCollectionAndInclusiveMembershipAudit; ContractTest.t02PolicyIsExternalNewsNotLawOriginalOrUnknownNewCollection. Four controlled keys, no invented kind/collection.|
|T02|ContractTest.t02; existing CategoryContentServiceTest.shouldExcludeFirstStepReportingFromPolicyInfo and shouldPutApprovedArticlesInFirstRateNewsAndNotInPolicyInfo.|
|T03|ContractTest.t03t06InferredLawCannotReachCategoryCountsOrUpdatesButBroaderSectorStillWorks; retained ContentSourceServiceTest producer-sector/unknown-source cases.|
|T04|CatalogTest.t04RealExpiredNewsStoredButNotCurrentPolicyAndCommunityIsolation; retained EditorialProjectionTest lifecycle/service tests; real NP IDs/states captured.|
|T05|NewsSummaryTest.t05t16PublicNewsKeepsLawIdentityAndPublicationButOmitsInternalFacts; fixed five ContentType assertion; no new Law entity.|
|T06|ContractTest.t06RuntimeKeywordLawCannotPopulateCategoryCollection; t06AuthoredCategoryOnlyLawIsValidWithoutInventingTopic; t03t06 broader-sector preservation; t06AuthoredCategoryCannotAuthorizeRuntimeInferredTopic. Existing IM-08 unproven-law rail rejection retained.|
|T07|RssFeedServiceTest.im09T07MissingPublicationNeverBecomesRetrievalToday; retained actual-file RSS parsing/signed/relevance-cache tests; explicit audit coverage note.|
|T08|CatalogTest real FL-002/eight-unactivated assertions; stored media/source audit; retained FlyerHelpProjectionTest exact media/identity tests.|
|T09|FlyerHelpProjectionTest.exactSemanticEnablesDualProjectionWithIdentityMediaAndProducerIntact, absenceAndUnknownValuesCannotUseProseImageCategoryOrTopicToQualify, jacksonRetainsExactSignalAndNeverPromotesMalformedStrings.|
|T10|FlyerHelpProjectionTest expired/approaching/inactive/Community cases; EditorialProjectionTest past-event-but-current Flyer fixture. No occurrence exclusion.|
|T11|CatalogTest approved OR-003/flagged exclusions and HTTP detail checks; retained ArticleServiceTest and OriginalsControllerTest publication/404 parity.|
|T12|CatalogTest.t01t08t11t12RealFourCollectionAndInclusiveMembershipAudit; all 15 structured rows and final decision table above; exact four Housing identities and independent supporting membership asserted.|
|T13|CatalogTest.t12t13ApprovedAdmissionsDoNotAdmitOtherSessionsOrRemoveSupportingMembership; FirstRateNewsMembershipTest.t13ExactApprovalRequiresIdentityKindCommunityAndCategory and t13DerivedFaqRequiresItsOwnApproval; ContractTest.t13AdmissionCannotBypassCurrentInventoryOrAuthoredTaxonomy. User-approved three positives; all four denied sessions tested in their own eligible Categories. Missing/malformed/kind boundaries also tested.|
|T14|Canonical identities, source/provenance and Community preserved; exact admission intersects independent current inventory and taxonomy gates. Article publication/404 guards and supporting memberships pass. No new Session/ContentType or storage duplicate.|
|T15|Retained IM-03 classification/validator, Topic/All Help and IM-06/07 authored proof tests; actual FAQ/Experts Category-only; zero-help Eviction Prevention stays0.|
|T16|CatalogTest.t16PublicCategoryAndArticle404ParityProtectInternals; NewsSummaryTest.t05t16; NewsControllerTest.im09T16ExplicitScopeAndInternalFieldsStayOutOfNewsEndpoint; retained OriginalsControllerTest.shouldGiveIdenticalResponsesForUnapprovedAndNonexistentArticles and exact public-field test.|
|T17|ContractTest.t17EqualDatePolicyOrderUsesCanonicalIdNotInputIteration and t17ApprovedArticleRepeatedProjectionKeepsOneCanonicalIdentity; retained supporting authored-order/Flyer duplicate tests.|
|T18|NewsSummaryTest.t18MissingOrUnsafeSourceUrlsStayAbsentRatherThanInvented; IM-08 source/media/Article navigation tests rerun in frontend/backend; real rail/HTTP payloads preserve scope and honest emptiness.|
|T19|Final 558 backend/package,144 frontend/type/build,14 Python/six validators; fixed-clock actual Spring MockMvc audit,9 packaged HTTP requests, focused built-app Chromium keyboard/link/layout smoke at375/1280px.|
|T20|Hash/mirror/diff checks, original catalog/lock/selector protection and bounded frontend fix; bounded diff and explicit later-package boundary.|

The existing fixture reconciliations affected CategoryContentServiceTest, CategoryPageServiceTest, NavigationServiceTest, UpdatesServiceTest, FeaturedItemSelectorTest and EditorialProjectionTest, only to add original authored proof where a fixture already asserted editorial Law placement. A FAQ test was renamed to test *source alone is insufficient*, preserving its negative assertion without asserting permanent FAQ exclusion. NewsControllerTest deliberately replaces its former **status-present** expectation with internal-field absence while retaining resident content assertions. Initial failing/errored runs are retained, not suppressed.


## Changed files, preservation and final handoff

Modified production: CategoryCollection.java, CategoryContentService.java, NavigationService.java, NewsController.java, NewsItem.java, JsonNewsRepository.java, RssFeedService.java, UpdatesService.java and CategoryPage.tsx. Added NewsSummary.java, FirstRateNewsMembership.java and the three-entry admission manifest. Existing direct-construction tests receive the new membership dependency; all eligibility assertions remain. Added/extended the named tests above, including the mixed-content frontend navigation case. All 28 touched source/test files have complete annotated mirrors; [manifest](../im09/annotated-mirrors.json). Exact changed/new files appear in [ending state](../im09/ending-state.txt).

All original source-of-truth catalogs, taxonomy, dependencies/lockfiles, production selectors, eight Flyers and user authority documents retain their starting hashes. No Flyer activation, event cutoff, Article approval, identity/provenance/Community mutation, provider/host inference, new ContentType or generalized workflow/recommendation system. The new private manifest records only the expressly authorized collection decision. HEAD remains unchanged; no reset/stash/staging/commit/push. Local evidence servers were stopped after capture.

**IM-09 is complete for its bounded scope**, including the initially blocked editorial decision now explicitly resolved. Existing sparse Law/cache/source-link limitations are disclosed rather than filled by inference. The matrix itself was preserved; this execution report records completion without editing the supplied authority.

IM-10 retains event occurrence; IM-11 then-current selector/collision regression; IM-12 full identity/PROVIDER/HOST/source/Community/public-boundary audit; IM-13 complete J1–J7/browser/WCAG/release gates; IM-14 original Admin Guide/documentation reconciliation and v2 closure. This run supplies J4/J7 and E3/E5 collection/privacy evidence without declaring those later packages or First Step v2 complete.


# IM-10 Execution Report.md
## Named acceptance mapping IM10-T01–T20

`EventOccurrenceTest` covers temporal interpretation; `EventProjectionTest` exercises real service gates with controlled variants and MockMvc HTTP; `EventOccurrenceCatalogTest` loads actual catalogs with a fixed Clock. Full test sources and complete annotated mirrors accompany this report.

|ID|Named executed evidence|
|---|---|
|T01|CatalogTest.t01t10t17RealFl008ConcludesAtNextLocalMidnightButRetainsIndependentFlyersAndHistory; original full row and absent timestamp assertions.|
|T02|ProjectionTest.t02t03t10t15t17SyntheticScheduledEndKeepsSameIdentityMediaSourceAndHistory; t02OptionalTimestampFieldsBindInExistingFlyerAndNewsParsers.|
|T03|OccurrenceTest.t03ExplicitEndInclusiveToNanosecondAndOffsetsAreEquivalent; ProjectionTest.t02t03t10t15t17.|
|T04|OccurrenceTest.t04t07DateOnlyLocalMidnightAndSpringFallDst (six cases); ProjectionTest.t17RealFl008HasNoStructuredEndAndUsesItsActualCalendarBoundary.|
|T05|OccurrenceTest.t05StartOnlyUsesOwningCommunityDateNotOffsetDateOrInventedDuration.|
|T06|OccurrenceTest.t06FutureOccurrenceRemainsUpcoming; ProjectionTest.t08NewsPublicationCannotBecomeEventOccurrence positive explicit start plus negative missing occurrence.|
|T07|OccurrenceTest.t07IndependentCommunityZonesAndClockZoneCannotChangeOwnership; t07ExplicitFallFoldOffsetsAndSpringTransitionRemainInstants; six local-date boundary cases; instant-based sort test.|
|T08|OccurrenceTest.t08MalformedDateNeverNormalizesOrUsesProse (three cases), t08MissingDateCannotUsePublicationAuditExpiryOrMedia, t08DateOnlyEndAndAmbiguousLocalTimestampAreDiagnosedWithoutInventingTime, t08EndBeforeStartIsAnExactDiagnostic; ProjectionTest unknown News/event cases; real NP-003 diagnostic.|
|T09|ProjectionTest.t09EditorialExpiryIsIndependentOfOccurrenceAndStewardshipState; existing EditorialProjectionTest rerun.|
|T10|ProjectionTest.t02t03t10t15t17 and t17RealFl008; CatalogTest real Category Flyers/raw/by-ID checks.|
|T11|ProjectionTest.t11t16CommunityProducerAndControlledKindAreIndependentGates; t18PublicHttpFactsKeepHistoricalFlyerButOmitPrivateFields scoped HTTP; frontend im10t11 tests for view/overview links and requests.|
|T12|ProjectionTest.t12OnlyFiveCanonicalContentTypesAndNoEventConversion; catalog Get Help exclusion; production ContentType unchanged and no new entity/repository.|
|T13|ProjectionTest.t08t13UnknownOccurrenceDoesNotMassFilterOtherViews; fixed real Meetings/Announcements counts unchanged.|
|T14|ProjectionTest.t14CountsPreviewsDedupAndInstantOrderingAgree; scoped packaged counts/overview checks.|
|T15|ProjectionTest.t02t03t10t15t17; t15UnsafeSourceIsAbsentWhileOriginalMediaAndSafeSourceSurvive; packaged JPEG retrieval and fresh built-app media/keyboard navigation.|
|T16|ProjectionTest.t11t16; exact controlled tag/source/Community/type counterexamples; retained original conflicting/no-kind tests.|
|T17|ProjectionTest.t17RealFl008 and t02t03t10t15t17; catalog before/after; synthetic fixture clearly separated from original row.|
|T18|ProjectionTest.t18PublicHttpFactsKeepHistoricalFlyerButOmitPrivateFields; packaged public-field recursion; full original Article/NewsSummary privacy/404 tests rerun. Geographic location `state=DE` remains a legitimate public fact.|
|T19|Full Maven verify/package, frontend tests/type/build, Python tests and six validators; packaged HTTP/JPEG and Chromium evidence below.|
|T20|Initial/final SHA inventories, unchanged HEAD/index, full source mirrors, exact diff/ending state and bounded architecture audit.|


## Regression results, HTTP/browser evidence and limitations

|Gate|Untouched baseline|Final|
|---|---|---|
|Maven JDK25 toolchain / Java17 release / verify + executable package|558 tests pass|**587 tests pass**, zero failures/errors/skips; package built|
|Frontend Vitest|144 tests / 21 files pass|**146 tests / 21 files pass**|
|TypeScript / Vite production build|Pass / pass|Pass / pass|
|Python unittest|14 pass|14 pass|
|Six catalog validators|All exit 0|All exit 0|
|Actual catalog fixed-clock audit|One intended failure proves gap; original snapshot retained|Pass; Events0, community Flyers6, all stored Flyers8|
|Packaged actual HTTP|No baseline network claim|11 API requests plus original JPEG pass|
|Built-app Chromium|No baseline browser claim|Focused Events/Flyers, keyboard, Community, source media, mobile/desktop, Spanish and contrast checks pass|

Final logs: [backend](../im10/final-backend-accepted.txt), [frontend](../im10/final-frontend.txt), [typecheck](../im10/final-typecheck.txt), [build](../im10/final-build.txt), [Python commands/exits](../im10/final-python.json). Initial focused run had one test-scaffold privacy failure: its recursive forbidden-name list incorrectly banned geographic `location.state`. The test now specifically rejects internal diagnostic/status fields while preserving the resident location fact. This was not an application failure; [failed run retained](../im10/focused-backend.txt). The first approved packaged smoke likewise over-banned the ArticleDetail public editorial revision `updatedDate`; it was corrected only for that existing Article endpoint contract, with Flyer audit omission retained. Its failed log is preserved. Final full suite includes the corrected assertion and all boundary tests. No test disabled or skipped.

[Packaged HTTP responses](../im10/packaged-http.json) use the **runtime clock** against untouched production catalogs with RSS refresh delayed; they do not pretend runtime is the fixed next-midnight scenario. Fixed-clock tests independently prove conclusion. [Browser result](../im10/browser-result.json) and [375px](../im10/flyers-375.png), [1280px](../im10/flyers-1280.png), [Spanish/contrast](../im10/flyers-spanish-contrast.png) screenshots show the built frontend, original loaded JPEG, keyboard view navigation, retained requested Community and no horizontal overflow/page errors.

Local bind/loopback and Chromium required sandbox escalation. Evidence servers were stopped after capture. These checks do not certify remote source content, complete event inventory, exhaustive WCAG or physical-device release quality; IM-13 retains full release verification.


## Preservation, architecture proof and handoff

[Preservation result](../im10/preservation-result.json) confirms all original catalogs, the eight Flyers and their media, first-rate-news-membership.json, taxonomy, source registry, dependencies/lockfiles, production selectors, Article guards and supplied authority documents retain their starting SHA256. All **1,056** starting files were hashed: **1,037 unchanged, 19 authorized existing source/test/mirror changes**, plus the new bounded source/tests/report/evidence. Full [mirror manifest](../im10/annotated-mirrors.json) covers all **15** touched source/test files. [Verification summary](../im10/verification-summary.json) links final counts and results.

The exact production additions are two optional fields on each existing concrete model, one stateless occurrence helper and one public Flyer DTO. Existing Notices aggregation and Flyer controllers consume them. There is **no new Event entity/repository/type**, no Resource conversion, no scheduling/recurrences/RSVP/ticketing, no database migration, no provider/host assignment and no Admin/workflow/recommendation system. `ContentType` still contains exactly RESOURCE, NEWS, FLYER, LAW and EXPERT, asserted by test and protected hash. All eight live Flyers still have **zero approved helpAccess assignments**; none gain VISUAL/Get Help eligibility from being events.

**IM-10 Satisfied.** This supplies J4/J7 and E1/E3/E5 occurrence/identity evidence only. IM-11 owns then-current real selector collisions, IM-12 full identity/relationship/Community/public-boundary audit, IM-13 release gates and IM-14 original-guide/deployment/documentation reconciliation and final closure. First Step v2 is not declared complete.


# IM-11 Execution Report.md
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


## Limitations and IM-12/13/14 handoff

Real catalog supplies no admitted actionable Flyer and hence no real positive TIMELY/VISUAL/multi-role Flyer example; these are clearly labeled synthetic tests, while all actual collisions remain genuine source snapshots. Exact standard urgency supports only the accepted DURABLE proxy, not permanent service availability. Resource/News copied source dates do not establish genuine catalog insertion; unknowns and internal technical ties remain explicit.

Local empty RSS/admitted LAW context is a coverage limitation, not proof that no relevant legislation exists. Original URLs/media are preserved and safely projected, without independent remote-source/media-content certification. The runtime epoch is separately captured, not represented as the historical Clock or an exhaustive future-calendar guarantee.

This supports **J1/J2/J7 and E2/E5**. IM-12 retains complete provider/HOST/identity/provenance/Community/public-boundary and deployment-config audits; IM-13 full J1–J7/WCAG/physical-device/release proof; IM-14 original-guide/deployment/documentation reconciliation and final closure. No new schema/status/ContentType/event platform, scores, editorial approvals, Flyer activation, second-Community launch, database, Admin/workflow/notification/personalization system was introduced.


## Proposed final disposition

**Satisfied — proposed for reviewer acceptance on the executed IM11-T01–T20 evidence.** Existing real selectors' outcomes and approved ranking/matching algorithms were preserved. The only production correction is separately supported by a failing-before/passing-after unsafe-source test. The authoritative matrix remains unchanged, later packages are not marked complete, and First Step v2 is not declared closed.


# IM-12 Execution Report.md
## Canonical CivicContent/content-kind inventory

**262 unique normalized records**, no duplicate IDs or missing normalized owners: Resource229, NewsItem10, Flyer8, Article3, FAQ6, ExpertAnswer6. Fixed five ContentTypes remain RESOURCE/NEWS/FLYER/LAW/EXPERT. Actual retained RSS/LAW cache is empty; LAW behavior is covered by executed synthetic regression, not fabricated live rows. Article remains NEWS; FAQ/Expert remain distinct concrete EXPERT models.

| Owning Community | Resource | Other concrete records |
|---|---:|---|
| bear-de | 10 | 0 |
| claymont-de | 8 | 0 |
| hockessin-de | 7 | 0 |
| middletown-de | 29 | 0 |
| new-castle-de | 41 | 0 |
| newark-de | 79 | 0 |
| newport-de | 2 | 0 |
| wilmington-de | 53 | News10, Flyer8, Article3, FAQ6, ExpertAnswer6 |

At the audit Clock, Housing Resources total **44 across Communities**; Wilmington Housing Help **36**, Housing Policy Info NP-004/NP-005, Flyers FL-002, First Rate News **OR-003, FAQ-002, EA-001, EA-005**; supporting FAQ FAQ-002 and Expert Q&A EA-001/EA-005 remain. Bear Food Help is **SD-032/SD-042**. Every collection count equals its distinct canonical IDs; cross-view overlap remains legitimate. No selectors, tie-break rules, collection predicates or role eligibility changed.

Machine evidence: [inventory](../im12/inventory.json), [before](../im12/catalog-before.json), [after](../im12/catalog-after.json), [summary](../im12/audit-summary.txt). Canonical ID survives each supported projection; ownership and source do not become a new record identity.


## Acceptance T01–T20

All rows **PASS within delivered v2 scope**; explicit partial-model/stewardship qualifications above are not concealed. Method names below are executed, not planned. `Catalog`=Im12CatalogAuditTest; `Boundary`=Im12BoundaryRegressionTest; `Additional`=Im12AdditionalBoundaryTest; `Ownership`=Im12OwnershipAndErrorTest; `Relationship`=Im12RelationshipTest.

| ID | Executed evidence / exact named tests |
|---|---|
| T01 | starting-state, inspect-first,1205-path baseline hashes; untouched602/146/14 and six validators. |
| T02 | Catalog.im12T02T03T06T07T09T15T17CompleteRawCurrentAndProjectionIdentityAudit; all262 rows, fixed enum and six concrete kinds; existing LAW synthetic cases. |
| T03 | Relationship.im12T03T04T05ExplicitProviderSurvivesNameChangeWithoutSourceHostParentOrNameInference; OrganizationIdentityTest; actual229 provider traces; Python test_im12_t03_explicit_unknown_supplementary_reference_is_rejected. |
| T04 | Same Relationship positive explicit provider and negative source/name/parent cases; actual association-audit.json; ScopedOrganizationPreviewTest.t14ExplicitAssociationIdsAndCommunityExcludeSourceAndNameOnlyInference. No typed HOST falsely asserted. |
| T05 | Relationship same method; Ownership.im12T10ExplicitOwnerCannotBeReplacedByProvidersPhysicalOfficeCity; Python test_im12_t05_missing_optional_reference_does_not_invent_identity;28 parent strings audited. |
| T06 | Catalog complete audit; producer-and-authored-provenance/authorship-facts; existing source registry and curated/RSS regression. Unknown directory sector explicitly recorded. |
| T07 | AuthoredTopicEvidenceTest.runtimeKeywordPlacementIsNotAuthoredNewsOrRssClassification, resourceSourceMappingCountsAsExplicitCategoryButTopicMustBeStored, conflictingDuplicateAuthoredClassificationDoesNotChooseAnArbitraryWinner; original Expert/FAQ validation regression. |
| T08 | Catalog.im12T08T09T11T12T17EveryCommunityCategoryTopicProviderAndDirectScopeIsIsolated; CivicCollectionCatalogTest.t12t13ApprovedAdmissionsDoNotAdmitOtherSessionsOrRemoveSupportingMembership; Boundary.im12T08T12ApprovedArticleRevisionAndGeographicStateRemainPublic. |
| T09 | Catalog full scope test1,160 responses; Boundary.im12T09ExplicitUnknownCommunityMustNotReturnAggregateInventory and im12T09T13ByIdScopeCannotReturnAnotherCommunitysRecord; Additional.im12T09AiExplicitCommunityFiltersRetrievalBeforePrompt; eight packaged Resource partitions. |
| T10 | Ownership explicit owner test; EditorialExpiryDiagnosticsTest.shouldUseOwningCommunityZonesAndExactScope; EventOccurrenceTest.t07IndependentCommunityZonesAndClockZoneCannotChangeOwnership; browser query continuity. |
| T11 | Catalog.im12T11ReadOnlyEveryResidentEndpointAndEveryObservedCommunityResponseInventory;26 mappings + field audit; Boundary.im12T11T12ResidentEndpointsCannotBypassEditorialPrivacyThroughRawModels. |
| T12 | Boundary privacy/public-field tests; Additional.im12T12AiCitationPreservesProducerWithoutRetrievalAudit, im12T12ConfidentialAddressAndUnsafeContactCannotBecomeResidentNavigation; Relationship.im12T12OrganizationWebsiteCannotBypassPublicNavigationGuard; Im12PublicNavigation.test.tsx12 explicit HTTP(S)/tel/unsafe-scheme/source-less-Expert/Article/Flyer navigation cases; existing media tests. |
| T13 | Boundary.im12T13UnexpectedExceptionMustNotExposeIngestionOrFilesystemDetails; Ownership.im12T13AiFailureCannotExposeProviderExceptionDetails; OriginalsControllerTest.shouldGiveIdenticalResponsesForUnapprovedAndNonexistentArticles and exact public-field tests; wrong-owner404 packaged cases. |
| T14 | packaged-check.py three scenarios; FirstRateNewsMembershipTest.t13ExactApprovalRequiresIdentityKindCommunityAndCategory, t13DerivedFaqRequiresItsOwnApproval, absentManifestFailsClosed, malformedManifestCannotSilentlyAdmitOrIgnoreDecisions, unsupportedRecordKindCannotBecomeAnAdmission. |
| T15 | EditorialExpiryDiagnosticsTest exact calendar/approaching/DST/retained tests; EditorialExpiryApiTest; EditorialExpiryReportCommandTest; actual packaged CLI and262-row lifecycle inventory. |
| T16 | EventOccurrenceCatalogTest.t01t10t17RealFl008ConcludesAtNextLocalMidnightButRetainsIndependentFlyersAndHistory; EventOccurrenceTest.t03ExplicitEndInclusiveToNanosecondAndOffsetsAreEquivalent, t04t07DateOnlyLocalMidnightAndSpringFallDst, t08MissingDateCannotUsePublicationAuditExpiryOrMedia; actualNP-003 UNKNOWN. |
| T17 | Catalog complete audit + full scope test, before==after assertion;44/36 Housing, Bear Food2,8 Flyers/0 Help, controlled collection dedup and exact four admissions. |
| T18 | association/authorship/stewardship inventories, explicit nulls/unknowns; existing missing-occurrence/source/URL tests; no invented relationships or eligibility. |
| T19 | Accepted clean631 backend/158 frontend/16 Python, TypeScript/build/six validators; actual packaged HTTP and built-browser provider/Topic/Food navigation. |
| T20 | preservation-check.json and annotated-mirrors.json; before-failure/after-fix logs; unchanged HEAD; source/data-flow map ties J3/J5/J7 and E1/E5, no new model or activation. |


## Preserved hashes and limitations

[Preservation check](../im12/preservation-check.json):1205 baseline paths, **1175 unchanged**,30 intentional existing-path changes (15 source +15 mirrors). Every production catalog/media baseline hash remains equal, including all eight Flyers, taxonomy, organizations/sources, original admissions and resources.communities.json. All27 explicitly protected catalog/selector/semantic paths match; allfour preexisting user document states/bytes match, including the deleted path's **null** hash. Complete annotated mirrors match all30 source bodies exactly. [Before](../im12/preservation-before.json)/[after](../im12/preservation-after.json).

Limitations are explicit: remote provider accuracy/availability, factual service area or legal meaning, RSS exhaustiveness, a Docker image build, generalized HOST persistence and separately unrepresented editorial workflow facts are not certified. Missing171 provider IDs, legacy directory sector and stored Resource owners require authorized stewardship, not invented values. Approved public Flyer Location shape is unchanged; actual Flyer data has no confidential-address counterexample. No known demonstrated required-v2 defect remains unresolved after final checks.


## IM-13/14 handoff

IM-13 retains release/accessibility/complete seven-journey acceptance. IM-14 retains original Admin Guide, source/organization/community authoring and historical-decision reconciliation; carry deployment manifest requirements, optional reference validation, explicit owner precedence and intentional aggregate endpoint contracts into that review. Dashboard/queues/notifications, generalized relationships, aliases, profiles, Institution/Party and service-area engine remain V3/Later. Do not launch another Community or treat this audit as approval to classify Flyers.


## Proposed final disposition

**Satisfied — for reviewer acceptance of IM-12 only.** The audit preserves the accepted resident collections, identities, sources, Community partitions and selectors; fixes demonstrated public/ownership/reference defects; supplies actual packaged and browser evidence; and identifies bounded stewardship/partial-model limitations without fabricated editorial decisions. Authoritative matrix disposition is unchanged. Work remains uncommitted and unpushed.


# IM-13 Execution Report.md
## Named T01–T20 acceptance mapping

| ID | Disposition | Evidence |
|---|---|---|
| T01 | PASS | starting state, inspect-first, baseline hashes |
| T02 | PASS | J1 browser check, HelpBrowseApiTest, HTTP captures |
| T03 | PASS | J2 browser IDs/positions, selector/catalog/collision suites |
| T04 | PASS, existing association navigation | J3 browser, explicit-provider/noninference tests |
| T05 | PASS, empty local Law acknowledged | J4 browser, membership/collection suites |
| T06 | PASS | EventOccurrenceTest/CatalogTest; FL-008 date-only local next-midnight and DST/structured boundaries; editorial expiry independent |
| T07 | PASS, manual workflow | isolated editor fixture/read/reload and exact validator diagnostics |
| T08 | PASS, CLI/manual boundary | packaged expiry logs and lifecycle/diagnostics tests |
| T09 | PASS | final 262-row audit, classification/Article/admission/Flyer tests |
| T10 | PASS | 1,160 scoped audit responses, Bear browser, Article scope regression |
| T11 | PASS | exact public HTTP allowlists plus privacy/AI/error suites |
| T12 | PASS | actual Docker build/runtime, executable three-manifest scenarios, SPA/assets |
| T13 | PASS for tested viewports | browser measured 375/1280/1440, screenshots, labeled synthetic Law |
| T14 | PASS for executed keyboard/DOM cases | browser navigation/fault checks, heading-focus regression tests |
| T15 | PASS for executed checks; broader accessibility NOT TESTED | Spanish/high-contrast/font/browser/AX artifacts and limitations above |
| T16 | PASS for supported local paths | source/media/absent-provider/unsafe scheme tests and browser captures; external reachability not certified |
| T17 | PASS | J/E tables in this report; no demonstrated unresolved v2 blocker |
| T18 | PASS | final Maven suites/catalog and endpoint reruns; protected original catalog hashes |
| T19 | PASS for executed gates | quality-gate table, command ledger and original/final logs |
| T20 | PASS | exact patch/file list, synchronized full mirrors, ending hash inventory and issue register |


## Final issue register and handoff

| State | Item | Disposition |
|---|---|---|
| PASS | Demonstrated scope, focus, hierarchy and contrast defects | Corrected within existing v2 contracts; final tests green |
| PASS | Interrupted HTTP release audit and missing report/mirrors/preservation deliverables | Completed in this handoff |
| FAIL | Unresolved demonstrated required-v2 release failure | None found in executed evidence |
| NOT TESTED | Full WCAG, screen reader and physical-device testing | No certification claimed; reviewer may require further testing for a separate release policy |
| NOT TESTED | Exhaustive external provider/feed coverage | Local empty Law cache is a data limitation, not evidence of no Delaware laws |
| IM-14 | Original Admin Guide, complete historical/deployment documentation and final v2 reconciliation | Remains pending; include manifest operations and manual scoped expiry/authoring guidance |
| V3/Later | Admin dashboard/queues/email, generalized provider/HOST persistence, semantic duplicate audit, demo library-management design, second-community launch | Deferred; no new implementation pulled forward |

Proposed next action is reviewer acceptance of IM-13 on this evidence, followed by IM-14. The authoritative matrix and final v2 status remain unchanged.
