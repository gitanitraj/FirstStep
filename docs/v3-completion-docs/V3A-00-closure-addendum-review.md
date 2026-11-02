# Review — V3A-00 Authority Reconciliation & Closure Addendum (Draft 1)

**Reviewed document:** *First Step V3A-00 — Authority Reconciliation & Closure Addendum*, Draft 1, dated October 26, 2026
**Review date:** October 26, 2026
**Repository state checked:** `copyofdev` @ `f4ae9e05f8e5687d9287dba82e5cf5f1a847ca59`, pushed to `origin/copyofdev`, clean working tree
**Method:** The addendum's claims were checked against the repository, the committed R0/R1 and C-02 evidence, and the application source. The three approved Library documents (acceptance contract, implementation framework, Homepage/Housing design decisions) are not in the repository and were not available to this review.

## Verdict

The addendum's structure and evidence discipline are sound, but **it should not go forward as written.** One repository blocker is not recognized in the draft: most cited test logs were never committed. The draft also has three internal inconsistencies, five ratings that conflict with the recorded evidence, and it does not include one defect found during this review.

## What holds up

- Commit `f4ae9e0` exists on `copyofdev`, is pushed to `origin`, and the working tree is clean.
- The C-02 facts in §2 are accurate: backend 643/643, frontend 162/162, Python 16/16, seven validator commands, and 404 for withdrawn detail links. Caveat: the frontend count requires `NODE_OPTIONS=--no-experimental-webstorage` (audit tooling issue T-01), which §2 does not mention.
- The approved documents and the original visual reference are absent from the repository. §5 and §7 correctly decline to claim closure.
- The evidence discipline is good. "Established" is not presented as acceptance. Controlled fixtures are kept separate from live evidence. Approving the C-01 editorial requirements is not presented as fixing the OR-003 defect.
- All 67 criterion IDs (H-01–H-09, C-01–C-09, T-01–T-06, AH-01–AH-05, N-01–N-08, U-01–U-09, R-01–R-05, F-01–F-07, S-01–S-09) appear exactly once in §4. Whether the IDs and the "abridged faithfully" wording match the approved contract could not be verified, because the contract is not in the repository.

## 1. Blocker — most test logs were never committed

`.gitignore` line 22 (`*.log`) silently excluded every log file from `f4ae9e0`.

| Gap | Count | Examples |
|---|---|---|
| Files listed in the evidence hash manifests but missing from the commit | **24 of 96** (20 of 66 in `evidence/sha256.json`; 4 of 30 in `evidence/supplement-sha256.json`) | `backend-verify-approved.log`, `frontend-test-compatible.log`, `python-0.log` … `python-7.log`, `supplement-runtime-8080.log` |
| Broken evidence links in `C-02-flyer-integrity-closure.md` | **4** | `evidence-c02/backend-verify.log`, `frontend-test.log`, `frontend-build.log`, `python-tests.log` |
| Broken evidence links in `V3A-00-evidence-report.md` (R1) | **8** | `evidence/backend-verify-approved.log`, `evidence/frontend-test-compatible.log`, `evidence/runtime-approved.log`, `evidence/supplement-runtime-8080.log` |

**Consequences**

- The test counts the addendum relies on (C-02 643/643 and 162/162; R1 631/631 and 162/162) are stated in committed documents but are **not backed by log evidence on GitHub**.
- R1's statement that "all 66 entries in `sha256.json` still match" holds locally. A fresh clone contains only 46 of those files.
- Closure condition §5(4), "the R0/R1 audit and C-02 historical evidence remain intact", is **not met in the repository**.

**Remedy (documentation only):** add a `.gitignore` exception such as `!docs/v3-completion-docs/**/*.log`, or force-add the files with `git add -f`. The logs still exist in the local working copy. §5 should list "evidence logs committed and manifest-complete" as an explicit gate.

## 2. Internal inconsistencies

### 2.1 Package ownership contradicts itself

This fails the addendum's own §6 step 4, which requires every criterion to map to exactly one primary package.

| Criteria | §3 backlog says | §4 crosswalk says |
|---|---|---|
| S-01 – S-04 | V3A-01 | V3A-09 |
| S-05 – S-06 | V3A-08 | V3A-09 |

### 2.2 ID collisions beyond the one it addresses

§1 notes only that audit **C-01** and contract **C-01** share a name. There are two more collisions:

| Audit ID (V3A-00 R1 / C-02) | Contract ID | Ambiguity in the addendum |
|---|---|---|
| **C-02** — flyer integrity | **C-02** — deterministic four-topic selection | Every contract C-row evidence anchor cites "C-02 closure", which reads as the Category criterion |
| **T-01 – T-04** — tooling and environment | **T-01 – T-06** — Topic destination | §3's "Node 26 workaround T-01" could refer to either |

**Remedy:** prefix audit identifiers wherever they appear next to contract IDs, for example `V3A00-C-01`, `V3A00-C-02` and `V3A00-T-01`. This annotates the IDs and does not renumber either set.

### 2.3 A rating outside the legend

C-09 is rated "Not verified against approved criterion". The §4 legend defines only *Established*, *Partially implemented* and *Missing*. Either add this label to the legend or assign one of the defined ratings.

## 3. Ratings that conflict with the evidence

| Criterion | Addendum rating | Recorded evidence | Suggested rating |
|---|---|---|---|
| **H-09** — ineligible or misleading fixture content not presented as genuine | Partially implemented | OR-003, a test fixture, is still served on the homepage as a genuine Original (R1 C-01). U-07 rates the same fact *Missing* | **Missing / known gap.** C-02 resolved the flyer part only |
| **H-06** — Community Events shows eligible events or a usable empty state | Partially implemented | R1 §5: the homepage Community Events column is NOT IMPLEMENTED | **Missing** |
| **H-04** — exactly eight categories in a desktop 2×4 layout | Partially implemented | R1 §5: seven pathways; the 2×4 arrangement is NOT IMPLEMENTED | **Missing.** The categories exist, but the required layout does not |
| **R-04** — Organization detail distinguishes available, unavailable and retrieval-failure states | Partially implemented | **New finding (§4 below):** any failure is shown as "Organization not found" | **Missing / known gap** (code inspection; runtime not yet demonstrated) |
| **N-05** — withdrawn flyers excluded from public previews, collections and details | Partially implemented | Demonstrated on live data by `FlyerWithdrawalRegressionTest` (12 tests, mutation-checked) and `evidence-c02/runtime-api-check.txt` | **Established** (C-02 live evidence) |

## 4. New finding — Organization page masks outages as "not found"

`frontend/src/pages/OrganizationPage/OrganizationPage.tsx:43` handles every request failure with `.catch(() => setFailed(true))`. That is the same pattern as `ArticlePage.tsx:63`, which produces audit defect D-01 (an HTTP 503 shown as "Article not found"). A server outage on an Organization page therefore tells residents the organization does not exist.

- **Status:** found by reading the code; not yet demonstrated at runtime. R1 tested a synthetic 503 only on Article, which is why the R1 defect register does not include this.
- **Effect on the addendum:** R-04 should be rated *Missing / known gap* (§3 above). The finding should be added to the §3 backlog for V3A-07, or grouped with the V3A-06 article 503-vs-404 fix.

## 5. Evidence anchors don't point to anything specific

Every H-row evidence cell reads "R1 §4 homepage inventory; approved design contract §§2, 10; OR-003/C-01 and C-02 closure". That includes H-02 (the hero), which relates to neither OR-003 nor C-02. Every row's verification text reads "demonstrate exact criterion". For rows rated *Missing*, the R1 finding IDs already exist and would make each row traceable:

| Criterion | R1 / review finding |
|---|---|
| U-02 | A-01 — Program Updates request carries no Community scope |
| U-07, H-09 | C-01 — OR-003 public test fixture |
| U-08 | D-01 — Article 503 shown as not found |
| R-04 | §4 of this review — Organization failure masking |
| N-04 | D-02 — no flyer enlargement |
| N-07 | D-06 — raw i18n key on an invalid Notices view |
| S-04 | D-03 — inconsistent loading announcements; D-05 — stub pages lack an `h1` |
| S-06 | D-04 — navigation to placeholders; D-05 — catch-all "Coming soon" |
| F-03 | D-07 — "1 organizations"; D-08 — no-match guidance |
| F-04 | D-09 — search state lost on Back |
| H-04, H-06 | R1 §5 crosswalk rows marked NOT IMPLEMENTED |

## 6. Smaller points

- **Approved-document hashes.** §6 step 1 keeps the approved files byte-identical except the framework's stale approval line. The committed framework's hash will therefore differ from the Library copy. Record both hashes and the exact line that changed, so the governed copy can be traced to the approved original.
- **Scope of `f4ae9e0`.** §1 calls it the "C-02 correction" commit. It also contains the complete V3A-00 R0/R1 audit and its evidence (except the excluded logs), so it is the audit baseline commit as well. Saying so avoids a reader looking for the audit in another commit.
- **Frontend test caveat.** Where §2 cites frontend 162/162, add "with `--no-experimental-webstorage` (audit T-01)". This matches S-07's "environment limitations noted".

## 7. Recommended disposition

I agree with the addendum's own recommendation, **"Ready for authority-document incorporation"**, once these corrections are made:

1. Commit the excluded evidence logs and confirm that both hash manifests are complete in the repository.
2. Make the §3 and §4 primary-package owners agree for S-01 – S-06.
3. Prefix the colliding audit IDs (C-01, C-02, T-01 – T-04).
4. Correct the five ratings in §3 of this review, and either define or replace C-09's label.
5. Add the Organization failure-masking finding (R-04) to the backlog.
6. Replace the generic evidence anchors with the finding IDs in §5 of this review, at least for rows rated *Missing*.

V3A-00 should remain **Escalation Required** until §5 of the addendum is met in the repository, including the evidence logs. A separate **Satisfied** closure record can then supersede it, as the addendum proposes.
