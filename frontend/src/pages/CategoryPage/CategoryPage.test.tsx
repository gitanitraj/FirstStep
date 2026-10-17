import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import CategoryPage from './CategoryPage';
import type { CategoryPage as CategoryPagePayload, ContentItem } from '../../types/api';

function item(id: string, title: string): ContentItem {
  return {
    contentType: 'RESOURCE',
    id,
    title,
    summary: `${title} summary`,
    organization: 'Some Org',
    location: 'Wilmington',
    cost: null,
    urgency: null,
    date: null,
    url: null,
    imageUrl: null,
  };
}

const housing: CategoryPagePayload = {
  metadata: {
    key: 'housing',
    label: 'Housing',
    icon: '🏠',
    totalCount: 74,
    countsByType: { RESOURCE: 44, LAW: 20, NEWS: 6, EXPERT: 3, FLYER: 1 },
    lastUpdated: '2026-07-25',
  },
  // Preserved in the payload, deliberately NOT rendered — Policy Info and Law
  // present this content as purposeful collections instead (Decision 051).
  updates: [
    {
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
  collections: [
    {
      key: 'get-help',
      labelKey: 'collection.getHelp',
      layer: 'ACTION',
      count: 2,
      items: [item('R1', 'Emergency Rental Assistance'), item('R2', 'Housing Choice Voucher')],
    },
    {
      key: 'law',
      labelKey: 'collection.law',
      layer: 'INFORMATION',
      count: 1,
      items: [{ ...item('L1', 'Relating to Rent Increases.'), contentType: 'LAW' }],
    },
    {
      key: 'expert-qa',
      labelKey: 'collection.expertQa',
      layer: 'SUPPORTING',
      count: 1,
      items: [{ ...item('EA1', 'How much notice must a landlord give?'), contentType: 'EXPERT' }],
    },
  ],
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

    it('orients the resident before offering anything to explore', async () => {
      renderAt('housing');

      expect(await screen.findByRole('heading', { name: /Housing/, level: 1 })).toBeInTheDocument();
      expect(screen.getByText(/44 resources/)).toBeInTheDocument();
      expect(screen.getByText(/20 laws/)).toBeInTheDocument();
      expect(fetch).toHaveBeenCalledWith('/api/category/housing');
    });

    it('offers topic discovery that navigates', async () => {
      renderAt('housing');

      expect(
        await screen.findByRole('heading', { name: 'Where do you need help?' }),
      ).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Emergency Shelter/ })).toHaveAttribute(
        'href',
        '/category/housing/emergency-shelter',
      );
    });

    it('renders every collection the payload carries', async () => {
      renderAt('housing');

      expect(await screen.findByRole('heading', { name: 'Get help' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Law' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Expert Q&A' })).toBeInTheDocument();
      expect(screen.getByText('Emergency Rental Assistance')).toBeInTheDocument();
    });

    it('groups the information collections under one heading', async () => {
      renderAt('housing');

      expect(await screen.findByRole('heading', { name: 'What to know' })).toBeInTheDocument();
    });

    it('does not render the updates pillar, whose content the collections now carry', async () => {
      renderAt('housing');
      await screen.findByRole('heading', { name: /Housing/, level: 1 });

      // The field is still in the payload for other consumers; this page must not
      // show the same content twice under two different framings.
      expect(screen.queryByRole('heading', { name: 'Current Updates' })).not.toBeInTheDocument();
    });

    it('renders the authorized secondary Organizations preview', async () => {
      renderAt('housing');
      await screen.findByRole('heading', { name: /Housing/, level: 1 });
      expect(screen.getByRole('heading', { name: 'Organizations' })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Housing Alliance/ })).toHaveAttribute('href', '/organization/housing-alliance');
    });

    it('places Get Help as a peer of What to Know, not a child of topic discovery', async () => {
      renderAt('housing');

      expect(await screen.findByRole('heading', { name: 'Get help', level: 2 })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'What to know', level: 2 })).toBeInTheDocument();
    });

    it('subordinates supporting collections beneath the primary ones', async () => {
      renderAt('housing');
      await screen.findByRole('heading', { name: 'What to know', level: 2 });

      // Primary collections are h3; supporting content sits a level deeper so it
      // never reads as a peer of Policy Info and Law.
      expect(screen.getByRole('heading', { name: 'Law', level: 3 })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Expert Q&A', level: 4 })).toBeInTheDocument();
    });

    it('names the supporting group for assistive tech even while it is visually hidden', async () => {
      // The RELATIONSHIP is architecture and must be expressed now; the visible
      // label is a Pass 2 decision, so the heading exists but is hidden.
      renderAt('housing');

      expect(
        await screen.findByRole('heading', { name: 'More background', level: 3 }),
      ).toBeInTheDocument();
    });

    it('counts in the singular when there is exactly one of a type', async () => {
      // Caught in live verification: the header read "1 flyers".
      renderAt('housing');

      expect(await screen.findByText(/1 flyer\b/)).toBeInTheDocument();
      expect(screen.getByText(/3 expert answers/)).toBeInTheDocument();
      expect(screen.queryByText(/1 flyers/)).not.toBeInTheDocument();
    });
  });

  it('shows truthful empty help while preserving available information', async () => {
    // Utilities is the real sparse case: no resources, no topics, laws only.
    stubFetch({
      success: true,
      data: {
        ...housing,
        collections: housing.collections.filter((c) => c.layer === 'INFORMATION'),
      },
      errorCode: null,
      errorMessage: null,
    });

    renderAt('housing');

    expect(await screen.findByRole('heading', { name: 'Law' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Get help' })).toBeInTheDocument();
    expect(screen.getByText('No current help is listed here yet. Try all help or return to the category.')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Expert Q&A' })).not.toBeInTheDocument();
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
