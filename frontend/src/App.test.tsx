import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import type { CategorySummary } from './types/api';

describe('App', () => {
  beforeEach(() => {
    const categories: CategorySummary[] = [
      { key: 'housing', label: 'Housing', icon: '🏠', resourceCount: 44, latestItems: [], latestPolicyUpdate: null },
    ];
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        json: () => Promise.resolve({ success: true, data: categories, errorCode: null, errorMessage: null }),
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('fetches and renders category data from the API', async () => {
    render(<App />);

    expect(await screen.findByText(/Housing — 44 resources/)).toBeInTheDocument();
  });
});
