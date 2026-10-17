# IM-04 Execution Report

**Disposition: Satisfied — authorized synthetic-only infrastructure requirement.**
**Production Flyer editorial activation: none (0 of 8).**
Executed October 16, 2026. This closes only IM-04 capability and evidence; it does not declare IM-05 or the full v2 product complete.

## Baseline, authority and preservation

Branch `copyofdev`; starting HEAD `e0c9e3f3fcb6757ab3abc41f8ce2ffc2f1be085b`, clean tracked/untracked state. Fetched and inspected the updated branch, then fast-forwarded to `3b8c3d6d4496c8321a4cb5f5c067661b1533c103`; its additions were the IM-04 order and `docs/IM-03 Execution Report.rtf`. Ending branch/HEAD are unchanged from that fast-forward. IM-01/02/03 were committed and present, rather than dirty as historical reports described. No predecessor rebuilt, reset, stash, staging, commit or push performed.

Read the authoritative execution matrix before the IM-04 order, P4, approved P5 especially P5-R2/§§13–15 and PROVIDER/HOST distinction, Decision 051 and its carried exceptions, preceding execution reports, and `claude.md`. Baseline backend verify passed 454 tests (0 failures/errors/skips); baseline frontend passed 126, Python passed 12, six validators passed, typecheck/build passed. Baseline logs are retained under evidence/im04.

Inspected CivicContent, fixed ContentType, Flyer, JsonFlyerRepository, FlyerService/Controller, EditorialLifecycle, CategoryContentService/CategoryPageService/TopicPageService and public DTO/controller contracts, ContentItem, frontend ContentCard, taxonomy and every stored Flyer. AI Find Help currently consumes ResourceServiceLike/resources, separate from Category Get Help; no AI pipeline change was made.

**Original gap:** Category Get Help used only `resources(...)`; Flyers were a separate collection. No controlled actionable-help/access field existed. Topic membership was not positive Get Help evidence. Baseline packaged responses show FL-002 in Housing Flyers but not Get Help and FL-008 in Community Support Flyers but not Get Help. Image, Category, Topic, title and descriptive tags were insufficient to authorize placement.

## Approved correction and precise contract

The missing semantic was surfaced before implementation. The user explicitly approved the optional CivicContent `helpAccess` field with sole exact token `ACTIONABLE_HELP_ACCESS`, synthetic-only proof and no live assignments. See [IM-04 Semantic Decision](IM-04%20Semantic%20Decision.md) for approval conditions and schema/API rules.

- Only exact equality admits a Flyer to Get Help; missing/null/unknown/wrong-case/padded runtime strings do not. No prose inference.
- Catalog omission is valid. Every present Flyer value other than the exact approved string, including explicit null, number, Boolean, array and object, fails `invalid_help_access` validation. The default schema command checks all eight Flyers in addition to its existing Resource/Expert gates. `--flyer-help-only --flyer-input PATH` runs the isolated rule. `--expert-only` remains its existing explicitly narrow gate; `--export-schema` remains the existing Resource-only export, not a new Flyer schema artifact.
- Jackson can deserialize the string, but the field is write-only and omitted from resident serialization. Unknown strings/numeric coercions never equal the token; object/array binding fails. Runtime invalid-string fixtures deliberately bypass the catalog gate to prove defense in depth.
- Public `helpFor(categoryKey, topic, communityId)` returns current Resources and exact-signal Flyers under existing Category/Community predicates. Supplied Topic must belong to that Category and match the record; null Topic means Category-wide. Existing null/blank Community behavior remains; explicit Community filters singular ownership. Resource eligibility does not require or consult helpAccess; no other ContentType gains admission.
- Current inventory comes from existing lifecycle-filtered services. No event-occurrence cutoff or ranking rule was added. Historical expired detail remains available under existing public-status rules.
- Each result deduplicates by canonical ID; Resources win an invalid cross-type ID collision. Flyers collection also deduplicates by ID. A Flyer in two collections contributes two view memberships but one unique catalog identity. No writes or duplicate RESOURCE records occur.
- Flyer cards retain original source URL and now expose encoded imageUrl using the single existing FlyerService encoding method. The same ContentItem is used in both views. Producer, Community, source record, original media, date and organization label remain intact. A host label is not converted into a permanent provider relationship.

## Complete real-record audit

Machine-readable record-by-record evidence: [real-flyer-audit.json](evidence/im04/real-flyer-audit.json).

Common actual facts for all eight: concrete class Flyer, canonical ContentType FLYER set by its constructor, one stored ID each, singular Community `wilmington-de`, explicit stored Category/Topic, producer reference retained, no stored editorial status or expirationDate, no controlled help/access signal, current Flyers=yes, current Get Help=no. IM-01 treats missing expiry as eligible, not evidence of verified freshness. Event dates are retained without new occurrence cutoffs (IM-10).

| ID | Actual title | Category / Topic | Producer ID | Original image |
|---|---|---|---|---|
| FL-001 | Summer Youth Enrichment Program — Registration Open | Community Events / Youth Programs | west-end-neighborhood-house | Youth.jpg |
| FL-002 | Know Your Rights: Eviction Prevention Info Session | Housing, Legal / Eviction Prevention | community-legal-aid | Eviction Help.png |
| FL-003 | Community Volunteer Recruitment Drive | Community Support / Volunteer Opportunities | united-way-delaware | Volunteer.jpg |
| FL-004 | Back-to-School Supply Drive Fundraiser | Community Support / School Supplies | ministry-of-caring | Fundraiser.jpg |
| FL-005 | Disability Services & Benefits Info Fair | Legal, Community Support / Disability Advocacy | de-visually-impaired | Disablility Info.jpg |
| FL-006 | Free Community Health Fair | Health / Medical Care | westside-family-healthcare | Health Fair.jpg |
| FL-007 | Free Furniture Giveaway | Furniture & Household / Furniture & Household Goods | wilmington-housing-authority | Furniture.jpg |
| FL-008 | United Neighbors 2026 | Community Support / Community Celebrations | westside-grows-together | United Neighbors 2026.jpg |

**Assessment for each row:** proven eligible for current Flyers under existing scope/status/editorial-expiry rules; proven ineligible for Get Help in the current stored classification because the required explicit semantic is absent; actual actionable-help classification **requires editorial/semantic decision**. This is not an editorial assertion that the record is non-help. No per-record assignment was authorized or written. FL-002's legal-aid session is suggestive but not preapproved; FL-008's art/food/music celebration does not establish access-to-help. The same restraint applies to all other rows. Each record's actual summary and original organization ID/label are preserved in the JSON audit.

Raw `organization_id` exists but is not a modeled PROVIDER/HOST relationship in Flyer. Neither source nor organization label proves a permanent provider. No relationship was manufactured. Full association audit belongs to IM-12.


## Exact changes

- `backend/.../shared/model/CivicContent.java`: optional write-only editorial semantic field.
- `backend/.../category/service/CategoryContentService.java`: reusable helpFor seam, Get Help composition, within-collection identity dedup, existing Flyer media resolution in both projections.
- `backend/src/test/.../category/service/FlyerHelpProjectionTest.java`: nine focused synthetic regression tests.
- `data-cleaning/scripts/validate_schema.py`: exact-token Flyer gate in default validation and isolated CLI mode.
- `data-cleaning/tests/test_flyer_help.py`: two tests including ten invalid-value CLI subcases with exact diagnostic and nonzero exit assertions.
- Complete annotated mirrors of all five touched/new source/test files under references/ and references/tests/.
- This report, approved semantic decision and evidence/im04/ audit, logs, runtime responses, synthetic fixtures/check script and preservation evidence.

No production `app/data`, taxonomy, dependency manifest or lockfile changes. No UI changes, new entity/ContentType, association model, generalized eligibility engine, occurrence logic, selector or Admin infrastructure.

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

## Reproducible verification and actual results

| Command | Final result |
|---|---|
| `bash /workspace/.firststep-tools/im01-mvn -Dtest=FlyerHelpProjectionTest,CategoryContentServiceTest test` | 25 passed, 0 failures/errors/skips (initial focused run); later write-only assertion covered by full run |
| `bash /workspace/.firststep-tools/im01-mvn verify` | **463 passed**, 0 failures/errors/skips; BUILD SUCCESS with JDK25 toolchain, Java17 release |
| `npm test` in frontend | **126 passed**, 19 files |
| `npx tsc --noEmit` in frontend | exit 0 |
| `npm run build` in frontend | exit 0 |
| `python3 -m unittest discover -s data-cleaning/tests -v` | **14 passed**, including ten invalid-token CLI subcases |
| Six `python3 data-cleaning/scripts/validate_<name>.py` commands | content_sources, homepage, navigation, news, organizations, schema all exit 0; 12 Expert records and 8 Flyers pass respective classification checks |
| `git diff --check` | clean |

Baseline and final logs are retained under [evidence/im04](evidence/im04/). Full backend result includes existing CategoryContentServiceTest, FlyerServiceTest, JsonFlyerRepositoryTest, EditorialLifecycleTest, EditorialExpiryApiTest, EditorialExpiryDiagnosticsTest, EditorialExpiryReportCommandTest, ExpertCategoryIntegrityTest and ExpertCategoryApiTest.

Default combined gate negative evidence: `python3 data-cleaning/scripts/validate_schema.py --flyer-input docs/v2-completion-plan/evidence/im04/synthetic-flyers.json` exits **1** with `SYN-invalid: invalid_help_access`, while other valid fixture entries pass this rule. See im04-invalid-default-gate.log. This is an expected rejection, not a failing release gate.

### Packaged API proof

Baseline runtime responses baseline-runtime-0.json through -5.json preserve the unchanged real catalog observations. Final runtime uses a **temporary** data directory copied from app/data, replacing only its Flyers with [synthetic-flyers.json](evidence/im04/synthetic-flyers.json); it is never copied back. The synthetic fixture intentionally includes UNKNOWN to prove runtime exclusion when validation is bypassed; it is not a valid publishable catalog.

Reproduce: copy app/data JSON files into `/tmp/im04-fixture-data`, copy the evidence synthetic-flyers.json there as flyers.json, then run from repository root:

```sh
/workspace/.firststep-tools/jdk-25+36/bin/java -jar backend/target/firststep-backend-0.1.0.jar --server.port=8096 --news.rss.initial-delay=3600000 --app.data.dir=/tmp/im04-fixture-data
python3 docs/v2-completion-plan/evidence/im04/packaged-check.py
```

All assertions passed. `/api/category/housing?communityId=synthetic-community` yields Get Help=[SYN-access,SYN-near], Flyers=[SYN-access,SYN-near,SYN-ordinary,SYN-invalid]. Expired/withdrawn/different Community fixtures are excluded appropriately. Both views preserve FLYER type, ID and image URL; `/api/flyers/SYN-access` retains Community and producer without exposing helpAccess; historical `/api/flyers/SYN-expired` remains available. Encoded `/images/seasonal/Eviction%20Help.png` returns nonempty image data with HTTP200. Unrelated Community returns no collections. See packaged-final.json and packaged check log.

An initial final-runtime startup encountered occupied port8094 (the earlier baseline process); the first request therefore hit the old baseline and failed the positive assertion. Diagnosed the stale listener, moved the new packaged process to8096, confirmed fresh startup/fixture loading and reran all assertions successfully. No failed assertion was weakened. Processes started for these checks were stopped after verification.

## Final preservation and boundaries

[protected-sha256.json](evidence/im04/protected-sha256.json) covers all catalog files and dependency manifests/lockfile; preservation-check.json confirms all 20 unchanged. Starting tree had no dirty predecessor files; final modified/untracked inventory is in git-final.txt. Only this package's files changed. No commit/push was authorized or performed.

IM-05/08 consume helpFor and own complete help/topic destination composition; no new resident page is claimed. IM-10 owns event occurrence/concluded-event visibility; editorial expiry remains IM-01. IM-12 owns full association/public-boundary audit. The existing raw Flyer API exposes its preexisting model fields; this change adds no new editorial/private response field and does not claim a full audit of that existing endpoint.

**IM-04's authorized infrastructure requirement is Satisfied by synthetic proof and passing gates. None of the eight production Flyers has helpAccess or an approved Get Help placement. Live activation requires separate record-level editorial decisions.**
