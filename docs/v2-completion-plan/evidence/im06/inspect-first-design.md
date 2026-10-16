# IM-06 inspect-first gap map and proposed minimal design

Written before production edits. Starting branch copyofdev, clean after accepted IM-04/05 commits 8e94be4 and6f7a955, both pushed. The attached updated matrix (including closure addendum) and IM-06 order control. Read approved P5 §§18–28, Featured-Topic P5 Alignment in v2 Architectural Resolutions.rtf, claude.md and predecessor reports/source.

Existing: helpFor(key,topic,communityId) is the authoritative distinct actionable inventory; lifecycle-filtered content services, review-approved Articles, relevance-admitted current RSS, controlled taxonomy names/slugs. No featured-topic selector exists. Generic navigation counts and broad Topic inventory are not seven-stage selection.

Metadata findings:

- Resource is a service listing with explicit urgency (standard/emergency); no evergreen or availability window field. Its createdDate comes from source retrieved date, not authentic catalog insertion.
- Flyer has eventDate, media, explicit stored createdDate, and optional exact approved helpAccess. All eight live records omit helpAccess. Event dates are not editorial expiry and must not become current-inventory cutoffs here.
- News has type (including policy-update/deadline), publishDate, optional explicit Topic; curated loader copies publication date into createdDate. Do not misrepresent that as catalog insertion. RSS createdDate also copies publication date; relevance-admitted cache is not complete remote inventory. No live curated News has an explicitly stored Topic.
- Article has actual createdDate and review approval/reviewedDate; only getPublishable may supply candidates.
- Expert/FAQ have no live Topic and cannot gain one from prose. Validate explicit Category/Topic relationship via existing taxonomy guard.
- Stable Topic identity is the existing canonical taxonomy/route slug, scoped by Category. No independent persisted Topic UUID exists. Renaming a controlled Topic would require deliberate taxonomy/routing change outside this package.

Proposed additions only:

1. Small current-inventory adapter: compose existing services, retain raw canonical model facts in memory, scope Community then Category then explicit controlled Topic, deduplicate IDs, join helpFor IDs. No raw repository reads for resident candidate delivery.
2. Read-only selector with immutable result/evidence/diagnostic records. Rank eligible nonzero-help Topics by lexicographic actionable count, maximum distinct TIMELY/DURABLE/VISUAL coverage, direct-action count, eligible explicit recent law/policy count, latest authentic catalog/approval date, stable canonical Topic slug. Emit internal diagnostics for all equal substantive tuples resolved by slug, including cutoff ties. Do not rank contextual-only Topics just to fill four.
3. Role feasibility uses a three-role matching calculation with distinct IDs, not selected final item IDs. IM-07 item collision/fill/card selection remains absent.
4. Strict nonblank explicit Community is required for selector calls, unlike historical null/blank unscoped browse inventory; invalid Category produces no fabricated candidates. Stable sorting uses Locale.ROOT slug behavior and no collection iteration order.
5. Genuine known createdDate for Flyer/Article and explicit approved Article reviewedDate may support newness. Exclude loader-manufactured Resource/News createdDate, retrieved, updatedDate, event and expiry timestamps from newness.

Pending narrow qualification question: approval requested for standard-urgency current Resource as DURABLE; exact actionable Flyer with eventDate today through30 Community calendar days as direct TIMELY/action; explicit LAW/policy-update with publication in past30 days as contextual TIMELY/late context. No new fields, inference or event-occurrence exclusion proposed. Do not implement disputed positive qualifications without the response. Missing facts produce zero evidence for that signal and bounded coverage diagnostics, without silently deleting a stage. Approaching editorial expiry alone never promotes a Topic.

No public endpoint or UI is needed; callable service + integration audit is sufficient. No new dependencies, permanent feature flags, catalog edits, IM-07/08/10 expansion or live Flyer activation.

## Qualification resolution (before selector production edits)

User approved Option1 with boundaries, recorded in IM-06 Qualification Decision.md. Apply exact urgency=standard Resource proxy; actionable Flyer event date [today,today+30] inclusive; explicit LAW/policy-update publication [today-30,today] inclusive. Shared Community timezone configuration and injected Clock define today. No new eligibility/occurrence gates. The prior question is resolved; proceed with all seven stages. FAQ/Expert authentic createdDate may support late newness only when already explicitly valid Topic candidates. Article reviewedDate is eligible approval provenance only for approved Articles. Raw loader-generated Resource/News creation dates remain excluded.

## Explicit-classification provenance refinement

Inspection of CivicContentClassifier and RssFeedService showed that populated runtime Category/Topic fields can be keyword-inferred. The selector must not treat these as authored facts. A read-only AuthoredTopicEvidence adapter extracts only stored classification metadata (plus exact existing Resource source mappings), then joins by concrete kind/ID to already-current service candidates. It does not read raw catalog records as eligibility results or change any loader/classifier/browse behavior. Stage-one helpFor IDs are intersected with this proof. Current RSS has no authored Topic proof channel, so keyword-classified RSS cannot qualify and is diagnosed as a coverage limitation; a complete remote-feed claim remains explicitly absent. Curated News with a genuinely stored Category/Topic can qualify. No new model field, editorial inference or catalog edit is introduced.
