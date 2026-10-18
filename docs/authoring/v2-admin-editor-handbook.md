# v2 Admin/editor handbook — reconciled IM-14 supplement

October 18, 2026. Current operating guidance within the approved Completion Contract, PC-01–PC-04, Decision 048/051/052 and v2.14 matrix §14E. This supplements [adding-content](adding-content.md), the [IM-02 expiry policy](editorial-expiry-supplement.md) and [runtime-input note](im12-runtime-inputs.md). Where their old examples conflict with this supplement, use the verified instructions here. Historical files remain evidence, not silently rewritten instructions.

## Original Admin Guide: source unavailable

The original *First Step Admin Guide — Expiry Eligibility and Editorial Stewardship* was not found in the working tree or locally reachable Git history searched for Admin/admin/Guide. IM-01/02 reported an inaccessible supplied Library source; no retrievable Library URI or original text is available in this checkout. This handbook is the necessary supplemental reconciliation explicitly authorized by IM-14 §3C/T10, **not** a claim that the missing guide was inspected or reconciled line by line. Approved PC-01–04 and the implemented IM-02 policy supply the expiry rules. If the original is later recovered, compare it through Contract change control rather than automatically expanding v2.

## Supported manual authoring workflow

1. Work in an authorized separate candidate copy of the complete catalog directory, with a recorded baseline/version. Preserve source records and original media. There is no v2 Admin CRUD UI, save endpoint, hot reload or automated publishing workflow.
2. Identify the concrete record kind and exact source file. Keep the stable canonical ID for a correction to the same offering. Use a new unused ID only for a distinct item; do not copy the historical FL-008 example as a new record. Cross-projection appearance is not a duplicate record.
3. Verify material facts, producer attribution and proposed classification against the actual source. Record genuine verification dates only; collection/retrieval/approval time is not a substitute. Do not turn uncertain provider, geography, eligibility, deadline or source-sector facts into inferred data.
4. Author the correct stored shape. Existing files use different vocabularies: Resource `organization_id` maps to Java `organizationId`; News uses `headline`, `source_id`, `source_url`, `expires`, `active`, `resource_tags`; other kinds generally carry nested `contentSource`. Inspect the existing model/loader and a same-kind record rather than renaming fields globally.
5. Run the applicable validation and loader/publication checks in the [runbook](v2-release-operations.md#validation-coverage). Inspect exact diagnostics, including supplementary-catalog coverage limits. A validator success does not prove source accuracy or editorial approval.
6. Review the change, preserve before/after metadata and rationale, publish only through the authorized catalog/deployment procedure, then reload/restart and verify the real scoped response. Do not renew expiry or set Article approval merely to make a test pass. No source-control commit/push is implicitly authorized by this handbook.

Fresh evidence: IM-13 `closure/editor-workflow.json` exercises create/read/update/reload, explicit owner/provider precedence, optional provider removal and exact negative provider/Topic diagnostics on temporary full catalog copies, leaving production catalogs unchanged.

## Kinds, types and maintained files

| Concrete kind | Stored input | Canonical ContentType | Important boundary |
|---|---|---|---|
| Resource | `app/data/resources.json`, `resources.communities.json` | RESOURCE | Offering-level contact/access facts; optional explicit provider ID |
| NewsItem | `app/data/news.json`; admitted RSS in memory | NEWS or LAW | Curated news vs selectively admitted Law; not every fetched bill enters Category discovery |
| Flyer | `app/data/flyers.json` | FLYER | Original image/media; occurrence and Help admission independent |
| FAQ | `app/data/faq.json` | EXPERT | Separate identity, explicit Category; no automatic Article review requirement |
| ExpertAnswer | `app/data/expert-answers.json` | EXPERT | Explicit Category; source/expert attribution preserved |
| Article | `app/data/articles.json` | NEWS | Separate Article approval and safe public reading DTO |

EVENT, FAQ, ARTICLE, PROGRAM, SERVICE and Get Help are not additional ContentTypes. A Flyer is not an Organization/provider. One content item may appear in multiple controlled collections while retaining its original identity.

## Ownership, placement and unknown relationships

Every normalized item has one owning Community. Write explicit `communityId` for newly authorized authoring based on the intended catalog partition. It takes precedence over the Resource loader's legacy primary-city/default normalization. All 229 original Resource rows currently omit stored `communityId`; do not rewrite them en masse. Owning Community does not establish service area, physical location or individual eligibility. Report provider-published requirements honestly; First Step does not decide whether a resident qualifies.

`category_tags` uses controlled Category labels from `app/data/taxonomy.json`. `subcategory` is an optional controlled Topic; its parent must be among the explicitly authored Categories. Shared Topic labels such as Eviction Prevention must be validated in the requested parent context. Expert/FAQ entries require nonempty explicit Category metadata even when raw historical/inactive records are retained. No title/body/producer/Topic inference manufactures missing Category membership. Descriptive tags are not navigation or proof of an authored Topic. LAW category/topic context also requires authored classification proof; RSS headline relevance alone is not that proof.

`ContentSource` is the producer/provenance role; `Organization` is the provider registry identity. For a Resource, `organization_id` must resolve if supplied; omission remains omission. Never derive it from equal names, parent display text, a source ID or office address. Resource phones/websites are offering-level facts and must not automatically become organization-wide facts. Safely scoped provider links use existing explicit associations and preserve Community; absence is displayed honestly. There is no typed persisted HOST relation or general relationship graph in v2. Do not create a provider record to stand in for a host, producer or unverified mention.

The 171 supplementary Resources are demo-origin material, retained unchanged and potentially useful for future library design. They are not all defective production providers because they lack IDs. The 262 unique canonical content IDs prove ID uniqueness, **not** semantic nonduplication. No duplicate pairs have been established by that count. Suspected overlap needs later record-level human analysis, not automated deletion or provider backfill.

The directory adapter retains `dscyf-directory` provenance with unknown governed producer sector. Do not assign a sector from its name. Governed notice feeds need verified registry identities/sectors. Missing source information must remain explicit; it must not become a global publication prerequisite that breaks unrelated valid content.

## Editorial expiry and human stewardship

The [original IM-02 supplement](editorial-expiry-supplement.md#interpret-editorial-lifecycle) supplies the detailed calendar and CLI policy; IM-10 now separately supplies occurrence behavior.

| Lifecycle | Current-inventory effect | Editorial action |
|---|---|---|
| ACTIVE | Eligible if other scope/status/classification/approval rules permit | Maintain normal verification; ACTIVE is not proof of source freshness |
| APPROACHING_EXPIRY | Still current and eligible; attention required | Verify accuracy/usefulness, contact source/provider as needed, qualify/update/replace or allow expiry |
| EXPIRED | Excluded from applicable current availability; retained in stored catalog | Review deliberately; do not delete history to clear the report |

Default approaching window: 30 **Community-local calendar days**, including the expiry date. An item expires at the next local midnight after its inclusive `expirationDate`, not after 30×24 hours. Wilmington zone is `America/New_York`; unlisted owners use `app.editorial-expiry.default-zone`, not the host machine's implicit zone. Overrides are documented in the runbook.

Missing/blank expiry does not automatically expire any kind or create an attention finding. Malformed nonblank dates are metadata problems, not a fourth lifecycle state or a fabricated ACTIVE value. The CLI isolates/identifies such records; resident assessment retains the implemented error behavior. Missing publication, verification or occurrence dates must not be converted into expiry. Stored inactive/withdrawn state and Article approval are independent gates. Retained historical detail where already supported does not imply current actionability.

Run the [read-only packaged report](v2-release-operations.md#expiry-diagnostics) at a recorded clock/scope. Exit 0 means a complete valid report (attention findings are normal); 1 means isolated invalid expiry metadata; 2 means invocation/config/catalog-load failure. The operator records a decision per finding: verify, correct, renew on evidence, replace deliberately, qualify, withdraw using existing controls, or retain history. No email, queue, ownership assignment, article generation or task routing is performed. Approaching expiry does not automatically improve featured selection.

## Event occurrence is a different clock

`EventOccurrence` uses structured `event_date`, offset-bearing `startsAt` and `endsAt` on News/Flyer records. A valid explicit end remains current **through** that instant; after it, the event is concluded. Date-only events remain current through their owning Community's local date. Start-only events use that local date; no duration is invented. Missing/malformed/contradictory structured facts yield UNKNOWN with internal diagnostics, not prose-derived times. Valid structured offsets/DST boundaries matter.

FL-008 (United Neighbors 2026) is date-only `2026-10-17`; it has no structured startsAt/endsAt. Narrative 1–4 p.m. was not backfilled. It becomes concluded for current Events at `2026-10-18T04:00:00Z` (midnight October 18 in New York). Synthetic exact-end fixtures are separate evidence. NP-003 has UNKNOWN occurrence although its editorial expiry approaches. On October 18 FL-008 may still be a contextual Flyer; event conclusion does not editorially expire it. Events and Flyers are independent projections. Do not treat the taxonomy Category “Community Events” as the notice kind `event`.

## Flyers and Get Help

A Flyer may appear in Flyers and Get Help from the same canonical identity only with the controlled `helpAccess=ACTIONABLE_HELP_ACCESS` signal and other applicable scope/currentness rules. The token needs an actual approved access-to-help purpose; neither the title, Category, event tag nor image authorizes it. **0/8 production Flyers currently have approved Help admission.** Synthetic overlap tests prove the infrastructure, not editorial approval of FL-002 or FL-008. Original media lives at `backend/src/main/resources/static/images/seasonal/`; store its exact filename and let the server encode its safe media URL.

## First Rate News and Article publishing

`app/data/first-rate-news-membership.json` independently admits ExpertAnswer EA-001, ExpertAnswer EA-005 and FAQ FAQ-002 for `wilmington-de`/`housing`. Retain exact kind/ID/Community/Category keys and editorial decision provenance. An interview/session, FAQ derivation, `verified` Boolean or `first-step` producer alone is not admission. Other valid Experts/FAQs remain supporting collections. Admissions never override classification, scope, lifecycle or review.

Approved Article OR-003 enters separately through Article review/category gates; the present four-item Housing projection is OR-003, FAQ-002, EA-001, EA-005. The manifest [three-state operational check](v2-release-operations.md#manifest-deployment) is mandatory when this four-item experience is intended. Do not broaden admission just to make a total partition cleanly.

The old adding-content Step 2c “no article surface yet” instruction is superseded: approved Articles now have `/app-next/originals/:id?communityId=...` and `/api/originals/:id?communityId=...`. Draft/in-review/flagged or nonpublic Articles cannot bypass the gate and receive not-found parity. Article approval is not a publication schedule; `publishDate` is descriptive under Decision 048. Public ArticleDetail intentionally excludes review state, flags, generator identity and internal lifecycle data.

Decision 048 requires generation and evaluation to be separate, including when AI assists both. The reviewer must evaluate the actual text/evidence, not inherit the generation rationale as proof. Distinguish verified fact, attributed claim, faithful AI summary and supported/qualified editorial inference. Check that wording is no more certain, positive, consequential or complete than the evidence. Check claim-to-source support, attribution, jurisdiction/dateline/scope, neutrality and unsupported guarantees; reachable URL is not substantive verification.

Record reviewer/date, exact flagged passages, issue, reason and recommended action. Disposition open flags as resolved/withdrawn/overridden with actor/date/reason, preserving immutable history; withdrawal means reviewer error, override means an objection knowingly remains. Re-review material revisions. An authorized human makes the publish decision. Structured validation can establish a well-formed review, not that the judgment was correct. No automatic article-writing/review/approval service is promised. FAQ/Expert material is not retroactively subjected to Article workflow merely because it is First Step-created.

## Corrections, recovery and editorial evidence

Keep source originals, internal review evidence, change rationale and authorized before/after records separately from public DTOs. Do not include secrets or private review details in resident captures. Suspected source contradictions need a recorded human disposition; do not silently infer the most convenient fact. A corrected source identity does not authorize rewriting unrelated records. Do not call a re-fetch a verification of every claim.

Use the [runbook recovery procedure](v2-release-operations.md#recovery-and-retention) for an isolated restore, validation and scoped publication check. Git supplies practical version history, not a complete backup or actor-level audit system. Permanent disposal is distinct from expiry/withdrawal/retention and needs an explicit authorized retention decision. V3 owns structured record-level change history and Admin reporting. Ongoing contact freshness, editorial accuracy, source reachability and real deployment security remain operator responsibilities; release tests do not certify them forever.
