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
}

/**
 * The whole category page in one response. Three pillars: Discover (groups /
 * topics), Connect (organizations), Stay Informed (updates).
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
}
