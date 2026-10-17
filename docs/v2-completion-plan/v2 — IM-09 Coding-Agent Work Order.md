# First Step v2 — IM-09 Coding-Agent Work Order
## Controlled Civic-Information Collections and Projection Contracts

**Status:** AUTHORIZED FOR EXECUTION — evidence-gated; not pre-accepted  
**Package:** IM-09  
**Execution class:** Implementation / Verification Required — verify first, then bounded correction where demonstrated  
**Depends on:** IM-08 — Satisfied on reviewed execution report; IM-01–IM-07 also Satisfied  
**Issued:** October 17, 2026  
**Governing authority:** Approved v2 Completion Contract; authoritative IM-01–IM-14 matrix with §14 IM-08 closure; approved P4/P5, including identity/classification/projection separation; Architecture Resolutions; Decision 051 Pass 1/Pass 2A and Decision 052; approved editorial public-DTO/Article review safeguards; IM-01–IM-08 accepted scope and evidence.

## 1. Mission and scope

Verify and, only when evidence demonstrates a gap, complete the **four controlled, Community- and Category-scoped What to Know collections**: **Policy Info, Law, Flyers, First Rate News**. Ensure Category Destination, context rail, applicable Topic/content endpoints and public response contracts remain accurate, traceable, deterministic and resident-safe. Preserve the **inclusive approved First Rate News** intention for qualifying First Step editorial material and qualifying Expert Sessions without indiscriminately treating every FAQ/Expert Answer as a session.

**Verification before redevelopment.** IM-08 already renders the four collection choices, approved OR-003 headline, bounded rail and source navigation. IM-09 tests their actual semantics. It must not rebuild the Category Destination, selectors, taxonomy, lifecycle, event model, or Admin platform.

### Four source-of-truth distinctions

1. **Identity:** CivicContent, underlying Article/News/FAQ/Expert/Flyer records and provenance stay intact; no duplicate item created for a view.
2. **Classification:** fixed five ContentTypes `RESOURCE`, `NEWS`, `FLYER`, `LAW`, `EXPERT`; Topic and Category must be explicitly valid where required. FAQ and ExpertAnswer are concrete kinds under EXPERT, not additional ContentTypes.
3. **Projection:** Policy Info, Law, Flyers, First Rate News are *controlled resident collections*, not new ContentTypes or inferred taxonomy nodes. A record can lawfully be in multiple projections if each independently qualifies.
4. **Editorial/publication:** original-source provenance, editorial approval, status and expiry are independent gates. Approved Article is not equivalent to all NEWS; EXPERT does not automatically mean First Rate News.

## 2. Mandatory protected baseline

Before modifying code:

1. Read repository `claude.md`, actual current authoritative matrix and this order. Confirm full branch/HEAD, working-tree status and protected files. IM-08 was **uncommitted/unpushed when reported**; verify current checkout, preserve existing edits and hash catalog/dependencies and pending work. No reset/stash/overwrite/staging/commit/push unless separately authorized.
2. Inspect live implementations of `CategoryDestinationService`, CategoryPage/CategoryContentService, Category Controller and DTO, collection switcher/rail, Category/Topic/All Help routes, `ContentItem`, source/mapping, and frontend tests. Identify exact resident API fields/allowlists and independent contextual collection endpoints.
3. Inspect News and RSS stores/services and sector filtering, `ContentSource` producer registry, FlyerService, ArticleService and public ArticleDetail approval/404 guards, FAQ/Expert Answer repositories/services and the explicit Category classifier/validator from IM-03. Inspect Decision 051 inclusive First Rate News evidence and current data/test expectations.
4. Run baseline backend Maven verify with repository's JDK25 toolchain, frontend tests/typecheck/build, Python unit tests and six validators where available. Preserve failures as evidence; don't silence or inherit checks as though newly executed.
5. Create an inspect-first **collection eligibility truth table** from actual code and P5/Decision 051 authority: row per concrete content class/source (curated NEWS, Article, RSS LAW, Flyer, FAQ, Expert Answer), with eligible collection(s), approval/status/expiry, Category/Community/Topic, provenance and source-link requirements. Separate approved behavior from *observed* implementation, including uncertainty.

## 3. Deliverable A — Policy Info controlled projection

- Verify the current Category Policy Info surface represents **external civic policy information, notices, updates and developments**, not an unqualified alias for every NEWS record.
- Confirm First Step-authored approved Originals stay in their editorial destination, and RSS LAW is not duplicated into Policy Info merely because NewsItem models it. Any deliberate, approved nonexclusive membership must be evidenced by controlled semantics, not type alone.
- Preserve existing ContentSource producer-level sector rules on Latest Updates and Community Notices; do not collapse government/community sectors into collection labels. Missing producer IDs must not be invented.
- Preserve current resident expiry/status and explicit Community/Category gates. Unknown kinds/metadata cannot generate new Policy Info subcollections.
- Keep title, Why It Matters (when present), date labels and source attribution faithful; no unsupported interpretation or false “verified” label.

## 4. Deliverable B — Law projection

- Preserve fixed `LAW` ContentType presentation semantics even where the underlying record is a NewsItem from Rome/RSS; do not add a Law entity or sixth ContentType.
- Maintain the difference between relevance-admitted RSS, signed-bill cache, stored/current inventories and remote feed completeness; **do not assume an unwarmed cache means no Delaware law exists**.
- Category/Topic law placement and right-rail eligibility may use only approved **authored, controlled** classification/provenance evidence, not headline keyword inference or unrelated recent RSS records. Do not manufacture Law classification, jurisdictional significance or resident obligations.
- Verify original feed/source URL and publication facts; no law effective date or event deadline should be inferred from a publication timestamp.
- If no currently eligible authored law exists for Housing, show an honest empty state while retaining legitimate broader law browsing where its approved contract differs.

## 5. Deliverable C — Flyers projection

- Current eligible `FLYER` records must remain available in **Flyers** irrespective of Get Help admission. Specifically FL-002 remains a contextual Housing Flyer with original image.
- Preserve same canonical ID, producer/source, Community/Category, image/media and navigation. Display source URL if valid; otherwise existing encoded media. No duplicate CivicContent or RESOURCE conversion.
- Preserve IM-04 optional exact `helpAccess=ACTIONABLE_HELP_ACCESS` as a separate positive Get Help gate. **None of eight production Flyers has this authorization**; do not add any. An actionable synthetic Flyer may be in both projections, but the reverse is not automatic.
- Editorial expiry is distinct from occurrence date. Do not mass-remove past-event Flyers or implement concluded-event logic: IM-10 owns that rule.

## 6. Deliverable D — First Rate News inclusive membership: mandatory decision-sensitive audit

The approved P5/Decision 051 baseline defines **First Rate News as a controlled inclusive projection of eligible First Step Originals/editorial content and qualifying Expert Sessions**. It is **not** a new type, and not every FAQ/Expert Answer is automatically a First Step editorial publication. IM-08 currently displays approved OR-003 and makes Expert/FAQ accessible elsewhere; it reports no existing linked Expert/video/session record in the rail. This may be a sparse catalog **or** a membership mismatch. IM-09 must determine which.

Before changing code, produce a real-record decision table covering at least OR-003, FAQ-002, EA-001 and EA-005 (and any other current plausible Expert Session records). Include:

- concrete record, ContentType, Community, explicitly approved Category/Topic, source/provenance and editorial/approval facts;
- whether the record constitutes an approved **Original**, an approved/qualifying **Expert Session**, a general FAQ/Expert Answer, or is **undetermined** based on existing controlled metadata;
- the specific **approved authority or controlled field** that permits membership, if any;
- current visibility in First Rate News, supporting FAQ/Expert surface and rail, and the difference, if applicable.

**Do not decide that all Expert/FAQ records qualify just to achieve the historical inclusive count of four.** Conversely, do not substitute a newly invented strict Originals-only definition for the approved inclusive contract. Historical strict=2 / inclusive=4 Housing counts are reference snapshots, not present-day required catalog counts. If available evidence unequivocally identifies eligible Expert Sessions, apply the existing publication/scope gates and include them; if not, document a narrow, record-level editorial/semantic decision request and do not invent session or approval metadata. A legitimate unresolved decision may prevent IM-09 from reaching Satisfied; report that precisely rather than quietly downgrading the contract.

Preserve the publication guard `draft → in-review → approved | flagged` for First Step Articles; no public “published” workflow state, no flagged/unapproved leak, and existing private/internal Article review detail must remain out of public DTOs. Article detail 404 parity for nonpublic records must be retained.

## 7. Deliverable E — controlled API/DTO, source and scope protection

- Public Category/Topic/collection aggregate returns only its documented allowlist of resident-facing fields. No IM-06/07 diagnostic evidence, reviewer IDs, ReviewFlags/FlagDisposition, raw `helpAccess`, internal statuses, unpublished Article body, private workflow metadata or hidden catalog fields.
- Preserve Community binding: explicit requested Community limits all collections/rail; unspecified resident request may use configured `app.default-community-id` where IM-08 established it. Never silently mix Communities. Topic is a controlled child of Category; Category-only records may be shown where permitted without fabricating Topics.
- Deduplicate within each collection by canonical identity without destroying permitted cross-collection membership. Deterministic order and counts should match eligible items, not raw stored totals.
- Original producer/source identity is not a PROVIDER/HOST relationship. No source-name-based organization attribution, no trust badge, no false provenance or implied legal interpretation.
- Preserve navigation for approved Article detail, external source, flyer image and valid Law links. Reject unsafe URL schemes and test missing destinations honestly.
- Honor present IM-08 collection switcher accessibility, focus, loading/error/empty behavior, mobile at 375px and high-contrast/Spanish labels as applicable; bounded correction only for an actual IM-09 regression.

## 8. Acceptance cases IM09-T01–T20

| ID | Required reproducible evidence |
|---|---|
| T01 | Enumerate actual four controlled collections; unknown metadata/kind cannot create fifth collection or extra taxonomy. |
| T02 | Policy Info is not a broad all-NEWS alias; test curated policy vs approved Original vs LAW examples and explicit membership. |
| T03 | Producer-level government/community sector rules and unknown-source handling remain intact. |
| T04 | Policy Info uses current status/expiry and Community/Category boundaries; real expired NP fixtures absent from resident current collection yet stored. |
| T05 | LAW retains valid ContentType presentation over correct underlying record, without new domain entity. |
| T06 | Authored Category/Topic proof prevents inferred RSS law from populating scoped collection/rail; lawful broad approved RSS surface unaffected. |
| T07 | Missing/unwarmed feed described as coverage limitation; source publication URL not fabricated, no publication→effective-date inference. |
| T08 | Flyers remain available independently of helpAccess, including real FL-002 media and all current eligible records. |
| T09 | Synthetic exact-token actionable Flyer may participate in Help and Flyers with same ID/type/source; null/invalid signal stays Flyers-only. |
| T10 | Flyer editorial expiry/status and Community scope; event date does not become IM-09 occurrence exclusion. |
| T11 | Real approved Originals (including OR-003 where current) appear in correct First Rate News scope; drafts/flagged remain absent from public collections/detail. |
| T12 | Real Expert/FAQ record-level inclusive-membership audit includes FAQ-002, EA-001 and EA-005; explicit positive/negative/undetermined basis documented. |
| T13 | Any positive qualifying Expert Session participates under accepted inclusive rules without indiscriminately including all EXPERT; negative examples excluded; unresolved semantic decision visibly reported. |
| T14 | First Rate News identity/approval/Category/Community preserved; no duplicated catalog identity or invented session type. |
| T15 | Controlled Category vs Topic only: category-only Expert/FAQ cannot gain inferred Topic; no unapproved classification. |
| T16 | Public Category aggregate and detail responses omit all editorial/internal/selector/`helpAccess` fields; approval/public 404 parity. |
| T17 | Stable collection IDs/counts/order/dedup under shuffled fixtures; cross-view membership doesn't create duplicate storage. |
| T18 | URL/Article/source/Flyer media navigation safe and intact, with correct Community query; rail empty state honest. |
| T19 | Full backend verify/package, frontend test/type/build, Python tests and all six validators; focused synthetic and real-catalog HTTP checks; browser accessibility smoke if UI affected. |
| T20 | Protected predecessor/catalog/taxonomy/manifest/lockfile checks, exact scoped diff, genuine provenance and no IM-10–14/V3 scope expansion. |

Tests should assert *specific* positive and negative conditions and use the real catalog alongside synthetic counterexamples. A single passing collection snapshot cannot substitute for publication, scope and privacy regressions.

## 9. Execution sequence

1. **Preserve:** inspect real branch and pending IM-08 files; hash protected existing source/catalog/dependencies, note permissions and no-commit boundary.
2. **Inspect and baseline:** produce collection eligibility truth table and current real-record membership audit *before edits*; run baseline gates and record exact failures.
3. **Determine demonstrated gaps:** distinguish already satisfied functionality, IM-09 omissions, current catalog sparsity, and unresolved editorial-authority questions.
4. **Implement bounded fixes only:** shared controlled collection predicates/DTO mapping, tests, minimum authoring/validation guidance if necessary. Do not silently choose new semantic membership criteria.
5. **Test:** targeted Java/Python/frontend tests, fixed-clock status/expiry, real Housing and another Category/Community, missing data and RSS cache limitation, Article privacy guards, safe URLs.
6. **Verify broader gates:** same repository toolchains, `mvn verify`, frontend tests/type/build, Python unittest and six validators; packaged API/browser checks as supported, with explicit limits.
7. **Evidence:** save before/after record table, named tests, exact commands and results, diff and hash checks, public DTO/URL samples and actual collection/rail screenshots if UI changed.
8. **Stop:** IM-10, IM-11, IM-12, IM-13, IM-14, event occurrence, new admin systems, second-Community launch and V3 enhancements remain untouched. No staging/commit/push or unapproved catalog changes.

## 10. Mandatory execution report

Return **IM-09 Execution Report** with the following evidence contract: starting branch/HEAD/worktree and protected hashes; governing authority and inspected source files; exact baseline commands/results; per-collection implemented-vs-approved truth table; real provenance/Community/category/approval audit; specific inclusive First Rate News decision table and any requested record-level editorial decision; demonstrated gaps; bounded production/data/test/doc edits (or none); acceptance evidence T01–T20 with named cases; full backend/frontend/Python/validator/build/runtime outcomes; public DTO/schema privacy results; exact unchanged-file proof; final `Satisfied`, `Still open — precise reason`, or `Escalation required` and later-package ownership. Do not report unrun checks as completed.

## 11. Approval and stop conditions

IM-09 may be marked **Satisfied** only when all four collection semantics, accepted inclusive membership, explicit scope/publication gates, safe public boundaries and supporting tests are evidenced. An unresolved **authoritative** Expert Session classification/approval decision must be reported instead of invented; preserve an open disposition if necessary. The agent can continue independent tests and corrections while awaiting such a decision, but must not silently reinterpret P5 or Decision 051.

**Instruction to coding agent:** Inspect first, protect all accepted IM-01–08 work, verify existing collection contracts, make only necessary IM-09 fixes, capture exact positive/negative evidence, and stop at the package boundary. Do not claim overall v2 completion.
