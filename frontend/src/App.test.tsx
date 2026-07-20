import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import type { CategorySummary, HomePayload } from './types/api';

const categories: CategorySummary[] = [
  { key: 'housing', label: 'Housing', icon: '🏠', resourceCount: 44, latestItems: [], latestPolicyUpdate: null },
];

const home: HomePayload = {
  aiConfig: { placeholder: 'Ask here', suggestedPrompts: [], chips: [] },
  updates: [],
  categories,
};

function ok(data: unknown) {
  return { json: () => Promise.resolve({ success: true, data, errorCode: null, errorMessage: null }) };
}

describe('App', () => {
  beforeEach(() => {
    // Path-aware: the Sidebar fetches /api/categories, MainContent fetches /api/home.
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url: string) =>
        Promise.resolve(url === '/api/home' ? ok(home) : ok(categories)),
      ),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('renders the layout shell (header brand and sidebar)', async () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: 'First Step' })).toBeInTheDocument();
    expect(screen.getByText('Categories')).toBeInTheDocument();
    // Sidebar's live category fetch resolves into the shell. Target the sidebar
    // checkbox specifically ("Housing" also appears in the category previews).
    expect(await screen.findByLabelText(/Housing/)).toBeInTheDocument();
  });
});
