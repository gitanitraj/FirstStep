# IM-10 execution report — event occurrence and independent Flyer visibility

Executed October 17, 2026. **Disposition: Satisfied**, within the IM-10 scope. Explicit occurrence facts now control current/upcoming Events independently of editorial expiry and Flyer visibility. All production catalogs and media are unchanged. No staging, commit or push; IM-11–14 remain outside this execution.

## Protected baseline and inspected authority

Starting branch `copyofdev`, full HEAD `67c006842a85fd0cde57bc0777802922c6925949`. IM-09 implementation was committed in this checkout. Only the two newly supplied IM-10 work-order/matrix documents were untracked; tracked baseline was clean. [Starting state](evidence/im10/starting-state.txt), [original SHA inventory](evidence/im10/preservation-before.json), [final comparison](evidence/im10/preservation-result.json), [ending state](evidence/im10/ending-state.txt).

Read `claude.md`, the IM-10 order and v2.10 matrix including §14A IM-09 closure, approved P5 §9 temporal metadata and §11 P5-R5b, Decision051's Community Events generalization obligation, IM-01 lifecycle/configuration and IM-04/08/09 implementation/evidence. Inspected CivicContent, Flyer and News models/loaders, EditorialLifecycle, FlyerService and media/detail behavior, producer-sector registry, notice-kind taxonomy, Notices service/controller/DTO, Category collections/Get Help, Article guards/NewsSummary, frontend Events/Meetings/Announcements/Flyers, tests and catalog validators. No external editorial/source-content or remote-feed completeness audit is claimed.

The [inspect-first truth table and design](evidence/im10/inspect-first.md) were written before production edits. Optional-field requirements, parsing/validator compatibility, runtime failure behavior and the public Flyer allowlist were documented before their implementation. The approved model already allows these temporal facts; no model redesign was needed.

Untouched baseline executed successfully: **558 backend tests and packaging; 144 frontend tests; TypeScript and production build; 14 Python tests; all six validators**. Exact commands and failed/final runs are in [reproduction ledger](evidence/im10/reproduce.md).

## Actual FL-008 metadata — no assumed scheduled end

The complete original row is preserved in [fl008-stored.json](evidence/im10/fl008-stored.json).

|Fact|Actual stored value / normalization|
|---|---|
|ID / title|`FL-008` / `United Neighbors 2026`|
|Concrete kind / ContentType|Flyer / `FLYER`, assigned by existing Flyer constructor; no new EVENT type|
|Community|`wilmington-de` explicitly stored|
|Producer|`contentSource.id=westside-grows-together`, `type=manual`; registry sector **COMMUNITY**; no source URL stored|
|Category / Topic|`Community Support` / `Community Celebrations`, explicitly stored|
|Notice classification|Stored tags `Free`, `Arts & Music`, `Community Celebrations`, **`event`**; existing controlled notice-kind classifier reads the exact tag|
|Occurrence|**`event_date=2026-10-17` only**. `startsAt` and `endsAt` are absent. No structured scheduled end exists.|
|Prose|Summary says “Saturday 1:00–4:00 pm”; those words are not parsed into occurrence facts.|
|Original media|`United Neighbors 2026.jpg`; existing encoded URL `/images/seasonal/United%20Neighbors%202026.jpg`|
|Location|7th Street Bridge; 7th Street between Adams St and Jackson St; Wilmington, DE|
|Organization|Display text `West Side Grows Together`; stored `organization_id=ORG-064` remains untouched and is not mapped into a PROVIDER/HOST assertion|
|Editorial metadata|`verified=true`; created/updated `2026-10-15`; no expirationDate or status; no helpAccess|

**Real FL-008 remains occurrence-current through October 17 in America/New_York, until October 18 at 04:00:00Z.** Its summary's 4 pm does not authorize an earlier cutoff. Its editorial assessment remains ACTIVE because no editorial expiry is supplied. At the next local midnight, only current Events membership ends; its Flyer/media and independently permitted historical reading remain.

The [synthetic scheduled-end variant](evidence/im10/synthetic-scheduled-end.json) adds `startsAt=2026-10-17T13:00:00-04:00` and `endsAt=2026-10-17T16:00:00-04:00` **in memory only**, to prove the separately required scheduled-end contract. These are synthetic test facts, not a correction or interpretation of production FL-008.

## Demonstrated gap and before/after audit

Before correction, CommunityNoticesService applied current editorial inventory, source sector and notice kind, but no occurrence cutoff. A real-catalog test at **2026-10-18T04:00:00Z** deliberately failed on FL-008 still appearing in Events. [Failing run](evidence/im10/demonstrated-gap.txt), [before audit](evidence/im10/catalog-before.json), [after audit](evidence/im10/catalog-after.json).

|Projection at that fixed instant|Before|After|
|---|---|---|
|Events|5: NP-003, FL-004, FL-006, FL-001, FL-008|**0**: concluded dated Flyers excluded; NP-003 has no explicit structured occurrence and is UNKNOWN|
|Community Flyers|6: FL-004, FL-002, FL-006, FL-001, FL-003, FL-008|**Same 6**, same IDs and media|
|Meetings|1|Same 1|
|Announcements|3|Same 3|
|Raw Flyer catalog / current Flyer service|8 / 8|Same 8 / 8|
|FL-008 Category contextual Flyers|Present|Present|
|FL-008 Get Help|Absent|Absent: no approved helpAccess|
|FL-008 status-safe historical by-ID|Present|Present|

Government-produced Flyers remain excluded from COMMUNITY-sector Notices but retain independently eligible Category/Flyer visibility. Removing expired or unknown-occurrence Events does not delete stored records or rewrite the broad announcements/news feeds. Before/after JSON also captures the landing previews, counts and Community Support Category collection.

## Occurrence truth table and precise time policy

`EventOccurrence` is a narrow stateless assessment helper using injected Clock and the existing configured owning-Community timezone map/default. It does not call, change or persist EditorialLifecycle's state. Fixed-clock tests deliberately use a Clock with an unrelated machine-style zone and two independent Community zones.

|Explicit occurrence facts|Current Events rule, subject to all other gates|Flyers/storage/history consequence|
|---|---|---|
|Valid `endsAt`|Inclusive through that exact instant; CONCLUDED immediately afterward|No automatic change|
|Date-only `event_date`|Current/upcoming through that Community-local calendar date; concludes next local midnight|No automatic change|
|Valid `startsAt`, no end|Uses the start instant's owning-Community date, current through that date; no duration invented|No automatic change|
|Future date/start/end|Eligible upcoming item, without treating occurrence as editorial expiry|Independent existing eligibility|
|Missing necessary occurrence|UNKNOWN / `missing_occurrence`; excluded from Events only|Otherwise eligible Flyer/media and other independent views retained|
|Malformed start/end/date|UNKNOWN / `invalid_starts_at`, `invalid_ends_at`, `invalid_event_date`; excluded from Events only|Stored facts retained; no prose/publication fallback|
|End earlier than explicit start|UNKNOWN / `end_before_start`|No mutation|
|Editorial EXPIRED, future occurrence|Excluded by existing current-inventory service regardless of event future|Current Flyers also excluded by editorial rule; historical status-safe reading remains independently permitted|
|Editorial APPROACHING_EXPIRY, future occurrence|Still eligible, existing attention signal unchanged|Independent normal Flyer eligibility|
|No event kind, conflicting kinds, wrong source sector, mismatched requested Community|Excluded regardless of occurrence/media/title|Other projections follow their own existing gates|

Timestamp fields use ISO offset timestamps, including `Z`; comparison normalizes to Instant. Local clock timestamps without offsets are rejected, avoiding ambiguous fall folds or nonexistent spring clock times. **Date-only `endsAt` is diagnosed `invalid_ends_at`**, rather than inventing midnight or 23:59:59. Authors use existing date-only `event_date` for a date-only event or provide an explicit offset timestamp for an exact end. All supplied fields are parsed; malformed supplied facts are not hidden by another valid field. End-only data can establish a cutoff; it does not create a fictitious start-date label.

Tests cover both 23-hour spring and 25-hour fall local days, explicit offset transitions, exact ends and one nanosecond afterward, the real October date, and New York versus Los Angeles ownership. Publication, retrieval, audit, expiration, headline, summary, media and producer identity never establish occurrence.

The helper's diagnostic is private/internal. The focused catalog test emits its assessments to the evidence JSON, including NP-003's missing occurrence. This is repeatable read-only authoring evidence, not an Admin queue, notification or generalized validation workflow. Existing six validators continue to pass unchanged: optional timestamp absence is backward compatible, and non-event records are not required to gain temporal fields.

## Bounded implementation and public presentation

- Optional `startsAt`/`endsAt` facts were added to existing **Flyer and NewsItem** models; existing JSON loaders already bind these fields. Existing `event_date` remains unchanged. No Event entity, repository, ContentType, recurrence, scheduling, RSVP or ticket model was added.
- `CommunityNoticesService` applies occurrence only at **EVENTS**, after the normal upstream current-inventory contract and alongside controlled kind, source sector and requested Community. Counts/items/previews share one per-view snapshot. Events sort by actual occurrence instants with canonical-ID ties; canonical dedup avoids counting repeated projection rows. Meetings and Announcements retain their existing membership rules.
- Optional explicit `communityId` now binds to Notices API, nav links and overview preview destinations. Omitted query preserves the previous aggregate scope; no guessed Community or second-community launch. Synthetic tests prove exact requested-scope isolation.
- Events display supplied occurrence facts, with a timestamp range when start and end exist. A News publication date is no longer displayed as an event date. No arbitrary “open now”, duration or verification freshness is asserted.
- Existing `/api/flyers` and `/api/flyers/{id}` formerly serialized internal models. **FlyerSummary** now exposes resident identity, Community, Category/Topic/tags, source ID/name/type/safe HTTP(S) URL, original organization/location/media and supplied occurrence fields. It omits editorial status/expiry, verified/audit/helpAccess, source retrieval and diagnostics. Historical public-status rules remain in FlyerService. Existing Article and IM-09 NewsSummary boundaries are unchanged.
- Original encoded Flyer media and safe navigation remain; unsafe Flyer source schemes are omitted from the resident DTO. Existing ContentCard rejects executable/data destinations. Gallery still renders original images; no duplicate normalized content is stored.

No lifecycle, selector, taxonomy, Article approval, IM-09 admission manifest, production record, provider/host relation, database, Admin UI or dependency/lockfile was changed.

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

Final logs: [backend](evidence/im10/final-backend-accepted.txt), [frontend](evidence/im10/final-frontend.txt), [typecheck](evidence/im10/final-typecheck.txt), [build](evidence/im10/final-build.txt), [Python commands/exits](evidence/im10/final-python.json). Initial focused run had one test-scaffold privacy failure: its recursive forbidden-name list incorrectly banned geographic `location.state`. The test now specifically rejects internal diagnostic/status fields while preserving the resident location fact. This was not an application failure; [failed run retained](evidence/im10/focused-backend.txt). The first approved packaged smoke likewise over-banned the ArticleDetail public editorial revision `updatedDate`; it was corrected only for that existing Article endpoint contract, with Flyer audit omission retained. Its failed log is preserved. Final full suite includes the corrected assertion and all boundary tests. No test disabled or skipped.

[Packaged HTTP responses](evidence/im10/packaged-http.json) use the **runtime clock** against untouched production catalogs with RSS refresh delayed; they do not pretend runtime is the fixed next-midnight scenario. Fixed-clock tests independently prove conclusion. [Browser result](evidence/im10/browser-result.json) and [375px](evidence/im10/flyers-375.png), [1280px](evidence/im10/flyers-1280.png), [Spanish/contrast](evidence/im10/flyers-spanish-contrast.png) screenshots show the built frontend, original loaded JPEG, keyboard view navigation, retained requested Community and no horizontal overflow/page errors.

Local bind/loopback and Chromium required sandbox escalation. Evidence servers were stopped after capture. These checks do not certify remote source content, complete event inventory, exhaustive WCAG or physical-device release quality; IM-13 retains full release verification.

## Preservation, architecture proof and handoff

[Preservation result](evidence/im10/preservation-result.json) confirms all original catalogs, the eight Flyers and their media, first-rate-news-membership.json, taxonomy, source registry, dependencies/lockfiles, production selectors, Article guards and supplied authority documents retain their starting SHA256. All **1,056** starting files were hashed: **1,037 unchanged, 19 authorized existing source/test/mirror changes**, plus the new bounded source/tests/report/evidence. Full [mirror manifest](evidence/im10/annotated-mirrors.json) covers all **15** touched source/test files. [Verification summary](evidence/im10/verification-summary.json) links final counts and results.

The exact production additions are two optional fields on each existing concrete model, one stateless occurrence helper and one public Flyer DTO. Existing Notices aggregation and Flyer controllers consume them. There is **no new Event entity/repository/type**, no Resource conversion, no scheduling/recurrences/RSVP/ticketing, no database migration, no provider/host assignment and no Admin/workflow/recommendation system. `ContentType` still contains exactly RESOURCE, NEWS, FLYER, LAW and EXPERT, asserted by test and protected hash. All eight live Flyers still have **zero approved helpAccess assignments**; none gain VISUAL/Get Help eligibility from being events.

**IM-10 Satisfied.** This supplies J4/J7 and E1/E3/E5 occurrence/identity evidence only. IM-11 owns then-current real selector collisions, IM-12 full identity/relationship/Community/public-boundary audit, IM-13 release gates and IM-14 original-guide/deployment/documentation reconciliation and final closure. First Step v2 is not declared complete.
