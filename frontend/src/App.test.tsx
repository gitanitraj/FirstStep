import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import HomePage from './pages/HomePage';
import type { HomePayload } from './types/api';

const home: HomePayload = {
  aiConfig: { placeholder: 'Ask here', suggestedPrompts: [], chips: [] },
  updates: [],
  categories: [
    { key: 'housing', label: 'Housing', icon: '🏠', resourceCount: 44, latestItems: [], latestPolicyUpdate: null },
  ],
  organizations: [{ name: 'American Red Cross', slug: 'american-red-cross', resourceCount: 6 }],
  delawareLaws: [],
  communityFlyers: [],
};

describe('Homepage frame', () => {
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

  it('renders the civic-portal frame and the Resource Discovery data', async () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );

    // Frame (renders immediately).
    expect(screen.getByText('First Step')).toBeInTheDocument();
    expect(
      screen.getByText(/Your trusted guide to community resources, program updates and local information\./),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /First Step home/i })).toHaveAttribute('href', '/');
    for (const label of ['Housing Assistance', 'Community Info', 'Important Notices', 'Life Assistance']) {
      expect(screen.getByRole('link', { name: new RegExp(label) })).toBeInTheDocument();
    }
    expect(screen.getByPlaceholderText(/Tell me what you need today/)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'New Delaware Laws' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Community Information' })).toBeInTheDocument();

    // Resource Discovery data arrives from the /api/home fetch (category href
    // detail is covered in ResourceDiscovery.test).
    expect(await screen.findByRole('link', { name: /American Red Cross/ })).toBeInTheDocument();
  });
});

describe('App routing', () => {
  it('routes a primary-nav destination to its stub page (under the /app-next basename)', () => {
    window.history.pushState({}, '', '/app-next/important-notices');
    render(<App />);

    expect(screen.getByRole('heading', { name: 'Important Notices' })).toBeInTheDocument();
    expect(screen.getByText('Coming soon.')).toBeInTheDocument();
  });
});
