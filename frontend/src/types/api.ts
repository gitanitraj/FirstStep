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
