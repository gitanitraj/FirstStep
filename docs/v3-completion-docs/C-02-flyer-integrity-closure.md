# C-02 — Bounded Flyer Publication Integrity Correction: Closure Record

**Branch:** `copyofdev` (base `a4ed4aeb0ff70bcbb34e164cd9ec87df695245d3`); changes are uncommitted, pending closure review.
**Authority:** C-02 Final Implementation Authorization (2026-10-23). Proposal: [C-02-flyer-integrity-proposal.md](C-02-flyer-integrity-proposal.md). Origin: [V3A-00 R1](V3A-00-evidence-report.md) finding C-02.
**Disposition:** **Ready for closure review.** The seven mismatched flyers are no longer resident-visible. Their records remain stored for correction. All backend, frontend, Python and validator suites pass. No unrelated content or model changes were made.

## 1. Final diff summary

| File | Change |
|---|---|
| `app/data/flyers.json` | `"status": "withdrawn"` added to FL-001 to FL-007 (7 lines). The preceding `organization_id` line on each record gains a trailing comma. No other field, record or file changed. [Exact diff](evidence-c02/proposed-flyers.diff) |
| `backend/src/test/.../category/service/CivicCollectionCatalogTest.java` | Housing `flyers` collection: expect `[]` (was `[FL-002]`); stored count of 8 still asserted |
| `backend/src/test/.../category/service/CategoryDestinationCatalogTest.java` | Housing page: assert no `flyers` collection (was: a collection containing FL-002). The 12-winner, no-private-field and never-featured assertions are unchanged |
| `backend/src/test/.../category/selection/FeaturedItemCatalogTest.java` | No Eviction Prevention diagnostic names FL-002 (was: `FLYER_HELP_ACCESS_REQUIRED` names FL-002) |
| `backend/src/test/.../shared/lifecycle/EditorialExpiryApiTest.java` | Meetings: live `[]`/0, plus a controlled fixture showing a current meeting listed and counted while an expired one is not. Past-event flyers: live FL-008 delivery and detail link, plus a controlled fixture showing a past-event flyer stays in inventory, detail lookup and Flyers |
| `backend/src/test/.../audit/FlyerWithdrawalRegressionTest.java` | **New**, 12 tests (§4) |
| `references/tests/*_annotated.java` (×4 updated, ×1 new) | Annotated mirrors for every touched test; code is verified identical to production apart from comments |

**Not changed:** poster images (identical to HEAD; hashes in [poster-sha256.txt](evidence-c02/poster-sha256.txt)), `data-cleaning/proposals/flyers.proposals.json`, any production Java or TypeScript, any DTO, model, validator or content type, and any other catalog record. FL identifiers, `organization_id`, `contentSource`, classification, dates and summaries are untouched.

**Mechanism:** the existing `EditorialLifecycle.isPublicStatus`, already applied by `FlyerService.getAll()` (every resident projection) and `FlyerService.getById()` (detail links). No code change was needed.

## 2. Poster-to-record comparison

### Evidence classes

So the reviewer can weigh each finding, every row is tagged with how it was established:

- **[R] Reproduced** — the source image file in `backend/src/main/resources/static/images/seasonal/` was inspected directly at full resolution during C-02 and compared field by field with `app/data/flyers.json`. All eight posters have this status.
- **[S] Screenshot** — corroborated by a rendered screenshot of the running application: V3A-00 R1 [page-1280-8.png](evidence/page-1280-8.png) (live catalog) and [supp-events-fixture-1280.png](evidence/supp-events-fixture-1280.png). This shows what residents were actually served. FL-005 and FL-007 were not captured in any screenshot.
- **[E] Earlier report** — stated by the earlier coding-agent investigation without file-level comparison. V3A-00 R0 noted only that "original poster images can contain dates/titles inconsistent with authored captions"; V3A-00 R1 named four mismatches (FL-001, FL-003, FL-004, FL-006) from screenshots. Both are superseded by [R].

**Limits.** [R] means inspection by the coding agent, not administrator confirmation; human review of these findings is still required (see the future requirements document). Nothing here verifies real-world facts such as whether an event happened or an organization's details. Only the consistency of record and image was assessed.

### Comparison

| ID | Record: title / organizer / date / location (image) | Poster shows | Title | Org | Date | Location | Evidence | Disposition |
|---|---|---|---|---|---|---|---|---|
| FL-001 | Summer Youth Enrichment Program — Registration Open / West End Neighborhood House / 2026-09-07 / 710 N. Lincoln St (`Youth.jpg`) | "The Monthly Focus on Physical Health", a youth-produced show by The Production Crew (CAUSE Theater); June 4, dinner 6 pm, show 6:30; Bellevue Community Center, 510 Duncan Rd | ✗ | ✗ | ✗ | ✗ | R, S, E | **Withdrawn** |
| FL-002 | Know Your Rights: Eviction Prevention Info Session / Community Legal Aid Society / 2026-08-30 / Wilmington Public Library, 10 E. 10th St (`Eviction Help.png`) | "Facing Eviction? Free legal help is available for Delaware renters": an ongoing referral service (bit.ly/eviction-help-de; 2-1-1 or 1-800-560-3372; Mon–Fri 8 am–9 pm) from five partners (Office of Eviction Defense, CLASI, DVLS, LSCD, 2-1-1). No session, date or venue | ✗ | Partial (CLASI is one of five) | ✗ | ✗ | R, S | **Withdrawn** |
| FL-003 | Community Volunteer Recruitment Drive / United Way of Delaware / 2026-09-15 / Rodney Square (`Volunteer.jpg`) | "Bocce Volunteers Needed", Special Olympics Delaware; Saturday May 16, 11 am–5 pm; sode.org | ✗ | ✗ | ✗ | ✗ (none on poster) | R, S, E | **Withdrawn** |
| FL-004 | Back-to-School Supply Drive Fundraiser / Ministry of Caring Inc / 2026-08-26 / 115 E. 14th St (`Fundraiser.jpg`) | 1st Annual Black-Tie Gala benefiting the American Legion Post #11 Building Fund; Saturday May 2, 2026, 6–10 pm; Joseph B. Stahl Post #30, 156A N. DuPont Hwy, New Castle | ✗ | ✗ | ✗ | ✗ | R, S, E | **Withdrawn** |
| FL-005 | Disability Services & Benefits Info Fair / Delaware Division for the Visually Impaired / 2026-07-30 / Porter State Service Center (`Disablility Info.jpg`) | "Delaware Families of Children with Hearing Challenges — Know Your Rights", HEAR Audiology. Informational; no event, date or venue | ✗ | ✗ | ✗ | ✗ | R | **Withdrawn**; attribution discrepancy (§3) |
| FL-006 | Free Community Health Fair / Westside Family Healthcare / 2026-09-02 / Eden Park Recreation Center (`Health Fair.jpg`) | Caribbean Women's Health & Wellness Conference 2026, hosted by Delaware Health and Social Services, Division of Public Health, and CACCNE (Caribbean American Chamber of Commerce); Saturday May 9, 10 am–2 pm; Buena Vista, 661 S. DuPont Hwy, New Castle | ✗ | ✗ | ✗ | ✗ | R, S, E | **Withdrawn**; see §3 note |
| FL-007 | Free Furniture Giveaway / Wilmington Housing Authority / 2026-08-15 / WHA Community Room, 1500 Bowers St (`Furniture.jpg`) | "Your Path to a Fully Furnished Home", The Foundation Project by Community Cares Delaware. Ongoing program: apply online; furniture hub at Wilmington Library, Tue 5–7 pm and Sat 9 am–12 pm | ✗ | ✗ | ✗ | ✗ | R | **Withdrawn**; attribution discrepancy (§3) |
| FL-008 | United Neighbors 2026 / West Side Grows Together / 2026-10-17 / 7th Street Bridge between Adams and Jackson St (`United Neighbors 2026.jpg`) | United Neighbors 2026; Saturday October 17, 1–4 pm; 7th Street Bridge between Adams and Jackson St; westsidegrows.org, @westsidegrowstogether | ✓ | ✓ | ✓ | ✓ | R, S | **Retained** under existing lifecycle rules |

**Corrections to the proposal.** FL-006's hosts are DHSS Division of Public Health and CACCNE; the proposal said "CACC". FL-001's producer is The Production Crew / CAUSE Theater. Neither changes a verdict.

### Provenance (reproduced from git history)

| Fact | Source |
|---|---|
| The seven mismatched posters were first committed as `1985.jpg`–`1997.jpg` on 2026-06-06 | `a69b31e` |
| The only original metadata was generic carousel captions ("Community Event", "Volunteer Opportunity", "Health Care Event", "Fundraiser") | `ceb6b8d:backend/src/main/resources/static/app.js` lines 308–314 |
| The files were renamed to theme names on 2026-06-15 | `9d527aa` |
| The FL records were first authored on 2026-07-11, after the renames | `be989ae` |
| `app/data/seasonal.json` and `data-cleaning/raw/original.json` were committed as empty files | `87c7f68`, `e0533b7` |
| FL-008 and its image were added separately | 2026-10-15 |

The user recalls richer metadata in an original JSON upload. **It was not found** in git history or in any of the 282 JSON/CSV files under `~/Documents`, `~/Downloads` and `~/Desktop`. If it exists elsewhere (for example a chat upload), it could support correcting these records.

## 3. Unresolved producer-attribution discrepancies

The records were **not** rewritten.

| ID | Record producer (`contentSource`) | Registry sector | Effect before C-02 | Poster producer | Status |
|---|---|---|---|---|---|
| FL-005 | `de-visually-impaired` (Delaware Division for the Visually Impaired) | Government | Appeared in **Program Updates** (`/api/updates/government`) | HEAR Audiology, a private provider | **Unresolved.** The government classification is **not verified** and must not be relied on |
| FL-007 | `wilmington-housing-authority` | Government | Appeared in **Program Updates** | Community Cares Delaware (The Foundation Project), a nonprofit | **Unresolved.** The government classification is **not verified** and must not be relied on |

Both records are withdrawn, so neither is resident-visible. Their `contentSource` and `organization_id` (ORG-059, ORG-061) remain as stored. Correcting them is an editorial decision: either re-attribute them to the poster's producer (which may require a new registry entry), or keep the records and replace the image.

**Additional observation (not in the authorization's scope, recorded for completeness):** FL-006 shows the inverse pattern. Its record names a community producer (`westside-family-healthcare`), while its poster is co-hosted by a government agency (DHSS Division of Public Health). It is also withdrawn, and the same unverified-attribution caution applies.

## 4. Regression coverage (`FlyerWithdrawalRegressionTest`, 12 tests)

| Authorization requirement | Test(s) |
|---|---|
| All seven withdrawn records remain in the repository | `shouldRetainAllSevenWithdrawnRecordsWithTheirOriginalMetadata`: 8 stored; the seven are `withdrawn` with original `organization_id` and `image`; FL-008 has no status |
| None appears in resident flyer inventory, Community Notices, Program Updates or Category projections | `…FromResidentFlyerInventory`, `…FromEveryCommunityNoticesView` (overview and four views), `shouldShowOnlyTheVerifiedFlyerInTheHomepageFlyerRow`, `…FromProgramUpdates` (government and community), `…FromEveryCategoryProjection` (every category from `/api/categories`, page and help) |
| Public detail endpoints return the established unavailable response | `shouldReturnTheEstablishedNotFoundResponseForWithdrawnDetailLinks`: HTTP 404, `success:false`, `errorCode:NOT_FOUND` |
| Withdrawal overrides otherwise eligible event and expiry dates | `shouldLeaveWithdrawalAsTheOnlyGateExcludingTheSevenRecords` (the seven are expiry-eligible, so status is the only gate); fixture pair `shouldAdmitTheFixtureFlyerWhenItIsNotWithdrawn` (control: future event and future expiry, shown in Events and Flyers) and `shouldExcludeAWithdrawnFlyerDespiteEligibleEventAndExpiryDates` |
| FL-008 remains retrievable where lifecycle rules allow | `shouldKeepTheVerifiedFlyerRetrievableUnderItsLifecycle`: detail 200, in Flyers, and in Events on the 2026-10-16 clock (event 2026-10-17) |
| Featured Housing selection remains consistent with approved rules | `shouldKeepTheApprovedFeaturedHousingSelection`: the same 12 winners as `CategoryDestinationCatalogTest` |

**Negative-test verification** ([mutation-check.txt](evidence-c02/mutation-check.txt)). The class was run against the pre-C-02 `flyers.json`, and 8 of 12 tests failed. Each failed on its own rule and named a specific withdrawn record: Housing and Notices expose FL-002; Updates exposes FL-005; detail returns 200 instead of 404; the inventory lists all 8. The 4 tests that do not depend on the hold passed: the two fixtures, FL-008 and the featured-Housing baseline. The approved file was then restored and byte-compared.

## 5. Test results (final, after all changes)

| Suite | Command | Result | Evidence |
|---|---|---|---|
| Backend | `mvn -B verify` (JDK 25, Maven 3.9.16) | **PASS — 643 run, 643 passed, 0 failures, 0 errors, 0 skipped**; BUILD SUCCESS (baseline 631 + 12 new) | [backend-verify.log](evidence-c02/backend-verify.log) |
| Frontend tests | `NODE_OPTIONS=--no-experimental-webstorage npm test -- --run` | **PASS — 162/162, 23 files** | [frontend-test.log](evidence-c02/frontend-test.log) |
| Frontend build | `npm run build` | **PASS** | [frontend-build.log](evidence-c02/frontend-build.log) |
| Python | `python3 -m unittest discover -s data-cleaning/tests -v` | **PASS — 16/16** | [python-tests.log](evidence-c02/python-tests.log) |
| Validators | `validate_content_sources`, `validate_homepage`, `validate_navigation`, `validate_news`, `validate_organizations`, `validate_schema`, `validate_schema --input app/data/resources.communities.json` | **All exit 0** | `evidence-c02/validate_*.log` |
| Runtime | Verified jar, live catalog, real clock 2026-10-23 | No withdrawn ID on any surface; seven detail links 404 `NOT_FOUND`; FL-008 200 | [runtime-api-check.txt](evidence-c02/runtime-api-check.txt) |

**Remaining failures: none.** The frontend still needs the Node 26 flag; this is pre-existing tooling issue T-01 from V3A-00, not a C-02 effect. Pre-change trial: 631 run with 4 failures and 1 error ([trial-mvn-failures.txt](evidence-c02/trial-mvn-failures.txt)), all resolved by the approved test updates.

## 6. Resident-visible effect (real date, 2026-10-23)

| Surface | Before | After |
|---|---|---|
| Homepage flyer row | 6 flyers | 1 (United Neighbors 2026) |
| Community Notices → Flyers | 6 | 1 (FL-008) |
| Community Notices → Meetings / Announcements | FL-002 / FL-003 present | Both flyers removed; Meetings has 0 items (test and runtime verified). Announcements contains no flyer; its remaining news items were not separately counted |
| Community Notices → Events | 0 (all dates past) | 0 (unchanged; FL-008's event was 2026-10-17) |
| Program Updates (government) | Included FL-005, FL-007 | Both removed |
| Housing category | FL-002 flyer collection | No flyer collection; featured winners unchanged |
| `/api/flyers/{FL-001…007}` | 200 | 404 `NOT_FOUND` |

Community flyer content is now thin (one poster, already past its event). The V3A homepage event column and flyer work will need verified flyers. The 12 poster images added on 2026-08-21 and referenced by no record are candidates, but they need their own record authoring and verification.

## 7. Evidence index

| Evidence | Path |
|---|---|
| Proposal (pre-approval analysis and trial) | [C-02-flyer-integrity-proposal.md](C-02-flyer-integrity-proposal.md) |
| Future review requirements | [C-02-future-media-review-requirements.md](C-02-future-media-review-requirements.md) |
| Data diff | [evidence-c02/proposed-flyers.diff](evidence-c02/proposed-flyers.diff) |
| Poster hashes | [evidence-c02/poster-sha256.txt](evidence-c02/poster-sha256.txt) |
| Pre-change trial failures | [evidence-c02/trial-mvn-failures.txt](evidence-c02/trial-mvn-failures.txt) |
| Mutation check | [evidence-c02/mutation-check.txt](evidence-c02/mutation-check.txt) |
| Final test logs | `evidence-c02/backend-verify.log`, `frontend-test.log`, `frontend-build.log`, `python-tests.log`, `validate_*.log` |
| Runtime check | [evidence-c02/runtime-api-check.txt](evidence-c02/runtime-api-check.txt), `runtime.log` |
| Earlier screenshots (class S) | [evidence/page-1280-8.png](evidence/page-1280-8.png), [evidence/supp-events-fixture-1280.png](evidence/supp-events-fixture-1280.png) |
