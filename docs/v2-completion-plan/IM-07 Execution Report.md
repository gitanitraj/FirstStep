# IM-07 execution report — deterministic featured-item selector

Executed October 17, 2026. **Implementation disposition: Satisfied on executed evidence; submitted for review.** IM-08 and v2 completion remain open. No commit or push.

## Starting state and authority

Branch `copyofdev`, HEAD `d3375b3f25078cb4c86b8efc94a4c4c6cc6b1425`, clean. `git merge-base --is-ancestor 54a90f97174fccff558ebe57b15f49ad4aa6ee16 HEAD` exited0 and `git cat-file -t` returned `commit`. The earlier proxy-blocked `/workspace` handoff did not describe this local checkout; no fetch was necessary. Starting/ending HEAD are identical.

All **824 preexisting tracked files** were hashed before edits. All remain byte-identical, including source catalogs, taxonomy, manifests, lockfiles, matrix, accepted IM-01–06 source/tests/mirrors and the two frontend timing fixes. No preexisting changes required accommodation. [Before](evidence/im07/preservation-before.json), [after](evidence/im07/preservation-after.json), [comparison](evidence/im07/preservation-result.json).

Read and followed: `claude.md`; IM-07 work order; v2.7 authoritative matrix/IM-06 closure; Approved 2B-P5 §§20–28/R6a–R6d; Approved Architecture Resolutions; Decision051; IM-06 qualification decision, execution report, reproduction instructions and selector/integration/catalog tests. Inspected taxonomy, lifecycle/timezone configuration, all current-inventory services and source repositories, publication/approval boundary, authored proof, source metadata, Flyer helpAccess/media and existing resident card mapping. [Inspect-first gap map/design](evidence/im07/inspect-first-design.md) was written before production changes, including the original actual-catalog audit and missing facts.

The user's explicit instruction to preserve all eight unapproved production Flyers and not infer VISUAL eligibility controls the contextual-Flyer ambiguity in the work order. Such Flyers receive neither a role nor fallback admission in this IM-07 column. They remain available through their existing contextual browse contracts. No new positive editorial assignment or interpretation was needed.

## Implementation and boundaries

One new internal Spring service, `FeaturedItemSelector.select(communityId, categoryKey, topic)`, returns immutable selection/candidate/card/provenance/position/diagnostic records. Topic accepts its controlled label (existing case-insensitive semantics) or canonical route slug. Nonblank explicit Community is mandatory (`IllegalArgumentException: Explicit Community required`); invalid Category/Topic/parent returns empty plus `INVALID_SCOPE`. No null-Community aggregation.

Candidates are the intersection of `FeaturedTopicInventory.current` (current service inventory, exact Community, lifecycle/status, authored Category/Topic proof, controlled child Topic, Expert/FAQ guards and approved Articles) and existing `helpFor`, `forTopic`, and Category collection projections. Canonical IDs deduplicate projection membership. Existing curated-over-RSS and Resource-first duplicate precedence is preserved. Current metadata cannot invent a new projection. RSS keyword-inferred Topics fail authored proof. Category-only Expert/FAQ content stays outside Topic selection. No raw repository path bypasses admission.

Each candidate snapshots its immutable existing `ContentItem`, action/context distinction, actual roles, meaningful date, authentic newness, evidence/limitations, Community and original source id/name/type/URL plus original Flyer image. Provider identity is never inferred from source/host names. Article navigation retains its canonical identity and existing internal reading semantics; source/provider URLs and Flyer encoded image paths use existing mappers. No public DTO, controller or endpoint changed. Diagnostics remain internal; IM-08 must deliberately project cards before any public exposure.

### Affirmative roles and metadata gaps

- **TIMELY direct:** current exact-token actionable Flyer plus valid structured event date in inclusive Community-local today through day+30. Earlier/later/missing/malformed dates do not qualify; the item may remain eligible for another role or fallback. No occurrence cutoff, duration or deadline inferred from prose, publication or expiry.
- **TIMELY contextual:** authored scoped LAW or exact `policy-update`, publication in inclusive today−30 through today. It remains contextual. Direct help outranks context within equal feasible coverage. Editorial approaching expiry alone provides no positive TIMELY role; it remains diagnosable under IM-02. No qualified stewardship-only TIMELY candidate is invented.
- **DURABLE:** current Resource with exact `urgency=standard`, retaining the approved conservative proxy interpretation. Undated/absent urgency is insufficient. This makes no promise of permanence or fresh verification.
- **VISUAL:** actual FLYER admitted by exact `ACTIONABLE_HELP_ACCESS` and the existing help projection, original nonblank image, resolved image URL, and original nonblank source ID/URL. Image presence/classification alone never qualifies. Missing source/media is explicitly unqualified. Synthetic positive fixtures supply these facts; none of the production Flyers does. Media paths/source URLs are preserved, not independently certified as remotely reachable.

Resources lack structured application/deadline/availability windows and genuine catalog dates. Resource createdDate is copied from retrieval; News/RSS createdDate from publication. Neither is used as newness. Existing genuine Flyer/Article/FAQ/Expert createdDate and approved Article review date can be used, excluding malformed/future dates. Event, expiry, retrieved and exposure history never substitute. These approved known-positive signals are not an exhaustive definition of all possible future TIMELY/DURABLE evidence. No missing service promise or model field is manufactured.

### Deterministic assignment and fallback

1. Compute maximum bipartite matching cardinality over distinct IDs and TIMELY/DURABLE/VISUAL.
2. Among assignments with that cardinality, compare the role tuple lexicographically in **TIMELY, DURABLE, VISUAL** order. A feasible filled role precedes unfilled. For each role, sort by help/direct relevance first; meaningful temporal distinction next (direct events ascending, contextual publication descending); genuine catalog/approval newness descending, unknown last; canonical ID ascending using Java `String.compareTo`. DURABLE/VISUAL have no manufactured temporal discriminator. Event date does not rank VISUAL or become newness.
3. Before accepting a winner, run augmenting-path feasibility over the remaining roles excluding already used IDs and the proposed winner. Reject choices that destroy maximum coverage. This is not ordinary greedy slot selection. With three fixed roles the matching check is linear in inventory; checking feasible choices is bounded quadratic overall, with no arbitrary server cap and no enumeration of all triples in production.
4. Emit role winners in semantic order. Fill to at most three with unused admitted candidates ordered **actionable first, genuine newness, canonical ID**. Every such position is `FALLBACK`, even if its item could qualify for an already occupied role. Fewer than three distinct admitted items yields fewer positions.
5. Record qualification evidence, winner-versus-next-feasible tie stage, `MISSING_ROLE`, `COVERAGE_COLLISION`, `METADATA_LIMITATION`, classification limitations and `STABLE_ITEM_ID_FALLBACK`. Technical ties concern materially equal feasible alternatives; they do not block delivery. Candidate, role-set and diagnostic ordering are deterministic.

An exhaustive independent test enumerates all legal assignments for200 seeded five-item role graphs and compares maximum cardinality and the full winning role tuple. It also shuffles inputs. Service fixtures demonstrate that a higher-ranked direct Flyer must move to VISUAL when independent contextual TIMELY permits greater coverage. No weighted score, personalization, exposure rotation, persistent feature flag or new entity.

## Actual fixed-clock catalog acceptance

[Actual Spring/repository audit](evidence/im07/real-item-audit.json) uses **2026-10-16T16:00:00Z** and real authored proof. [Stored-row audit](evidence/im07/stored-catalog-audit.json) separately records canonical IDs/metadata/file hashes; stored records are never assumed current or actionable. [Original IM-06 baseline](evidence/im07/baseline/real-selector-audit.json) is retained for comparison.

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
- Complete annotated mirrors: `references/FeaturedItemSelector_annotated.java` and `references/tests/FeaturedItem{SelectorTest,AssignmentTest,CatalogTest}_annotated.java`. [Mirror manifest](evidence/im07/annotated-mirrors.json).
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

## Quality gates and reproducibility

[Exact commands, runtime options, failed attempts and reproduction](evidence/im07/reproduce.md). All final gates exited0.

| Gate | Baseline | Final |
|---|---|---|
|JDK25 Maven verify, Java17 release|502 tests; complete verify/package passes on untouched HEAD archive after environment cache repair|**529 tests,0 failures/errors/skips; verify and executable packaging pass**|
|Focused IM-07 + IM-06 catalog|n/a|28 passed (27 new +1 retained)|
|Frontend npm test|133 passed with Node webstorage disabled|133 passed /20 files|
|Typecheck / build|both pass|both pass|
|Python unittest|14 passed|14 passed|
|Six catalog validators|six pass|six pass|
|Preservation / mirrors / diff|clean tracked baseline,824 hashes|824 unchanged; four complete mirrors; whitespace checks pass|

Environment-only issues: JDK25 dynamic Mockito attachment stalled until the existing mockito-core jar was preloaded as a Java agent; initial Node26 webstorage storage errors disappear with `NODE_OPTIONS=--no-experimental-webstorage`; initial Maven packaging lacked cached plugin dependencies and required an approved cache download. No test, requirement, dependency, production behavior or assertion was weakened. Logs retain initial failures and successful corrected runs.

Spring integration plus real stored repositories establishes actual selector behavior; Maven verify proves packaging. No new featured UI, browser interaction or accessibility flow is claimed. Existing frontend regression, keyboard/focus/accessibility tests and build pass; IM-08/13 own new featured UI, responsive/browser and release validation. No complete remote RSS-feed or external link-reachability audit is claimed. IM-12 owns broader provider/HOST/source audit.

## Ending disposition and handoff

**IM-07 implementation is Satisfied on the executed acceptance evidence above, pending reviewer acceptance.** HEAD remains d3375b3; all existing tracked files remain unchanged. New implementation/tests/mirrors/report/evidence are reviewable and uncommitted. No staging, reset, stash, commit or push. The authoritative matrix was preserved rather than self-editing reviewer acceptance.

IM-01–06 eligibility, classification, scope, Article approval, helpAccess and topic-selection contracts are preserved. Missing structured service windows, authentic Resource/News catalog dates and unapproved Flyer semantics remain metadata gaps with editorial/catalog stewards; they do not justify inferred roles or data changes. IM-08 owns Category integration/card navigation; IM-10 occurrence rules; IM-11 later then-current real-catalog regression; IM-12 provider/host/source/Community audit; IM-13 release/browser/accessibility gates; IM-14 documentation and v2 closure. None was started or declared complete here.
