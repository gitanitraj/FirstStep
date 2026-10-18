# v2 implemented architecture and original-promise reconciliation

October 18, 2026 — IM-14 proposed reconciliation, pending reviewer acceptance. The [Completion Contract](../v2-completion-plan/v2%20Completion%20Contract.txt) controls the release boundary; v2.14 matrix §14E accepts IM-01–13. This supplement resolves stale present-tense statements in the earlier architecture/README/authoring documents while preserving them as history. The [closure ledger](../v2-completion-plan/evidence/im14/closure-ledger.json) is the single requirement-level disposition record; the [execution report](../v2-completion-plan/IM-14%20Execution%20Report.md) controls this package's proposed outcome. It does not self-accept IM-14.

## Original thesis and three layers

The philosophy remains: bring trusted public information together for residents without changing underlying facts. Wilmington-first does not mean source-code-only Wilmington: each normalized content record has one owning Community, and eight partitions were exercised without requiring a second production launch. Ownership, physical location, service area and resident eligibility are different facts.

| Original layer | Actual implementation | Boundary |
|---|---|---|
| Shared Domain | `shared/model/CivicContent`, Community, ContentSource, fixed ContentType; Resource, NewsItem, Flyer, FAQ, ExpertAnswer, Article; taxonomy and Organization registry | Small shared model, not a generalized knowledge graph or complete persisted P5 ER schema |
| Business Capabilities | Category/Topic/Help services and deterministic selectors, controlled collections, explicit provider detail, Search, Updates, Notices, Article publication, lifecycle and maintenance CLI | Separate ownership/classification/eligibility/projection/selection; no Admin CRUD/workflow platform |
| Information Pipeline | JSON loaders, scheduled RSS/ROME ingestion, source adaptation/classification, DTO mapping and React delivery | Stages are responsibilities, not five complete orchestration services |

Class names in the tables are under `backend/src/main/java/org/firststep/backend/` unless otherwise qualified. Exact source/test paths are in the ledger.

## Five-stage spine: retained responsibilities, not invented automation

| Stage | Implemented responsibility | What is not claimed |
|---|---|---|
| Collect | External JSON repositories and RSS feed registry/`RssFeedService`; preserved original Flyer media | General website scraping, submissions moderation or provider API connectors |
| Generate metadata | Human-authored Flyer/News/Expert/FAQ/Article metadata; existing data-cleaning scripts and loader normalization | Automatic OCR/Exiftool/AI extraction or scheduled unstructured processing; historical Milestone2 remains deferred |
| Normalize | Canonical CivicContent/concrete models; legacy Resource adapter/source mappings; controlled taxonomy and source identity | A wholesale rewrite of171 demo records, database migration or universal schema for every catalog |
| Enrich | Conservative shared classification/tagging at ingestion, authored-field precedence, explicit associations, citation-source resolution and editorial content/review metadata | Generalized relationship graph, automatic translation, generalized summaries/interviews or semantic ranking |
| Deliver | Same-origin REST DTOs and React SPA at `/app-next/`; current Help/Category/Topic/Notices/Updates/Originals/provider surfaces; bounded Search and graceful AI fallback | Native mobile, SMS, newsletters/social publication, complete remote AI-provider success or every-kind Search union |

Sources: `00-philosophy.md` pipeline/layers, `02-information-flow.md` stages, Contract§6.3, P4§9. Architecture diagrams do not prove proposed automation exists. Source provenance is retained appropriately to each kind; the semantic Citation obligation survives without requiring a universal citation-management platform. Known-source identity does not prove every claim was verified.

## The eight recoverable task-DoD bullets

`00-philosophy.md` Definition of Done contains eight actual bullets. Historical-question resolution§5 says the original historical eight-point artifact was not recovered. These facts coexist: the repository text is recoverable now, but is not proof that it is the identical lost original. Each recoverable obligation is reconciled below under Contract§6.5; the approved Contract remains the **only release Definition of Done**. No reconstructed checklist is promoted to a second gate, and none of the eight creates new product work.

| Recoverable bullet, in source order | Disposition and evidence |
|---|---|
| Follow architecture specification | Approved interpretation: Contract/P4/P5+Architecture Resolutions, current logical/physical distinctions below, accepted package reports |
| Preserve traceability to original source | Implemented + verified: ContentSource, Article byline/review boundary, public citation-source DTOs; IM-12 identity/provenance and IM-13 HTTP evidence |
| Include tests | Implemented + verified: final source-bound631/162/16 suites and six validators |
| Update documentation if domain changes | Documented IM-14 reconciliation/handbook/runbook/ledger; historical decisions preserved |
| Remain deployable | Implemented + verified: fresh Docker, external manifest scenarios, packaged SPA/media asset hashes |
| Keep demo functional | Approved interpretation: preserved legacy root/demo assets and functional current SPA; no requirement to redesign the retired root UI; IM-13 SPA/runtime evidence |
| No Wilmington-specific assumptions unless intentionally scoped | Implemented + verified: explicit scope/default ownership, eight-partition regression, named Housing editorial admissions are deliberately scoped |
| Support future multi-community expansion where applicable | Approved interpretation: exactly-one ownership and scoped reusable services; no launch/onboarding/CRUD requirement |

## Roadmap and decisions: supersession, not unfinished parallel releases

| Historical commitment | Current terminal interpretation |
|---|---|
| Milestone1 Community/ContentSource/JSON migration | Implemented foundation; source/partition identity is tested, JSON remains accepted storage |
| Milestone2 Flyer metadata extraction | Explicit V3/Later deferral under Contract§9/P4§9; hand-authored Flyer metadata and original images already satisfy required journeys |
| Search MVP | Implemented narrow union; expansion to Article/FAQ/Expert/complete Law is separate future work, not inferred from Decision052 |
| First non-Wilmington communities | Loaded distinct owner partitions prove capability; quality-controlled second-community production launch is explicitly deferred by historical resolution§1 |
| Redesign steps1–3 taxonomy/Expert/scaffold | Implemented, followed by actual explicit Expert classification and current React SPA |
| Steps4–5 shell/hero/updates composition | Backend BFF capabilities survive; earlier shell/hero replaced by Decision021, not rebuilt as another acceptance target |
| Steps6–8 | Explicitly replaced by Decision021 A–I and later front-door/Category decisions |
| A/B/C/D/E | Frame/routing, degraded AI interaction, Laws delivery seam, resource discovery and original-media carousel implemented; no claim of live nonempty Law/remote AI success |
| D0 blanket enrichment of171 supplementary rows | All 171 already have stored Topics and the targeted schema invocation passes with warnings. A blanket future completeness/provider-backfill prerequisite is superseded by finite Contract/P4 and matrix§14D/E; no full substantive curation or fabricated provider/Topic data is claimed |
| F | Actual Category/Topic/All-Help navigation; Decision051 purposeful collections replace three-pillars/universal Explorer navigation; taxonomy groups remain presentation, not a fourth domain taxonomy |
| G | Existing explicit-provider page/navigation, not speculative organization grouping from names or a general graph |
| H | Current `/updates` and `/community-notices` controlled views; historical “Important Notices” label is not a second required page |
| I | Tested keyboard/responsive/Spanish/high-contrast behavior; complete accessibility certification and real devices are not claimed |
| G1c per-collection routes, UpdateItem convergence, CSS quarantine exception | Explicit later work; current in-place expansion and accepted generic collection behavior satisfy Contract; no invisible requirement to refactor payload/style debt |
| SliceK / Decision048/052 | Article review/public detail and navigation admission built; older adding-content “hold article, no home” is superseded. Generation/review remain distinct and human publication decisions remain manual |

Decision051's earlier narrow First Rate News/source-only rejection and deferred Organizations block are historical design steps. P4/P5, IM-08/09 record-level editorial admission and accepted matrix§14D/E now authorize the current four-item controlled collection and bounded explicit-provider Organizations preview. They do not authorize blanket Expert admission or generalized HOST/provider graph implementation. Older numerical totals and topic names in examples are not current fixed-catalog evidence.

## P4/P5 logical model versus physical representation

| Concept/invariant | Actual representation and evidence | Interpretation/limitation |
|---|---|---|
| CivicContent/type identity | Six stored concrete kinds; NewsItem represents NEWS/LAW, Article=NEWS, FAQ/Expert=EXPERT | Five ContentTypes only; no new Offering/Event/Audience/Featured entity |
| Community cardinality | `CivicContent.communityId`; explicit Resource owner wins, otherwise existing primary-city/default adapter | Exactly one normalized owner; geographic/eligibility separation. No many-to-many publication or global shared content identity required. Architecture Resolutions§2 closes GEO-01-C |
| Category/Topic | Controlled taxonomy, explicit Expert/FAQ membership, `AuthoredTopicEvidence` for relevant Topic/Law evidence | A Topic is scoped by parent Category, not a globally unique display string; shared labels are disambiguated by requested Category. Never synthesize parent membership from a Topic |
| Source vs provider/host | `ContentSource`; `Organization`; optional Resource JSON `organization_id`→Java `organizationId` | Narrow PROVIDER-like links implemented. P5 `ContentOrganizationAssociation` and typed HOST persistence **not implemented**; accepted bounded J3/J5 need no fabricated relation. No completed physical P5 claim |
| Offering service/contact facts | Resource retains offering contacts/location/eligibility; independent Organization registry | Do not elevate a service phone/address to provider-wide facts; no eligibility decision engine |
| Audience | Existing explicit population/eligibility fields | Discovery may reuse reliable facts; no Seniors taxonomy/destination, inferred audience or legal qualification |
| Lifecycle | `EditorialLifecycle`, shared Clock/config, current vs retained repository views; CLI diagnostics | Active/approaching/expired, missing/invalid date handling; manual attention, no queues |
| Occurrence | `notices/service/EventOccurrence`, date-only/offset ends/UNKNOWN | Separate Events boundary; retained contextual Flyer and editorial expiry independent |
| Controlled collections | `CategoryDestinationService`, `CategoryContentService`, `FirstRateNewsMembership`, generic frontend ContentCollection | Four What-to-Know collections, supporting Expert/FAQ, explicit admissions; not arbitrary ContentType enumeration |
| Topic selector | `FeaturedTopicSelector`: help count→role completeness→direct actions→authored context→catalog/approval newness→stable key+diagnostic | Full approved hierarchy; Help first/context second. No manually persisted featured flag |
| Item selector | `FeaturedItemSelector`: distinct role coverage, role relevance/meaningful time/newness/stable fallback+diagnostic; R6a–d | No duplicate item in one column; missing role yields honest FALLBACK. Retrieval timestamp is not authentic catalog newness |
| Public boundaries | ResidentContent/NewsSummary/FlyerSummary/ArticleDetail/DecisionSummary; approved visibility/404 parity and safe URLs | Review, generation, lifecycle/selection/ingestion fields stay internal; Community is scope, not authorization |
| Persistence and stewardship | JSON startup loading, in-memory RSS, manual validation/reload and protected history | No DB migration, universal audit/change-history/backup system, automated editorial workflow or source truth certification |

No model redesign is required by ordinary ties, optional metadata or a sparse authentic visual role. Fixed Housing outcomes:36 eligible Wilmington Help records; Transitional Housing, Emergency Shelter, Homeownership and Rental Assistance featured; durable winners HA-002/016/011/008 with two distinct truthful fallbacks each. All 8 production Flyers retain original identity/media and0 Help approval. These are accepted catalog evidence, not promises of permanent live counts as dates or authorized records change.

## Historical questions and remaining scope

All five questions are explicitly mapped in the ledger: Community capability required/launch deferred; provenance required/universal Citation abstraction not independently required; Seniors illustrative; no established standalone numerical performance gate; historical DoD superseded by Contract. No historical “Verify” is silently relabeled as future work.

The [Admin handbook](../authoring/v2-admin-editor-handbook.md) documents supported human authoring/review/expiry operations and original-guide unavailability. The [runbook](../authoring/v2-release-operations.md) documents actual build/deployment, three-state manifest truth, validator limits and isolated recovery. The [V3 register](../v2-completion-plan/V3-Later%20Deferral%20Register.md) lists explicit future work with authority and owner.

Final IM-13 evidence is source-bound and accepted on submitted evidence, not an independent reviewer rerun. IM-14 statically checked that all 402 inputs still match and all 22 command-log hashes/exits remain valid. Original interrupted/failed integrity evidence remains intact. Comprehensive WCAG certification, physical-device/screen-reader execution, exhaustive remote provider/source/feed correctness and successful remote AI-provider response remain NOT TESTED. Empty admitted local Law cache is not evidence of no relevant Delaware law. Release evidence never replaces ongoing human editorial verification.
