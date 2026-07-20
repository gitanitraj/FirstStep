import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import MainContent from './MainContent';
import type { HomePayload } from '../types/api';

const home: HomePayload = {
  aiConfig: {
    placeholder: 'Ask here',
    suggestedPrompts: ['I need help paying rent'],
    chips: [
      { value: 'urgent', label: '🚨 Urgent', urgent: true },
      { value: 'housing', label: '🏠 Housing', urgent: false },
    ],
  },
  updates: [
    { type: 'flyer', id: 'FL-007', title: 'Furniture Giveaway', summary: 'Free furniture', date: '2026-08-15', source: 'WHA', url: null, urgency: null },
  ],
  categories: [
    { key: 'housing', label: 'Housing', icon: '🏠', resourceCount: 44, latestItems: [], latestPolicyUpdate: null },
  ],
};

describe('MainContent', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        json: () => Promise.resolve({ success: true, data: home, errorCode: null, errorMessage: null }),
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('fetches /api/home once and distributes it to the sections', async () => {
    render(<MainContent />);

    // Hero renders immediately.
    expect(screen.getByRole('heading', { name: /what do you need help with/i })).toBeInTheDocument();

    // Important Updates seeded from /api/home.updates.
    expect(await screen.findByText('Furniture Giveaway')).toBeInTheDocument();
    // Category previews from /api/home.categories.
    expect(screen.getByText('44 resources')).toBeInTheDocument();
    // Backend-driven suggested prompt from aiConfig.
    expect(screen.getByRole('button', { name: 'I need help paying rent' })).toBeInTheDocument();

    // Single request on mount — one /api/home call (Important Updates was seeded,
    // so it did not also fetch /api/updates on mount).
    const calls = (globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls;
    expect(calls).toHaveLength(1);
    expect(calls[0][0]).toBe('/api/home');
  });
});
