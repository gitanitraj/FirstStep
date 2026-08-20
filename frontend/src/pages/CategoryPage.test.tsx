import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import CategoryPage from './CategoryPage';
import type { CategoryPage as CategoryPagePayload } from '../types/api';

const housing: CategoryPagePayload = {
  metadata: {
    key: 'housing',
    label: 'Housing',
    icon: '🏠',
    totalCount: 73,
    countsByType: { RESOURCE: 44, LAW: 20, NEWS: 5, EXPERT: 3, FLYER: 1 },
    lastUpdated: '2026-07-25',
  },
  updates: [
    {
      // `type` and `contentType` deliberately DISAGREE: `type` says "news" for a
      // signed bill. The page must badge from contentType, so that when Slice H
      // deletes `type` nothing here needs to change.
      contentType: 'LAW',
      id: 'L1',
      title: 'Relating to Rent Increases.',
      summary: 'A bill about rent.',
      date: '2026-07-23',
      source: 'Delaware Legislature',
      url: 'https://example.gov/L1',
      urgency: null,
      categoryTags: ['Housing'],
    },
  ],
  groups: [
    {
      label: 'Need Help Right Away',
      topics: [{ name: 'Emergency Shelter', slug: 'emergency-shelter', count: 10, countsByType: {} }],
    },
  ],
  topics: [],
  organizations: [{ name: 'Housing Alliance', slug: 'housing-alliance', resourceCount: 12 }],
};

function stubFetch(body: unknown) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ json: () => Promise.resolve(body) }));
}

function renderAt(key: string) {
  return render(
    <MemoryRouter initialEntries={[`/category/${key}`]}>
      <Routes>
        <Route path="/category/:key" element={<CategoryPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('CategoryPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe('with a loaded category', () => {
    beforeEach(() => {
      stubFetch({ success: true, data: housing, errorCode: null, errorMessage: null });
    });

    it('renders all three pillars from one request', async () => {
      renderAt('housing');

      // Header
      expect(await screen.findByRole('heading', { name: /Housing/, level: 1 })).toBeInTheDocument();
      expect(screen.getByText(/44 resources/)).toBeInTheDocument();
      expect(screen.getByText(/20 laws/)).toBeInTheDocument();
      // Stay Informed
      expect(screen.getByRole('heading', { name: 'Current Updates' })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'Relating to Rent Increases.' })).toBeInTheDocument();
      // Discover
      expect(screen.getByRole('heading', { name: 'Browse Resources' })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Emergency Shelter/ })).toHaveAttribute(
        'href',
        '/category/housing/emergency-shelter',
      );
      // Connect
      expect(screen.getByRole('heading', { name: 'Organizations' })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Housing Alliance/ })).toHaveAttribute(
        'href',
        '/organization/housing-alliance',
      );

      expect(fetch).toHaveBeenCalledWith('/api/category/housing');
    });

    it('counts in the singular when there is exactly one of a type', async () => {
      // Caught in live verification: the header read "1 flyers".
      renderAt('housing');

      expect(await screen.findByText(/1 flyer\b/)).toBeInTheDocument();
      expect(screen.getByText(/3 expert answers/)).toBeInTheDocument();
      expect(screen.queryByText(/1 flyers/)).not.toBeInTheDocument();
    });

    it('badges from contentType even when type disagrees', async () => {
      renderAt('housing');

      expect(await screen.findByText('Law')).toBeInTheDocument();
      expect(screen.queryByText('News')).not.toBeInTheDocument();
    });
  });

  it('shows a way out when the category key is unknown', async () => {
    stubFetch({
      success: false,
      data: null,
      errorCode: 'NOT_FOUND',
      errorMessage: 'Category not found: nope',
    });

    renderAt('nope');

    expect(await screen.findByRole('alert')).toHaveTextContent("We couldn't find that category.");
    expect(screen.getByRole('link', { name: /Back to home/ })).toBeInTheDocument();
    // The raw backend message is not shown to residents.
    expect(screen.queryByText(/Category not found: nope/)).not.toBeInTheDocument();
  });
});
