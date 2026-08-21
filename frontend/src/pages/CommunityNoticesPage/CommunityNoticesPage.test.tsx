import { render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import CommunityNoticesPage from './CommunityNoticesPage';
import type { CommunityNoticesPage as PageData, ContentItem, NoticeView } from '../../types/api';

const item = (id: string, title: string, imageUrl: string | null = null): ContentItem => ({
  contentType: 'FLYER',
  id,
  title,
  summary: `Summary ${id}`,
  organization: 'Ministry of Caring',
  location: null,
  cost: null,
  urgency: null,
  date: '2026-09-12',
  url: null,
  imageUrl,
});

const counts = (over: Partial<Record<NoticeView, number>> = {}) => ({
  EVENTS: 0,
  MEETINGS: 0,
  ANNOUNCEMENTS: 0,
  FLYERS: 0,
  ...over,
});

function mockPage(page: PageData) {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      json: () => Promise.resolve({ success: true, data: page, errorCode: null, errorMessage: null }),
    }),
  );
}

/**
 * Renders at a REAL route rather than by passing a prop, because the route IS the
 * input under test. A prop-driven render would prove the component can display a
 * view, not that the URL selects it.
 */
function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/community-notices" element={<CommunityNoticesPage />} />
        <Route path="/community-notices/:view" element={<CommunityNoticesPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('CommunityNoticesPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('should request the view named in the URL, not the landing route', async () => {
    mockPage({ view: 'MEETINGS', counts: counts(), items: [], previews: [] });

    renderAt('/community-notices/meetings');

    await screen.findByRole('heading', { name: 'Community Notices' });
    // Typed, bookmarked or shared — the view resolves without passing through
    // the landing route first.
    expect(fetch).toHaveBeenCalledWith('/api/community-notices/meetings');
  });

  it('should mark the URL-named view as the current page', async () => {
    mockPage({ view: 'FLYERS', counts: counts({ FLYERS: 2 }), items: [], previews: [] });

    renderAt('/community-notices/flyers');

    const nav = await screen.findByRole('navigation', { name: 'Community Notices views' });
    const current = within(nav).getAllByRole('link').filter((a) => a.getAttribute('aria-current') === 'page');
    expect(current).toHaveLength(1);
    expect(current[0]).toHaveTextContent('Flyers');
  });

  it('should render all four view cards on a view route, not only on the landing route', async () => {
    // The cards are the page's navigation, not the landing page's content.
    mockPage({ view: 'EVENTS', counts: counts({ EVENTS: 3, FLYERS: 5 }), items: [], previews: [] });

    renderAt('/community-notices/events');

    const nav = await screen.findByRole('navigation', { name: 'Community Notices views' });
    expect(within(nav).getAllByRole('link')).toHaveLength(4);
  });

  it('should show counts from the active view payload so the nav never fills in late', async () => {
    mockPage({ view: 'EVENTS', counts: counts({ EVENTS: 3, FLYERS: 5 }), items: [], previews: [] });

    renderAt('/community-notices/events');

    const nav = await screen.findByRole('navigation', { name: 'Community Notices views' });
    expect(within(nav).getByRole('link', { name: /Flyers/ })).toHaveTextContent('5');
  });

  it('should render a real sample of each view on the landing route', async () => {
    // The landing route must earn its URL rather than route onward.
    mockPage({
      view: 'OVERVIEW',
      counts: counts({ EVENTS: 4, FLYERS: 1 }),
      items: [],
      previews: [
        { view: 'EVENTS', count: 4, items: [item('FL-1', 'Community health fair')] },
        { view: 'FLYERS', count: 1, items: [item('FL-1', 'Community health fair', '/img/fair.png')] },
      ],
    });

    renderAt('/community-notices');

    expect(await screen.findByRole('heading', { name: 'Events' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Flyers' })).toBeInTheDocument();
    // The true total, not the preview size — "see all (4)" has to mean something.
    expect(screen.getByRole('link', { name: /See all \(4\)/ })).toBeInTheDocument();
  });

  it('should NOT render a preview section for a view the payload left empty', async () => {
    mockPage({
      view: 'OVERVIEW',
      counts: counts({ EVENTS: 1 }),
      items: [],
      previews: [
        { view: 'EVENTS', count: 1, items: [item('FL-1', 'Community health fair')] },
        { view: 'MEETINGS', count: 0, items: [] },
      ],
    });

    renderAt('/community-notices');

    await screen.findByRole('heading', { name: 'Events' });
    // Decision 041: an empty group is never rendered.
    expect(screen.queryByRole('heading', { name: 'Meetings' })).not.toBeInTheDocument();
  });

  it('should render flyers as images because the poster IS the content', async () => {
    mockPage({
      view: 'FLYERS',
      counts: counts({ FLYERS: 1 }),
      items: [item('FL-1', 'Community health fair', '/img/fair.png')],
      previews: [],
    });

    renderAt('/community-notices/flyers');

    const image = await screen.findByRole('img', { name: 'Community health fair' });
    expect(image).toHaveAttribute('src', '/img/fair.png');
  });

  it('should still list a flyer that has no poster rather than dropping it', async () => {
    mockPage({
      view: 'FLYERS',
      counts: counts({ FLYERS: 1 }),
      items: [item('FL-1', 'Neighborhood board meeting', null)],
      previews: [],
    });

    renderAt('/community-notices/flyers');

    expect(await screen.findByText('Neighborhood board meeting')).toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('should show the view-specific empty state when a view has nothing in it', async () => {
    mockPage({ view: 'MEETINGS', counts: counts(), items: [], previews: [] });

    renderAt('/community-notices/meetings');

    expect(await screen.findByText('No meetings posted right now.')).toBeInTheDocument();
  });

  it('should surface a failed request as an alert instead of an empty view', async () => {
    // An empty view and a broken request are different facts. Rendering the
    // empty state for both would tell a resident nothing went wrong.
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('Unknown notices view: newsletters')));

    renderAt('/community-notices/newsletters');

    expect(await screen.findByRole('alert')).toHaveTextContent('Unknown notices view: newsletters');
  });
});
