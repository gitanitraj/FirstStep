export interface CivicContentSummary {
  id: string;
  communityId: string;
  title: string;
  summary: string;
  verified: boolean;
}

export interface SearchResult {
  type: string;
  score: number;
  content: CivicContentSummary;
}

export interface CategorySummary {
  key: string;
  label: string;
  icon: string;
  resourceCount: number;
  latestItems: SearchResult[];
  latestPolicyUpdate: CivicContentSummary | null;
}

// ===== AI guidance (POST /api/decide) — mirrors the backend ai/dto records =====

export interface ContentSource {
  id: string;
  name: string;
  type: string;
  url: string;
  retrieved: string;
}

export interface DecisionRequest {
  userQuery: string;
  urgent: boolean;
  preferredCategories: string[];
}

export interface DecisionStep {
  order: number;
  title: string;
  action: string;
  why: string;
}

export interface Citation {
  sourceType: string;
  id: string;
  label: string;
  contentSource: ContentSource | null;
}

export interface DecisionResponse {
  answerTitle: string;
  steps: DecisionStep[];
  citations: Citation[];
  notes: string;
}

// ===== Important Updates (GET /api/updates) — mirrors updates/dto/UpdateItem =====

/**
 * What a piece of CivicContent IS. Mirrors the backend enum. Presentation labels
 * are derived from this in the UI — the backend never sends a display string.
 */
export type ContentType = 'RESOURCE' | 'NEWS' | 'LAW' | 'FLYER' | 'EXPERT';

export interface UpdateItem {
  contentType: ContentType;
  id: string;
  title: string;
  summary: string;
  date: string | null;
  source: string | null;
  url: string | null;
  urgency: string | null;
  categoryTags: string[] | null;
}

/** One content type's updates within a sector page. Mirrors updates/dto/UpdateGroup. */
export interface UpdateGroup {
  contentType: ContentType;
  count: number;
  items: UpdateItem[];
}

/**
 * A whole sector's updates. Mirrors updates/dto/UpdatesPage.
 *
 * Serves BOTH destination pages — Latest Updates (`government`) and Community
 * Notices (`community`) — because the only thing separating them is who
 * published the content.
 *
 * `groups` only ever contains types that HAVE content: the backend never builds
 * an empty group, so "do not render empty groups" (Decision 045) is guaranteed by
 * the payload rather than by a guard here.
 */
export interface UpdatesPage {
  sector: 'GOVERNMENT' | 'COMMUNITY' | 'FIRST_STEP';
  totalCount: number;
  groups: UpdateGroup[];
}

// ===== Homepage aggregate (GET /api/home) — mirrors home/dto records =====

export interface AiChip {
  value: string;
  label: string;
  urgent: boolean;
}

export interface AiConfig {
  placeholder: string;
  suggestedPrompts: string[];
  chips: AiChip[];
}

export interface OrgSummary {
  name: string;
  slug: string;
  resourceCount: number;
}

export interface LawItem {
  title: string;
  url: string | null;
  date: string | null;
}

export interface FlyerCard {
  imageUrl: string;
  title: string;
  organization: string | null;
  eventDate: string | null;
}

/**
 * One entry in the homepage's Community Resources column.
 *
 * `kind` is the guardrail made visible in the type: a `category` pathway routes
 * to that category page and had its label/icon resolved from the taxonomy; a
 * `discovery` pathway is a controlled query over existing CivicContent metadata
 * and authored its own. Seniors is the only `discovery` entry today, and it is
 * deliberately NOT a category (Decision 041).
 */
export interface ResourcePathway {
  key: string;
  label: string;
  icon: string;
  kind: 'category' | 'discovery';
}

export interface HomePayload {
  aiConfig: AiConfig;
  communityResources: ResourcePathway[];
  originals: ContentItem[];
  delawareLaws: LawItem[];
  communityFlyers: FlyerCard[];
}

// ===== Category page aggregate (GET /api/category/{key}) — mirrors category/dto =====

export interface CategoryMetadata {
  key: string;
  label: string;
  icon: string;
  totalCount: number;
  countsByType: Partial<Record<ContentType, number>>;
  /** Most recent EDITORIAL date in the updates feed — never a record's load date. */
  lastUpdated: string | null;
}

export interface TopicNavigation {
  name: string;
  slug: string;
  count: number;
  countsByType: Partial<Record<ContentType, number>>;
}

export interface TopicGroup {
  label: string;
  topics: TopicNavigation[];
}

/**
 * One piece of CivicContent normalized for display as a card. Mirrors
 * shared/dto/ContentItem.
 *
 * Note there is no legacy `type` string here — this shape was defined after the
 * Decision 036 exit criterion, so `contentType` is the only discriminator.
 */
export interface ContentItem {
  contentType: ContentType;
  id: string;
  title: string;
  summary: string | null;
  organization: string | null;
  /** City only — a browse card never carries a street address. */
  location: string | null;
  cost: string | null;
  urgency: string | null;
  date: string | null;
  url: string | null;
  /** Flyers only — resolved and URL-encoded server-side. Null everywhere else. */
  imageUrl: string | null;
}

/**
 * A First Step Original article, as a reader receives it.
 *
 * **This is deliberately NOT ContentItem.** ContentItem is the card shape shared
 * by topic pages, search, notices and the homepage; adding `body` to it would put
 * a full article on every card payload in the product. Two shapes, two jobs.
 *
 * Eleven fields, and the ones ABSENT are the point: no `generatedBy`, no
 * `editorialReview`, no reviewer, flags or dispositions, no `verified`. The
 * server's record type has no component for them, so they cannot arrive here —
 * and nothing in this client may infer them.
 */
export interface ArticleDetail {
  id: string;
  title: string;
  summary: string | null;
  /** Why a resident should care. Authored for readers, so it is shown. */
  whyItMatters: string | null;
  /** The full text. Blank lines separate paragraphs. */
  body: string | null;
  /** The public credit. NOT who composed the prose — that stays server-side. */
  byline: string | null;
  /**
   * A controlled disclosure key, or null. Currently only `ai-assisted`.
   *
   * Resolved through the dictionary to standard wording, so the statement reads
   * identically across articles and exists in both languages. The client renders
   * the key it is given and infers nothing.
   */
  disclosure: string | null;
  /** Descriptive, never a gate — an approved article may carry no date. */
  publishDate: string | null;
  updatedDate: string | null;
  categoryTags: string[] | null;
  subcategory: string | null;
}

/** One door on Find Help. `destination` is null when the pathway is not built yet. */
export interface CategoryGateway {
  key: string;
  label: string;
  icon: string | null;
  destination: string | null;
  available: boolean;
}

/** One service under an organization. Contact belongs to the SERVICE, not the org. */
export interface ServiceLine {
  /** Null when the service has no name distinct from its organization. */
  title: string | null;
  category: string | null;
  city: string | null;
  phone: string | null;
}

export interface DirectoryEntry {
  name: string;
  slug: string;
  services: ServiceLine[];
}

/**
 * Find Help — where a resident works out where to begin.
 *
 * Three levels of engagement: gateways (I know what I need), openToEveryone
 * (I need a starting point), directory (I know who I'm looking for).
 */
export interface FindHelpPage {
  gateways: CategoryGateway[];
  openToEveryone: ContentItem[];
  directory: DirectoryEntry[];
}

/**
 * One organization and the First Step resources it provides.
 *
 * `id` is canonical identity, `slug` is the public URL. The client links by slug
 * and never constructs one — slugs come from the server, which is the only place
 * that knows the registry's current spelling.
 */
export interface OrganizationPage {
  id: string;
  name: string;
  slug: string;
  /**
   * The organization's own site, or null — and null is common and deliberate.
   * Only a website the source data explicitly labels "Organization website" is
   * promoted; a program page belongs to the service that runs it.
   */
  website: string | null;
  resources: ContentItem[];
}

/** The five Community Notices routes. The URL is the source of truth for which is active. */
export type NoticeView = 'OVERVIEW' | 'EVENTS' | 'MEETINGS' | 'ANNOUNCEMENTS' | 'FLYERS';

export interface NoticePreview {
  view: NoticeView;
  count: number;
  items: ContentItem[];
}

/**
 * One page in five states. Mirrors notices/dto/CommunityNoticesPage.
 *
 * `counts` is present on EVERY response because the four navigation cards render
 * on every route — a view returning only its own items would leave the nav
 * numbers to fill in after the page had already drawn.
 *
 * `items` carries the active view and is empty on OVERVIEW; `previews` is the
 * reverse.
 */
export interface CommunityNoticesPage {
  view: NoticeView;
  counts: Partial<Record<NoticeView, number>>;
  items: ContentItem[];
  previews: NoticePreview[];
}

// ===== Topic page (GET /api/category/{key}/{topic}) — mirrors category/dto =====

export interface TopicMetadata {
  categoryKey: string;
  categoryLabel: string;
  categoryIcon: string;
  name: string;
  slug: string;
  totalCount: number;
  countsByType: Partial<Record<ContentType, number>>;
}

export interface TopicPage {
  metadata: TopicMetadata;
  items: ContentItem[];
  /** Controlled Topic narrowing, with help-only counts. */
  topics?: TopicNavigation[];
}

/**
 * Which resident job a collection serves (Decision 051).
 *
 *   ACTION       help me act        — programs, services, actionable opportunities
 *   INFORMATION  help me understand — the WHAT TO KNOW collections
 *   SUPPORTING   supporting exploration inside WHAT TO KNOW
 *
 * Presentation structure, not a domain concept — and deliberately carries no
 * spatial meaning. Where a layer renders is the page's decision, not the payload's.
 */
export type CollectionLayer = 'ACTION' | 'INFORMATION' | 'SUPPORTING';

/**
 * One purposeful collection on the category destination.
 *
 * A collection is an editorial selection, NOT a projection of `contentType`:
 * Policy Info and First Rate News are both NEWS, and the two supporting
 * collections are both EXPERT. It follows that the collections do not partition
 * the category — the destination provides discovery pathways, not an exhaustive
 * accounting.
 *
 * `labelKey` is an i18n key, never a display string. All matching items are
 * carried, so "See more" expands in place rather than fetching again. An empty
 * collection is never sent.
 */
export interface CategoryCollection {
  key: string;
  labelKey: string;
  layer: CollectionLayer;
  count: number;
  items: ContentItem[];
}

/**
 * The whole category page in one response.
 *
 * The three pillars — Discover (groups / topics), Connect (organizations), Stay
 * Informed (updates) — describe what the page KNOWS. `collections` carries what a
 * resident WANTS: purposeful collections ordered by the destination composition
 * (Decision 051). The pillars keep their exact semantics; a field is retired only
 * when shown obsolete, never left as a silent duplicate.
 *
 * `groups` and `topics` are MUTUALLY EXCLUSIVE — a category grouped in
 * navigation.json returns groups and an empty topic list, one absent from it
 * returns a flat topic list and no groups.
 */
export interface CategoryPage {
  metadata: CategoryMetadata;
  updates: UpdateItem[];
  groups: TopicGroup[];
  topics: TopicNavigation[];
  organizations: OrgSummary[];
  collections: CategoryCollection[];
}
