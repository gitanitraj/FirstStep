import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import HomePage from './pages/HomePage';
import type { HomePayload } from './types/api';

const home: HomePayload = {
  aiConfig: { placeholder: 'Ask here', suggestedPrompts: [], chips: [] },
  communityResources: [
    { key: 'housing', label: 'Housing', icon: '🏠', kind: 'category' },
    { key: 'seniors', label: 'Seniors', icon: '🧓', kind: 'discovery' },
  ],
  originals: [
    {
      contentType: 'EXPERT',
      id: 'FAQ-001',
      title: 'How do I apply for SNAP benefits?',
      summary: 'Apply online through Delaware ASSIST.',
      organization: 'First Step',
      location: null,
      cost: null,
      urgency: null,
      date: '2026-07-11',
      url: null,
    },
  ],
  delawareLaws: [],
  communityFlyers: [],
};

describe('The front door', () => {
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

  it('renders the header, the AI question and every section in order', async () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );

    // The three header zones: brand, AI banner, accessibility controls.
    expect(screen.getByRole('link', { name: /First Step home/i })).toHaveAttribute('href', '/');
    expect(screen.getByRole('button', { name: 'Toggle high contrast' })).toBeInTheDocument();

    // The nav row — four items, and NO category competing with the pathways
    // other than Housing, which is deliberately privileged.
    const nav = screen.getByRole('navigation', { name: 'Primary' });
    for (const label of ['About', 'Housing', 'Community', 'Updates']) {
      expect(within(nav).getByRole('link', { name: label })).toBeInTheDocument();
    }

    // The three pathways, equal in weight.
    for (const title of ['Discover', 'Connect', 'Stay Informed']) {
      expect(screen.getByRole('heading', { name: title })).toBeInTheDocument();
    }

    // Passive discovery: the renamed RSS scroll and its route to Updates.
    expect(
      screen.getByRole('heading', { name: 'New Laws in Delaware' }),
    ).toBeInTheDocument();

    expect(screen.getByRole('heading', { name: 'Community Information' })).toBeInTheDocument();
    expect(screen.getByRole('contentinfo')).toBeInTheDocument();
  });

  it('routes a category pathway and a discovery pathway differently', async () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );

    // The distinction that must survive: Seniors is NOT a category, so it must
    // not route to /category/seniors. If someone "simplifies" the href logic,
    // this is what fails.
    expect(await screen.findByRole('link', { name: /Housing/ })).toHaveAttribute(
      'href',
      '/category/housing',
    );
    expect(screen.getByRole('link', { name: /Seniors/ })).toHaveAttribute(
      'href',
      '/discover/seniors',
    );
  });

  it('shows First Step Originals from the payload', async () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );

    expect(await screen.findByText('How do I apply for SNAP benefits?')).toBeInTheDocument();
  });
});

describe('App routing', () => {
  it('routes a global-nav destination to its stub page (under the /app-next basename)', () => {
    // /updates and /community-notices are REAL pages now (Slice I), so this
    // asserts the stub mechanism on a destination that is still one.
    window.history.pushState({}, '', '/app-next/find-help');
    render(<App />);

    expect(screen.getByRole('heading', { name: 'Find Help' })).toBeInTheDocument();
    expect(screen.getByText('Coming soon.')).toBeInTheDocument();
  });
});
