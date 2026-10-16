# First Step v2 — IM-06 Coding-Agent Work Order
## Deterministic Featured-Topic Selector

**Status:** AUTHORIZED FOR IMPLEMENTATION — evidence-gated acceptance  
**Execution package:** IM-06  
**Execution class:** Implementation Required  
**Dependencies:** IM-01, IM-03, IM-05 — Satisfied on reviewed execution-report evidence  
**Issued:** October 16, 2026  
**Authorities:** v2 Completion Contract; authoritative reconciled IM-01–IM-14 matrix and IM-05 closure addendum; approved 2B-P4 and 2B-P5 (especially §§18–19, §§20–28 and invariants); **Approved Architecture Resolutions — Featured-Topic P5 Alignment**; Decision 051 Pass 2A; IM-01–IM-05 reports. Where abbreviated matrix language says “classified-content counts,” the later approved resolution controls: **help first, context second**, with the **entire seven-stage hierarchy**.

---

## 1. Mission and package boundary

Build a **reusable deterministic service selecting up to four controlled Topics** for a given Community and Category. Selection favors the strength of **currently eligible resident-actionable inventory**, not the total volume of News/Law/FAQ or raw stored records. Selection returns auditable reasons/diagnostics and is reliable on repeat, in shuffled input order, and for categories other than Housing.

**IM-06 implements the selector and its contract.** IM-07 later implements TIMELY/DURABLE/VISUAL **three-item selection** within each featured Topic. IM-08 later integrates featured columns into the redesigned Category Destination UI. Do **not** declare either complete in this package. A minimal internal/integration invocation of the selector is appropriate; a full featured-column page is not required.

## 2. Proven predecessor baseline — verify before coding

- **IM-01:** shared current inventory and `EditorialLifecycle`; ACTIVE and APPROACHING_EXPIRY remain otherwise eligible, EXPIRED does not. Editorial nearing expiry is stewardship attention, **not** selection priority.
- **IM-02:** diagnostic/reporting, not public ranking or an editorial workflow.
- **IM-03:** explicit Expert/FAQ Category classification; 12 approved Category-only Expert/FAQ records have **no Topic**; do not infer Topic from text.
- **IM-04:** exact controlled `helpAccess=ACTIONABLE_HELP_ACCESS` admits eligible Flyer to Get Help. **0/8 real Flyers** have that value. Don't activate them or count their visual presence as direct help absent approval.
- **IM-05:** `CategoryContentService.helpFor(categoryKey, topic, communityId)`, controlled Topic resolution, `/api/category/{key}/help` and help-only counts, Community-scope behavior. Agent reported **36 current Wilmington Housing help RESOURCE IDs**, nine controlled Topics (Transitional Housing 11; Emergency Shelter 9; Rental Assistance 4; Homeownership 4; Sober Living 3; Senior Housing 2; Youth Housing 2; Public Housing 1; Eviction Prevention 0). Data is a point-in-time audit, **not a hard-coded count or selection order**. IM-05 reported 470 backend tests, 133 frontend tests, 14 Python tests, six passing validators; changes were **uncommitted/unpushed** at report time.

Inspect actual source, data and git working tree; historical branch/commit state is not authoritative over the current checkout. Preserve any IM-05 uncommitted modifications and all accepted predecessor work. No reset, stash, staging, commit, push, dependency upgrade, catalog mutation, or new Flyer activation unless separately authorized.

## 3. Mandatory inspect-first evidence

1. Read `claude.md`, current matrix/architecture resolution/P5, and prior IM reports; record branch, full HEAD, `git status`, diff and protected-file inventory.
2. Inspect `CategoryContentService.helpFor`, Category/Topic service/controller/DTO shapes, `TaxonomyService`, `NavigationService`, current-vs-raw repository interfaces, Community ownership, status/approval/source filters, RSS relevance-admitted LAW and current projection endpoints, available temporal metadata, and catalog validators/tests.
3. Identify available **explicit** metadata for direct actions/deadlines, TIMELY/DURABLE/VISUAL *qualification*, law/policy Topic relevance, catalog/approval newness, and stable Topic keys. **Do not invent data from narrative or infer relationships from headlines.** Record missing signals and how deterministic rules work without them.
4. Run baseline Maven tests, frontend tests, validators and build/type gates; capture actual results. Audit the **then-current** real Housing Category/Topic scoped candidate inventory and one additional Category. Capture raw vs eligible vs actionable vs contextual counts by **distinct canonical CivicContent ID**.
5. Produce inspect-first gap map and a proposed *smallest* reuse-based design **before production edits**. Escalate only a genuinely missing approved semantic; ordinary missing metadata/ties are covered by fallback/diagnostics.

## 4. Required seven-stage authoritative Topic ordering

Apply in **this exact lexicographic order** (do not collapse into an undocumented weighted score):

1. **Resident-actionable inventory strength/count.** Count current, Community/Category/Topic-valid, truly actionable help opportunities once per identity. Reuse `helpFor` or equivalent authoritative eligibility; do not count Flyers merely because they are Flyers, or count the same item twice across projections. More eligible direct-help inventory wins this stage.
2. **Featured-role completeness.** Assess availability of qualifying **TIMELY, DURABLE, VISUAL** *candidates* for the Topic using approved explicit metadata, preserving the IM-07 role meanings. This stage measures achievable distinct role coverage, not selected winner IDs or final three-card ordering. Respect multi-role distinctness: one record cannot fill two simultaneous positions. Do not invent a Flyer role where none is valid.
3. **Direct resident-action opportunities.** Among otherwise tied Topics, distinguish meaningful actual access events, deadlines, enrollment/service availability and other explicit direct steps; don't infer an access opportunity from a contextual headline or provider identity.
4. **Explicitly relevant law/policy context.** Use only eligible, Topic-classified and meaningfully related legal/policy items as a **late** discriminator; never promote arbitrary RSS laws based on recency or text keyword alone; relevance-admitted cache is not a claim of complete remote inventory.
5. **Catalog/approval newness.** Apply only after stronger criteria tie, using existing authentic provenance timestamps/approval facts. Do not substitute event dates, editorial-expiry dates, inferred catalog insertion chronology, or presentation history. Missing/indistinguishable metadata must fall through deterministically.
6. **Stable immutable Topic key.** Use an invariant tie-break independent of input iteration/order, locale and machine clock when all substantive metadata is tied.
7. **Admin diagnostic.** Where the ordering reaches technical Topic-key resolution rather than an editorially meaningful difference, emit a bounded inspectable diagnostic but **do not interrupt resident delivery**. No dashboard, queue, assignment, email or manual override UI.

**Help first, context second** is controlling. A Topic containing numerous laws/updates but no actionable help cannot outrank a Topic with materially stronger qualifying direct help merely on total content volume. The old matrix's shorthand "initial ranking based on eligible classified-content counts" is explicitly superseded by the approved full P5 alignment.

## 5. Candidate and eligibility contract

- Establish **Community first, Category second**, then validate that each candidate Topic is a controlled child of the requested Category. Do not derive Topics from arbitrary record fields, tags, titles or search terms.
- Eligible means the correct current-publication/lifecycle/status/approval gates as relevant, plus explicit Topic and parent Category membership; Community ownership must match. A raw stored expired record is not a candidate.
- Count **distinct CivicContent IDs**, including multi-projection dual membership only once within the same Topic. A content record may legitimately contribute to distinct Topics if explicitly classified to each.
- Contextual NEWS/LAW and other non-Get-Help records may supply **late contextual or role-qualification evidence** only when their projection/Topic rules truly allow it. They do **not** inflate step-one help counts. Expert/FAQ Category-only items do not gain inferred Topics.
- Do not redefine Community as physical service area or resident eligibility. Explicit cross-Community cases must remain isolated; don't extend null/blank-Community aggregation into a fabricated user-selection policy. Record any pre-existing unscoped behavior separately for IM-12.
- Prefer established models and methods; minimal immutable selector result/diagnostic DTO is allowed where needed. No persisted `featured=true`, scoring column, new ContentType/Topic/Offering entity or recommendation engine.
- If semantic metadata cannot prove a specific signal (e.g. direct deadline or catalog newness), it **does not qualify** for that discriminator; record the coverage limitation, continue through remaining stages and raise a diagnostic when the final technical fallback is needed. Do not infer from prose.

## 6. Role-completeness boundary with IM-07

IM-06 may implement the **smallest shared qualification helper** needed to determine whether a Topic has *possible* TIMELY/DURABLE/VISUAL coverage with distinct eligible items. It must not implement IM-07's selected three items, same-role item collision ordering, missing-role item fill, or detailed card composition.

- **TIMELY:** real resident-relevant near-term availability/action, contextual policy change later than direct-help timing, editorial approaching-expiry last (stewardship, not automatically more important).
- **DURABLE:** stable, useful eligible content lacking a near-term action boundary as established by existing explicit facts; don't invent evergreen status solely from undated content without checking approved meaning.
- **VISUAL:** eligible relevant Flyer where the approved role permits. Preserve exact `helpAccess` gate for *actionable Flyer* strength; assess any contextual visual eligibility under approved controlled Topic/projection rules and do not equate visual with actionable help.
- A multi-role item counts for at most one slot in maximum distinct-role coverage; test a Flyer that could be TIMELY and VISUAL and another independently TIMELY candidate.

If role qualification genuinely depends on an unavailable domain fact, stop that *specific* disputed qualification and report a bounded semantic question; do not invent a generic `roleScore` or silently downgrade the approved ordering. Provide a safe deterministic behavior for all uncontested candidates.

## 7. Deliverables

**A — Selector contract:** callable, read-only, Community/Category-scoped selector yielding **up to four ordered controlled Topics**, with meaningful selection evidence/diagnostic independent of presentation layout. Return fewer than four if only fewer qualify; do not manufacture Topics or pad with contextual-only candidates merely to reach four unless the approved eligibility rules explicitly allow that behavior.

**B — Metadata/eligibility adapters:** small, reusable integration of existing current help and explicitly Topic-classified civic content, with distinct-identity counts and meaningful tie-break evidence. Avoid multiple contradictory expiry/Category/provider rules.

**C — Diagnostic:** deterministic, internal/read-only report or log of stable-key fallback, referencing Category/Community/Topic and indistinguishable signals. Never make resident response fail because a diagnostic was emitted; no workflow engine.

**D — Audit and documentation:** real Housing snapshot, one other Category, synthetic second Community, tests including shuffled inputs, explicit limitation record on missing metadata; synchronized annotated source/test references following `claude.md`; complete IM-06 execution report.

## 8. Required acceptance scenarios

| ID | Scenario | Required evidence |
|---|---|---|
| IM06-T01 | Select up to four | Up to four existing Topic keys, no fabricated Category/Topic or persisted featured flag |
| IM06-T02 | Help-first priority | Context-heavy law/news Topic cannot displace Topic with stronger direct-help inventory |
| IM06-T03 | Current-only counts | Expired, withdrawn, inactive, unapproved not counted; approaching expiry remains otherwise eligible |
| IM06-T04 | Duplicate projection identity | Same actionable Flyer in Flyers+Get Help counted once for actionable inventory |
| IM06-T05 | Explicit categories/topics | Invalid parent, unknown Topic and prose/keyword-only Expert Topic never enter candidate inventory |
| IM06-T06 | Complete seven-stage hierarchy | Dedicated multi-topic fixture demonstrates precedence at each discriminator in order |
| IM06-T07 | Role completeness | TIMELY/DURABLE/VISUAL distinct coverage breaks tied help-counts; multi-role item does not count twice |
| IM06-T08 | Direct action stage | Authentic action deadline/access opportunity breaks otherwise equal Topics before law/policy context |
| IM06-T09 | Law/policy late discriminator | Only explicitly Topic-relevant context breaks remaining tie; RSS freshness alone doesn't classify it |
| IM06-T10 | Newness is late | Genuine catalog/approval newness considered only after stages 1–4, absent values do not invent chronology |
| IM06-T11 | Stable-key tie | Reversed/randomized input iteration yields identical deterministic order independent of map/set order |
| IM06-T12 | Admin diagnostic | Technical fallback creates diagnostic without blocking selection or leaking editorial fields publicly |
| IM06-T13 | Housing real catalog | Snapshot of actual current Topics, counts, selected order and rationale; explicitly reconcile 0-help Eviction Prevention if still true |
| IM06-T14 | Additional Category | Food or Health controlled Category fixture, no Housing-specific hard-code |
| IM06-T15 | Two Communities | Scoped inventories yield independent Topic selections; no accidental cross-Community counting |
| IM06-T16 | Missing/weak metadata | Safe fallback, no prose invention, no arbitrary law/provider/recency priority |
| IM06-T17 | Nonmutation | No source catalog changes; repeatable fixed-data/fixed-clock output; no new dependency/ContentType/entity |
| IM06-T18 | Predecessor and release boundaries | IM-01–05 regression pass; no IM-07 three-item finalizer, IM-08 UI, IM-10 occurrence cutoff, IM-12 general identity audit |

Use focused unit/service tests, stored-catalog integration tests, and where applicable HTTP contract tests. If no resident endpoint changes, do not fabricate page-level featured UI/browser proof; IM-08 and IM-13 own that evidence.

## 9. Quality gates and execution sequence

1. **Protect baseline:** record git state and hashes for uncommitted IM-05 files and protected catalog/manifests. Read P5/resolution before applying older matrix shorthand.
2. **Inspect:** APIs, taxonomy, lifecycle/projections, temporal facts, selected source/provider semantics, tests, existing selector if any.
3. **Run baseline:** JDK25 toolchain Maven `verify`, frontend `npm test`, `npx tsc --noEmit`, `npm run build`, Python unit tests and all six catalog validators; log actual counts. IM-05 previously reported 470 backend/133 frontend/14 Python, which are only historical comparison figures.
4. **Design bounded selector:** document candidate eligibility, lexicographic keys, missing-data handling, role feasibility and diagnostic interface **before production code changes**.
5. **Implement minimally:** no catalog authoring, no new taxonomy, no new permanent flags; reuse current read paths, not raw stored results.
6. **Prove acceptance:** named positive/negative tests for all IM06-T01–T18; real Housing and second Category; synthetic second Community and collisions. Capture fixed-clock evidence and ordered IDs/reasons.
7. **Run complete gates:** Maven `verify`, frontend tests/type/build, Python tests/validators, repository checks. Provide reproducible commands, results and changed-file inventory. Run a packaged/API integration check **if a new public API is introduced**; otherwise focus on callable selector contract.
8. **Report and stop:** return IM-06 execution report with the evidence contract below; do not begin IM-07/08 or call v2 complete. Do not stage/commit/push without separate authorization.

## 10. Evidence contract and package disposition

Report exact: package/starting disposition/ending disposition; branch/full HEAD/starting and ending dirty state; source/fixtures inspected; gap; chosen selector contract and full seven-stage ordering; eligibility source and distinct-identity semantics; missing metadata policy; files changed; actual executable tests/validators/commands and pass counts; real Housing count/result + limitations; additional Category and second Community; role-feasibility proof vs IM-07 boundary; diagnostic evidence; protected predecessor hash comparison; journey/epic mapping **J2, J7 / E2, E5**; downstream handoff; no V3 pull-forward.

**Mark IM-06 `Satisfied` only if** all stages of the approved ordering are faithfully implemented and exercised, direct-help precedence is evidenced, deterministic diagnostic fallback works, source data remains protected, generalization is proven, and full quality gates pass. If approved factual metadata cannot support a mandatory stage, explicitly document its coverage and request a narrow decision where needed rather than silently replace the hierarchy. A passing build alone is insufficient.

**Final coding-agent instruction:** Implement IM-06 only, under the approved P5 Featured-Topic Alignment resolution. Verify existing source seams and protect IM-05. **Help first, context second.** Return full, reproducible evidence. No independent redesign of P5, no IM-07 item selector or IM-08 page composition, no V3 capability, and no v2 closure declaration.
