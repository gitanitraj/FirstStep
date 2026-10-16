# First Step v2 — IM-04 Coding-Agent Work Order

## Flyer / Get Help Dual-Projection Rule

**Status:** AUTHORIZED FOR VERIFICATION FIRST; BOUNDED CORRECTION ONLY IF EVIDENCED  
**Package:** IM-04  
**Execution class:** Verification First — Bounded Correction If Needed  
**Direct dependency:** IM-02 — Satisfied  
**Predecessor context:** IM-01 and IM-03 accepted on their execution reports; inspect the actual checkout, do not infer commit state  
**Issued:** October 16, 2026  
**Authority:** First Step v2 Completion Contract; authoritative IM-01–IM-14 execution matrix; approved P4/P5 logical classification model (particularly Get Help, controlled projections and Flyer overlap); Decision 051; IM-01 through IM-03 execution reports; approved Architecture Resolutions.  
**Journey/Epic:** J1, J4, J7; E2, E3, E5 (and identity boundary relevant to E1).

## 1. Mission and controlling decision

**Verify first** whether First Step already permits **one eligible, actionable FLYER CivicContent record** to appear in both the **Flyers** projection and **Get Help**, without cloning the underlying record, recasting it as a RESOURCE, or inferring help semantics from a flyer’s existence.

If verification demonstrates a gap, implement only the smallest change needed to enforce the approved P5 rule and prove it with regression evidence.

The approved semantic contract is:

- **Flyers projection:** an otherwise eligible, correctly scoped FLYER qualifies because of its FLYER identity/classification.
- **Get Help projection:** requires an otherwise eligible, properly scoped **resident-actionable help or access opportunity**. FLYER alone is **not** enough.
- A **non-RESOURCE** record may enter Get Help only on the basis of an **explicit controlled semantic** identifying actionable help/access, not a topic label, image, headline, description, keywords, organization name, or the FLYER ContentType alone.
- One record may legitimately appear in multiple controlled projections, but its canonical CivicContent identity, original ContentType and provenance stay unchanged.
- A resident-actionable access event may involve a **HOST** organization; hosting must not be misrepresented as permanent **PROVIDER** responsibility. Do not implement the full relationship-model audit reserved for IM-12.
- Editorial expiry is governed by IM-01. Event **occurrence** eligibility is separately owned by IM-10; do not invent occurrence logic in IM-04.

**Do not interpret “Flyer appears in both projections” as an unconditional demand to put every flyer into Get Help.**

## 2. Protect and establish the real starting state

The IM-03 report records `copyofdev` at HEAD `0945c61b0e51074f4912b56c3bd130b6702379f3` after a fast-forward, with **23 preexisting modified/untracked IM-02 files preserved**, and IM-03 changes uncommitted at the end of that run. These are historical observations, not guarantees about the checkout the agent now receives.

Before edits:

1. Capture branch, full HEAD, `git status --short`, tracked/untracked changes and applicable catalog diffs. Identify which IM-01, IM-02 and IM-03 files are committed or still in the working tree.
2. Preserve every predecessor/user modification. For dirty predecessor files, record hashes or equivalent before-and-after proof; do not reset, stash, overwrite, autoformat broadly, stage, commit or push.
3. Confirm IM-01 lifecycle, IM-02 diagnostics, and IM-03 explicit Expert/FAQ Category guards and data corrections are accessible. If a necessary predecessor is absent, report the discrepancy rather than rebuilding it.
4. Inspect the governing current matrix and code before treating any class, service name or field as authoritative.
5. Run focused baseline tests and relevant catalog validators; record actual commands and results before implementation.

## 3. Mandatory repository and catalog inspection

Trace the *actual* data flow, distinguishing stored identity from resident projection:

- `CivicContent`, fixed `ContentType`, Flyer model/JSON, content metadata, `communityId`, `contentSource`, category/topic fields, existing explicit controlled help/access semantics (if any), and organization/provider/host associations.
- Raw and resident-facing Flyer repositories/services; `EditorialLifecycle` filtering and retained historical/detail behavior.
- Existing Category content/page and Topic services/DTOs, Get Help collection or equivalent current projection, Flyers carousel/collection, Find Help and other help-consuming services as relevant, and front-end/API contracts.
- Existing Category/Topic integrity rules and producer/source versus provider/host distinctions.
- Current real flyer records (including the known FL-002 and FL-008 where present), their Category/Topic membership, status/expiry, links, available structured semantics and relevant existing projection membership.
- Tests, validators, documentation and prior decision/evidence references establishing how resident-actionable help is explicitly represented today.

Produce a real-record **candidate audit**: record ID, concrete class/ContentType, Community, Category/Topic, source, canonical identity, expiry/status, structured actionable-help signal (present/absent), current Flyers/Get Help membership, and justified target membership. Classify observations as **proven eligible**, **proven ineligible**, or **requires editorial/semantic decision**; do not manufacture a result from suggestive prose.

The known examples FL-002 and FL-008 are inspection targets, **not preapproved Get Help placements**. If an actual record’s purpose is uncertain, request a concise editorial decision with its ID, factual content summary, candidate controlled semantic and reason; leave the data unaltered until approved. Use synthetic fixtures to prove semantics when no real record has approved actionable-help classification. Do not create a fake production flyer just to satisfy a test.

## 4. Deliverable A — Verify existing dual-projection behavior

Demonstrate, using the existing API/service contract if present, that a correctly classified and current actionable Flyer:

1. appears in Flyers;
2. appears in Get Help when the approved explicit controlled semantic is present;
3. uses **the same immutable CivicContent ID** and FLYER canonical type in both views;
4. retains its Community, producer provenance, original media/detail/source link, and existing status/expiry gates; and
5. is not persisted a second time, transformed to RESOURCE, or counted twice in a single deduplicated collection merely because it has two projection memberships.

Demonstrate the converse: a non-actionable FLYER appears in the appropriate Flyers collection but **does not** enter Get Help solely because it has a flyer image, relevant Category/Topic, organization/source, or promotional wording.

**Do not claim acceptance merely because a Flyer already appears in an existing Topic list.** The question is Get Help’s meaning and membership, not generic topic discoverability.

Where the fully redesigned Get Help/All-[Category]-Help surface is not yet built, establish the smallest reusable backend projection/contract and test it in isolation; record that IM-05/IM-08 own actual destination composition. Do not falsely claim a missing UI was end-to-end tested.

## 5. Deliverable B — Bounded correction, only on proven failure

If the present code already enforces the rule, add focused evidence and mark **no production change necessary**. If not:

- Reuse existing *controlled* actionable-help/access metadata where it satisfies P5; keep projection logic centralized enough to avoid contradictory paths.
- If no existing approved structured semantic can represent non-RESOURCE actionable help, **do not unilaterally invent a schema field, metadata vocabulary or inference rule**. Report the precise representational gap and a minimal proposal for explicit authorization. Ordinary missing per-record classification is an editorial/data issue, not a reason to reopen P5.
- Apply IM-01 current-inventory eligibility. ACTIVE and APPROACHING_EXPIRY remain eligible if all other criteria pass; EXPIRED or independently excluded status does not enter applicable current projections. Raw catalog and historical records remain retained.
- Preserve Category/Community scoping and Category/Topic integrity. Do not derive classification from title/body or source identity.
- Distinguish `ContentSource` (producer) from `Organization` (provider/host). Do not infer a permanent provider relationship from an event host.
- Ensure the returned view preserves FLYER identity and source/media navigation. Avoid publication of editorial-only internals.
- Make only narrowly justified, explicitly approved catalog changes, documenting before/after identity and source fields.

No new ContentType, RESOURCE duplicate, Offering entity, generalized graph, permanent featured flag, ranking system, Admin UI or database migration is permitted.

## 6. Deliverable C — Scope and downstream handoff

IM-04 does **not** build the complete All-[Category]-Help browse page or the redesigned Topic sub-destination (IM-05); feature selectors (IM-06/07); Category Destination composition/context rail (IM-08); full collection/provenance audit (IM-09/12); or event `endsAt`, date-only occurrence cutoff, and concluded-event visibility rules (IM-10).

If an actionable access-event Flyer has an occurrence date, preserve its existing temporal facts and distinguish **editorial expiry** from **event occurrence**. Do not invent arbitrary event durations or exclude events under new occurrence rules within IM-04. Explicitly log IM-10 integration responsibility.

The deliverable for IM-05 is a tested and documented **help-eligibility/projection seam** with clear contract and scope, regardless of whether it required new production code.

## 7. Required acceptance matrix

| ID | Scenario | Required outcome |
|---|---|---|
| IM04-T01 | Current Flyer eligible for Flyers | Returned as FLYER in Flyers with same stored ID |
| IM04-T02 | Explicitly actionable, eligible Flyer | Included in Get Help **and** Flyers without second stored CivicContent |
| IM04-T03 | Non-actionable Flyer | Flyers yes; Get Help no |
| IM04-T04 | Flyer with help-like title/body only | No Get Help admission from free-text inference |
| IM04-T05 | Flyer with relevant Category/Topic but no help semantic | Topic/category relevance does not alone grant Get Help membership |
| IM04-T06 | Same item in both projections | Stable ID, type, Community, producer and media/detail link; no duplicated catalog record |
| IM04-T07 | Expired actionable Flyer | Retained in raw catalog; excluded from applicable current Flyers and Get Help |
| IM04-T08 | APPROACHING_EXPIRY actionable Flyer | Retains dual-projection eligibility if otherwise valid; editorial-attention state is not ranking priority |
| IM04-T09 | Inactive/withdrawn Flyer | Excluded from applicable current projections independent of expiration date |
| IM04-T10 | Category and Topic scoping | No cross-Category/Topic leakage or manufactured classification |
| IM04-T11 | Community partition | No cross-Community leakage or Wilmington-only hard-coding; stable singular Community ownership |
| IM04-T12 | HOST versus PROVIDER access-event case | Hosting does not imply permanent provision; preserve actual explicit relationship evidence |
| IM04-T13 | Duplicate view memberships | One identity in storage; any aggregation/count semantics explicitly documented and deduplicated where intended |
| IM04-T14 | Real catalog audit | All currently stored Flyers inspected; supported real example(s) tested, uncertainties logged rather than guessed |
| IM04-T15 | No existing eligible real flyer | Synthetic controlled positive/negative fixtures prove both directions; no fabricated live data correction |
| IM04-T16 | IM-01/02/03 regression | Lifecycle, stewardship diagnostics and Expert/FAQ classification unchanged and tested |
| IM04-T17 | API/read-model contract if implemented | Identity/type/media navigation preserved; no editorial/private field leakage |
| IM04-T18 | Scope protection | No new ContentType/domain redesign, event-occurrence implementation, unrelated UI or V3 expansion |

Tests may use other method names, but the final report must map each applicable case to **actual named evidence**. When an endpoint is not built, say which layer was tested and name the downstream owner; do not present isolated tests as a completed resident page.

## 8. Quality gates and reproducibility

Follow the repository toolchain and established test practice. IM-03 reported **454 passing backend tests**, **126 frontend tests**, **12 Python classification unit tests** and **six passing catalog validators** at the end of its execution. These are comparison baselines, not assumed results or guaranteed current repository counts.

Required verification:

1. Baseline focused tests and actual catalog audit before changes.
2. New or existing unit/integration cases for both positive and negative projection membership, identity, lifecycle, scoping, and provenance.
3. Full backend `mvn verify` using configured JDK 25 toolchain; report actual total, failures, errors and skips.
4. Relevant Python catalog validation and any IM-03 classification tests, particularly if data touched.
5. Frontend `npm test`, `npx tsc --noEmit`, and `npm run build` where available; mark any environment-specific limitation accurately.
6. Representative packaged runtime/read-model checks if a relevant API or integration exists; record exact paths and observations. Do not require a new public endpoint solely for evidence.
7. Verify `app/data` and dependency/lockfile diffs are strictly bounded; prove preexisting uncommitted changes survived.

Never weaken assertions, skip verification or silently accept a failing gate merely to mark IM-04 complete.

## 9. Mandatory execution sequence

**Inspect → protect baseline → audit real records → test present behavior → identify exact gap → obtain editorial/semantic authorization if needed → smallest correction → targeted regression → broader quality gates → evidence report → stop.**

Where inspection proves the contract already holds, produce a **verification-only** result and do not manufacture implementation work.

Do not modify another IM package, start IM-05, or stage/commit/push unless separately authorized. Do not modify catalogs merely to create a favorable example.

## 10. Required execution report / disposition

Return a package-specific `IM-04 Execution Report` containing:

- Starting/ending branch and HEAD, full dirty-state inventory, proof of predecessor change preservation.
- Authority reviewed, files/classes/data inspected and actual existing Get Help / Flyers read paths.
- Real flyer candidate audit (all stored records, IDs, structured actionable semantics, evidence and uncertain cases).
- Original observed behavior, reproduction commands, exact demonstrated gap or conclusion **no change needed**.
- Record-level editorial decisions requested/granted, if any; otherwise explicitly say none was needed.
- Exact code/data/test/doc modifications and rationale, or zero-production-change verification result.
- Named acceptance mapping IM04-T01–T18, with tests, fixture IDs, assertions, and limitations.
- Reproducible commands and actual outcomes, including backend/frontend/validators and packaged read-model checks when applicable.
- Identity, projection count, Category, Community, producer and HOST/PROVIDER guard evidence.
- Scope/non-goals and downstream IM-05/08/10/12 integration notes.
- Final disposition: **Satisfied**, **Verification First — Bounded Correction If Needed (open)**, or documented escalation/blocker, justified by evidence.

**IM-04 is `Satisfied` only if the dual-projection rule is demonstrably implemented or already verified, the negative non-help cases remain excluded, all identity/eligibility safeguards survive, applicable quality gates pass, and evidence is complete.** A change that compiles is not acceptance; nor is the presence of Flyers in a Topic result.

## 11. Final coding-agent instruction

Execute **only IM-04**, verification-first, against the actual current checkout. Preserve all predecessor work. Prove or minimally establish that an explicitly resident-actionable, eligible Flyer can participate in both Flyers and Get Help with one unchanged FLYER CivicContent identity, while ordinary non-help Flyers do not enter Get Help by implication. Reuse IM-01 eligibility and approved P5 controlled semantics. If structured actionable/help semantics do not exist, surface the narrowly documented decision point instead of inventing a metadata field or relying on prose inference. Produce the audit, named regression tests and complete execution report, and stop at IM-04's boundary.
