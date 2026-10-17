# IM-12 identity and resident-boundary map

Frozen audit instant: 2026-10-17T16:00:00Z. `catalog-before.json` was captured before production edits. `catalog-after.json` is deeply equal, including every raw normalized row, lifecycle assessment, source, provider trace and per-Community/Category collection ID/order/count. Raw means repository storage view after existing normalization, not an assertion that every field was explicitly stored. `inventory.json.rawStoredFiles` separately enumerates missing stored ownership/source fields.

## Stored identity → normalization → current → projection

| Concrete kind / stored input | Normalization and current boundary | Resident projection / classification proof |
|---|---|---|
| Resource / resources.json + resources.communities.json | JsonResourceRepository resolves directory source and classifier; ResourceService shared lifecycle; explicit Community wins, otherwise existing city/default adapter | CategoryContentService help, Topic, provider detail, Search; Resource category adapter retained; authored Topic requires stored proof. New legacy ResidentContent exposes provider ID without inferring one. |
| NewsItem / news.json | JsonNewsRepository maps published/expires/active/resource_tags; NewsService current; by-class distinction preserved | NewsSummary; Policy Info source-sector + relevance; Events occurrence separate; descriptive event tags do not author a Topic. |
| NewsItem LAW / retained RSS cache | RssFeedService relevancy/current filter plus authoredCategoryTags/authoredSubcategory snapshots | Broad news/RSS can contain admitted LAW; category Law additionally needs authored Category/Topic evidence. Actual retained cache empty; synthetic law tests cover the distinction. |
| Flyer / flyers.json | JsonFlyerRepository, FlyerService status/lifecycle; imageUrlFor single media encoder | FlyerSummary, Category/Topic/Flyers, Community Notices; EventOccurrence only Events; exact helpAccess required for Help. All eight original rows have no signal. |
| Article NEWS / articles.json | JsonArticleRepository retains all; ArticleService approval AND public status; current adds lifecycle | Approved ArticleDetail and controlled category/Topic/First Rate News; original internal review/generatedBy never public. Public revision updatedDate retained. |
| FAQ EXPERT / faq.json | JsonFaqRepository validates controlled Category/optional Topic; FaqService lifecycle | Separate FAQ identity; ResidentContent question/answer/derivation pointer; explicit independent manifest decision for FAQ-002; derivation alone is no admission. |
| ExpertAnswer EXPERT / expert-answers.json | JsonExpertAnswerRepository same controlled classification, separate concrete model; ExpertAnswerService lifecycle | ResidentContent expert credentials/contact/session date and Q/A; source/verified/session date/affiliation cannot confer admission; only EA-001/005 manifest decisions. |

The category and search service algorithms remain internal. PublicSearchResult and PublicCategorySummary adapt their completed results; they do not rerank, deduplicate anew, or change counts. All five ContentTypes are unchanged; six concrete classes share them. Article and FAQ/Expert IDs never change into each other.

## Field decisions (including nested facts)

| Domain field family | Resident disposition / reason |
|---|---|
| id, contentType, title, summary, communityId | Public canonical identity/ownership where already in response shape; ArticleDetail retains its approved narrower contract. |
| category_tags, subcategory, tags | Public controlled classification/descriptive facts, no conversion from descriptive tags to navigation. LAW uses authored snapshots in NewsSummary. |
| publishDate; ArticleDetail.updatedDate | Public publication/revision facts. Not event dates or editorial approval timestamps. |
| verified, status, expirationDate, createdDate; non-Article updatedDate | Private verification/lifecycle/audit. Public collection eligibility still enforced internally. |
| editorialReview, generatedBy, flags/override/reviewer/diagnostics; authored-proof snapshots, helpAccess, selector tieStage/candidates | Private editorial/ingestion/selection data. No raw model exposed by newly adapted endpoints. |
| contentSource.id/name/type/url | Public attribution, independently resolved producer. HTTP(S) URL with host for new Source DTO; absent/unsafe URL remains null. |
| contentSource.retrieved | Internal ingestion audit; removed from legacy nested content and AI citation response. |
| Resource category/organization/organization_id/parent_organization, description/population/eligibility and age/gender, county/access_mode/cost/urgency/notes | Public offering facts preserving legacy consumers. Parent is a display string, not hierarchy. Notes contain actual hours/eligibility and are not blanket-redacted. |
| Locations label/address/city/state/zip/confidential | Public location facts; new Resource Place hides address/zip when confidential=true. State=DE remains public; source city never proves service availability. FlyerSummary's established Location shape remains unchanged; actual Flyers have no confidential-address counterexample. |
| Phones number/label; websites url/label; Expert contact email | Public offering/expert contacts, no organization phone invention. New website facts allow absolute HTTP(S); phone numbers remain data, existing UI forms tel navigation. |
| FAQ question/answer/source_expert_answer_id; Expert question/answer/name/credentials/organization/contact/session_date | Public distinct authorship/supporting content. FAQ pointer is provenance, never inherited collection membership or fabricated Originals route. |

Concrete endpoint field paths for every observed response are enumerated in `endpoint-field-audit.json`, with before/after leaks. `scoped-endpoints-after.json` contains another 1,160 field/path/ID assertions over all eight owners, every Category/Topic/All Help and all 64 provider destinations per owner. Empty responses are coverage, not proof of remote data completeness.

## All externally reachable resident API mappings

| Endpoint(s), verb GET unless stated | Boundary and scope |
|---|---|
| /api/health | Plain OK, no domain data. |
| /api/seasonal-images | Public image filenames only; configured filesystem path not returned. |
| /api/resources; /api/resources/{id} | New ResidentContent allowlist. Omitted scope retains aggregate/global-ID legacy behavior; explicit scope intersects current list/status-safe historical detail. |
| /api/faqs; /api/faqs/{id} | New independent FAQ allowlist, same optional scope semantics. |
| /api/expert-answers; /api/expert-answers/{id} | New independent Expert allowlist, same optional scope semantics. |
| /api/flyers; /api/flyers/{id} | Existing FlyerSummary retained; new explicit-scope intersection, omitted scope preserved. |
| /api/search?q=... | Internal SearchResult → new PublicSearchResult nested allowlist. Existing configured default and explicit ownership filter unchanged. |
| /api/categories | Internal CategorySummary → PublicCategorySummary; nested ranking items and latestPolicyUpdate cannot bypass public DTOs. Existing omitted-scope aggregate convention retained. |
| /api/category/{key}; /api/category/{key}/help; /api/category/{key}/{topic} | Existing CategoryPage/TopicPage/controlled destination DTOs, configured default Community preserved; explicit requested Community and canonical URLs retained. |
| /api/organization/{slug} | Existing OrganizationPage/ContentItem uses registry ID; omitted scope aggregate, explicit scope exact; website now absolute HTTP(S) or null. |
| /api/news; /api/news/rss | Existing NewsSummary; omitted scope aggregate, explicit scope exact. RSS retained cache is not remote feed exhaustiveness. |
| /api/community-notices; /api/community-notices/{view} | Existing CommunityNoticesPage/ContentItem; omitted scope intentional aggregate, explicit scope exact; controlled views. |
| /api/originals/{id} | Existing ArticleDetail approval/status gate, plus optional exact Community; missing/unapproved/flagged/wrong-owner all NOT_FOUND with same ID-specific message. |
| /api/updates; /api/updates/{sector} | Existing UpdateItem/UpdatesPage public DTO; intentionally aggregate legacy sector feed, no supported Community query. Not repurposed into Category-scoped feed. |
| /api/home; /api/find-help | Existing homepage/discovery/directory aggregate DTOs, no supported Community query; page context not a new Community launch. |
| POST /api/decide | New DecisionSummary/PublicCitation source allowlist. Omitted scope retains legacy aggregate retrieval; explicit scope filters before prompt. Provider exceptions become generic public notes; no provider/network/model added. |
| Errors on those paths | NOT_FOUND approval parity retained. Unexpected errors return generic INTERNAL_ERROR; full details server-side. Missing query parameter remains bounded public BAD_REQUEST message. |

The 26 handler mappings (including parametric families) and concrete declared return types are in endpoints-before/after.json; http-before/after.json covers each mapping. Spring's framework /error dispatch and static assets are framework/static resources, not an extra raw catalog controller. A controller is not scope-aware merely because a client appends an unsupported query; intentional legacy aggregate contracts above are explicit.

## Findings and correction ledger

| Finding | Demonstration before correction | Bounded correction / severity |
|---|---|---|
| Direct and nested domain serialization | demonstrated-gaps.txt eight endpoint failures | New explicit public adapters; v2 public-boundary defect, fixed. Internal service types untouched. |
| Explicit scope ignored by lists/details | same log: four list + five by-ID failures | Optional exact-owner intersection; v2 isolation defect, fixed; omitted query behavior retained. |
| Explicit owner overwritten by physical office | Im12OwnershipAndErrorTest synthetic bear-de owner/Wilmington office | Explicit owner precedence in one loader; v2 J5 defect, fixed. All 229 current legacy owner results unchanged. |
| Unexpected/AI exception text leaked | same log, direct handler and throwing provider | Generic response + server-side logging, fixed. |
| AI source retrieval timestamp and optional scope bypass | additional-before.txt two failures | Public citation DTO, explicit retrieval scope only; fixed, no AI feature expansion. |
| Confidential Resource address / unsafe website | additional-before.txt third failure (first assertion stops at address); isolated after case asserts all | Public Place and contact/site records; confidential address/zip suppressed, safe URL or null. Actual HA-006 already stores no confidential address. |
| Organization website bypasses frontend guard | organization-navigation-before.txt one failure; identity case passes | Two existing public projection expressions sanitize website; provider matching/ranking unchanged. |
| Optional supplementary explicit provider IDs unchecked | additional-python-before.txt one failure | Validator checks supplied IDs in resources.communities.json; missing optional IDs remain valid. |
| 171 missing provider IDs; 28 parent display strings; legacy stored Community absent229; directory sector unknown | inventory + actual traces | Metadata stewardship: no registry/profile/source/owner/host invention. Existing normalization and offering contacts still work; no required v2 relation demonstrated unrepresentable. |
| No typed HOST or many-to-many association persistence | Actual Flyer organizer refs + models + provider-only service path | Honest partial-model/V3 gap. No live actionable Flyer, no supplied independent host identity requiring a blocked resident path; no generalized graph authorized. |

J3: Category Organizations link → exact registry slug/ID → ownership-filtered offering cards (actual traces and browser). J5: synthetic explicit owner survives office mismatch, missing provider identity remains missing, name/source/parent cannot create identity. J7: producer/classification/Community/public projection preserve canonical IDs; Events remain Flyers and source sector is separate from provider. E1/E5 cover the same identity/validation boundary. J1/J2/J4/J6 retain before/after snapshots plus regression suites.
