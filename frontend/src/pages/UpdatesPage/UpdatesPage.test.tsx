import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import UpdatesPage from './UpdatesPage';
import type { UpdateItem, UpdatesPage as UpdatesPageData } from '../../types/api';

const item = (id: string, contentType: UpdateItem['contentType'], title: string): UpdateItem => ({
  contentType,
  id,
  title,
  summary: `Summary ${id}`,
  date: '2026-08-06',
  source: 'Delaware General Assembly',
  url: null,
  urgency: null,
  categoryTags: null,
});

function mockPage(page: UpdatesPageData) {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      json: () => Promise.resolve({ success: true, data: page, errorCode: null, errorMessage: null }),
    }),
  );
}

function renderPage(sector: 'government' | 'community') {
  return render(
    <MemoryRouter>
      <UpdatesPage sector={sector} />
    </MemoryRouter>,
  );
}

describe('UpdatesPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('should render one section per group, in payload order', async () => {
    mockPage({
      sector: 'GOVERNMENT',
      totalCount: 3,
      groups: [
        { contentType: 'NEWS', count: 1, items: [item('N1', 'NEWS', 'A policy change')] },
        { contentType: 'LAW', count: 2, items: [item('L1', 'LAW', 'A signed bill'), item('L2', 'LAW', 'Another bill')] },
      ],
    });

    renderPage('government');

    expect(await screen.findByRole('heading', { name: 'news items' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'laws' })).toBeInTheDocument();
  });

  it('should NOT render a group the payload does not contain', async () => {
    // The guarantee comes from the server, which never builds an empty group —
    // this asserts the frontend does not invent one by enumerating ContentType.
    mockPage({
      sector: 'COMMUNITY',
      totalCount: 1,
      groups: [{ contentType: 'FLYER', count: 1, items: [item('F1', 'FLYER', 'A community flyer')] }],
    });

    renderPage('community');

    expect(await screen.findByRole('heading', { name: 'flyers' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'laws' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'news items' })).not.toBeInTheDocument();
  });

  it('should title itself by sector, because the producer IS the distinction', async () => {
    mockPage({ sector: 'GOVERNMENT', totalCount: 0, groups: [] });
    const { unmount } = renderPage('government');
    expect(await screen.findByRole('heading', { name: 'Latest Updates' })).toBeInTheDocument();
    unmount();

    mockPage({ sector: 'COMMUNITY', totalCount: 0, groups: [] });
    renderPage('community');
    expect(await screen.findByRole('heading', { name: 'Community Notices' })).toBeInTheDocument();
  });

  it('should keep items in the order the server sent them', async () => {
    // Reverse-chronological WITHIN a group is the ordering that carries meaning;
    // the client must not re-sort.
    mockPage({
      sector: 'GOVERNMENT',
      totalCount: 2,
      groups: [{
        contentType: 'LAW',
        count: 2,
        items: [item('L1', 'LAW', 'Newer bill'), item('L2', 'LAW', 'Older bill')],
      }],
    });

    renderPage('government');

    const group = await screen.findByRole('region', { name: 'laws' });
    const titles = within(group).getAllByRole('listitem').map((li) => li.textContent);
    expect(titles[0]).toMatch(/Newer bill/);
    expect(titles[1]).toMatch(/Older bill/);
  });

  it('should show the sector-specific empty state when nothing is published', async () => {
    mockPage({ sector: 'COMMUNITY', totalCount: 0, groups: [] });

    renderPage('community');

    expect(await screen.findByText('No community notices right now.')).toBeInTheDocument();
  });
});
