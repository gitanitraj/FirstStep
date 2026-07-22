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

export interface UpdateItem {
  type: string; // "news" | "flyer"
  id: string;
  title: string;
  summary: string;
  date: string | null;
  source: string | null;
  url: string | null;
  urgency: string | null;
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

export interface HomePayload {
  aiConfig: AiConfig;
  updates: UpdateItem[];
  categories: CategorySummary[];
  organizations: OrgSummary[];
  delawareLaws: LawItem[];
  communityFlyers: FlyerCard[];
}
