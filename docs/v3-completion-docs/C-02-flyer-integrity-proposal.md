# C-02 — Bounded Flyer Integrity Correction: Proposal for Approval

Status: **APPROVED and implemented (2026-10-23). See [C-02-flyer-integrity-closure.md](C-02-flyer-integrity-closure.md).** This proposal is kept as written at proposal time. Corrections made since are recorded in the closure document; for example, FL-006's hosts are DHSS Division of Public Health and CACCNE.
Baseline: `copyofdev` @ `a4ed4aeb0ff70bcbb34e164cd9ec87df695245d3`. Origin: V3A-00 R1 finding C-02.

## 1. Record-to-image comparison

Every flyer record that residents can currently see (all 8; none has a `status`, an `expirationDate` or `helpAccess`) was compared field by field against the poster file it references. The poster images were inspected directly in `backend/src/main/resources/static/images/seasonal/`.

| ID | Record: title / organizer / event date / location | Poster actually shows | Title | Organizer | Date | Location | Verdict |
|---|---|---|---|---|---|---|---|
| FL-001 | Summer Youth Enrichment Program — Registration Open / West End Neighborhood House / 2026-09-07 / West End Neighborhood House, 710 N. Lincoln St (`Youth.jpg`) | "The Monthly Focus on Physical Health" youth show, The Production Crew; June 4, dinner 6 pm, show 6:30; Bellevue Community Center, 510 Duncan Rd | ✗ | ✗ | ✗ | ✗ | **Mismatch** |
| FL-002 | Know Your Rights: Eviction Prevention Info Session / Community Legal Aid Society / 2026-08-30 / Wilmington Public Library, 10 E. 10th St (`Eviction Help.png`) | "Facing Eviction? Free legal help is available for Delaware renters": an ongoing referral service (bit.ly/eviction-help-de, 2-1-1, Mon–Fri hours) from a coalition (Office of Eviction Defense, CLASI, DVLS, LSCD, 2-1-1). No session, date or venue | ✗ (no info session) | Partial (CLASI is one of five) | ✗ (none on poster) | ✗ (none on poster) | **Mismatch** |
| FL-003 | Community Volunteer Recruitment Drive / United Way of Delaware / 2026-09-15 / Rodney Square, 1000 N. King St (`Volunteer.jpg`) | "Bocce Volunteers Needed", Special Olympics Delaware; Saturday May 16, 11 am–5 pm; sode.org | ✗ | ✗ | ✗ | ✗ | **Mismatch** |
| FL-004 | Back-to-School Supply Drive Fundraiser / Ministry of Caring Inc / 2026-08-26 / Ministry of Caring, 115 E. 14th St (`Fundraiser.jpg`) | 1st Annual Black-Tie Gala benefiting the American Legion Post #11 Building Fund; Saturday May 2, 2026, 6–10 pm; Joseph B. Stahl Post #30, 156A N. DuPont Hwy, New Castle | ✗ | ✗ | ✗ | ✗ | **Mismatch** |
| FL-005 | Disability Services & Benefits Info Fair / Delaware Division for the Visually Impaired / 2026-07-30 / Porter State Service Center, 509 W. 8th St (`Disablility Info.jpg`) | "Delaware Families of Children with Hearing Challenges — Know Your Rights", HEAR Audiology. Informational; no event, date or venue | ✗ | ✗ | ✗ | ✗ | **Mismatch** |
| FL-006 | Free Community Health Fair / Westside Family Healthcare / 2026-09-02 / Eden Park Recreation Center, 601 N. Van Buren St (`Health Fair.jpg`) | Caribbean Women's Health & Wellness Conference 2026, hosted by CACC and partners; Saturday May 9, 10 am–2 pm; Buena Vista, 661 S. DuPont Hwy, New Castle | ✗ | ✗ | ✗ | ✗ | **Mismatch** |
| FL-007 | Free Furniture Giveaway / Wilmington Housing Authority / 2026-08-15 / WHA Community Room, 1500 Bowers St (`Furniture.jpg`) | "Your Path to a Fully Furnished Home", The Foundation Project by Community Cares Delaware. Ongoing program: apply online; furniture hub at Wilmington Library, Tue 5–7 pm and Sat 9 am–12 pm | ✗ | ✗ | ✗ (ongoing, no date) | ✗ | **Mismatch** |
| FL-008 | United Neighbors 2026 / West Side Grows Together / 2026-10-17 / 7th Street Bridge between Adams St and Jackson St (`United Neighbors 2026.jpg`) | United Neighbors 2026; Saturday October 17, 1–4 pm; 7th Street Bridge between Adams and Jackson St; westsidegrows.org, @westsidegrowstogether | ✓ | ✓ | ✓ | ✓ | **Verified match** |

**Result: 7 confirmed mismatches (FL-001 to FL-007) and 1 verified match (FL-008).**

### Provenance

The seven mismatched posters were committed on 2026-06-06 (`a69b31e`) as `1985.jpg` to `1997.jpg`. The only metadata ever attached to them was generic carousel captions in the legacy `static/app.js` (`ceb6b8d`: "Community Event", "Volunteer Opportunity", "Health Care Event", "Fundraiser"). On 2026-06-15 (`9d527aa`) they were renamed to theme names (`Youth.jpg`, `Fundraiser.jpg` …), and the FL records were authored later (`be989ae`, 2026-07-11) to fit those names rather than the posters.

`app/data/seasonal.json` and `data-cleaning/raw/original.json` were committed as empty files. A complete search of git history and of all 282 JSON/CSV files under `~/Documents`, `~/Downloads` and `~/Desktop` found no richer upload metadata. FL-008 was added separately on 2026-10-15 and matches its poster.

## 2. Where these flyers currently reach residents

All resident surfaces read flyers through one choke point, `FlyerService.getAll()`, which already applies `EditorialLifecycle.isCurrent` (public status + editorial expiry).

| Surface | Flyers today | After the proposed hold |
|---|---|---|
| Homepage Community flyer row (`/api/home`, `communityFlyers`) | 6 (community-sector) | FL-008 only |
| Community Notices: Flyers / Meetings / Announcements / Events | Flyers 6; FL-002 in Meetings; FL-003 in Announcements; Events 0 (all dates past) | Flyers: FL-008; Meetings and Announcements lose FL-002 / FL-003; Events unchanged (0) |
| Category pages (e.g. Housing flyer collection = FL-002) | Category-tagged placements | Withdrawn flyers drop out |
| Program Updates `/updates` (government-sector flyers) | FL-005, FL-007 | Removed (2 fewer items) |
| Search, navigation counts, `/api/flyers` | All 8 | FL-008 |
| Historical direct link `/api/flyers/{id}` | All 8 | Withdrawn IDs return 404 (`getById` applies `isPublicStatus`) |

## 3. Proposed hold mechanism — existing `status: "withdrawn"`, no code change

Add one field, `"status": "withdrawn"`, to FL-001 through FL-007 in `app/data/flyers.json`. Nothing else changes.

Why this mechanism:

- **It already exists and is already enforced.** `EditorialLifecycle.isPublicStatus` treats `inactive` and `withdrawn` as non-public; `FlyerService.getAll()` and `getById()` both apply it. `EventProjectionTest` already pins `event.status="withdrawn"` hiding a flyer.
- **Records, IDs, images and catalog references are preserved.** The repository keeps withdrawn records ("the repository retains expired and withdrawn records"). FL identifiers, `organization_id`, `contentSource`, classification and image filenames are untouched.
- **Validation still covers held records.** All validators still inspect all 8 records ("status/expiry never conceal bad metadata"), so a held record cannot quietly decay.
- **Reversible per record.** Removing the field restores a flyer after its record is corrected against its poster.
- **No new model or content type, no Admin UI, no image recognition.**

`withdrawn` is preferred over `inactive` because it states that an editor removed the record; `inactive` reads like a lifecycle or expiry state.

**Alternatives considered and not proposed:**

- Rewriting the seven records to describe their posters. That is the "reconstruct prototype history" work this order excludes. Most of the posters are past (May–June 2026) or not events, and the organizations in them (American Legion Post #11, Special Olympics DE, HEAR Audiology, Community Cares Delaware, CACC) are not in the content-source registry, so it would also add identities.
- Deleting records or images. That violates the preservation requirement.
- Pointing records at the 12 unreferenced posters added on 2026-08-21. That is new content and needs its own verification.

## 4. Trial evidence (isolated worktree; working tree untouched)

The exact proposed change was applied in a disposable detached worktree of `a4ed4ae` (scratchpad), never in the working tree.

- Validators (`validate_content_sources`, `validate_homepage`, `validate_navigation`, `validate_news`, `validate_organizations`, `validate_schema`) and `python3 -m unittest discover -s data-cleaning/tests`: **all exit 0**.
- Backend `mvn -B test`: **631 run, 626 pass, 4 failures, 1 error.** Failures are listed in [evidence-c02/trial-mvn-failures.txt](evidence-c02/trial-mvn-failures.txt); the exact data diff is in [evidence-c02/proposed-flyers.diff](evidence-c02/proposed-flyers.diff).
- The 12 featured Housing winners are unchanged: no flyer was ever a featured item.

## 5. Test impact and proposed test changes

All five failures are real-catalog tests that pin FL-001 or FL-002 as current facts. In each case the behavior under test is still valid; only the catalog fact used to demonstrate it changes. Each test is kept, re-scoped to keep its intent, and no assertion about other records changes.

| Test (line) | What it asserted | Why it fails | Proposed change |
|---|---|---|---|
| `CivicCollectionCatalogTest` t01… (62) | Housing `flyers` collection = `[FL-002]` | FL-002 withheld | Expect `[]`; add an assertion that FL-002 is still stored (`findAll` keeps 8) |
| `CategoryDestinationCatalogTest` t04 (64) | Housing page has a `flyers` collection containing FL-002 | The collection is now absent (empty collections are omitted) | Assert there is no `flyers` collection; the 12-winner and no-private-field assertions are unchanged |
| `FeaturedItemCatalogTest` t19 (66) | Eviction Prevention emits diagnostic `FLYER_HELP_ACCESS_REQUIRED` naming FL-002 | A withheld flyer is no longer in the inventory, so it is never diagnosed | Assert that no diagnostic names a withdrawn ID. The diagnostic behavior stays covered by the fixture-based `FeaturedItemSelectorTest` |
| `EditorialExpiryApiTest` …ExpiredCommunityMeetings… (86) | Meetings = `[FL-002]`, count 1, NP-009/NP-010 excluded | FL-002 withheld | Expect `[]`, count 0; the NP-009/NP-010 expiry exclusions (the test's purpose) are unchanged |
| `EditorialExpiryApiTest` …PastEventFlyers… (113) | All 8 flyers delivered by `/api/flyers` and `/api/flyers/{id}` | Withdrawn IDs are no longer delivered | Re-scope to its stated intent, a **past-event** flyer keeping its detail link, using FL-008 (event 2026-10-17, past and not withdrawn). Keep "8 stored, none with `expirationDate`" |

**New regression test** (CLAUDE.md: a fixed defect gets a regression test), proposed as `FlyerMediaHoldRegressionTest` against the real catalog:

- FL-001 to FL-007 are absent from `/api/home` flyer cards, `/api/community-notices/{flyers,meetings,announcements,events}`, `/api/category/housing`, `/api/updates/government`, `/api/flyers` and search, and `/api/flyers/{id}` returns 404.
- FL-008 is still present on Notices Flyers, the homepage row and `/api/flyers/FL-008`.
- **Isolates the rule under test:** it asserts that the seven records have no `expirationDate`, so `status: "withdrawn"` is the only gate that can be excluding them. A pass cannot be caused by expiry or occurrence dates.
- All 8 records remain stored with unchanged IDs, `organization_id` and `image`.

Frontend tests mock their API and are unaffected. The full suite (backend, frontend with the Node flag, Python tests, validators) runs after the change, together with a browser check of the homepage flyer row with a single card.

## 6. Recommendation: recording administrator confirmation of flyer/media consistency

Mirror the Article review boundary (Decision 048) on flyers with one optional object per record, same vocabulary:

```json
"mediaReview": {
  "status": "verified",
  "reviewedDate": "2026-10-23",
  "reviewer": "human:editorial",
  "checked": ["title", "organization", "event_date", "location"]
}
```

Two possible enforcement levels, smallest first:

1. **Validator-only (no runtime change):** `validate_schema.py` fails any resident-eligible flyer (no `withdrawn`/`inactive` status) that has an `image` but lacks `mediaReview.status == "verified"`. Unverified media can then never ship unnoticed, and the runtime stays as it is.
2. **Fail-closed runtime gate (later, if wanted):** `FlyerService.getAll()` also requires a verified `mediaReview` for flyers with images. This is one filter plus tests, as for Articles.

**Recommended: level 1 now.** It is the smallest maintainable enforcement, reuses an established pattern, needs no new content type or Admin surface, and fits a future V3B stewardship workflow, which would write the same field. FL-008 would receive the first `mediaReview` record. Implementing level 1 is a separate approval from the hold.
