import { render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import TopicPage from './TopicPage';
import type { TopicPage as TopicPagePayload } from '../../types/api';

const shelter: TopicPagePayload = {
  metadata: {
    categoryKey: 'housing',
    categoryLabel: 'Housing',
    categoryIcon: '🏠',
    name: 'Emergency Shelter',
    slug: 'emergency-shelter',
    totalCount: 2,
    countsByType: { RESOURCE: 1, FLYER: 1 },
  },
  items: [
    {
      contentType: 'RESOURCE', id: 'R1', title: 'Ministry of Caring Shelter',
      summary: 'Overnight beds.', organization: 'Ministry of Caring', location: 'Wilmington',
      cost: 'free', urgency: 'emergency', date: null, url: 'https://example.org',
    },
    {
      contentType: 'FLYER', id: 'F1', title: 'Winter shelter drive',
      summary: null, organization: 'Community Center', location: null,
      cost: null, urgency: null, date: '2026-08-01', url: null,
    },
  ],
};

function stubFetch(body: unknown) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ json: () => Promise.resolve(body) }));
}

function renderAt(key: string, topic: string) {
  return render(
    <MemoryRouter initialEntries={[`/category/${key}/${topic}`]}>
      <Routes>
        <Route path="/category/:key/:topic" element={<TopicPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('TopicPage', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('lists the topic content from one request', async () => {
    stubFetch({ success: true, data: shelter, errorCode: null, errorMessage: null });
    renderAt('housing', 'emergency-shelter');

    expect(await screen.findByRole('heading', { name: 'Emergency Shelter', level: 1 })).toBeInTheDocument();
    expect(screen.getByText('Ministry of Caring Shelter')).toBeInTheDocument();
    expect(screen.getByText('Winter shelter drive')).toBeInTheDocument();
    // Both content types render through the same card.
    expect(screen.getByText('Resource')).toBeInTheDocument();
    expect(screen.getByText('Flyer')).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledWith('/api/category/housing/emergency-shelter');
  });

  it('shows a breadcrumb using the category LABEL, not the url key', async () => {
    stubFetch({ success: true, data: shelter, errorCode: null, errorMessage: null });
    renderAt('housing', 'emergency-shelter');

    // Scoped to the breadcrumb landmark: the page frame also renders a
    // "Housing Assistance" primary-nav link, and an unscoped /Housing/ query
    // matches that one first.
    const crumbs = await screen.findByRole('navigation', { name: 'Breadcrumb' });
    const crumb = within(crumbs).getByRole('link', { name: /Housing/ });
    expect(crumb).toHaveAttribute('href', '/category/housing');
  });

  it('counts listings in the singular when there is one', async () => {
    stubFetch({
      success: true,
      data: { ...shelter, metadata: { ...shelter.metadata, totalCount: 1 }, items: [shelter.items[0]] },
      errorCode: null, errorMessage: null,
    });
    renderAt('housing', 'emergency-shelter');

    expect(await screen.findByText(/1 listing\b/)).toBeInTheDocument();
    expect(screen.queryByText(/1 listings/)).not.toBeInTheDocument();
  });

  it('says a declared topic is empty rather than looking broken', async () => {
    stubFetch({
      success: true,
      data: { ...shelter, metadata: { ...shelter.metadata, totalCount: 0 }, items: [] },
      errorCode: null, errorMessage: null,
    });
    renderAt('housing', 'emergency-shelter');

    expect(await screen.findByText(/Nothing is listed under this topic yet/)).toBeInTheDocument();
  });

  it('offers a way back to the category when the topic is unknown', async () => {
    stubFetch({
      success: false, data: null, errorCode: 'NOT_FOUND',
      errorMessage: 'Topic not found: housing/nope',
    });
    renderAt('housing', 'nope');

    expect(await screen.findByRole('alert')).toHaveTextContent("We couldn't find that topic.");
    expect(screen.getByRole('link', { name: /Back to the category/ })).toHaveAttribute(
      'href', '/category/housing',
    );
    // The raw backend message is not shown to residents.
    expect(screen.queryByText(/Topic not found/)).not.toBeInTheDocument();
  });
});
