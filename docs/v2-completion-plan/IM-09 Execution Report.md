# IM-09 execution report — controlled civic-information collections

**Executed:** October 17, 2026. **Disposition: Satisfied — IM-09 implementation and acceptance complete.** The initially unresolved editorial membership question was resolved by the user's explicit record-level decision during execution. Housing First Rate News now contains OR-003, FAQ-002, EA-001 and EA-005 under independent eligibility gates. Four collection audits, named acceptance tests and regression evidence are below. No commit or push; no later IM package started.

## Baseline and protection

Branch `copyofdev`, starting/ending HEAD `ba767eadd33e571441861f7fd5c4d47b98acca4c` (“IM-08 Changes”). IM-08 was committed in this checkout. Before evidence creation, only the user-added IM-09 work order and v2.9 matrix were untracked; no tracked changes. Both authority files remain byte-identical. [Starting state](evidence/im09/starting-state.txt), [before hashes](evidence/im09/preservation-before.json).

All 962 starting tracked/untracked files were hashed. Final comparison: **920 unchanged;42 authorized existing source/test/mirror changes**, plus new implementation/tests/mirrors/report/evidence. All original catalogs, taxonomy, lockfiles, production selectors and authority documents remain unchanged. A new narrowly scoped editorial admission manifest and one frontend Article-link condition implement the authorized decision. One IM-07 **test fixture** now explicitly records the authored law evidence it already claimed; selector production logic is unchanged. [After hashes](evidence/im09/preservation-after.json), [comparison](evidence/im09/preservation-result.json).

Baseline newly executed: **537 backend tests with Maven verify/package;143 frontend tests; TypeScript/build;14 Python tests; six catalog validators — all pass**. This is not inherited IM-08 evidence. Existing JDK25 toolchain/Java17 release, explicit Mockito agent and Node webstorage workaround were reused. Exact commands/failures/reproduction: [ledger](evidence/im09/reproduce.md).

Inspected `claude.md`, IM-09 order, v2.9 matrix §14, approved P4/P5, Completion Contract, Architecture Resolutions, Decision051/052, accepted IM-03–08 reports/tests, Category/Topic/All Help projections and public DTOs, frontend switcher/rail/link handling, News/RSS retained/current/signed inventories, source registry/sectors, Flyer lifecycle/media/helpAccess, Article approval/detail guards, Expert/FAQ models/repositories/classification and validators. The [inspect-first truth table/gap map](evidence/im09/inspect-first-truth-table.md) and [record decision table](evidence/im09/first-rate-news-decision.md) were written before production edits.

## Authority reconciliation: First Rate News remains inclusive

Approved P5 §17 explicitly describes eligible First Step NEWS/Originals **plus eligible EXPERT content**, retaining original identity/provenance. P4-16 includes approved Expert material and appropriate FAQ content; the Completion Contract includes approved Expert/FAQ material. Current v2.9 §14 and IM-09 require qualifying Expert Sessions and expressly prohibit both blanket EXPERT inclusion and an Originals-only reinterpretation.

Local `references/decisions.md` Decision051 also rejects producer/session-label unions as editorial qualification. Initial inspection found no controlled per-record Expert/FAQ collection admission mechanism. The initial audit and exact editorial question were preserved in [decision evidence](evidence/im09/first-rate-news-decision.md). The user then explicitly resolved the question; no classification was inferred to reach a historical count.

Before implementing the decision, the smallest representation was documented in that evidence file: **`app/data/first-rate-news-membership.json`**, three exact `(recordKind, recordId, communityId, categoryKey)` admissions, only for this collection. Record kind distinguishes existing concrete FAQ and ExpertAnswer, retaining EXPERT ContentType. The private manifest cannot approve Articles, mutate records, create Topics or bypass current inventory, Community or taxonomy. Missing manifest denies additional Expert/FAQ admissions; malformed/unsupported entries fail startup. Deployment must include this file in its existing `app.data.dir` alongside the catalogs; absence intentionally fails closed.

### Record-level decision table — final outcome

|Record / kind / ContentType|Authored Category / Topic|Provenance and approval evidence|Final First Rate News decision / supporting status|
|---|---|---|---|
|OR-003 / Article / NEWS|Housing, Legal / Eviction Prevention|first-step/manual; Article approved2026-08-12; publication2026-08-10|Previously approved Original; Housing and Legal membership retained; internal Article/headline link.|
|FAQ-002 / FAQ / EXPERT|Housing / none|first-step/faq; verified=true; derives from EA-005; source URL absent|**Explicitly approved independently by user**, Housing/Wilmington; also remains supporting FAQ. Derivation does not grant approval.|
|EA-001 / ExpertAnswer / EXPERT|Housing / none|de-volunteer-legal-services/expert-session; session2026-06-15; verified=true; Maria Alvarez; original source URL absent|**Explicitly approved by user**, Housing/Wilmington; also remains supporting Expert.|
|EA-005 / ExpertAnswer / EXPERT|Housing / none|Same producer/session date/expert; independent answer identity; source for FAQ-002; original source URL absent|**Explicitly approved by user**, Housing/Wilmington; separate canonical identity and supporting Expert membership retained.|
|EA-002 / ExpertAnswer / EXPERT|Food / none|de-dhss/expert-session; session2026-06-15; verified=true|**Not authorized** for First Rate News; Food supporting Expert retained.|
|EA-003 / ExpertAnswer / EXPERT|Employment / none|community-legal-aid/expert-session; session2026-05-20; verified=true|**Not authorized**; Employment supporting Expert retained.|
|EA-004 / ExpertAnswer / EXPERT|Health / none|westside-family-healthcare/expert-session; session2026-06-15; verified=true|**Not authorized**; Health supporting Expert retained.|
|EA-006 / ExpertAnswer / EXPERT|Utilities / none|de-dhss/expert-session; session2026-06-15; verified=true|**Not authorized**; Utilities supporting Expert retained.|
|FAQ-001/003/004/005/006 / FAQ / EXPERT|Food/Legal/Utilities/Health/Employment respectively / none|first-step/faq; verified=true;001 derives from EA-002,004 from EA-006|No new authorization; own supporting FAQ collection retained.|
|OR-001 / Article / NEWS|Housing, Community Support / Eviction Prevention|Article review flagged|Excluded by unchanged Article publication guard; no manifest override.|
|OR-002 / Article / NEWS|Health / Medical Care|Article review flagged|Excluded by unchanged Article publication guard.|

All 15 structured source records and final decisions: [record-membership-audit.json](evidence/im09/record-membership-audit.json). Session date, verification, producer, Category and FAQ linkage are facts, **never independent admission rules**. Contact websites were not copied into absent original session/source URLs. The four-item Housing result follows these exact approvals and independent gates. No additional editorial decision remains unresolved for IM-09.

## Four collection audits: approved versus observed and corrected

Fixed-clock actual repository/HTTP audit: **2026-10-16T16:00:00Z**. [Before](evidence/im09/collection-audit-before.json), [after](evidence/im09/collection-audit-after.json), [intermediate pre-admission](evidence/im09/collection-audit-pre-admission.json), [stored rows/provenance](evidence/im09/stored-collection-records.json). Raw stored rows, current eligible items and projection membership are recorded separately.

|Collection|Required eligibility/truth table|Actual Housing membership|Audit outcome|
|---|---|---|---|
|**Policy Info**|Current external civic NewsItem NEWS, Category/Community scope; excludes First Step producer, Articles, LAW and Flyers. Sector is a separate producer-registry concept.|**NP-004, NP-005**, both active; original producer/source/publication retained.|Verified with real records and synthetic all-NEWS counterexamples. Unknown type cannot create extra collection. Missing/unknown producer is never invented; sector exclusion is independent rather than a global content-validity rule.|
|**Law**|Underlying NewsItem with existing LAW ContentType; current/relevance gate; scoped placement needs original authored Category, and Topic placement needs original authored Topic. No publication→effective/deadline inference.|**Empty in the admitted local cache**, refresh deliberately delayed.|A demonstrated runtime-keyword placement gap was corrected in collections, Category updates and navigation counts/Topic counts. Broader admitted/signed feeds remain available under their own contract. Empty cache is a coverage limitation, not proof that Delaware has no law.|
|**Flyers**|Current real FLYER, Category/Community scope, original canonical ID/media/source; Get Help requires its separate exact token; editorial expiry independent of event occurrence.|**FL-002**, original image preserved, no helpAccess.|Verified independently of Get Help; current image/source context retained. Type guard, canonical dedup and stable equal-date ordering preserve the contract. No past-event cutoff or activation.|
|**First Rate News**|Eligible approved editorial material **and qualifying Expert Sessions/appropriate approved Expert/FAQ material**, retaining identities and independent publication gates.|**OR-003, FAQ-002, EA-001, EA-005**, in deterministic date/ID order.|Inclusive definition implemented using exact user-approved admissions. Supporting memberships retained; other Expert records remain excluded.|

Supporting FAQ/Expert are SUPPORTING projections, not a fifth civic-information collection. The four INFORMATION keys stay exactly `policy-info`, `law`, `flyers`, `first-rate-news`, with empty groups omitted in API and truthful switcher empty state already provided by IM-08. No independent collection URL endpoint exists; Category aggregate owns them. Topic/All Help returns actionable help, not an inferred Law/Expert browse.

Real source inventory:10 stored NP records; NP-001/002/006/009/010 remain stored but are **EXPIRED** and absent from current projections. NP-003 is **APPROACHING_EXPIRY**, still current; NP-004/005/007/008 active. Food/Wilmington Policy Info is NP-003/NP-007; its four audited groups otherwise empty. Food/bear-de has no civic-information group here despite positive Food Help. Legal/Wilmington has Flyers FL-002/FL-005 and First Rate News OR-003; Law/Policy Info empty. This proves scope differences without importing Housing items into Food or another Community.

All **8 production Flyers have 0 approved helpAccess assignments**, byte-identical. Housing FL-002 remains contextual; no Source/Host relationship becomes a provider. Flyer SHA256:

`5a9c17c1d64294cb5e0d144adfe5a7208763fa617654bb13b357fd6230cd24aa`

No remote feed refresh/completeness, legal interpretation, source content verification or provider/host audit is claimed. The existing right rail remains conservative: law rail also requires the preexisting authored Topic-proof adapter; no current real scoped law qualifies. Category-only authored law can now appear in the Category Law collection without inventing a Topic. No new rail threshold or inferred membership was added.

## Demonstrated gaps and bounded corrections

Before production edits, `CivicCollectionContractTest` failed3 of4 cases: runtime-inferred RSS LAW entered scoped Law, equal-date Policy Info depended on iteration order, and repeated approved Article identity inflated its collection count. Before-catalog HTTP audit passed while documenting actual membership; it was not mistaken for inclusive acceptance.

Implemented corrections:

1. `NewsItem` keeps **internal, nonserialized original authoredCategoryTags/authoredSubcategory snapshots**, captured by JsonNewsRepository before classifier normalization. These reflect existing stored fields; they are not new catalog/editorial metadata or publication decisions. Actual RSS entries have no authored snapshot, so keyword-generated Category/Topic cannot become scoped Law proof. CategoryContentService, NavigationService and Category UpdatesService enforce the relevant proof. Existing production selectors/sector registry/RSS relevance admission remain unchanged.
2. Collection factory deduplicates canonical IDs and reports unique counts. Dated/alphabetical collections gain canonical-ID tie order. Supporting FAQ/Expert authored file order remains intentional; it is not replaced by an invented ranking. Cross-view membership remains legal and does not duplicate storage.
3. `/api/news` and `/api/news/rss` now use **NewsSummary**, a public allowlist retaining canonical identity, current content, source ID/name/type/valid HTTP(S) URL, actual publication, original Why It Matters, legacy display metadata and explicit Community. Internal status/expiry/verified/audit/authored-proof/helpAccess fields are absent. LAW exposes only authored placement fields, not inferred Category/Topic as authored facts. The same broader feed items remain accessible; an optional explicit Community query filters exactly, with no guessed default/aggregation across requested scope. Existing app callers inspected do not consume the removed internal fields.
4. RSS missing publication date now remains **null**, instead of borrowing update time or retrieval “today.” Actual updated time is retained separately internally. No effective date/event deadline/newness is inferred. This corrects publication truth, not event occurrence or relevance ranking.
5. `FirstRateNewsMembership` loads exact scoped admissions for EA-001/EA-005/FAQ-002. Category composition intersects them with current Expert/FAQ services and existing taxonomy/Community gates, then sorts and canonical-deduplicates the merged result. Approved Articles still come exclusively through `ArticleService.getPublishable()`. Supporting projections remain independent. The frontend creates internal Article links only for NEWS in this controlled collection, retaining authentic Expert/FAQ source URLs or honest unlinked cards when absent.

News publication remains descriptive; an undated otherwise-current source is not excluded solely for missing publication. Approved Article workflow `draft/in-review/approved/flagged` is untouched, no new “published” state. ArticleDetail/public404 parity remains intact. Expert/FAQ Category-only authoring from IM-03 remains unchanged and cannot acquire a Topic from prose.

Public NewsSummary has no selection/approval/review fields. Category/Topic/Article allowlists remain independently tested. Unsafe executable/data/protocol-relative News source URLs are returned absent; existing shared card/rail safe-navigation and original encoded Flyer media remain covered. New Why It Matters mapping copies the source value verbatim without interpreting law. It is preserved on news/feed DTOs; no Category UI redesign was made.

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

## Actual commands, results and runtime/privacy proof

[Full ledger](evidence/im09/reproduce.md), [verification summary](evidence/im09/verification-summary.json).

|Gate|Baseline|Final|
|---|---|---|
|JDK25 Maven verify/package, release17|537 pass|**558 pass,0 failures/errors/skips; clean compile, executable jar packaged**|
|Frontend tests|143 /21 files pass|144 /21 files pass|
|TypeScript /Vite build|pass/pass|pass/pass|
|Python unittest|14 pass|14 pass|
|Six catalog validators|all exit0|all exit0|
|Actual repository HTTP audit|3 audit tests pass before edits|4 pass with scope/publication/privacy and approved/denied membership assertions; before/after JSON retained|
|Packaged network HTTP|not claimed baseline|**9 requests pass**, four memberships, scoped News, approved/flagged/missing Article guards|
|Source protection/complete mirrors/whitespace|962 files hashed|42 authorized starting-file changes,920 unchanged;28 source/test mirrors; no unexpected edits|

Full final logs: baseline-backend.txt; final-backend-admissions.txt; final-frontend-admissions-accepted.txt; final-typecheck-admissions.txt; final-build-admissions.txt; final-python-admissions-{0..6}.txt. Intermediate test scaffolding had a missing assertion import and TempDir argument; fixed. An incremental IDE-produced test class reported unresolved ClassifierFixture despite its unchanged import/source being present; clean Maven compilation resolved it. final-backend-first.txt/final-backend.txt retain positive-fixture/bytecode failures; final-backend-clean.txt passed550 before the additional authored-Topic test; final-backend-accepted.txt passes551 before the editorial decision. final-backend-admissions.txt passes558 after implementation of that decision. The first new frontend test attempted to inspect a card still hidden by the existing two-card preview; its final form explicitly expands the list and all 144 pass. All tests executed, none skipped.

Actual packaged jar ran against original catalogs plus the approved admission manifest in `app/data` on localhost8109 with RSS refresh delayed. [Smoke script](evidence/im09/http-smoke.py), [HTTP responses](evidence/im09/packaged-http.json), [result](evidence/im09/http-smoke-admissions.txt). Sandbox initially denied server bind/loopback access; explicit approved identical reruns completed. Runtime clock snapshot is separated from the fixed-clock audit. Source/Article URLs, Why It Matters and resident field allowlists were verified over HTTP; no remote provider/feed-content accuracy certification.

The bounded frontend change preserves Article navigation while preventing fabricated `/originals/EA-*` or `/originals/FAQ-*` destinations. Existing switcher, keyboard/focus, error/loading, Spanish/high-contrast and navigation regressions pass. [Fresh Chromium result](evidence/im09/browser-result.json) and screenshots at [375px](evidence/im09/first-rate-news-375.png) / [1280px](evidence/im09/first-rate-news-1280.png) verify actual built-app keyboard collection selection, all four cards after expansion, authentic/absent links, approved Article reading with Community retained, no horizontal overflow and no page errors. This is a focused smoke, not exhaustive WCAG/physical-device/release verification. Prior IM-08 evidence is untouched.

## Changed files, preservation and final handoff

Modified production: CategoryCollection.java, CategoryContentService.java, NavigationService.java, NewsController.java, NewsItem.java, JsonNewsRepository.java, RssFeedService.java, UpdatesService.java and CategoryPage.tsx. Added NewsSummary.java, FirstRateNewsMembership.java and the three-entry admission manifest. Existing direct-construction tests receive the new membership dependency; all eligibility assertions remain. Added/extended the named tests above, including the mixed-content frontend navigation case. All 28 touched source/test files have complete annotated mirrors; [manifest](evidence/im09/annotated-mirrors.json). Exact changed/new files appear in [ending state](evidence/im09/ending-state.txt).

All original source-of-truth catalogs, taxonomy, dependencies/lockfiles, production selectors, eight Flyers and user authority documents retain their starting hashes. No Flyer activation, event cutoff, Article approval, identity/provenance/Community mutation, provider/host inference, new ContentType or generalized workflow/recommendation system. The new private manifest records only the expressly authorized collection decision. HEAD remains unchanged; no reset/stash/staging/commit/push. Local evidence servers were stopped after capture.

**IM-09 is complete for its bounded scope**, including the initially blocked editorial decision now explicitly resolved. Existing sparse Law/cache/source-link limitations are disclosed rather than filled by inference. The matrix itself was preserved; this execution report records completion without editing the supplied authority.

IM-10 retains event occurrence; IM-11 then-current selector/collision regression; IM-12 full identity/PROVIDER/HOST/source/Community/public-boundary audit; IM-13 complete J1–J7/browser/WCAG/release gates; IM-14 original Admin Guide/documentation reconciliation and v2 closure. This run supplies J4/J7 and E3/E5 collection/privacy evidence without declaring those later packages or First Step v2 complete.
