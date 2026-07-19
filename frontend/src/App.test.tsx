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

  it('renders the layout shell (header brand and sidebar)', async () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: 'First Step' })).toBeInTheDocument();
    expect(screen.getByText('Categories')).toBeInTheDocument();
    // Sidebar's live category fetch resolves into the shell.
    expect(await screen.findByText('Housing')).toBeInTheDocument();
  });
});
