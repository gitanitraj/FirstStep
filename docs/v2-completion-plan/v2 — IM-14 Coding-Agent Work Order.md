# First Step v2 — IM-14 Coding-Agent Work Order

**Package:** IM-14 — Documentation Reconciliation and v2 Closure  
**Authorized:** October 18, 2026  
**Starting disposition:** Final Closure Package — execution authorized, **not Satisfied**  
**Predecessor:** IM-01–IM-13 accepted; IM-13 closure documented in authoritative matrix §14E.  
**Mission:** Make the v2 Completion Contract, actual implementation, historical obligations, operations/authoring instructions, evidence ledger, and V3/Later deferrals agree. **Only issue a v2 closure recommendation if no required v2 obligation remains unresolved.** Do not self-approve the final closure.

## 1. Binding authorities and exact starting-state discipline

Read the supplied **IM-13-closed authoritative matrix first**; then inspect the repository's actual `claude.md`, v2 Completion Contract and original promise documents (including philosophy/application architecture), decisions and relevant P4/P5 design, approved Architecture Resolutions, Resolution of Five Open Historical Questions, Open Required Work Closure Backlog, J1–J7/E1–E5 mapping, original Admin Guide, all IM-01–IM-13 execution reports, final IM-13 integrity/closure supplements, current authoring/deployment docs and V3/Later parking lot. Enumerate what exists, what is absent, and contradictions with exact path/section citations. An unavailable original document is **not** presumed reconciled; retrieve it from approved repository/project sources if present, otherwise identify the exact document/obligation gap and prepare a traceable substitute or escalation without pretending to have read it.

Before modifying anything, record branch, full HEAD, tracked/dirty/staged/untracked/deleted paths, relevant ignored outputs, and hashes of every authoritative/user-managed file and production/catalog input. The IM-12/13 history includes a deliberately deleted old tracked matrix and untracked replacement authority documents: **do not restore, overwrite, stage, reset, stash, commit or push** those states. Treat the supplied IM-13-closed matrix as controlling for this work order, but do not self-update its IM-14 terminal status. Use an inspect-first discrepancy register before proposing edits.

## 2. Authoritative closure ledger (required output)

Create a **single traceable closure ledger** covering every original v2 Completion Contract obligation, recovered historical unfinished requirement, the five resolved historical questions, the Open Required Work Closure Backlog, P4/P5 approved concept, J1–J7, E1–E5, IM-01–IM-14 and the finite v2 boundary. For EACH entry record:

- Exact source and section / requirement ID (or `source unavailable` explicitly), requirement as approved and whether it was later superseded/clarified;
- `Implemented + verified`, `Satisfied by approved interpretation`, `Documented operational/manual v2 process`, `Explicit V3/Later deferral`, `Open — v2 required`, or `Unresolved — evidence needed` (do not conflate the latter states);
- Concrete implementation class/endpoint/record or document path, test/evidence file and package/report; actual acceptance date as appropriate;
- Who owns any remaining work, resident impact, and why a purported deferral is authorized rather than silently dropped.

**No unresolved `Open — v2 required`, `Unresolved — evidence needed`, or inherited `Verify` item may be hidden or silently converted to V3.** If found, mark IM-14 Blocked or Implementation Required and present bounded corrective options. The agent may fix only truly documentary inconsistencies within this work order; new application development requires explicit reviewer decision/change control.

## 3. Required reconciliations

### A. Original promises and historical obligations

Compare original five-stage spine **Collect → Generate metadata → Normalize → Enrich → Deliver** and three-layer architecture (Shared Domain, Business Capabilities, Information Pipeline) to what v2 *actually* implements. Verify the original eight-point Definition of Done and the revised/replaced milestones, decisions and phase/slice commitments. Clearly label which original scope was satisfied, reinterpreted by approved Decision 051/052 or later resolution, or explicitly moved to V3. Do **not** infer that a documented architecture diagram proves all proposed pipeline automation exists. Preserve First Step's Wilmington-first, multi-Community partition with second-community launch deferred.

Reconcile the **five historical-question resolutions**, the complete Open Required Work Closure Backlog, canonical **seven journeys and five epics**, and all relevant carried defects including expiry enforcement, Flyers/Get Help overlap, Expert/FAQ explicit Category tags, and Community Events generalization. The runtime v2 terminal test is finite; do not automatically bring V3 features back into v2 merely because they were historically discussed.

### B. P4/P5 logical design vs actual technical representation

Inventory CivicContent six concrete kinds/five ContentTypes; Community ownership vs physical eligibility; Category/Topic controlled placement/authored proof; ContentSource producer vs Organization provider identity; narrow PROVIDER/HOST proposal vs existing `Resource.organizationId` and **no implemented typed HOST association**; lifecycle/expiry vs occurrence; controlled civic collections; qualified First Rate News admissions; deterministic Topic/item selection and honest fallbacks. Identify logical-model coverage, partial physical implementation and actual resident-journey justification separately. Do **not** state that all approved P5 physical modeling has been built when only bounded v2 behavior exists.

### C. Admin/editor authoring and stewardship guidance

Locate and reconcile the **original Admin Guide** with the accepted IM-02 interim `editorial-expiry-supplement.md` and subsequent changes; make a usable **v2 Admin/editor handbook or exact supplemental reconciliation** that explains:

- Authoring, validation and reload for JSON files; IDs, concrete kinds, canonical ContentTypes, explicit Community precedence, Category/Topic proof and `resources.communities.json` coverage limitations;
- Editorial lifecycle: ACTIVE/current, APPROACHING_EXPIRY/current + attention, EXPIRED/not current + retained; 30-day Community-local window, calendar boundary and CLI diagnostics; missing/invalid dates; read-only reports versus manual review;
- Event occurrence: valid `event_date`, `startsAt`, `endsAt`, Community-local midnight and exact ends, concluded vs unknown; never infer time from prose or treat event end as editorial expiry;
- Flyer independent contextual visibility and `helpAccess=ACTIONABLE_HELP_ACCESS` controlled admission; **0/8 current production Flyers approved for Help**; do not imply a Flyer is a provider;
- First Rate News editorial process: independent Wilmington/Housing admissions EA-001, EA-005, FAQ-002; approved OR-003 through separate Article gate; nonadmitted Experts remain supporting; manifest maintenance validation and provenance/source handling;
- Organizations: optional explicit `organizationId`, producer versus provider versus HOST, unassigned provider and geographic caveats, safely scoped public links and privacy; how to handle unknowns without fabricating data;
- Supported v2 manual workflow and evidence-based editorial-review standard (Decision 048): generation ≠ evaluation, reviewer ≠ generator, attribution/scope/claim checks and public-publishing gates. Do not promise an Admin UI, dashboard, queue, email or automated article creation.

### D. Runtime configuration, deployment, validation, and release operations

Document **exact paths, variables and actual inspected scripts** rather than invented commands: external `app.data.dir` / `APP_DATA_DIR`, executable jar/Dockerfile packaging, media/assets, JVM/Node environment quirks required for tests, six validators and any unvalidated supplementary catalogs, JDK25 toolchain/Java17 release, frontend TypeScript/Vite, current tests, clean-start/reload instructions, and scoped manual expiry CLI. If deployment details cannot be inspected, mark the uncertainty; do not infer production configuration from a test fixture.

**Mandatory manifest truth:** `app/data/first-rate-news-membership.json` contains the three exact admissions. Valid present → four Housing First Rate News items including OR-003; **missing manifest → application starts but only OR-003**, no blanket Expert admission; **malformed manifest → fails before resident readiness**. Explain required catalog-volume copy and failure/alert checks. Do not claim missing manifest always fails startup. Preserve actual `resources.communities.json`, original Flyer media, actual source/provenance and safe source URL handling.

### E. Release truth, certification limits, V3/Later deferrals

Reconcile IM-13's **source-bound final verification**: 402 inputs unchanged, 631 backend tests, 162 frontend tests, 16 Python tests, six validators, Docker real distribution, 65 corrected API requests, four SPA/media paths and all J1–J7 within approved v2 scope. Retain the interrupted-run history and prior unsuccessful integrity supplement, pointing to the final successful closure evidence, without relabeling original failures as passes.

Clearly state **NOT TESTED**: comprehensive WCAG certification, physical devices, screen-reader execution, exhaustive remote source/provider/RSS correctness and remote AI-provider success. The local admitted LAW cache being empty is not evidence Delaware has no relevant laws. Distinguish release evidence from ongoing editorial freshness and real-world source accuracy.

Maintain a complete, grouped **V3/Later parking lot** (Admin UI/CRUD/dashboard, editorial queues, workflow assignments/email/automated content, generalized relationships/HOST and sophisticated profiles, second-community launch, richer personalization/recommendations, database/CI modernization, library-management evolution, etc.) with explicit authority for deferral. The **171 demo-origin Resources without provider IDs** are future library-design inputs, not automatically defective production providers; 262 distinct canonical IDs do **not** demonstrate the absence of semantic duplicates. Preserve data without backfilling, deduplicating or designing the future library in IM-14.

## 4. Acceptance evidence — IM14-T01–T20

| ID | Required executed/documentary proof |
|---|---|
| T01 | Inspect actual working tree and sources; protected starting hashes and controlling matrix / no unauthorized Git edits. |
| T02 | Every Completion Contract clause independently mapped to real code/test/document or explicit approved disposition. |
| T03 | Original architectural promises, pipeline stages, eight-point DoD, roadmap replacement and Decision 051/052 reconciled without fabricated automation. |
| T04 | All five resolved historical questions correctly reflected, with actual decisions and verification evidence. |
| T05 | Every Open Required Work Closure Backlog line has a terminal, explicitly authorized disposition or open blocker. |
| T06 | J1–J7 mapped to final IM-13 evidence and approved manual/editor boundary; no unsupported UI claim. |
| T07 | E1–E5 crosswalk complete and noncontradictory. |
| T08 | Every IM-01–IM-13 terminal acceptance linked to report/evidence; IM-13 interruption and final closure integrity chronology accurate. |
| T09 | P4/P5 logical concepts vs implemented boundaries audited, especially partial PROVIDER/HOST physical representation. |
| T10 | Original Admin Guide located/reconciled or explicitly unavailable with necessary supplement/disposition; no false claim it was inspected. |
| T11 | Expiry policy, timezone, read-only CLI, approaches/expired/manual stewardship faithfully documented. |
| T12 | Event occurrence, date-only FL-008, NP-003 missing occurrence, independence of Flyers and helpAccess accurately documented. |
| T13 | First Rate News record-level admissions, independent Article approval, 3-state manifest deployment correctly represented. |
| T14 | Community/Category/Topic/producer/Organization/Resource ownership, authored proof and validation instructions match actual implemented behavior. |
| T15 | Deployment and validator/reproducibility commands verified against actual repo/scripts, with any nonvalidated file gap disclosed. |
| T16 | Full final runtime/source/HTTP/privacy and accessibility **tested vs NOT TESTED** boundaries explicitly reconciled. |
| T17 | Demo 171 and potential semantic duplicates carried accurately as future library-design input, with no fabricated record-level duplicate findings. |
| T18 | Approved V3/Later deferral map comprehensive and no silent drop of required v2 obligations. |
| T19 | Evidence index, exact change/diff/document hashes and unresolved-issue register complete; any documentation edits limited, reviewable. |
| T20 | Closure decision explicitly conditioned on zero unresolved v2-required items, package acceptance and reviewer authorization; no self-certification. |

## 5. Deliverables

Produce under `docs/v2-completion-plan/` (or the matching existing approved documentation hierarchy):

1. `IM-14 Execution Report.md` with T01–T20 PASS/FAIL/NOT TESTED, starting/ending Git state, exact references and proposed disposition.
2. `evidence/im14/closure-ledger.*` — exhaustive requirement-level Completion Contract/historical-question/backlog/P4/P5/J/E/IM crosswalk with exact paths and statuses.
3. `evidence/im14/document-discrepancy-register.md` and corrected or supplemental **Admin/editor authoring + expiry** guidance, citing original where available.
4. Deployment/configuration/manifest/validator/runbook reconciliation using actual repo commands and paths; explicit operations checklists.
5. Updated architecture/source-of-truth documentation limited to verified reality, **V3/Later deferral register**, and final evidence index/terminal-state proposal.
6. Change-protection inventory (SHA-256, diff, annotated mirrors for any code touched), and final issue register separating v2 blockers, documentation corrections, unsupported claims, untested coverage and future stewardship.

Avoid needless duplicate documents: extend existing authoritative files where appropriate and preserve provenance. Do not overwrite the supplied controlling matrix or original/user-managed documents without explicit authorization. Do not rewrite production catalogs, restore old deleted matrix paths, stage/commit/push, or expand V3 infrastructure.

## 6. Exit and decision rules

**Verification/reconciliation first.** This is a documentation-and-closure package; no routine product rewrite or full release rerun. IM-13 already supplies the final fresh source-bound quality gates. Run narrow checks for actual edited documentation commands or broken links, not invented acceptance tests. A documentation contradiction that is genuinely required for v2 should be fixed minimally and referenced. A demonstrated required product defect is **not** automatically authorized code work: report the exact failing journey/invariant and request bounded change control.

If all required obligations have explicit evidence and no `Open — v2 required`, unresolved `Verify`, or other gap remains, recommend **IM-14 Satisfied / First Step v2 eligible for reviewer closure** with a complete signed-off evidence matrix, still **without self-declaring v2 complete**. If not, recommend **Blocked** or **Implementation Required** with specific owners and corrections. Final v2 closure and any public release/certification claim are separate reviewer decisions.
